package io.github.hishamhashmi100.yoink.download

import android.content.Context
import android.os.Build
import io.github.hishamhashmi100.yoink.data.Download
import java.io.File

/** What yt-dlp printed for a failed download, for the "Copy details" button. */
object DownloadLog {

    private fun file(context: Context, id: Long) = File(context.filesDir, "logs/$id.txt")

    fun write(context: Context, download: Download, lines: List<String>, error: Throwable) {
        val app = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val text = buildString {
            appendLine("Yoink $app · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Video ${download.videoId} · ${download.quality.name}")
            appendLine()
            lines.forEach(::appendLine)
            appendLine()
            appendLine(error.toString())
            generateSequence(error.cause) { it.cause }.forEach { appendLine("Caused by: $it") }
        }
        file(context, download.id).apply { parentFile?.mkdirs() }.writeText(text.take(20_000))
    }

    fun read(context: Context, id: Long): String? = file(context, id).takeIf { it.exists() }?.readText()

    fun delete(context: Context, id: Long) {
        file(context, id).delete()
    }
}
