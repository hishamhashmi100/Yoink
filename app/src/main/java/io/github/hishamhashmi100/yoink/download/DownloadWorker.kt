package io.github.hishamhashmi100.yoink.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.data.DownloadStatus
import io.github.hishamhashmi100.yoink.yoink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** The file is much shorter than the video on YouTube. */
class IncompleteDownloadException(val gotSec: Long, val expectedSec: Long) :
    Exception("Incomplete download: got ${gotSec}s of ${expectedSec}s")

data class LiveProgress(
    val stage: String,
    /** 0–100, or negative when unknown (merging, converting). */
    val percent: Float,
    val etaSeconds: Long,
    val speed: String?,
)

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val app = context.yoink
    private val dao = app.repository.dao
    private val downloadId = inputData.getLong(KEY_ID, -1L)

    @Volatile private var thumb: Bitmap? = null

    // The last lines yt-dlp printed, saved with a failed download for "Copy details".
    private val log = ArrayDeque<String>()

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foregroundInfo(app.getString(R.string.app_name), queued())

    override suspend fun doWork(): Result {
        val download = dao.get(downloadId) ?: return Result.failure()
        val workDir = jobDir(applicationContext, downloadId)
        setForeground(foregroundInfo(download.title, queued()))
        setProgress(toData(queued()))
        thumb = withContext(Dispatchers.IO) { NotificationThumbs.load(applicationContext, download.videoId) }

        return try {
            val finished = slots.withPermit { download(download, workDir) }
            runCatching { DownloadLog.write(applicationContext, finished, synchronized(log) { log.toList() }, null) }
            Result.success()
        } catch (e: Throwable) {
            if (e is CancellationException || isStopped) {
                withContext(NonCancellable) { onStopped(workDir) }
                throw if (e is CancellationException) e else CancellationException("Stopped")
            }
            val message = DownloadErrors.readable(applicationContext, e)
            val lines = synchronized(log) { log.toList() }
            runCatching { DownloadLog.write(applicationContext, download, lines, e) }
            dao.setStatus(downloadId, DownloadStatus.FAILED, message)
            Notifications.failed(applicationContext, download, message)
            Result.failure()
        }
    }

    private suspend fun onStopped(workDir: File) {
        if (dao.get(downloadId) == null) {
            // Cancelled by the user: the repository already removed the row.
            workDir.deleteRecursively()
        } else {
            // Stopped by the system; WorkManager runs it again and yt-dlp resumes from the .part files.
            dao.setStatus(downloadId, DownloadStatus.QUEUED)
        }
    }

    private suspend fun download(download: Download, workDir: File): Download {
        dao.setStatus(downloadId, DownloadStatus.DOWNLOADING)
        withContext(Dispatchers.IO) { workDir.mkdirs() }
        Engine.ensureFresh(applicationContext)

        val res = applicationContext.resources
        var stage = res.getString(R.string.stage_starting)
        var destinations = 0
        val live = MutableStateFlow(LiveProgress(stage, -1f, -1L, null))

        val request = YoutubeDLRequest(YouTubeUrl.watchUrl(download.videoId)).apply {
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--http-chunk-size", "10M")
            addOption("--write-thumbnail")
            addOption("--convert-thumbnails", "jpg")
            addOption("--write-info-json")
            addOption("-o", File(workDir, "media.%(ext)s").absolutePath)
            addOption("-o", "thumbnail:" + File(workDir, "thumb.%(ext)s").absolutePath)
            addOption("-o", "infojson:" + File(workDir, "info").absolutePath)
            download.quality.applyTo(this)
        }

        coroutineScope {
            // Notifications are rate-limited by the system, so report at most once a second.
            val reporter = launch {
                var last: LiveProgress? = null
                while (isActive) {
                    val current = live.value
                    if (current != last) {
                        report(download.title, current)
                        last = current
                    }
                    delay(1_000)
                }
            }
            try {
                val response = Engine.execute(applicationContext, request) { progress, eta, line ->
                    // Progress lines repeat many times a second; keep the log to the lines that say something.
                    if (!line.startsWith("[download]") || "Destination" in line || "100%" in line || "ERROR" in line) {
                        synchronized(log) {
                            log.addLast(line)
                            while (log.size > 80) log.removeFirst()
                        }
                    }
                    when {
                        line.startsWith("[download] Destination:") -> {
                            destinations++
                            stage = res.getString(
                                if (download.quality.isAudio || destinations > 1) R.string.stage_audio else R.string.stage_video
                            )
                        }
                        line.startsWith("[Merger]") -> stage = res.getString(R.string.stage_merging)
                        line.startsWith("[ExtractAudio]") -> stage = res.getString(R.string.stage_converting)
                    }
                    val downloading = line.startsWith("[download]")
                    live.value = LiveProgress(
                        stage = stage,
                        percent = if (downloading) progress else -1f,
                        etaSeconds = if (downloading) eta else -1L,
                        speed = if (downloading) SPEED.find(line)?.groupValues?.get(1) ?: live.value.speed else null,
                    )
                }
                // yt-dlp prints its warnings (e.g. formats YouTube held back) on stderr.
                val warnings = response.err.lineSequence().filter { "WARNING" in it }.take(30).toList()
                synchronized(log) {
                    log.addAll(warnings)
                    while (log.size > 110) log.removeFirst()
                }
            } finally {
                reporter.cancel()
            }
        }

        report(download.title, LiveProgress(res.getString(R.string.stage_saving), -1f, -1L, null))
        val saved = withContext(Dispatchers.IO) { finish(download, workDir) }
        val finished = saved.copy(error = qualityNote(saved))
        dao.update(finished)
        Notifications.completed(applicationContext, finished)
        return finished
    }

    /**
     * When a video came out well below the quality asked for, says so, and whether YouTube held the higher
     * qualities back (yt-dlp warns about missing formats) or the video simply doesn't have them.
     */
    private fun qualityNote(download: Download): String? {
        if (download.quality.isAudio) return null
        val got = download.resolution ?: return null
        val wanted = download.quality.height ?: 720 // "Best" should be at least HD for most videos.
        if (got >= wanted * 0.9) return null
        val heldBack = synchronized(log) { log.any { HELD_BACK.containsMatchIn(it) } }
        return applicationContext.getString(
            if (heldBack) R.string.note_held_back else R.string.note_best_available,
            MediaInfo.label(got),
        )
    }

    private suspend fun finish(download: Download, workDir: File): Download {
        val files = workDir.listFiles().orEmpty().filter { it.isFile }
        val media = files
            .filter { it.extension.lowercase() in MEDIA_EXTENSIONS && it.name.startsWith("media.") }
            .maxByOrNull { it.length() }
            ?: throw IllegalStateException(applicationContext.getString(R.string.error_generic))

        val info = files.firstOrNull { it.name.endsWith(".info.json") }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
        val title = info?.optString("title")?.takeIf { it.isNotBlank() } ?: download.title
        val channel = info?.let { it.optString("channel").ifBlank { it.optString("uploader") } }?.takeIf { it.isNotBlank() }
            ?: download.channel
        val duration = info?.optDouble("duration")?.takeUnless { it.isNaN() }?.toLong() ?: download.durationSec

        // YouTube sometimes sends only the start of a stream. Don't save that as a finished download.
        val mediaInfo = MediaInfo.read(applicationContext, media.absolutePath)
        val actual = mediaInfo?.durationSec
        if (duration != null && actual != null && duration >= 15 && actual < duration * 0.9) {
            workDir.deleteRecursively() // Otherwise a retry would find the short file and call it done.
            throw IncompleteDownloadException(actual, duration)
        }

        val thumbnail = files.firstOrNull { it.name.startsWith("thumb.") }?.let { thumb ->
            val dest = File(applicationContext.filesDir, "thumbs/${download.id}.jpg")
            dest.parentFile?.mkdirs()
            thumb.copyTo(dest, overwrite = true).absolutePath
        }

        val settings = app.settings.settings.first()
        val saved = Storage.save(applicationContext, media, title, download.quality.isAudio, settings.saveToGallery)
        workDir.deleteRecursively()

        return download.copy(
            title = title,
            channel = channel,
            durationSec = duration,
            thumbnailPath = thumbnail,
            fileUri = saved.uri,
            mimeType = saved.mimeType,
            sizeBytes = saved.sizeBytes,
            resolution = mediaInfo?.resolution?.takeUnless { download.quality.isAudio },
            status = DownloadStatus.COMPLETED,
            error = null,
        )
    }

    private suspend fun report(title: String, progress: LiveProgress) {
        setProgress(toData(progress))
        Notifications.showProgress(applicationContext, downloadId, title, progress, thumb)
    }

    private fun queued() = LiveProgress(applicationContext.getString(R.string.stage_queued), -1f, -1L, null)

    private fun foregroundInfo(title: String, progress: LiveProgress) = ForegroundInfo(
        Notifications.progressId(downloadId),
        Notifications.progress(applicationContext, downloadId, title, progress, thumb),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    companion object {
        const val TAG = "download"
        private const val KEY_ID = "id"
        private const val KEY_STAGE = "stage"
        private const val KEY_PERCENT = "percent"
        private const val KEY_ETA = "eta"
        private const val KEY_SPEED = "speed"
        private const val ID_TAG_PREFIX = "download-id:"

        private val MEDIA_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "mp3", "m4a", "opus", "ogg")
        // yt-dlp's warnings when YouTube withholds formats (JS challenge, PO token, SABR, missing URLs).
        private val HELD_BACK = Regex("challenge|PO Token|formats? (may be|are) missing|missing a URL|SABR|skipped", RegexOption.IGNORE_CASE)

        private val SPEED = Regex("""at\s+(~?\s*[\d.]+\s*[KMGT]?i?B/s)""")

        /** At most two downloads run at once; the rest wait here, shown as "Queued". */
        private val slots = Semaphore(2)

        fun workName(id: Long) = "download-$id"

        fun jobDir(context: Context, id: Long) = File(context.cacheDir, "jobs/$id")

        fun idFromTags(tags: Set<String>): Long? =
            tags.firstOrNull { it.startsWith(ID_TAG_PREFIX) }?.removePrefix(ID_TAG_PREFIX)?.toLongOrNull()

        fun enqueue(context: Context, id: Long) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_ID to id))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(TAG)
                .addTag(ID_TAG_PREFIX + id)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
        }

        fun readProgress(data: Data) = LiveProgress(
            stage = data.getString(KEY_STAGE).orEmpty(),
            percent = data.getFloat(KEY_PERCENT, -1f),
            etaSeconds = data.getLong(KEY_ETA, -1L),
            speed = data.getString(KEY_SPEED),
        )

        private fun toData(p: LiveProgress) = workDataOf(
            KEY_STAGE to p.stage,
            KEY_PERCENT to p.percent,
            KEY_ETA to p.etaSeconds,
            KEY_SPEED to p.speed,
        )
    }
}
