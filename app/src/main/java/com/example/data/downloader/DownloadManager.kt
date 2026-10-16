package com.example.data.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DownloadTaskEntity
import com.example.data.model.BooruPost
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val downloadDao = db.downloadDao()
    private val postDao = db.postDao()
    private val tagDao = db.tagDao()
    private val okHttpClient = NetworkClient.okHttpClient

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    val allTasks: Flow<List<DownloadTaskEntity>> = downloadDao.getAllDownloadTasks()

    fun enqueueDownload(
        post: BooruPost,
        preferOriginal: Boolean = true
    ) {
        val downloadUrl = if (preferOriginal) {
            post.fileUrl.ifBlank { post.sampleUrl ?: post.previewUrl }
        } else {
            post.sampleUrl ?: post.fileUrl
        }

        val ext = post.fileExtension.ifBlank {
            downloadUrl.substringAfterLast('.', "jpg").substringBefore('?')
        }
        val fileName = "BooruVault_${post.source.name}_${post.id}.$ext"
        val downloadId = "${post.source.name}_${post.id}"

        val task = DownloadTaskEntity(
            downloadId = downloadId,
            postKey = post.uniqueKey,
            postId = post.id,
            source = post.source.name,
            previewUrl = post.previewUrl,
            downloadUrl = downloadUrl,
            fileName = fileName,
            status = "WAITING"
        )

        scope.launch {
            downloadDao.insertTask(task)
            // Automatically import metadata to local database so it's queryable offline
            savePostAndTags(post)
            startDownloadJob(task, post)
        }
    }

    private fun startDownloadJob(task: DownloadTaskEntity, post: BooruPost) {
        val job = scope.launch {
            try {
                downloadDao.updateProgress(task.downloadId, "DOWNLOADING", 0, 0, 0)

                val request = Request.Builder()
                    .url(task.downloadUrl)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorMsg = "HTTP error: ${response.code}"
                    downloadDao.setFailed(task.downloadId, "FAILED", errorMsg)
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    downloadDao.setFailed(task.downloadId, "FAILED", "Empty response body")
                    return@launch
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()

                val savedUri = saveToStorage(inputStream, task.fileName, totalBytes) { bytesDownloaded, progress ->
                    // Throttled progress update
                    if (progress % 5 == 0 || bytesDownloaded == totalBytes) {
                        scope.launch {
                            downloadDao.updateProgress(task.downloadId, "DOWNLOADING", progress, bytesDownloaded, totalBytes)
                        }
                    }
                }

                if (savedUri != null) {
                    downloadDao.setCompleted(task.downloadId, "COMPLETED", savedUri.toString())
                    postDao.setDownloaded(post.uniqueKey, true, savedUri.toString())
                } else {
                    downloadDao.setFailed(task.downloadId, "FAILED", "Failed to write file to storage")
                }
            } catch (e: CancellationException) {
                downloadDao.updateProgress(task.downloadId, "CANCELLED", 0, 0, 0)
            } catch (e: Exception) {
                downloadDao.setFailed(task.downloadId, "FAILED", e.localizedMessage ?: "Unknown error")
            } finally {
                activeJobs.remove(task.downloadId)
            }
        }
        activeJobs[task.downloadId] = job
    }

    fun cancelDownload(downloadId: String) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        scope.launch {
            downloadDao.updateProgress(downloadId, "CANCELLED", 0, 0, 0)
        }
    }

    fun retryDownload(task: DownloadTaskEntity) {
        cancelDownload(task.downloadId)
        scope.launch {
            val post = postDao.getPostByKey(task.postKey)
            if (post != null) {
                downloadDao.updateProgress(task.downloadId, "WAITING", 0, 0, 0)
                startDownloadJob(task, BooruPost(
                    id = post.postId,
                    source = runCatching { com.example.data.model.BooruSource.valueOf(post.source) }.getOrDefault(com.example.data.model.BooruSource.DANBOORU),
                    postUrl = post.postUrl,
                    previewUrl = post.previewUrl,
                    sampleUrl = post.sampleUrl,
                    fileUrl = post.fileUrl,
                    fileExtension = post.fileExtension,
                    width = post.width,
                    height = post.height,
                    fileSize = post.fileSize,
                    rating = com.example.data.model.BooruRating.fromString(post.rating),
                    score = post.score,
                    tags = emptyList(),
                    artist = post.artist,
                    sourceUrl = post.sourceUrl,
                    createdAt = post.createdAt
                ))
            }
        }
    }

    fun deleteTask(downloadId: String) {
        cancelDownload(downloadId)
        scope.launch {
            downloadDao.deleteTask(downloadId)
        }
    }

    fun clearCompleted() {
        scope.launch {
            downloadDao.clearCompletedTasks()
        }
    }

    private suspend fun savePostAndTags(post: BooruPost) {
        postDao.insertPost(
            com.example.data.local.entity.PostEntity(
                key = post.uniqueKey,
                postId = post.id,
                source = post.source.name,
                postUrl = post.postUrl,
                previewUrl = post.previewUrl,
                sampleUrl = post.sampleUrl,
                fileUrl = post.fileUrl,
                fileExtension = post.fileExtension,
                width = post.width,
                height = post.height,
                fileSize = post.fileSize,
                rating = post.rating.name,
                score = post.score,
                artist = post.artist,
                sourceUrl = post.sourceUrl,
                createdAt = post.createdAt,
                isDownloaded = false,
                isFavorite = post.isFavorite
            )
        )
        if (post.tags.isNotEmpty()) {
            val tags = post.tags.map {
                com.example.data.local.entity.TagEntity(
                    tagKey = "${it.source.name}_${it.name}",
                    name = it.name,
                    category = it.category.name,
                    source = it.source.name,
                    count = it.count,
                    isCustom = it.isCustom
                )
            }
            val crossRefs = post.tags.map {
                com.example.data.local.entity.PostTagCrossRef(
                    postKey = post.uniqueKey,
                    tagKey = "${it.source.name}_${it.name}"
                )
            }
            tagDao.insertTags(tags)
            tagDao.insertPostTagCrossRefs(crossRefs)
        }
    }

    private fun saveToStorage(
        inputStream: InputStream,
        fileName: String,
        totalBytes: Long,
        onProgress: (bytesDownloaded: Long, progressPercent: Int) -> Unit
    ): Uri? {
        val mimeType = when {
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".gif", true) -> "image/gif"
            fileName.endsWith(".webp", true) -> "image/webp"
            else -> "image/jpeg"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/BooruVault")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
                ?: return null

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    writeStreamWithProgress(inputStream, outputStream, totalBytes, onProgress)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val vaultDir = File(picturesDir, "BooruVault")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val file = File(vaultDir, fileName)
            FileOutputStream(file).use { outputStream ->
                writeStreamWithProgress(inputStream, outputStream, totalBytes, onProgress)
            }
            Uri.fromFile(file)
        }
    }

    private fun writeStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (bytesDownloaded: Long, progressPercent: Int) -> Unit
    ) {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var totalRead: Long = 0

        input.use { inStream ->
            output.use { outStream ->
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
                    outStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                    onProgress(totalRead, percent)
                }
                outStream.flush()
            }
        }
    }
}
