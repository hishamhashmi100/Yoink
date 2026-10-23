package io.github.hishamhashmi100.yoink.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.launch

/** Handles the Cancel and Retry buttons on download notifications. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val app = context.yoink
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

    companion object {
        const val ACTION_CANCEL = "io.github.hishamhashmi100.yoink.action.CANCEL"
        const val ACTION_RETRY = "io.github.hishamhashmi100.yoink.action.RETRY"
        const val EXTRA_ID = "download_id"
    }
}
