package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LocalGalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val cacheSizeMb: String = "Calculating...",
    val totalDownloadedArtworks: Int = 0,
    val feedbackMessage: String? = null
)

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val localGalleryRepository: LocalGalleryRepository,
    private val db: AppDatabase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }

        viewModelScope.launch {
            localGalleryRepository.getDownloadedPosts().collect { list ->
                _uiState.update { it.copy(totalDownloadedArtworks = list.size) }
            }
        }

        calculateCacheSize()
    }

    fun setDarkMode(option: Int) {
        viewModelScope.launch {
            appPreferences.setDarkModeOption(option)
        }
    }

    fun setDefaultQualityOriginal(original: Boolean) {
        viewModelScope.launch {
            appPreferences.setDefaultQualityOriginal(original)
        }
    }

    fun setBlurSensitive(blur: Boolean) {
        viewModelScope.launch {
            appPreferences.setBlurSensitive(blur)
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            appPreferences.setWifiOnly(wifiOnly)
        }
    }

    fun setMaxConcurrent(limit: Int) {
        viewModelScope.launch {
            appPreferences.setMaxConcurrentDownloads(limit)
        }
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch {
            appPreferences.setGridColumnCount(cols)
        }
    }

    fun setAutoImportTags(autoImport: Boolean) {
        viewModelScope.launch {
            appPreferences.setAutoImportTags(autoImport)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            localGalleryRepository.clearSearchHistory()
            _uiState.update { it.copy(feedbackMessage = "Search history cleared.") }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.cacheDir.deleteRecursively()
            }
            calculateCacheSize()
            _uiState.update { it.copy(feedbackMessage = "Cache cleaned successfully.") }
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            val sizeMb = String.format("%.2f MB", size / (1024.0 * 1024.0))
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(cacheSizeMb = sizeMb) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
