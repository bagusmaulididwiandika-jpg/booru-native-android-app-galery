package com.example.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.downloader.DownloadManager
import com.example.data.model.BooruPost
import com.example.data.model.BooruRating
import com.example.data.model.BooruSource
import com.example.data.model.BooruTag
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BooruRepository
import com.example.data.repository.LocalGalleryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExploreUiState(
    val query: String = "",
    val selectedSource: BooruSource? = null, // null means All Sources
    val activeRatings: Set<BooruRating> = setOf(BooruRating.SAFE, BooruRating.QUESTIONABLE, BooruRating.EXPLICIT, BooruRating.UNKNOWN),
    val posts: List<BooruPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val tagSuggestions: List<BooruTag> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val gridColumnCount: Int = 2,
    val blurSensitive: Boolean = true
)

class ExploreViewModel(
    private val booruRepository: BooruRepository,
    private val localGalleryRepository: LocalGalleryRepository,
    private val downloadManager: DownloadManager,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autocompleteJob: Job? = null

    init {
        // Collect preferences
        viewModelScope.launch {
            appPreferences.preferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        gridColumnCount = prefs.gridColumnCount,
                        blurSensitive = prefs.blurSensitive
                    )
                }
            }
        }

        // Collect search history
        viewModelScope.launch {
            localGalleryRepository.getSearchHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history.map { h -> h.query }) }
            }
        }

        // Initial search with empty query (latest posts)
        search(resetPage = true)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        autocompleteJob?.cancel()
        if (newQuery.isNotBlank() && !newQuery.endsWith(" ")) {
            autocompleteJob = viewModelScope.launch {
                delay(300)
                val lastWord = newQuery.split(" ").lastOrNull() ?: ""
                if (lastWord.length >= 2) {
                    val suggestions = booruRepository.getAutocompleteTags(lastWord, _uiState.value.selectedSource)
                    _uiState.update { it.copy(tagSuggestions = suggestions) }
                } else {
                    _uiState.update { it.copy(tagSuggestions = emptyList()) }
                }
            }
        } else {
            _uiState.update { it.copy(tagSuggestions = emptyList()) }
        }
    }

    fun applySuggestion(tag: BooruTag) {
        val current = _uiState.value.query.trim()
        val parts = current.split(" ").filter { it.isNotBlank() }.toMutableList()
        if (parts.isNotEmpty()) {
            parts.removeAt(parts.lastIndex)
        }
        parts.add(tag.name)
        val newQuery = parts.joinToString(" ") + " "
        _uiState.update { it.copy(query = newQuery, tagSuggestions = emptyList()) }
        search(resetPage = true)
    }

    fun onSourceSelected(source: BooruSource?) {
        _uiState.update { it.copy(selectedSource = source) }
        search(resetPage = true)
    }

    fun toggleRating(rating: BooruRating) {
        val current = _uiState.value.activeRatings.toMutableSet()
        if (current.contains(rating)) {
            if (current.size > 1) { // Keep at least one rating
                current.remove(rating)
            }
        } else {
            current.add(rating)
        }
        _uiState.update { it.copy(activeRatings = current) }
        search(resetPage = true)
    }

    fun search(resetPage: Boolean = true) {
        searchJob?.cancel()
        val page = if (resetPage) 1 else _uiState.value.currentPage + 1
        val q = _uiState.value.query.trim()

        if (resetPage && q.isNotBlank()) {
            viewModelScope.launch {
                localGalleryRepository.recordSearch(q)
            }
        }

        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = resetPage,
                    isLoadingMore = !resetPage,
                    errorMessage = null,
                    tagSuggestions = emptyList()
                )
            }

            val result = booruRepository.searchPosts(
                query = q,
                source = _uiState.value.selectedSource,
                page = page,
                limit = 20,
                allowedRatings = _uiState.value.activeRatings
            )

            result.fold(
                onSuccess = { newPosts ->
                    _uiState.update { current ->
                        val combined = if (resetPage) newPosts else (current.posts + newPosts).distinctBy { it.uniqueKey }
                        current.copy(
                            posts = combined,
                            currentPage = page,
                            canLoadMore = newPosts.isNotEmpty(),
                            isLoading = false,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = error.localizedMessage ?: "Failed to fetch images from booru servers"
                        )
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        if (!_uiState.value.isLoading && !_uiState.value.isLoadingMore && _uiState.value.canLoadMore) {
            search(resetPage = false)
        }
    }

    fun downloadPost(post: BooruPost) {
        downloadManager.enqueueDownload(post, preferOriginal = true)
    }

    fun toggleFavorite(post: BooruPost) {
        val newFav = !post.isFavorite
        viewModelScope.launch {
            localGalleryRepository.importTagsForPost(post)
            localGalleryRepository.toggleFavorite(post.uniqueKey, newFav)
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map {
                        if (it.uniqueKey == post.uniqueKey) it.copy(isFavorite = newFav) else it
                    }
                )
            }
        }
    }

    fun setColumnCount(columns: Int) {
        viewModelScope.launch {
            appPreferences.setGridColumnCount(columns)
        }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", tagSuggestions = emptyList()) }
        search(resetPage = true)
    }
}
