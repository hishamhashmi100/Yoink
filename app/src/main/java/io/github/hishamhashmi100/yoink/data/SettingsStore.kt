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

/** What happens when a link is shared to Yoink. */
enum class ShareMode {
    /** The full sheet with every quality. */
    ASK,
    /** A small sheet with just Video and Audio, usable right away. */
    VIDEO_OR_AUDIO,
    /** Download the video in [Settings.videoQuality] with no sheet. */
    VIDEO,
    /** Download audio (MP3) with no sheet. */
    AUDIO,
}

data class Settings(
    val shareMode: ShareMode = ShareMode.ASK,
    /** What "Video" means when Yoink doesn't ask for a quality. */
    val videoQuality: Quality = Quality.BEST,
    /** Also publish finished files to Movies/Yoink and Music/Yoink. */
    val saveToGallery: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { p ->
        // Before share modes there was a single "default quality"; carry it over.
        val legacy = Quality.fromName(p[DEFAULT_QUALITY])
        Settings(
            shareMode = p[SHARE_MODE]?.let { name -> ShareMode.entries.firstOrNull { it.name == name } }
                ?: when {
                    legacy == null -> ShareMode.ASK
                    legacy.isAudio -> ShareMode.AUDIO
                    else -> ShareMode.VIDEO
                },
            videoQuality = Quality.fromName(p[VIDEO_QUALITY])?.takeUnless { it.isAudio }
                ?: legacy?.takeUnless { it.isAudio }
                ?: Quality.BEST,
            saveToGallery = p[SAVE_TO_GALLERY] ?: false,
            theme = p[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            dynamicColor = p[DYNAMIC_COLOR] ?: true,
        )
    }

    val firstUpdateCheckDone: Flow<Boolean> = store.data.map { it[FIRST_UPDATE_DONE] ?: false }

    /** The app version we last sent an "update available" notification for. */
    val notifiedVersion: Flow<String?> = store.data.map { it[NOTIFIED_VERSION] }

    suspend fun setShareMode(mode: ShareMode) = store.edit { it[SHARE_MODE] = mode.name }
    suspend fun setVideoQuality(quality: Quality) = store.edit { it[VIDEO_QUALITY] = quality.name }

    suspend fun setSaveToGallery(value: Boolean) = set(SAVE_TO_GALLERY, value)
    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(value: Boolean) = set(DYNAMIC_COLOR, value)
    suspend fun setFirstUpdateCheckDone() = set(FIRST_UPDATE_DONE, true)
    suspend fun setNotifiedVersion(version: String) = store.edit { it[NOTIFIED_VERSION] = version }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) = store.edit { it[key] = value }

    private companion object {
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality") // Only read, to migrate old settings.
        val SHARE_MODE = stringPreferencesKey("share_mode")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val SAVE_TO_GALLERY = booleanPreferencesKey("save_to_gallery")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FIRST_UPDATE_DONE = booleanPreferencesKey("first_update_done")
        val NOTIFIED_VERSION = stringPreferencesKey("notified_version")
    }
}
