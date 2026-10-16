package com.example.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.downloader.DownloadManager
import com.example.data.model.BooruPost
import com.example.data.model.BooruTag
import com.example.data.model.TagCategory
import com.example.data.repository.LocalGalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class PostDetailUiState(
    val post: BooruPost? = null,
    val isImported: Boolean = false,
    val isFavorite: Boolean = false,
    val customTagInput: String = "",
    val feedbackMessage: String? = null
)

class PostDetailViewModel(
    private val localGalleryRepository: LocalGalleryRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    fun setPost(post: BooruPost) {
        _uiState.update { it.copy(post = post, isFavorite = post.isFavorite) }
        checkIfImported(post.uniqueKey)
    }

    private fun checkIfImported(key: String) {
        viewModelScope.launch {
            val localPost = localGalleryRepository.getPostByKey(key)
            if (localPost != null) {
                _uiState.update {
                    it.copy(
                        isImported = true,
                        isFavorite = localPost.isFavorite,
                        post = it.post?.copy(
                            isDownloaded = localPost.isDownloaded,
                            localUri = localPost.localUri,
                            tags = if (localPost.tags.isNotEmpty()) localPost.tags else it.post.tags
                        )
                    )
                }
            }
        }
    }

    fun importTags() {
        val post = _uiState.value.post ?: return
        viewModelScope.launch {
            localGalleryRepository.importTagsForPost(post)
            _uiState.update {
                it.copy(
                    isImported = true,
                    feedbackMessage = "Imported ${post.tags.size} tags to local database!"
                )
            }
        }
    }

    fun addCustomTag() {
        val post = _uiState.value.post ?: return
        val tagText = _uiState.value.customTagInput.trim()
        if (tagText.isBlank()) return

        viewModelScope.launch {
            localGalleryRepository.addCustomTagToPost(post.uniqueKey, post.source, tagText)
            val newTag = BooruTag(
                name = tagText,
                category = TagCategory.USER_CUSTOM,
                source = post.source,
                isCustom = true
            )
            val updatedTags = post.tags + newTag
            _uiState.update {
                it.copy(
                    customTagInput = "",
                    post = it.post?.copy(tags = updatedTags),
                    feedbackMessage = "Added custom tag: $tagText"
                )
            }
        }
    }

    fun onCustomTagInputChanged(text: String) {
        _uiState.update { it.copy(customTagInput = text) }
    }

    fun toggleFavorite() {
        val post = _uiState.value.post ?: return
        val newFav = !_uiState.value.isFavorite
        viewModelScope.launch {
            localGalleryRepository.importTagsForPost(post)
            localGalleryRepository.toggleFavorite(post.uniqueKey, newFav)
            _uiState.update { it.copy(isFavorite = newFav) }
        }
    }

    fun downloadImage(preferOriginal: Boolean = true) {
        val post = _uiState.value.post ?: return
        downloadManager.enqueueDownload(post, preferOriginal)
        _uiState.update { it.copy(feedbackMessage = "Download queued in background!") }
    }

    fun copyAllTags(context: Context) {
        val post = _uiState.value.post ?: return
        val tagString = post.tags.joinToString(" ") { it.name }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BooruVault Tags", tagString))
        _uiState.update { it.copy(feedbackMessage = "Copied ${post.tags.size} tags to clipboard!") }
    }

    fun exportMetadataJson(context: Context): String {
        val post = _uiState.value.post ?: return ""
        val json = JSONObject().apply {
            put("id", post.id)
            put("source", post.source.displayName)
            put("postUrl", post.postUrl)
            put("fileUrl", post.fileUrl)
            put("rating", post.rating.name)
            put("score", post.score)
            put("artist", post.artist)
            put("width", post.width)
            put("height", post.height)
            put("createdAt", post.createdAt)
            val tagsArray = JSONArray()
            post.tags.forEach { tag ->
                val tagObj = JSONObject().apply {
                    put("name", tag.name)
                    put("category", tag.category.name)
                    put("isCustom", tag.isCustom)
                }
                tagsArray.put(tagObj)
            }
            put("tags", tagsArray)
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BooruVault Metadata", json.toString(2)))
        _uiState.update { it.copy(feedbackMessage = "Metadata JSON copied to clipboard!") }
        return json.toString(2)
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
