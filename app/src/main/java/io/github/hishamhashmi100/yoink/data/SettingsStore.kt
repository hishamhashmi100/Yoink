package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.hishamhashmi100.yoink.download.Quality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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
    val appearance: Appearance = Appearance(),
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
            appearance = Appearance(
                theme = enumOf(p[THEME], ThemeMode.SYSTEM),
                pureBlack = p[PURE_BLACK] ?: false,
                dynamicColor = p[DYNAMIC_COLOR] ?: true,
                seedColor = p[SEED_COLOR] ?: YOINK_RED,
                palette = enumOf(p[PALETTE], PaletteStyle.TONAL),
                contrast = enumOf(p[CONTRAST], Contrast.STANDARD),
                corners = enumOf(p[CORNERS], CornerStyle.ROUND),
                font = enumOf(p[FONT], FontStyle.DEFAULT),
            ),
        )
    }

    /** The settings right now. Activities use it so their first frame is already in the chosen theme. */
    fun current(): Settings = runBlocking { settings.first() }

    val savedThemes: Flow<List<SavedTheme>> = store.data.map { SavedTheme.listFromJson(it[SAVED_THEMES]) }

    val firstUpdateCheckDone: Flow<Boolean> = store.data.map { it[FIRST_UPDATE_DONE] ?: false }

    /** The app version we last sent an "update available" notification for. */
    val notifiedVersion: Flow<String?> = store.data.map { it[NOTIFIED_VERSION] }

    suspend fun setShareMode(mode: ShareMode) = store.edit { it[SHARE_MODE] = mode.name }
    suspend fun setVideoQuality(quality: Quality) = store.edit { it[VIDEO_QUALITY] = quality.name }

    suspend fun setSaveToGallery(value: Boolean) = set(SAVE_TO_GALLERY, value)

    suspend fun setAppearance(appearance: Appearance) = store.edit {
        it[THEME] = appearance.theme.name
        it[PURE_BLACK] = appearance.pureBlack
        it[DYNAMIC_COLOR] = appearance.dynamicColor
        it[SEED_COLOR] = appearance.seedColor
        it[PALETTE] = appearance.palette.name
        it[CONTRAST] = appearance.contrast.name
        it[CORNERS] = appearance.corners.name
        it[FONT] = appearance.font.name
    }

    /** Saves [appearance] under [name], replacing a saved theme with the same name. */
    suspend fun saveTheme(name: String, appearance: Appearance) = store.edit { p ->
        val themes = SavedTheme.listFromJson(p[SAVED_THEMES]).filterNot { it.name.equals(name, ignoreCase = true) }
        p[SAVED_THEMES] = SavedTheme.listToJson(themes + SavedTheme(name, appearance))
    }

    suspend fun deleteTheme(name: String) = store.edit { p ->
        p[SAVED_THEMES] = SavedTheme.listToJson(SavedTheme.listFromJson(p[SAVED_THEMES]).filterNot { it.name == name })
    }
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
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val SEED_COLOR = intPreferencesKey("seed_color")
        val PALETTE = stringPreferencesKey("palette")
        val CONTRAST = stringPreferencesKey("contrast")
        val CORNERS = stringPreferencesKey("corners")
        val FONT = stringPreferencesKey("font")
        val SAVED_THEMES = stringPreferencesKey("saved_themes")
        val FIRST_UPDATE_DONE = booleanPreferencesKey("first_update_done")
        val NOTIFIED_VERSION = stringPreferencesKey("notified_version")
    }
}
