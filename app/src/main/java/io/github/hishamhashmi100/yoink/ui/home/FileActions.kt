package io.github.hishamhashmi100.yoink.ui.home

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import io.github.hishamhashmi100.yoink.download.DownloadLog
import java.io.File

/** Share and "open with" for library items, via FileProvider for files in app storage. */
object FileActions {

    fun share(context: Context, download: Download) {
        if (contentUri(context, download) == null) return // Shows "file missing".
        shareChooser(context, download)?.let { start(context, it) }
    }

    /** The system share sheet for a download, or null if its file is gone. Also used by the "Yoinked!" notification. */
    fun shareChooser(context: Context, download: Download): Intent? {
        val uri = contentUri(context, download, quiet = true) ?: return null
        val send = Intent(Intent.ACTION_SEND)
            .setType(download.mimeType ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, download.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, download.title)
    }

    fun openWith(context: Context, download: Download) {
        val uri = contentUri(context, download) ?: return
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, download.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(context, Intent.createChooser(view, context.getString(R.string.action_open_with)))
    }

    private fun contentUri(context: Context, download: Download, quiet: Boolean = false): Uri? {
        val uri = download.fileUri?.let(Uri::parse)
        if (uri == null) {
            if (!quiet) Toast.makeText(context, R.string.file_missing, Toast.LENGTH_SHORT).show()
            return null
        }
        if (uri.scheme != "file") return uri
        val file = File(uri.path.orEmpty())
        if (!file.exists()) {
            if (!quiet) Toast.makeText(context, R.string.file_missing, Toast.LENGTH_SHORT).show()
            return null
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    /** Copies yt-dlp's notes for a download, so they can be pasted into a bug report. */
    fun copyDetails(context: Context, downloadId: Long) {
        val details = DownloadLog.read(context, downloadId)
        if (details == null) {
            Toast.makeText(context, R.string.no_details, Toast.LENGTH_SHORT).show()
            return
        }
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Yoink details", details))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }
}
