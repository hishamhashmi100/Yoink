package io.github.hishamhashmi100.yoink.download

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Wraps youtubedl-android: one-time init, running yt-dlp as a cancellable child process,
 * and updating yt-dlp without replacing it under a running process.
 */
object Engine {

    /** Marks errors from starting the downloader itself, so they get their own message. */
    const val ENGINE_FAILED = "Downloader failed to start"

    private val initLock = Mutex()
    @Volatile private var initialized = false

    // Updates take this lock and wait until nothing is running; new runs wait for the update.
    private val updateLock = Mutex()
    private val running = MutableStateFlow(0)

    suspend fun ensureReady(context: Context) {
        if (initialized) return
        initLock.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().init(context.applicationContext)
                    FFmpeg.getInstance().init(context.applicationContext)
                } catch (e: Throwable) {
                    // Includes Errors (e.g. a class that failed to load), which would otherwise crash the app.
                    throw YoutubeDLException("$ENGINE_FAILED: ${e.message ?: e.javaClass.simpleName}", e)
                }
            }
            initialized = true
        }
    }

    /**
     * Runs yt-dlp. Cancelling the calling coroutine kills the process.
     * The library adds `--js-runtimes quickjs:…` and `--ffmpeg-location` by itself.
     */
    suspend fun execute(
        context: Context,
        request: YoutubeDLRequest,
        onLine: ((progress: Float, etaSeconds: Long, line: String) -> Unit)? = null,
    ): YoutubeDLResponse {
        ensureReady(context)
        updateLock.withLock { running.update { it + 1 } }
        try {
            return withContext(Dispatchers.IO) {
                val processId = UUID.randomUUID().toString()
                val killOnCancel = coroutineContext[Job]!!.invokeOnCompletion { cause ->
                    if (cause != null) YoutubeDL.getInstance().destroyProcessById(processId)
                }
                try {
                    runInterruptible {
                        YoutubeDL.getInstance().execute(request, processId) { progress, eta, line ->
                            onLine?.invoke(progress, eta, line)
                        }
                    }
                } finally {
                    killOnCancel.dispose()
                }
            }
        } finally {
            running.update { it - 1 }
        }
    }

    /** Pulls the latest stable yt-dlp. Returns true if a new version was installed. */
    suspend fun update(context: Context): Boolean {
        ensureReady(context)
        return updateLock.withLock {
            running.first { it == 0 }
            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().updateYoutubeDL(
                    context.applicationContext,
                    YoutubeDL.UpdateChannel.STABLE,
                ) == YoutubeDL.UpdateStatus.DONE
            }
        }
    }

    @Volatile private var freshnessChecked = false

    /**
     * The yt-dlp bundled in the APK can be many months old by the time someone installs the app, and YouTube
     * breaks old versions (they may get only part of a video). Before the first download or quality check in
     * each app session, update it if it's more than 30 days old. Failures are ignored; the daily job retries.
     */
    suspend fun ensureFresh(context: Context) {
        if (freshnessChecked) return
        freshnessChecked = true
        val released = runCatching { version(context) }.getOrNull()
            ?.let { runCatching { LocalDate.parse(it.take(10), DateTimeFormatter.ofPattern("yyyy.MM.dd")) }.getOrNull() }
            ?: return
        if (ChronoUnit.DAYS.between(released, LocalDate.now()) > 30) {
            runCatching { update(context) }
        }
    }

    suspend fun version(context: Context): String? {
        ensureReady(context)
        return withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().versionName(context.applicationContext)
                ?: YoutubeDL.getInstance().version(context.applicationContext)
        }
    }
}
