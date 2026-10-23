package io.github.hishamhashmi100.yoink.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.ui.MainActivity

object Notifications {

    private const val CHANNEL_PROGRESS = "progress"
    private const val CHANNEL_STATUS = "status"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    context.getString(R.string.channel_progress),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
                NotificationChannel(
                    CHANNEL_STATUS,
                    context.getString(R.string.channel_status),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        )
    }

    fun progressId(downloadId: Long): Int = 1_000 + (downloadId % 1_000_000).toInt() * 2
    private fun resultId(downloadId: Long): Int = progressId(downloadId) + 1

    fun progress(context: Context, downloadId: Long, title: String, progress: LiveProgress): Notification {
        val detail = buildList {
            add(progress.stage)
            if (progress.percent >= 0) add("${progress.percent.toInt()}%")
            progress.speed?.let(::add)
            if (progress.etaSeconds > 0) add(context.getString(R.string.eta_left, formatDuration(progress.etaSeconds)))
        }.joinToString(" · ")

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progress.percent.coerceAtLeast(0f).toInt(), progress.percent < 0)
            .setContentIntent(openApp(context, downloadId, play = false))
            .addAction(0, context.getString(R.string.action_cancel), action(context, NotificationActionReceiver.ACTION_CANCEL, downloadId))
            .build()
    }

    fun showProgress(context: Context, downloadId: Long, title: String, progress: LiveProgress) =
        post(context, progressId(downloadId), progress(context, downloadId, title, progress))

    fun completed(context: Context, download: Download) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(download.title)
            .setContentText(context.getString(R.string.notification_done))
            .setAutoCancel(true)
            .setContentIntent(openApp(context, download.id, play = true))
            .build()
        post(context, resultId(download.id), notification)
    }

    fun failed(context: Context, download: Download, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_failed))
            .setContentText(download.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${download.title}\n$message"))
            .setAutoCancel(true)
            .setContentIntent(openApp(context, download.id, play = false))
            .addAction(0, context.getString(R.string.action_retry), action(context, NotificationActionReceiver.ACTION_RETRY, download.id))
            .build()
        post(context, resultId(download.id), notification)
    }

    fun cancelResult(context: Context, downloadId: Long) {
        NotificationManagerCompat.from(context).cancel(resultId(downloadId))
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openApp(context: Context, downloadId: Long, play: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (play) intent.putExtra(MainActivity.EXTRA_PLAY_DOWNLOAD, downloadId)
        return PendingIntent.getActivity(
            context,
            resultId(downloadId) * 2 + if (play) 1 else 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun action(context: Context, action: String, downloadId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            progressId(downloadId),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(NotificationActionReceiver.EXTRA_ID, downloadId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
