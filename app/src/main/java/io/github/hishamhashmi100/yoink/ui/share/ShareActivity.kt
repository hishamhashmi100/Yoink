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
 */
class ShareActivity : ComponentActivity() {

    private val videoId = MutableStateFlow<String?>(null)
    private var quick = false
    private var videoQuality = Quality.BEST
    private var pendingQuality: Quality? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pendingQuality?.let(::startDownload)
        }

    // Notifications let the sheet be tucked away and come back when the qualities are ready.
    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = YouTubeUrl.extractId(sharedText(intent))
        if (id == null) {
            Toast.makeText(this, R.string.share_not_youtube, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val settings = yoink.settings.settings.first()
            videoQuality = settings.videoQuality
            // "Right away" modes skip the sheet entirely.
            val immediate = when (settings.shareMode) {
                ShareMode.VIDEO -> settings.videoQuality
                ShareMode.AUDIO -> Quality.AUDIO
                else -> null
            }
            if (immediate != null && savedInstanceState == null) {
                videoId.value = id
                onQualityPicked(immediate)
                return@launch
            }
            quick = settings.shareMode == ShareMode.VIDEO_OR_AUDIO
            show(id)
            if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
                askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            blurBehind(settings.appearance.glass)
            setContent {
                // No backdrop: YouTube (blurred, with glass on) shows through behind the sheet.
                YoinkTheme(settings, backdrop = false) {
                    val current = videoId.collectAsStateWithLifecycle().value ?: return@YoinkTheme
                    val state = remember(current) { Probes.state(current) }
                        .collectAsStateWithLifecycle(ShareUiState(videoId = current)).value
                    ShareSheet(
                        state = state,
                        videoQuality = videoQuality,
                        onPick = ::onQualityPicked,
                        onMinimize = ::minimize,
                        onClose = ::close,
                    )
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
        YouTubeUrl.extractId(sharedText(intent))?.let(::show)
    }

    private fun show(id: String) {
        videoId.value = id
        Probes.visibleVideo = id
        Probes.start(this, id, quick = quick, videoQuality = videoQuality)
        Probes.restored(this, id)
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

    private fun onQualityPicked(quality: Quality) {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed()) {
            pendingQuality = quality
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startDownload(quality)
        }
    }

    private fun notificationsAllowed() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun startDownload(quality: Quality) {
        pendingQuality = null
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
