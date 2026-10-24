package io.github.hishamhashmi100.yoink.ui.share

import android.app.Application
import android.content.Context
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.Engine
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ShareUiState(
    val videoId: String = "",
    val title: String? = null,
    val channel: String? = null,
    val durationSec: Long? = null,
    val loadingFormats: Boolean = true,
    val available: Set<Quality> = emptySet(),
    val formatsError: String? = null,
    /** The small Video / Audio sheet: no quality check, the buttons work straight away. */
    val quick: Boolean = false,
) {
    val thumbnailUrl: String get() = YouTubeUrl.thumbnailUrl(videoId)
}

/**
 * Finds the title and available qualities for shared videos. Lives outside the share sheet, so the check keeps
 * going when the sheet is swiped away or the user switches apps; when it finishes off screen, a notification
 * brings the sheet back (Android doesn't let background apps open screens by themselves).
 */
object Probes {

    private val all = MutableStateFlow<Map<String, ShareUiState>>(emptyMap())
    private val jobs = mutableMapOf<String, Job>()

    /** The video whose sheet is on screen right now, if any. */
    @Volatile var visibleVideo: String? = null

    fun state(videoId: String): Flow<ShareUiState> = all.map { it[videoId] ?: ShareUiState(videoId = videoId) }

    fun current(videoId: String): ShareUiState? = all.value[videoId]

    /**
     * Starts checking a video, unless it's already being checked or was checked successfully.
     * In [quick] mode there's no quality check: Video ([videoQuality]) and Audio work immediately, and only
     * the title is looked up.
     */
    fun start(context: Context, videoId: String, quick: Boolean = false, videoQuality: Quality = Quality.BEST) {
        val app = context.yoink
        synchronized(jobs) {
            val known = all.value[videoId]
            val reusable = known != null && known.quick == quick && !known.loadingFormats && known.formatsError == null
            if ((jobs[videoId]?.isActive == true && known?.quick == quick) || reusable) return
            jobs.remove(videoId)?.cancel()
            val initial = if (quick) {
                ShareUiState(videoId = videoId, loadingFormats = false, available = setOf(videoQuality, Quality.AUDIO), quick = true)
            } else {
                ShareUiState(videoId = videoId)
            }
            all.update { it + (videoId to initial) }
            jobs[videoId] = app.appScope.launch { check(app, videoId, listFormats = !quick) }
        }
    }

    /** The ✕ in the sheet: stop checking and forget the video. */
    fun cancel(context: Context, videoId: String) {
        synchronized(jobs) { jobs.remove(videoId)?.cancel() }
        all.update { it - videoId }
        ShareNotifications.cancel(context, videoId)
    }

    /** The sheet was swiped away: keep checking, and leave a notification to bring it back. */
    fun minimize(context: Context, videoId: String) {
        current(videoId)?.let { ShareNotifications.show(context, it, alert = false) }
    }

    /** The sheet is back on screen, so its notification isn't needed. */
    fun restored(context: Context, videoId: String) = ShareNotifications.cancel(context, videoId)

    /** A quality was picked; the download notification takes over from here. */
    fun done(context: Context, videoId: String) {
        ShareNotifications.cancel(context, videoId)
        all.update { it - videoId }
    }

    private suspend fun check(app: Application, videoId: String, listFormats: Boolean) {
        // Phase 1: oEmbed answers in ~100 ms, so the sheet shows a title right away.
        val meta = runCatching { withContext(Dispatchers.IO) { fetchOEmbed(videoId) } }.getOrNull()
        if (meta != null) update(videoId) { it.copy(title = it.title ?: meta.first, channel = it.channel ?: meta.second) }
        if (!listFormats) return

        // Phase 2: yt-dlp lists the real formats, which takes a few seconds.
        try {
            val json = probe(app, videoId)
            update(videoId) {
                it.copy(
                    title = it.title ?: json.optString("title").ifBlank { null },
                    channel = it.channel ?: json.optString("channel").ifBlank { null },
                    durationSec = json.optDouble("duration").takeUnless { d -> d.isNaN() }?.toLong(),
                    loadingFormats = false,
                    available = availableQualities(json),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            update(videoId) {
                it.copy(
                    loadingFormats = false,
                    available = setOf(Quality.BEST, Quality.AUDIO),
                    formatsError = app.getString(R.string.share_formats_failed),
                )
            }
        }
        // Finished while nobody is looking: pop up a notification with quick download buttons.
        if (visibleVideo != videoId) current(videoId)?.let { ShareNotifications.show(app, it, alert = true) }
    }

    private fun update(videoId: String, change: (ShareUiState) -> ShareUiState) {
        all.update { map -> map[videoId]?.let { map + (videoId to change(it)) } ?: map }
    }

    private suspend fun probe(app: Application, videoId: String): JSONObject {
        val request = YoutubeDLRequest(YouTubeUrl.watchUrl(videoId)).apply {
            addOption("--dump-single-json")
            addOption("--no-playlist")
        }
        Engine.ensureFresh(app)
        val response = Engine.execute(app, request)
        return JSONObject(response.out)
    }

    private fun fetchOEmbed(videoId: String): Pair<String?, String?> {
        val target = URLEncoder.encode(YouTubeUrl.watchUrl(videoId), "UTF-8")
        val connection = URL("https://www.youtube.com/oembed?url=$target&format=json").openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        return try {
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            json.optString("title").ifBlank { null } to json.optString("author_name").ifBlank { null }
        } finally {
            connection.disconnect()
        }
    }

    private fun availableQualities(json: JSONObject): Set<Quality> {
        val formats = json.optJSONArray("formats")
        val heights = mutableListOf<Int>()
        var hasAudio = false
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                val vcodec = f.codec("vcodec")
                val acodec = f.codec("acodec")
                val height = f.optInt("height", 0)
                if (vcodec != null && height > 0) heights += height
                if (acodec != null) hasAudio = true
            }
        }
        val maxHeight = heights.maxOrNull() ?: 0
        return buildSet {
            if (maxHeight > 0 || hasAudio) add(Quality.BEST)
            // Allow a little slack for odd sizes like 1072p.
            Quality.entries.filter { it.height != null && maxHeight >= it.height * 9 / 10 }.forEach(::add)
            if (hasAudio) add(Quality.AUDIO)
        }
    }

    private fun JSONObject.codec(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "none" }
}
