package com.example.data.repository

import com.example.data.model.*
import com.example.data.remote.NetworkClient
import com.example.data.remote.dto.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*

class BooruRepository {
    private val danbooruApi = NetworkClient.danbooruApi
    private val gelbooruApi = NetworkClient.gelbooruApi
    private val yandereApi = NetworkClient.yandereApi

    suspend fun searchPosts(
        query: String,
        source: BooruSource?,
        page: Int,
        limit: Int = 20,
        allowedRatings: Set<BooruRating> = setOf(BooruRating.SAFE, BooruRating.QUESTIONABLE, BooruRating.EXPLICIT, BooruRating.UNKNOWN)
    ): Result<List<BooruPost>> = runCatching {
        val trimmedQuery = query.trim()
        val posts = when (source) {
            BooruSource.DANBOORU -> fetchDanbooru(trimmedQuery, page, limit)
            BooruSource.GELBOORU -> fetchGelbooru(trimmedQuery, page, limit)
            BooruSource.YANDERE -> fetchYandere(trimmedQuery, page, limit)
            null -> fetchAllSources(trimmedQuery, page, limit)
        }
        posts.filter { post ->
            allowedRatings.contains(post.rating)
        }
    }

    private suspend fun fetchAllSources(query: String, page: Int, limit: Int): List<BooruPost> = coroutineScope {
        val perSourceLimit = (limit / 3).coerceAtLeast(10)
        val danbooruDeferred = async { runCatching { fetchDanbooru(query, page, perSourceLimit) }.getOrDefault(emptyList()) }
        val gelbooruDeferred = async { runCatching { fetchGelbooru(query, page, perSourceLimit) }.getOrDefault(emptyList()) }
        val yandereDeferred = async { runCatching { fetchYandere(query, page, perSourceLimit) }.getOrDefault(emptyList()) }

        val combined = danbooruDeferred.await() + gelbooruDeferred.await() + yandereDeferred.await()
        combined.distinctBy { it.uniqueKey }
    }

    private suspend fun fetchDanbooru(query: String, page: Int, limit: Int): List<BooruPost> {
        val dtos = danbooruApi.getPosts(if (query.isEmpty()) null else query, page, limit)
        return dtos.mapNotNull { it.toDomain() }
    }

    private suspend fun fetchGelbooru(query: String, page: Int, limit: Int): List<BooruPost> {
        // Gelbooru uses pid (0-indexed page number)
        val pid = (page - 1).coerceAtLeast(0)
        val tagsParam = if (query.isBlank()) "sort:updated" else query
        val resp = gelbooruApi.getPosts(tagsParam, pid, limit)
        val list = resp.post ?: emptyList()
        return list.mapNotNull { it.toDomain() }
    }

    private suspend fun fetchYandere(query: String, page: Int, limit: Int): List<BooruPost> {
        val dtos = yandereApi.getPosts(if (query.isEmpty()) null else query, page, limit)
        return dtos.mapNotNull { it.toDomain() }
    }

    suspend fun getAutocompleteTags(query: String, source: BooruSource?): List<BooruTag> {
        if (query.isBlank()) return emptyList()
        val cleanQuery = query.trim().lowercase()
        return runCatching {
            when (source) {
                BooruSource.DANBOORU, null -> {
                    danbooruApi.getTags(cleanQuery).map {
                        BooruTag(
                            name = it.name,
                            category = TagCategory.fromInt(it.category),
                            source = BooruSource.DANBOORU,
                            count = it.postCount
                        )
                    }
                }
                BooruSource.GELBOORU -> {
                    val resp = gelbooruApi.getTags(cleanQuery)
                    (resp.tag ?: emptyList()).map {
                        BooruTag(
                            name = it.tag ?: cleanQuery,
                            category = TagCategory.GENERAL,
                            source = BooruSource.GELBOORU,
                            count = it.count
                        )
                    }
                }
                BooruSource.YANDERE -> {
                    yandereApi.getTags(cleanQuery).map {
                        BooruTag(
                            name = it.name,
                            category = TagCategory.fromInt(it.type),
                            source = BooruSource.YANDERE,
                            count = it.count
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun DanbooruPostDto.toDomain(): BooruPost? {
        val preview = previewFileUrl ?: largeFileUrl ?: fileUrl
        val file = fileUrl ?: largeFileUrl ?: preview ?: return null
        val postUrl = "https://danbooru.donmai.us/posts/$id"

        val tagsList = mutableListOf<BooruTag>()
        fun addTags(str: String?, category: TagCategory) {
            str?.split(" ")?.filter { it.isNotBlank() }?.forEach { name ->
                tagsList.add(BooruTag(name = name, category = category, source = BooruSource.DANBOORU))
            }
        }
        addTags(tagStringArtist, TagCategory.ARTIST)
        addTags(tagStringCharacter, TagCategory.CHARACTER)
        addTags(tagStringCopyright, TagCategory.COPYRIGHT)
        addTags(tagStringMeta, TagCategory.META)
        addTags(tagStringGeneral, TagCategory.GENERAL)

        if (tagsList.isEmpty() && !tagString.isNullOrBlank()) {
            addTags(tagString, TagCategory.GENERAL)
        }

        return BooruPost(
            id = id,
            source = BooruSource.DANBOORU,
            postUrl = postUrl,
            previewUrl = preview ?: file,
            sampleUrl = largeFileUrl ?: file,
            fileUrl = file,
            fileExtension = fileExt ?: "jpg",
            width = imageWidth,
            height = imageHeight,
            fileSize = fileSize,
            rating = BooruRating.fromString(rating),
            score = score,
            tags = tagsList,
            artist = tagStringArtist?.split(" ")?.firstOrNull { it.isNotBlank() },
            sourceUrl = source,
            createdAt = createdAt
        )
    }

    private fun GelbooruPostDto.toDomain(): BooruPost? {
        val preview = previewUrl ?: sampleUrl ?: fileUrl
        val file = fileUrl ?: sampleUrl ?: preview ?: return null
        val postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=$id"

        val tagsList = tags?.split(" ")?.filter { it.isNotBlank() }?.map {
            BooruTag(name = it, category = TagCategory.GENERAL, source = BooruSource.GELBOORU)
        } ?: emptyList()

        val ext = file.substringAfterLast('.', "jpg").substringBefore('?')

        return BooruPost(
            id = id,
            source = BooruSource.GELBOORU,
            postUrl = postUrl,
            previewUrl = preview ?: file,
            sampleUrl = sampleUrl ?: file,
            fileUrl = file,
            fileExtension = ext,
            width = width,
            height = height,
            fileSize = null,
            rating = BooruRating.fromString(rating),
            score = score,
            tags = tagsList,
            artist = null,
            sourceUrl = source,
            createdAt = createdAt
        )
    }

    private fun YanderePostDto.toDomain(): BooruPost? {
        val preview = previewUrl ?: sampleUrl ?: jpegUrl ?: fileUrl
        val file = fileUrl ?: jpegUrl ?: sampleUrl ?: preview ?: return null
        val postUrl = "https://yande.re/post/show/$id"

        val tagsList = tags?.split(" ")?.filter { it.isNotBlank() }?.map {
            BooruTag(name = it, category = TagCategory.GENERAL, source = BooruSource.YANDERE)
        } ?: emptyList()

        val createdStr = createdAt?.let {
            val date = Date(it * 1000)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        }

        return BooruPost(
            id = id,
            source = BooruSource.YANDERE,
            postUrl = postUrl,
            previewUrl = preview ?: file,
            sampleUrl = sampleUrl ?: jpegUrl ?: file,
            fileUrl = file,
            fileExtension = fileExt ?: "jpg",
            width = width,
            height = height,
            fileSize = fileSize,
            rating = BooruRating.fromString(rating),
            score = score,
            tags = tagsList,
            artist = author,
            sourceUrl = source,
            createdAt = createdStr
        )
    }
}
