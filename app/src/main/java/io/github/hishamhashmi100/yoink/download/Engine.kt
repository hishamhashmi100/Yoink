package io.github.hishamhashmi100.yoink.download

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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
import java.util.UUID

/**
 * Wraps youtubedl-android: one-time init, running yt-dlp as a cancellable child process,
 * and updating yt-dlp without replacing it under a running process.
 */
object Engine {

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
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
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

    suspend fun version(context: Context): String? {
        ensureReady(context)
        return withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().versionName(context.applicationContext)
                ?: YoutubeDL.getInstance().version(context.applicationContext)
        }
    }
}
