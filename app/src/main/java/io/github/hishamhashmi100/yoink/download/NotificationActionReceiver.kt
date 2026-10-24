package io.github.hishamhashmi100.yoink.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.YoinkApp
import io.github.hishamhashmi100.yoink.ui.share.Probes
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Handles the Cancel and Retry buttons on download notifications. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.yoink
        val videoId = intent.getStringExtra(EXTRA_VIDEO)
        if (videoId != null) {
            handleShare(app, intent, videoId)
            return
        }
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_CANCEL -> app.repository.cancel(id)
                    ACTION_RETRY -> {
                        Notifications.cancelResult(app, id)
                        app.repository.retry(id)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Buttons on the "Ready" notification from a tucked-away share sheet. */
    private fun handleShare(app: YoinkApp, intent: Intent, videoId: String) {
        when (intent.action) {
            ACTION_PROBE_CANCEL -> Probes.cancel(app, videoId)
            ACTION_QUICK_DOWNLOAD -> {
                val quality = Quality.fromName(intent.getStringExtra(EXTRA_QUALITY)) ?: return
                val state = Probes.current(videoId)
                val pending = goAsync()
                app.appScope.launch {
                    try {
                        app.repository.enqueue(videoId, state?.title, state?.channel, quality)
                        Probes.done(app, videoId)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(app, R.string.share_started, Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_CANCEL = "io.github.hishamhashmi100.yoink.action.CANCEL"
        const val ACTION_QUICK_DOWNLOAD = "io.github.hishamhashmi100.yoink.action.QUICK_DOWNLOAD"
        const val ACTION_PROBE_CANCEL = "io.github.hishamhashmi100.yoink.action.PROBE_CANCEL"
        const val EXTRA_VIDEO = "video_id"
        const val EXTRA_QUALITY = "quality"
        const val ACTION_RETRY = "io.github.hishamhashmi100.yoink.action.RETRY"
        const val EXTRA_ID = "download_id"
    }
}
