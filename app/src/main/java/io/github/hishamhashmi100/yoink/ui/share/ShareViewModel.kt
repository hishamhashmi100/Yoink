package io.github.hishamhashmi100.yoink.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.Engine
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) {
    val thumbnailUrl: String get() = YouTubeUrl.thumbnailUrl(videoId)
}

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state

    fun load(videoId: String) {
        if (_state.value.videoId == videoId) return
        _state.value = ShareUiState(videoId = videoId)

        // Phase 1: oEmbed answers in ~100 ms, so the sheet shows a title right away.
        viewModelScope.launch {
            val meta = runCatching { withContext(Dispatchers.IO) { fetchOEmbed(videoId) } }.getOrNull()
            if (meta != null) _state.update { it.copy(title = it.title ?: meta.first, channel = it.channel ?: meta.second) }
        }

        // Phase 2: yt-dlp lists the real formats, which takes a few seconds.
        viewModelScope.launch {
            try {
                val json = probe(videoId)
                _state.update {
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
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingFormats = false,
                        available = setOf(Quality.BEST, Quality.AUDIO),
                        formatsError = getApplication<Application>().getString(R.string.share_formats_failed),
                    )
                }
            }
        }
    }

    private suspend fun probe(videoId: String): JSONObject {
        val request = YoutubeDLRequest(YouTubeUrl.watchUrl(videoId)).apply {
            addOption("--dump-single-json")
            addOption("--no-playlist")
        }
        val response = Engine.execute(getApplication<Application>(), request)
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
