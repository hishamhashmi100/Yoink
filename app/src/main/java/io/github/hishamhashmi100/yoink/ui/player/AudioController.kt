package io.github.hishamhashmi100.yoink.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.hishamhashmi100.yoink.data.Download
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class NowPlaying(
    val downloadId: Long?,
    val title: String,
    val artist: String?,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
    val hasNext: Boolean,
)

/**
 * Plays MP3s in the background without opening the full-screen player, like a music app: the library's
 * audio files become the queue, the mini player and the system media controls drive it.
 */
object AudioController {

    // Audio queue items are tagged so the mini player ignores videos playing in PlayerActivity.
    private const val PREFIX = "audio:"

    private var future: ListenableFuture<MediaController>? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect(context: Context) = withController(context) {}

    /** Plays [queue] in order, starting at [start]. */
    fun play(context: Context, queue: List<Download>, start: Int) = withController(context) { c ->
        c.setMediaItems(queue.map(::item), start.coerceIn(0, queue.lastIndex.coerceAtLeast(0)), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause(context: Context) = withController(context) { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next(context: Context) = withController(context) { it.seekToNextMediaItem() }

    /** Back to the start of the track, or the previous track if it just started. */
    fun previous(context: Context) = withController(context) { it.seekToPrevious() }

    /**
     * Takes a deleted download out of the player. If it was playing, the next track starts (or playback stops
     * when nothing is left), so a deleted MP3 doesn't keep playing from memory.
     */
    fun remove(context: Context, downloadId: Long) {
        if (future == null) return // Nothing has been played this session.
        withController(context) { c ->
            val ids = setOf(PREFIX + downloadId, downloadId.toString())
            for (i in c.mediaItemCount - 1 downTo 0) {
                if (c.getMediaItemAt(i).mediaId in ids) c.removeMediaItem(i)
            }
            if (c.mediaItemCount == 0) c.stop()
        }
    }

    fun stop(context: Context) = withController(context) { c ->
        c.stop()
        c.clearMediaItems()
    }

    /** 0–1 through the current track. */
    fun progress(): Float {
        val c = future?.takeIf { it.isDone }?.let { runCatching { it.get() }.getOrNull() } ?: return 0f
        val duration = c.duration
        return if (duration > 0) (c.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    private fun withController(context: Context, block: (MediaController) -> Unit) {
        val app = context.applicationContext
        val executor = ContextCompat.getMainExecutor(app)
        val f = future ?: MediaController.Builder(app, SessionToken(app, ComponentName(app, PlaybackService::class.java)))
            .buildAsync()
            .also { created ->
                future = created
                created.addListener({
                    runCatching { created.get() }.getOrNull()?.let {
                        it.addListener(listener)
                        publish(it)
                    } ?: run { future = null }
                }, executor)
            }
        f.addListener({ runCatching { f.get() }.getOrNull()?.let(block) }, executor)
    }

    private fun item(download: Download): MediaItem = MediaItem.Builder()
        .setMediaId(PREFIX + download.id)
        .setUri(download.fileUri)
        .setMimeType(download.mimeType)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(download.title)
                .setArtist(download.channel)
                .setArtworkUri(download.thumbnailPath?.let { Uri.fromFile(File(it)) })
                .build()
        )
        .build()

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _nowPlaying.value = if (item == null || !item.mediaId.startsWith(PREFIX)) {
            null
        } else {
            NowPlaying(
                downloadId = item.mediaId.removePrefix(PREFIX).toLongOrNull(),
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString(),
                artworkUri = item.mediaMetadata.artworkUri,
                isPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
                hasNext = player.hasNextMediaItem(),
            )
        }
    }
}
