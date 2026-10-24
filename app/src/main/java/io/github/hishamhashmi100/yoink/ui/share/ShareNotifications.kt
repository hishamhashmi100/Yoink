package io.github.hishamhashmi100.yoink.ui.share

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.NotificationActionReceiver
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.YouTubeUrl

/** The share sheet, tucked away: "Checking…" while it works, then "Ready" with quick download buttons. */
object ShareNotifications {

    private fun id(videoId: String) = 2_000_000 + (videoId.hashCode() and 0xFFFFF)

    fun show(context: Context, state: ShareUiState, alert: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val videoId = state.videoId
        val title = state.title ?: context.getString(R.string.share_loading_title)
        val builder = if (state.loadingFormats) {
            NotificationCompat.Builder(context, Notifications.CHANNEL_PROGRESS)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.share_checking_tap))
                .setProgress(0, 0, true)
                .setSilent(true)
        } else {
            NotificationCompat.Builder(context, Notifications.CHANNEL_SHARE_READY)
                .setContentTitle(context.getString(R.string.share_ready))
                .setContentText(title)
                .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setSilent(!alert)
                .setAutoCancel(true)
                .apply {
                    if (Quality.BEST in state.available) {
                        addAction(0, Quality.BEST.label(context.resources), quick(context, state, Quality.BEST))
                    }
                    if (Quality.AUDIO in state.available) {
                        addAction(0, Quality.AUDIO.label(context.resources), quick(context, state, Quality.AUDIO))
                    }
                }
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(reopen(context, videoId))
            .setDeleteIntent(receiver(context, NotificationActionReceiver.ACTION_PROBE_CANCEL, videoId, null))
            .build()
        NotificationManagerCompat.from(context).notify(id(videoId), notification)
    }

    fun cancel(context: Context, videoId: String) {
        NotificationManagerCompat.from(context).cancel(id(videoId))
    }

    /** Opens the share sheet again; the results are already there. */
    private fun reopen(context: Context, videoId: String): PendingIntent {
        val intent = Intent(context, ShareActivity::class.java)
            .setAction(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, YouTubeUrl.watchUrl(videoId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, id(videoId), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun quick(context: Context, state: ShareUiState, quality: Quality): PendingIntent =
        receiver(context, NotificationActionReceiver.ACTION_QUICK_DOWNLOAD, state.videoId, quality)

    private fun receiver(context: Context, action: String, videoId: String, quality: Quality?): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_VIDEO, videoId)
            .putExtra(NotificationActionReceiver.EXTRA_QUALITY, quality?.name)
        val code = id(videoId) * 4 + (quality?.ordinal ?: 3)
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
