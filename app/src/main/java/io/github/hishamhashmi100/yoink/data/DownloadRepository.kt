package io.github.hishamhashmi100.yoink.data

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.hishamhashmi100.yoink.download.DownloadLog
import io.github.hishamhashmi100.yoink.download.DownloadWorker
import io.github.hishamhashmi100.yoink.download.LiveProgress
import io.github.hishamhashmi100.yoink.download.MediaInfo
import io.github.hishamhashmi100.yoink.download.Quality
import io.github.hishamhashmi100.yoink.download.Storage
import io.github.hishamhashmi100.yoink.ui.player.AudioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(private val context: Context, val dao: DownloadDao) {

    private val workManager = WorkManager.getInstance(context)

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

    suspend fun retry(id: Long) {
        dao.setStatus(id, DownloadStatus.QUEUED)
        DownloadWorker.enqueue(context, id)
    }

    /** Stops a queued or running download and forgets it. */
    suspend fun cancel(id: Long) {
        // The row goes first: the worker treats a missing row as "cancelled by the user".
        dao.delete(id)
        workManager.cancelUniqueWork(DownloadWorker.workName(id))
        withContext(Dispatchers.IO) {
            DownloadWorker.jobDir(context, id).deleteRecursively()
            DownloadLog.delete(context, id)
        }
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
