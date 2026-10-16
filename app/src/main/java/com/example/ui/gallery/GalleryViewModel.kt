package com.example.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CollectionEntity
import com.example.data.model.BooruPost
import com.example.data.model.BooruRating
import com.example.data.model.BooruSource
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LocalGalleryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GallerySort(val label: String) {
    LATEST("Newest First"),
    OLDEST("Oldest First"),
    SCORE("Highest Score"),
    SOURCE("By Source")
}

data class GalleryUiState(
    val posts: List<BooruPost> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val selectedCollectionId: Long? = null,
    val selectedCollectionName: String? = null,
    val selectedSourceFilter: BooruSource? = null,
    val selectedRatingFilter: BooruRating? = null,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val sortOption: GallerySort = GallerySort.LATEST,
    val isMultiSelectMode: Boolean = false,
    val selectedPostKeys: Set<String> = emptySet(),
    val gridColumnCount: Int = 2,
    val blurSensitive: Boolean = true,
    val showCreateCollectionDialog: Boolean = false,
    val showAddToCollectionDialog: Boolean = false
)

class GalleryViewModel(
    private val localGalleryRepository: LocalGalleryRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val allDownloadedPosts = MutableStateFlow<List<BooruPost>>(emptyList())

    init {
        // Collect downloaded posts
        viewModelScope.launch {
            localGalleryRepository.getDownloadedPosts().collect { list ->
                allDownloadedPosts.value = list
                applyFilters()
            }
        }

        // Collect collections
        viewModelScope.launch {
            localGalleryRepository.getAllCollections().collect { cols ->
                _uiState.update { it.copy(collections = cols) }
            }
        }

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
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setSourceFilter(source: BooruSource?) {
        _uiState.update { it.copy(selectedSourceFilter = source) }
        applyFilters()
    }

    fun setRatingFilter(rating: BooruRating?) {
        _uiState.update { it.copy(selectedRatingFilter = rating) }
        applyFilters()
    }

    fun toggleOnlyFavorites() {
        _uiState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
        applyFilters()
    }

    fun setSortOption(sort: GallerySort) {
        _uiState.update { it.copy(sortOption = sort) }
        applyFilters()
    }

    fun selectCollection(collection: CollectionEntity?) {
        _uiState.update {
            it.copy(
                selectedCollectionId = collection?.collectionId,
                selectedCollectionName = collection?.name
            )
        }
        if (collection != null) {
            viewModelScope.launch {
                localGalleryRepository.getCollectionWithPosts(collection.collectionId).collect { posts ->
                    allDownloadedPosts.value = posts
                    applyFilters()
                }
            }
        } else {
            viewModelScope.launch {
                localGalleryRepository.getDownloadedPosts().collect { posts ->
                    allDownloadedPosts.value = posts
                    applyFilters()
                }
            }
        }
    }

    private fun applyFilters() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val src = _uiState.value.selectedSourceFilter
        val rating = _uiState.value.selectedRatingFilter
        val favOnly = _uiState.value.onlyFavorites

        var filtered = allDownloadedPosts.value.filter { post ->
            val matchesSrc = src == null || post.source == src
            val matchesRating = rating == null || post.rating == rating
            val matchesFav = !favOnly || post.isFavorite
            val matchesQuery = q.isEmpty() || post.tags.any { it.name.lowercase().contains(q) } ||
                    (post.artist?.lowercase()?.contains(q) == true) ||
                    post.source.displayName.lowercase().contains(q)

            matchesSrc && matchesRating && matchesFav && matchesQuery
        }

        filtered = when (_uiState.value.sortOption) {
            GallerySort.LATEST -> filtered
            GallerySort.OLDEST -> filtered.reversed()
            GallerySort.SCORE -> filtered.sortedByDescending { it.score ?: 0 }
            GallerySort.SOURCE -> filtered.sortedBy { it.source.name }
        }

        _uiState.update { it.copy(posts = filtered) }
    }

    fun toggleMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = !it.isMultiSelectMode,
                selectedPostKeys = emptySet()
            )
        }
    }

    fun togglePostSelection(postKey: String) {
        val current = _uiState.value.selectedPostKeys.toMutableSet()
        if (current.contains(postKey)) {
            current.remove(postKey)
        } else {
            current.add(postKey)
        }
        _uiState.update { it.copy(selectedPostKeys = current) }
    }

    fun selectAll() {
        val allKeys = _uiState.value.posts.map { it.uniqueKey }.toSet()
        _uiState.update { it.copy(selectedPostKeys = allKeys) }
    }

    fun deleteSelectedPosts() {
        viewModelScope.launch {
            _uiState.value.selectedPostKeys.forEach { key ->
                localGalleryRepository.deletePost(key)
            }
            _uiState.update { it.copy(isMultiSelectMode = false, selectedPostKeys = emptySet()) }
        }
    }

    fun createCollection(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            localGalleryRepository.createCollection(name, description)
            _uiState.update { it.copy(showCreateCollectionDialog = false) }
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            localGalleryRepository.deleteCollection(collectionId)
            if (_uiState.value.selectedCollectionId == collectionId) {
                selectCollection(null)
            }
        }
    }

    fun addSelectedToCollection(collectionId: Long) {
        viewModelScope.launch {
            _uiState.value.selectedPostKeys.forEach { key ->
                localGalleryRepository.addPostToCollection(collectionId, key)
            }
            _uiState.update {
                it.copy(
                    showAddToCollectionDialog = false,
                    isMultiSelectMode = false,
                    selectedPostKeys = emptySet()
                )
            }
        }
    }

    fun setShowCreateCollectionDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateCollectionDialog = show) }
    }

    fun setShowAddToCollectionDialog(show: Boolean) {
        _uiState.update { it.copy(showAddToCollectionDialog = show) }
    }

    fun setColumnCount(columns: Int) {
        viewModelScope.launch {
            appPreferences.setGridColumnCount(columns)
        }
    }
}
