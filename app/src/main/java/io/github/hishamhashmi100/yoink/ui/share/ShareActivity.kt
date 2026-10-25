package io.github.hishamhashmi100.yoink.ui.share

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.GlassStyle
import io.github.hishamhashmi100.yoink.data.ShareMode
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives links from YouTube's Share menu (or youtube.com / youtu.be links) and shows a
 * quality picker over the calling app. Picking a quality queues the download and closes;
 * swiping the sheet away tucks it into a notification while the check carries on.
 * Playlist links get the playlist sheet, which queues every video in one go.
 */
class ShareActivity : ComponentActivity() {

    private val videoId = MutableStateFlow<String?>(null)
    /** The playlist the link points to, if any. */
    private val playlistId = MutableStateFlow<String?>(null)
    /** Showing the playlist sheet rather than the video's. */
    private val wholePlaylist = MutableStateFlow(false)
    private var quick = false
    private var videoQuality = Quality.BEST
    private var pendingDownload: (() -> Unit)? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pendingDownload?.invoke()
            pendingDownload = null
        }

    // Notifications let the sheet be tucked away and come back when the qualities are ready.
    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = sharedText(intent)
        val id = YouTubeUrl.extractId(text)
        val list = YouTubeUrl.extractPlaylistId(text)
        if (id == null && list == null) {
            Toast.makeText(this, R.string.share_not_youtube, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val settings = yoink.settings.settings.first()
            videoQuality = settings.videoQuality
            // "Right away" modes skip the sheet entirely (a whole playlist always asks first).
            val immediate = when (settings.shareMode) {
                ShareMode.VIDEO -> settings.videoQuality
                ShareMode.AUDIO -> Quality.AUDIO
                else -> null
            }
            if (id != null && immediate != null && savedInstanceState == null) {
                videoId.value = id
                onQualityPicked(immediate)
                return@launch
            }
            quick = settings.shareMode == ShareMode.VIDEO_OR_AUDIO
            if (id != null) show(id, list) else showPlaylist(list!!)
            if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
                askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            blurBehind(settings.appearance.glass)
            setContent {
                // No backdrop: YouTube (blurred, with glass on) shows through behind the sheet.
                YoinkTheme(settings, backdrop = false) {
                    val list = playlistId.collectAsStateWithLifecycle().value
                    val current = videoId.collectAsStateWithLifecycle().value
                    val whole = wholePlaylist.collectAsStateWithLifecycle().value
                    if (list != null && (current == null || whole)) {
                        val state = remember(list) { Playlists.state(list) }
                            .collectAsStateWithLifecycle(PlaylistUiState(playlistId = list)).value
                        PlaylistSheet(state = state, onPick = ::onPlaylistPicked, onClose = ::closePlaylist)
                    } else if (current != null) {
                        val state = remember(current) { Probes.state(current) }
                            .collectAsStateWithLifecycle(ShareUiState(videoId = current)).value
                        ShareSheet(
                            state = state,
                            videoQuality = videoQuality,
                            onPick = ::onQualityPicked,
                            onMinimize = ::minimize,
                            onClose = ::close,
                            onWholePlaylist = if (list != null) ({ showPlaylist(list) }) else null,
                        )
                    }
                }
            }
        }
    }

    /** With a glass look, blur the app behind the sheet like frosted glass (Android 12 and newer). */
    private fun blurBehind(glass: GlassStyle) {
        if (Build.VERSION.SDK_INT < 31 || glass == GlassStyle.OFF) return
        val radius = (if (glass == GlassStyle.FROSTED) 48 else 14) * resources.displayMetrics.density
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.also { it.blurBehindRadius = radius.toInt() }
    }

    override fun onStart() {
        super.onStart()
        videoId.value?.let {
            Probes.visibleVideo = it
            Probes.restored(this, it)
        }
    }

    override fun onStop() {
        if (Probes.visibleVideo == videoId.value) Probes.visibleVideo = null
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask: a second share (or tapping a "Ready" notification) switches the sheet to that video.
        val text = sharedText(intent)
        val id = YouTubeUrl.extractId(text)
        val list = YouTubeUrl.extractPlaylistId(text)
        when {
            id != null -> show(id, list)
            list != null -> {
                videoId.value?.let { Probes.cancel(this, it) }
                videoId.value = null
                showPlaylist(list)
            }
        }
    }

    private fun show(id: String, list: String? = null) {
        playlistId.value = list
        wholePlaylist.value = false
        videoId.value = id
        Probes.visibleVideo = id
        Probes.start(this, id, quick = quick, videoQuality = videoQuality)
        Probes.restored(this, id)
    }

    /** The playlist sheet, for a playlist link or "Download the whole playlist" on a video from one. */
    private fun showPlaylist(list: String) {
        // The video's own check isn't needed any more.
        videoId.value?.let { Probes.cancel(this, it) }
        playlistId.value = list
        wholePlaylist.value = true
        Playlists.start(this, list, videoId.value)
    }

    /** The playlist sheet has no notification to tuck into: closing it stops listing the videos. */
    private fun closePlaylist() {
        playlistId.value?.let(Playlists::cancel)
        finish()
    }

    /** Swiped down, Back, or tapped outside: keep checking, with a notification to come back to. */
    private fun minimize() {
        videoId.value?.let { Probes.minimize(this, it) }
        finish()
    }

    /** The ✕: stop checking and close. */
    private fun close() {
        videoId.value?.let { Probes.cancel(this, it) }
        finish()
    }

    private fun onQualityPicked(quality: Quality) = withNotifications { startDownload(quality) }

    private fun onPlaylistPicked(quality: Quality) = withNotifications { startPlaylist(quality) }

    /** Downloads report their progress in notifications, so ask for them first if needed. */
    private fun withNotifications(download: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
            pendingDownload = download
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            download()
        }
    }

    private fun notificationsAllowed() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun startPlaylist(quality: Quality) {
        val list = playlistId.value ?: return
        val state = Playlists.current(list)?.takeIf { it.entries.isNotEmpty() } ?: return
        lifecycleScope.launch {
            val count = yoink.repository.enqueuePlaylist(list, state.title, state.entries, quality)
            Playlists.cancel(list)
            Toast.makeText(
                applicationContext,
                resources.getQuantityString(R.plurals.playlist_started, count, count),
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    private fun startDownload(quality: Quality) {
        val id = videoId.value ?: return
        val state = Probes.current(id)
        lifecycleScope.launch {
            yoink.repository.enqueue(id, state?.title, state?.channel, quality)
            Probes.done(applicationContext, id)
            Toast.makeText(applicationContext, R.string.share_started, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sharedText(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }
}
