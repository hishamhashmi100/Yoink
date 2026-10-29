package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.hishamhashmi100.yoink.download.DownloadLog
import io.github.hishamhashmi100.yoink.download.DownloadWorker
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.download.MediaInfo
import io.github.hishamhashmi100.yoink.download.Notifications
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.QueueResumeWorker
import io.github.hishamhashmi100.yoink.download.Storage
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(private val context: Context, val dao: DownloadDao) {

    companion object {
        /** Playlist videos handed to the downloader at once; it runs two at a time. */
        private const val MAX_ACTIVE = 2

        /** How long waiting videos hold off after YouTube limits the connection. */
        const val PAUSE_MINUTES = 30L
        private const val KEY_PAUSED_UNTIL = "paused_until"
    }

    private val workManager = WorkManager.getInstance(context)
    private val prefs = context.getSharedPreferences("queue", Context.MODE_PRIVATE)

    /** Waiting videos are holding off because YouTube is limiting the connection (see [pauseQueue]). */
    val isPaused: Boolean get() = System.currentTimeMillis() < prefs.getLong(KEY_PAUSED_UNTIL, 0L)

    val downloads: Flow<List<Download>> = dao.observeAll()

    /** Live progress of running downloads, keyed by download id. */
    val progress: Flow<Map<Long, LiveProgress>> =
        workManager.getWorkInfosByTagFlow(DownloadWorker.TAG).map { infos ->
            infos.filter { it.state == WorkInfo.State.RUNNING }
                .mapNotNull { info ->
                    DownloadWorker.idFromTags(info.tags)?.let { it to DownloadWorker.readProgress(info.progress) }
                }
                .toMap()
        }

    suspend fun enqueue(videoId: String, title: String?, channel: String?, quality: Quality): Long {
        val id = dao.insert(
            Download(
                videoId = videoId,
                title = title ?: videoId,
                channel = channel,
                quality = quality,
                status = DownloadStatus.QUEUED,
            )
        )
        DownloadWorker.enqueue(context, id)
        return id
    }

    /**
     * Queues every video in a playlist. They wait in line and start a couple at a time (see [pump]), so a long
     * playlist doesn't start hundreds of downloads, or notifications, at once. Returns how many were queued.
     */
    suspend fun enqueuePlaylist(playlistId: String, playlistTitle: String?, entries: List<PlaylistEntry>, quality: Quality): Int {
        val now = System.currentTimeMillis()
        dao.insertAll(
            entries.mapIndexed { i, entry ->
                Download(
                    videoId = entry.videoId,
                    title = entry.title,
                    channel = entry.channel,
                    durationSec = entry.durationSec,
                    quality = quality,
                    status = DownloadStatus.WAITING,
                    // Keeps the playlist's order, in the list and in line.
                    createdAt = now + i,
                    playlistId = playlistId,
                    playlistTitle = playlistTitle,
                )
            }
        )
        pump()
        updatePlaylistNotice(playlistId)
        return entries.size
    }

    private val queueLock = Mutex()

    /** Hands waiting playlist videos to the downloader whenever fewer than [MAX_ACTIVE] are on their way. */
    suspend fun pump() = queueLock.withLock {
        if (isPaused) return@withLock
        val free = MAX_ACTIVE - dao.countWithStatus(listOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING))
        if (free <= 0) return@withLock
        for (next in dao.oldestWithStatus(DownloadStatus.WAITING, free)) {
            dao.setStatus(next.id, DownloadStatus.QUEUED)
            DownloadWorker.enqueue(context, next.id)
        }
    }

    /** The playlist's one notification: how many are saved so far, or that it's done. */
    suspend fun updatePlaylistNotice(playlistId: String) {
        Notifications.playlist(context, playlistId, dao.getByPlaylist(playlistId), paused = isPaused)
    }

    /**
     * YouTube is limiting this connection ("confirm you're not a bot", HTTP 429). Starting the next video right
     * away would fail too and only keep the limit going, so the waiting videos hold off for [PAUSE_MINUTES], then
     * carry on by themselves. A download that goes through in the meantime (a Retry) ends the pause early.
     */
    fun pauseQueue() {
        prefs.edit().putLong(KEY_PAUSED_UNTIL, System.currentTimeMillis() + PAUSE_MINUTES * 60_000).apply()
        QueueResumeWorker.schedule(context, PAUSE_MINUTES)
    }

    /** A download went through, so YouTube isn't limiting the connection (any more). */
    fun connectionWorks() {
        if (!prefs.contains(KEY_PAUSED_UNTIL)) return
        prefs.edit().remove(KEY_PAUSED_UNTIL).apply()
        QueueResumeWorker.cancel(context)
    }

    /** The pause is over: carry on with the waiting videos. */
    suspend fun resumeQueue() {
        prefs.edit().remove(KEY_PAUSED_UNTIL).apply()
        pump()
        dao.getByStatus(DownloadStatus.WAITING).mapNotNull { it.playlistId }.distinct().forEach { updatePlaylistNotice(it) }
    }

    suspend fun retry(id: Long) {
        dao.setStatus(id, DownloadStatus.QUEUED)
        DownloadWorker.enqueue(context, id)
    }

    /** Stops a queued or running download and forgets it. */
    suspend fun cancel(id: Long) {
        val playlistId = dao.get(id)?.playlistId
        // The row goes first: the worker treats a missing row as "cancelled by the user".
        dao.delete(id)
        workManager.cancelUniqueWork(DownloadWorker.workName(id))
        withContext(Dispatchers.IO) {
            DownloadWorker.jobDir(context, id).deleteRecursively()
            DownloadLog.delete(context, id)
        }
        pump()
        playlistId?.let { updatePlaylistNotice(it) }
    }

    /** Stops and forgets everything in the Downloading list (waiting, running and failed). The Library stays. */
    suspend fun cancelAll() {
        val unfinished = dao.getAllExcept(DownloadStatus.COMPLETED)
        dao.deleteAllExcept(DownloadStatus.COMPLETED)
        workManager.cancelAllWorkByTag(DownloadWorker.TAG)
        withContext(Dispatchers.IO) {
            unfinished.forEach {
                DownloadWorker.jobDir(context, it.id).deleteRecursively()
                DownloadLog.delete(context, it.id)
            }
        }
        unfinished.mapNotNull { it.playlistId }.distinct().forEach { Notifications.cancelPlaylist(context, it) }
    }

    /** Removes a finished download together with its file and thumbnail. */
    suspend fun delete(download: Download) {
        // Stop it first if it's in the player, otherwise it keeps playing from memory.
        AudioController.remove(context, download.id)
        withContext(Dispatchers.IO) {
            download.fileUri?.let { Storage.delete(context, it) }
            download.thumbnailPath?.let { File(it).delete() }
        }
        dao.delete(download.id)
        // And out of the user's playlists.
        context.yoink.database.playlistDao().removeDownload(download.id)
    }

    /** Fills in the resolution of videos downloaded before it was recorded. */
    suspend fun fillMissingResolutions() = withContext(Dispatchers.IO) {
        dao.getByStatus(DownloadStatus.COMPLETED)
            .filter { !it.quality.isAudio && it.resolution == null && it.fileUri != null }
            .forEach { download ->
                MediaInfo.read(context, download.fileUri!!)?.resolution?.let { dao.setResolution(download.id, it) }
            }
    }

    /** Marks rows whose work vanished (e.g. app data cleared mid-download) as failed. */
    suspend fun reconcile(interruptedMessage: String, createdBefore: Long) {
        val active = dao.getByStatus(DownloadStatus.QUEUED) + dao.getByStatus(DownloadStatus.DOWNLOADING)
        for (download in active.filter { it.createdAt < createdBefore }) {
            val infos = workManager.getWorkInfosForUniqueWorkFlow(DownloadWorker.workName(download.id)).first()
            if (infos.none { !it.state.isFinished }) {
                dao.setStatus(download.id, DownloadStatus.FAILED, interruptedMessage)
            }
        }
    }
}
