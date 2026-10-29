package io.github.hishamhashmi100.yoink.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.yoink
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NowPlaying(
    val downloadId: Long?,
    val title: String,
    val artist: String?,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean = false,
    /** One of Player.REPEAT_MODE_OFF, _ALL (the whole queue) or _ONE (this track). */
    val repeatMode: Int,
    val shuffle: Boolean = false,
    /** Position of this track in [AudioController.queue]. */
    val index: Int = 0,
)

/** One track in the play queue, as the full-screen player lists it. */
data class QueueEntry(val index: Int, val title: String, val artist: String?, val artworkUri: Uri?)

/**
 * Plays MP3s in the background without opening the full-screen player, like a music app: the library's
 * audio files become the queue, the mini player and the system media controls drive it.
 */
object AudioController {

    // Audio queue items are tagged so the mini player ignores videos playing in PlayerActivity.
    private const val PREFIX = "audio:"
    private const val EXTRA_THUMBNAIL = "thumbnail"

    private var future: ListenableFuture<MediaController>? = null

    // Themed covers for the rest of the queue are made here, one at a time and at low priority, so they don't
    // compete with the screen (scrolling, animations) while the first track is already playing.
    private val coverMaker = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "themed-covers")
    }.asCoroutineDispatcher()
    private var coverJob: Job? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect(context: Context) = withController(context) {}

    /**
     * Plays [queue] in order, starting at [start]. [shuffle] turns shuffle on or off for this queue (and is
     * remembered); left out, the last shuffle setting carries on.
     */
    fun play(context: Context, queue: List<Download>, start: Int, shuffle: Boolean? = null) {
        if (queue.isEmpty()) return
        val app = context.yoink
        val first = start.coerceIn(0, queue.lastIndex)
        coverJob?.cancel()
        app.appScope.launch {
            val appearance = app.settings.settings.first().appearance
            // Videos share the player and have their own loop setting, so put the MP3 one back.
            val repeat = app.settings.audioRepeat.first()
            val shuffleOn = shuffle ?: app.settings.audioShuffle.first()
            if (shuffle != null) app.settings.setAudioShuffle(shuffle)
            // Only the first track's cover is made before playing. Making every cover first kept a long list
            // waiting several seconds before the first sound.
            val theme = withContext(Dispatchers.IO) {
                ThemedArtwork.theme(app, appearance).also { theme ->
                    ThemedArtwork.clearOthers(app, theme)
                    ThemedArtwork.render(ThemedArtwork.file(app, theme, queue[first].thumbnailPath), theme, queue[first].thumbnailPath)
                }
            }
            val items = queue.mapIndexed { i, download ->
                val cover = ThemedArtwork.file(app, theme, download.thumbnailPath)
                // The others are made below; if the first one couldn't be, the plain thumbnail stands in.
                item(download, cover.takeIf { i != first || it.exists() }?.let(Uri::fromFile))
            }
            withContext(Dispatchers.Main) {
                withController(app) { c ->
                    c.repeatMode = repeat
                    c.shuffleModeEnabled = shuffleOn
                    c.setMediaItems(items, first, 0L)
                    c.prepare()
                    c.play()
                }
            }
            // The rest, in the order they come up, while the first one plays.
            coverJob = app.appScope.launch(coverMaker) {
                for (i in (first + 1 until queue.size) + (0 until first)) {
                    ensureActive()
                    ThemedArtwork.render(ThemedArtwork.file(app, theme, queue[i].thumbnailPath), theme, queue[i].thumbnailPath)
                }
            }
        }
    }

    fun togglePlayPause(context: Context) = withController(context) { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    /** Off → repeat all → repeat this track → off, like a music app. */
    fun cycleRepeat(context: Context) = withController(context) { c ->
        val next = PlaybackService.nextRepeatMode(c.repeatMode)
        c.repeatMode = next
        val app = context.yoink
        app.appScope.launch { app.settings.setAudioRepeat(next) }
    }

    /** Turns shuffle on or off, says so (it's otherwise easy to miss), and remembers it. */
    fun toggleShuffle(context: Context) = withController(context) { c ->
        val on = !c.shuffleModeEnabled
        c.shuffleModeEnabled = on
        val app = context.yoink
        Toast.makeText(app, if (on) R.string.player_shuffle_on_toast else R.string.player_shuffle_off_toast, Toast.LENGTH_SHORT).show()
        app.appScope.launch { app.settings.setAudioShuffle(on) }
    }

    fun seekTo(context: Context, positionMs: Long) = withController(context) { it.seekTo(positionMs) }

    /** Jumps to the track at [index] in the queue. */
    fun skipTo(context: Context, index: Int) = withController(context) { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
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
        val (position, duration) = position()
        return if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    /** Where the current track is and how long it is, in milliseconds (duration 0 while unknown). */
    fun position(): Pair<Long, Long> {
        val c = future?.takeIf { it.isDone }?.let { runCatching { it.get() }.getOrNull() } ?: return 0L to 0L
        return c.currentPosition.coerceAtLeast(0L) to c.duration.coerceAtLeast(0L)
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

    /** [artwork] is the themed cover, which the system media player takes its colors from. */
    private fun item(download: Download, artwork: Uri?): MediaItem = MediaItem.Builder()
        .setMediaId(PREFIX + download.id)
        .setUri(download.fileUri)
        .setMimeType(download.mimeType)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(download.title)
                .setArtist(download.channel)
                .setArtworkUri(artwork ?: download.thumbnailPath?.let { Uri.fromFile(File(it)) })
                // The real thumbnail, for the mini player.
                .setExtras(Bundle().apply { putString(EXTRA_THUMBNAIL, download.thumbnailPath) })
                .build()
        )
        .build()

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        if (item == null || !item.mediaId.startsWith(PREFIX)) {
            _nowPlaying.value = null
            _queue.value = emptyList()
            return
        }
        _nowPlaying.value = NowPlaying(
            downloadId = item.mediaId.removePrefix(PREFIX).toLongOrNull(),
            title = item.mediaMetadata.title?.toString().orEmpty(),
            artist = item.mediaMetadata.artist?.toString(),
            artworkUri = thumbnail(item),
            isPlaying = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
            index = player.currentMediaItemIndex,
        )
        // In playing order, so with shuffle on the list shows the shuffled order.
        _queue.value = playOrder(player).map { i ->
            val entry = player.getMediaItemAt(i)
            QueueEntry(i, entry.mediaMetadata.title?.toString().orEmpty(), entry.mediaMetadata.artist?.toString(), thumbnail(entry))
        }
    }

    private fun playOrder(player: Player): List<Int> {
        val count = player.mediaItemCount
        val timeline = player.currentTimeline
        if (timeline.windowCount != count || count == 0) return (0 until count).toList()
        val shuffle = player.shuffleModeEnabled
        val order = generateSequence(timeline.getFirstWindowIndex(shuffle)) { index ->
            timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffle).takeIf { it != C.INDEX_UNSET }
        }.take(count).toList()
        return if (order.size == count) order else (0 until count).toList()
    }

    /** The real thumbnail; the artwork the system sees is the themed copy. */
    private fun thumbnail(item: MediaItem): Uri? =
        item.mediaMetadata.extras?.getString(EXTRA_THUMBNAIL)?.let { Uri.fromFile(File(it)) } ?: item.mediaMetadata.artworkUri
}
