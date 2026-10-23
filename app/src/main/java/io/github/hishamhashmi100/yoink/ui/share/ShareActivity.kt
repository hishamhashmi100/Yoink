package io.github.hishamhashmi100.yoink.ui.share

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.YouTubeUrl
import io.github.hishamhashmi100.yoink.ui.theme.YoinkTheme
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives links from YouTube's Share menu (or youtube.com / youtu.be links) and shows a
 * quality picker over the calling app. Picking a quality queues the download and closes.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()
    private var pendingQuality: Quality? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            pendingQuality?.let(::startDownload)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId = YouTubeUrl.extractId(sharedText(intent))
        if (videoId == null) {
            Toast.makeText(this, R.string.share_not_youtube, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val settings = yoink.settings.settings.first()
            val defaultQuality = settings.defaultQuality
            if (defaultQuality != null && savedInstanceState == null) {
                viewModel.load(videoId)
                onQualityPicked(defaultQuality)
                return@launch
            }
            viewModel.load(videoId)
            setContent {
                YoinkTheme(settings) {
                    val state = viewModel.state.collectAsStateWithLifecycle().value
                    ShareSheet(state = state, onPick = ::onQualityPicked, onDismiss = ::finish)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask: a second share while the sheet is open replaces the video.
        YouTubeUrl.extractId(sharedText(intent))?.let(viewModel::load)
    }

    private fun onQualityPicked(quality: Quality) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingQuality = quality
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startDownload(quality)
        }
    }

    private fun startDownload(quality: Quality) {
        pendingQuality = null
        val state = viewModel.state.value
        lifecycleScope.launch {
            yoink.repository.enqueue(state.videoId, state.title, state.channel, quality)
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
