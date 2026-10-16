package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "booru_vault_preferences")

data class UserPreferences(
    val darkModeOption: Int = 1, // 0 = Light, 1 = Dark, 2 = System
    val gridColumnCount: Int = 2,
    val defaultQualityOriginal: Boolean = true,
    val blurSensitive: Boolean = true,
    val wifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 2,
    val autoImportTags: Boolean = true
)

class AppPreferences(private val context: Context) {
    companion object {
        private val KEY_DARK_MODE = intPreferencesKey("dark_mode_option")
        private val KEY_GRID_COLUMNS = intPreferencesKey("grid_column_count")
        private val KEY_QUALITY_ORIGINAL = booleanPreferencesKey("default_quality_original")
        private val KEY_BLUR_SENSITIVE = booleanPreferencesKey("blur_sensitive")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")
        private val KEY_AUTO_IMPORT_TAGS = booleanPreferencesKey("auto_import_tags")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            darkModeOption = prefs[KEY_DARK_MODE] ?: 1, // Default dark
            gridColumnCount = prefs[KEY_GRID_COLUMNS] ?: 2,
            defaultQualityOriginal = prefs[KEY_QUALITY_ORIGINAL] ?: true,
            blurSensitive = prefs[KEY_BLUR_SENSITIVE] ?: true,
            wifiOnly = prefs[KEY_WIFI_ONLY] ?: false,
            maxConcurrentDownloads = prefs[KEY_MAX_CONCURRENT] ?: 2,
            autoImportTags = prefs[KEY_AUTO_IMPORT_TAGS] ?: true
        )
    }

    suspend fun setDarkModeOption(option: Int) {
        context.dataStore.edit { it[KEY_DARK_MODE] = option }
    }

    suspend fun setGridColumnCount(columns: Int) {
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = columns.coerceIn(2, 5) }
    }

    suspend fun setDefaultQualityOriginal(original: Boolean) {
        context.dataStore.edit { it[KEY_QUALITY_ORIGINAL] = original }
    }

    suspend fun setBlurSensitive(blur: Boolean) {
        context.dataStore.edit { it[KEY_BLUR_SENSITIVE] = blur }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setMaxConcurrentDownloads(limit: Int) {
        context.dataStore.edit { it[KEY_MAX_CONCURRENT] = limit.coerceIn(1, 4) }
    }

    suspend fun setAutoImportTags(autoImport: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_IMPORT_TAGS] = autoImport }
    }
}
