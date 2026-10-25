package io.github.hishamhashmi100.yoink.ui.share

import android.app.Application
import android.content.Context
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.PlaylistEntry
import io.github.hishamhashmi100.yoink.download.Engine
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class PlaylistUiState(
    val playlistId: String,
    val title: String? = null,
    val channel: String? = null,
    val entries: List<PlaylistEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val thumbnailUrl: String? get() = entries.firstOrNull()?.let { YouTubeUrl.thumbnailUrl(it.videoId) }
}

/** Lists the videos in a shared playlist, for the playlist sheet. */
object Playlists {

    /** More than anyone would want from one share, and keeps a channel's whole upload list from running forever. */
    private const val MAX_VIDEOS = 1000

    // What YouTube lists in place of videos nobody can watch any more.
    private val UNAVAILABLE = setOf("[Private video]", "[Deleted video]", "[Unavailable video]")
    private val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")

    private val all = MutableStateFlow<Map<String, PlaylistUiState>>(emptyMap())
    private val jobs = mutableMapOf<String, Job>()

    fun state(playlistId: String): Flow<PlaylistUiState> =
        all.map { it[playlistId] ?: PlaylistUiState(playlistId = playlistId) }

    fun current(playlistId: String): PlaylistUiState? = all.value[playlistId]

    /** Starts listing a playlist, unless that's already under way or done. [videoId]: the video it was shared from. */
    fun start(context: Context, playlistId: String, videoId: String?) {
        val app = context.yoink
        synchronized(jobs) {
            val known = all.value[playlistId]
            if (jobs[playlistId]?.isActive == true || (known != null && !known.loading && known.error == null)) return
            all.update { it + (playlistId to PlaylistUiState(playlistId = playlistId)) }
            jobs[playlistId] = app.appScope.launch { load(app, playlistId, videoId) }
        }
    }

    /** The sheet was closed: stop listing and forget it. */
    fun cancel(playlistId: String) {
        synchronized(jobs) { jobs.remove(playlistId)?.cancel() }
        all.update { it - playlistId }
    }

    private suspend fun load(app: Application, playlistId: String, videoId: String?) {
        try {
            val request = YoutubeDLRequest(YouTubeUrl.playlistUrl(playlistId, videoId)).apply {
                // Just the list (ids, titles, lengths), not each video's details: seconds instead of minutes.
                addOption("--flat-playlist")
                addOption("--dump-single-json")
                addOption("--playlist-end", MAX_VIDEOS.toString())
                addOption("--socket-timeout", "15")
            }
            Engine.ensureFresh(app)
            val json = JSONObject(Engine.execute(app, request).out)
            val entries = entries(json)
            all.update { map ->
                map + (playlistId to PlaylistUiState(
                    playlistId = playlistId,
                    title = json.optString("title").ifBlank { null },
                    channel = json.optString("channel").ifBlank { json.optString("uploader") }.ifBlank { null },
                    entries = entries,
                    loading = false,
                    error = if (entries.isEmpty()) app.getString(R.string.playlist_empty) else null,
                ))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            all.update { map ->
                map + (playlistId to PlaylistUiState(playlistId, loading = false, error = app.getString(R.string.playlist_failed)))
            }
        }
    }

    private fun entries(json: JSONObject): List<PlaylistEntry> {
        val array = json.optJSONArray("entries") ?: return emptyList()
        return (0 until array.length())
            .mapNotNull { i ->
                val entry = array.optJSONObject(i) ?: return@mapNotNull null
                val id = entry.optString("id").takeIf { VIDEO_ID.matches(it) } ?: return@mapNotNull null
                val title = entry.optString("title")
                if (title in UNAVAILABLE) return@mapNotNull null
                PlaylistEntry(
                    videoId = id,
                    title = title.ifBlank { id },
                    channel = entry.optString("channel").ifBlank { entry.optString("uploader") }.ifBlank { null },
                    durationSec = entry.optDouble("duration").takeUnless { it.isNaN() }?.toLong(),
                )
            }
            // A video can be in a playlist twice; it only needs downloading once.
            .distinctBy { it.videoId }
    }
}
