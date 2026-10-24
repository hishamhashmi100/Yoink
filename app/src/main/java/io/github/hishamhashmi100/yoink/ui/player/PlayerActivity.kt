package io.github.hishamhashmi100.yoink.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.ui.MainActivity
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

@OptIn(UnstableApi::class)
/** Full-screen player. Video pauses when you leave; MP3s keep playing in the background. */
class PlayerActivity : ComponentActivity() {

    private lateinit var playerView: PlayerView
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var download: Download? = null

    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            // Landscape videos go landscape; vertical ones (Shorts) stay as they are.
            if (videoSize.width > videoSize.height) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        // The loop button in the controls: remember it for the next video.
        override fun onRepeatModeChanged(repeatMode: Int) {
            if (download == null) return
            val app = yoink
            app.appScope.launch { app.settings.setVideoRepeat(repeatMode) }
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
            setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
            artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FIT
        }

        // An ✕ in the corner to leave the player, shown and hidden together with the other controls.
        val density = resources.displayMetrics.density
        val close = ImageButton(this).apply {
            setImageResource(R.drawable.ic_close)
            setBackgroundResource(R.drawable.bg_player_button)
            contentDescription = getString(R.string.close_player)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener { finish() }
        }
        val size = (44 * density).toInt()
        val margin = (16 * density).toInt()
        val root = FrameLayout(this).apply {
            addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(close, FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START))
        }
        ViewCompat.setOnApplyWindowInsetsListener(close) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            (view.layoutParams as FrameLayout.LayoutParams).setMargins(bars.left + margin, bars.top + margin, 0, 0)
            view.requestLayout()
            insets
        }
        playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            close.visibility = visibility
        })
        setContentView(root)
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
            if (found.mimeType?.startsWith("audio/") == true) {
                AudioController.play(this@PlayerActivity, listOf(found), 0)
                startActivity(
                    Intent(this@PlayerActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
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
            controller.addListener(playerListener)
            download?.let { play(controller, it) }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controller()?.let { controller ->
            controller.removeListener(playerListener)
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
        controller.repeatMode = runBlocking { yoink.settings.videoRepeat.first() }
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
