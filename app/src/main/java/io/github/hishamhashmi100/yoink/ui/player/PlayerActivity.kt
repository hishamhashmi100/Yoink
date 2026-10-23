package io.github.hishamhashmi100.yoink.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
/** Full-screen player. Video pauses when you leave; MP3s keep playing in the background. */
class PlayerActivity : ComponentActivity() {

    private lateinit var playerView: PlayerView
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var download: Download? = null

    private val sizeListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            // Landscape videos go landscape; vertical ones (Shorts) stay as they are.
            if (videoSize.width > videoSize.height) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        playerView = PlayerView(this).apply {
            keepScreenOn = true
            setShowNextButton(false)
            setShowPreviousButton(false)
            artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FIT
        }
        setContentView(playerView)
        loadDownload(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadDownload(intent)
    }

    private fun loadDownload(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        lifecycleScope.launch {
            val found = yoink.repository.dao.get(id)
            if (found?.fileUri == null) {
                finish()
                return@launch
            }
            download = found
            controller()?.let { play(it, found) }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val controller = controller() ?: return@addListener
            playerView.player = controller
            controller.addListener(sizeListener)
            download?.let { play(controller, it) }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controller()?.let { controller ->
            controller.removeListener(sizeListener)
            if (download?.mimeType?.startsWith("audio/") != true) controller.pause()
        }
        playerView.player = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onStop()
    }

    private fun controller(): MediaController? =
        controllerFuture?.takeIf { it.isDone && !it.isCancelled }?.let { runCatching { it.get() }.getOrNull() }

    private fun play(controller: MediaController, download: Download) {
        val mediaId = download.id.toString()
        // Coming back to something already playing (e.g. from the media notification) shouldn't restart it.
        if (controller.currentMediaItem?.mediaId == mediaId) return
        val metadata = MediaMetadata.Builder()
            .setTitle(download.title)
            .setArtist(download.channel)
            .setArtworkUri(download.thumbnailPath?.let { Uri.fromFile(File(it)) })
            .build()
        val item = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(download.fileUri)
            .setMimeType(download.mimeType)
            .setMediaMetadata(metadata)
            .build()
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
    }

    companion object {
        private const val EXTRA_ID = "download_id"

        fun intent(context: Context, downloadId: Long): Intent =
            Intent(context, PlayerActivity::class.java).putExtra(EXTRA_ID, downloadId)
    }
}
