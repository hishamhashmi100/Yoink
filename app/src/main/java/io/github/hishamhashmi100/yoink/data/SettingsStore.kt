package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.hishamhashmi100.yoink.download.Quality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** When set, sharing a link downloads in this quality without showing the sheet. */
    val defaultQuality: Quality? = null,
    /** Also publish finished files to Movies/Yoink and Music/Yoink. */
    val saveToGallery: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            defaultQuality = Quality.fromName(p[DEFAULT_QUALITY]),
            saveToGallery = p[SAVE_TO_GALLERY] ?: false,
            theme = p[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
        )
    }

    val firstUpdateCheckDone: Flow<Boolean> = store.data.map { it[FIRST_UPDATE_DONE] ?: false }

    suspend fun setDefaultQuality(quality: Quality?) = store.edit {
        if (quality == null) {
            it.remove(DEFAULT_QUALITY)
        } else {
            it[DEFAULT_QUALITY] = quality.name
        }
    }

    suspend fun setSaveToGallery(value: Boolean) = set(SAVE_TO_GALLERY, value)
    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(value: Boolean) = set(DYNAMIC_COLOR, value)
    suspend fun setFirstUpdateCheckDone() = set(FIRST_UPDATE_DONE, true)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) = store.edit { it[key] = value }

    private companion object {
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val SAVE_TO_GALLERY = booleanPreferencesKey("save_to_gallery")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FIRST_UPDATE_DONE = booleanPreferencesKey("first_update_done")
    }
}
