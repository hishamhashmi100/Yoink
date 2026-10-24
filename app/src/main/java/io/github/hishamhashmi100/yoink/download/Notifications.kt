package io.github.hishamhashmi100.yoink.download

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.ui.MainActivity
import io.github.hishamhashmi100.yoink.ui.home.FileActions
import io.github.hishamhashmi100.yoink.ui.home.HomeTab
import io.github.hishamhashmi100.yoink.ui.player.PlayerActivity

object Notifications {

    const val CHANNEL_PROGRESS = "progress"
    const val CHANNEL_SHARE_READY = "share_ready"
    private const val CHANNEL_STATUS = "status"
    private const val CHANNEL_UPDATES = "updates"
    private const val UPDATE_ID = 999

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
                NotificationChannel(
                    CHANNEL_SHARE_READY,
                    context.getString(R.string.channel_share_ready),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
                NotificationChannel(
                    CHANNEL_UPDATES,
                    context.getString(R.string.channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        )
    }

    fun progressId(downloadId: Long): Int = 1_000 + (downloadId % 1_000_000).toInt() * 2
    private fun resultId(downloadId: Long): Int = progressId(downloadId) + 1

    fun progress(context: Context, downloadId: Long, title: String, progress: LiveProgress, thumb: Bitmap? = null): Notification {
        val detail = buildList {
            if (progress.percent >= 0) add("${progress.percent.toInt()}%")
            progress.speed?.let(::add)
            if (progress.etaSeconds > 0) add(context.getString(R.string.eta_left, formatDuration(progress.etaSeconds)))
        }.joinToString(" · ").ifEmpty { progress.stage }

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setSubText(progress.stage)
            .setLargeIcon(thumb)
            // Android only colourises ongoing (foreground) notifications like this one: a full Yoink-red card.
            .setColor(yoinkRed(context))
            .setColorized(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progress.percent.coerceAtLeast(0f).toInt(), progress.percent < 0)
            .setContentIntent(openDownloads(context, downloadId))
            .addAction(0, context.getString(R.string.action_cancel), action(context, NotificationActionReceiver.ACTION_CANCEL, downloadId))
            .build()
    }

    fun showProgress(context: Context, downloadId: Long, title: String, progress: LiveProgress, thumb: Bitmap? = null) =
        post(context, progressId(downloadId), progress(context, downloadId, title, progress, thumb))

    fun completed(context: Context, download: Download) {
        val thumb = NotificationThumbs.cached(context, download.videoId, download.thumbnailPath)
        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(yoinkRed(context))
            .setContentTitle(context.getString(R.string.notification_done))
            .setContentText(download.title)
            .setLargeIcon(thumb)
            .setAutoCancel(true)
            .setContentIntent(openPlayer(context, download.id))
            .addAction(0, context.getString(R.string.action_play), openPlayer(context, download.id))
        if (thumb != null) {
            // Expanded, the notification shows the thumbnail big.
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumb)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(download.title)
            )
        }
        FileActions.shareChooser(context, download)?.let { share ->
            val pending = PendingIntent.getActivity(
                context,
                resultId(download.id) * 2 + 1,
                share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, context.getString(R.string.action_share), pending)
        }
        post(context, resultId(download.id), builder.build())
    }

    fun failed(context: Context, download: Download, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(yoinkRed(context))
            .setLargeIcon(NotificationThumbs.cached(context, download.videoId, download.thumbnailPath))
            .setContentTitle(context.getString(R.string.notification_failed))
            .setContentText(download.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${download.title}\n$message"))
            .setAutoCancel(true)
            .setContentIntent(openDownloads(context, download.id))
            .addAction(0, context.getString(R.string.action_retry), action(context, NotificationActionReceiver.ACTION_RETRY, download.id))
            .build()
        post(context, resultId(download.id), notification)
    }

    fun updateAvailable(context: Context, version: String) {
        val open = PendingIntent.getActivity(
            context,
            UPDATE_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(yoinkRed(context))
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText(context.getString(R.string.update_available_text, version))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        post(context, UPDATE_ID, notification)
    }

    fun yoinkRed(context: Context): Int = ContextCompat.getColor(context, R.color.yoink_red)

    fun cancelResult(context: Context, downloadId: Long) {
        NotificationManagerCompat.from(context).cancel(resultId(downloadId))
    }

    private fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openDownloads(context: Context, downloadId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            progressId(downloadId),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_TAB, HomeTab.DOWNLOADING.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openPlayer(context: Context, downloadId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            resultId(downloadId),
            PlayerActivity.intent(context, downloadId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

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
