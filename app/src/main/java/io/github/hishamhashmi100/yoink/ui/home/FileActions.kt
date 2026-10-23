package io.github.hishamhashmi100.yoink.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.data.Download
import java.io.File

/** Share and "open with" for library items, via FileProvider for files in app storage. */
object FileActions {

    fun share(context: Context, download: Download) {
        val uri = contentUri(context, download) ?: return
        val send = Intent(Intent.ACTION_SEND)
            .setType(download.mimeType ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TITLE, download.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(context, Intent.createChooser(send, download.title))
    }

    fun openWith(context: Context, download: Download) {
        val uri = contentUri(context, download) ?: return
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, download.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(context, Intent.createChooser(view, context.getString(R.string.action_open_with)))
    }

    private fun contentUri(context: Context, download: Download): Uri? {
        val uri = download.fileUri?.let(Uri::parse)
        if (uri == null) {
            Toast.makeText(context, R.string.file_missing, Toast.LENGTH_SHORT).show()
            return null
        }
        if (uri.scheme != "file") return uri
        val file = File(uri.path.orEmpty())
        if (!file.exists()) {
            Toast.makeText(context, R.string.file_missing, Toast.LENGTH_SHORT).show()
            return null
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
        }
    }
}
