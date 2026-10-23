package io.github.hishamhashmi100.yoink.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Moves finished files out of the cache into app storage or the shared gallery folders. */
object Storage {

    data class Saved(val uri: String, val mimeType: String, val sizeBytes: Long)

    fun save(context: Context, source: File, title: String, audio: Boolean, toGallery: Boolean): Saved {
        val ext = source.extension.lowercase()
        val mime = mimeType(ext, audio)
        val name = "${safeName(title)}.$ext"
        val size = source.length()
        val uri = if (toGallery) {
            saveToMediaStore(context, source, name, mime, audio)
        } else {
            saveToAppFolder(context, source, name, audio)
        }
        source.delete()
        return Saved(uri, mime, size)
    }

    fun delete(context: Context, uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() }
        } else {
            // Fails harmlessly if the item was removed in the gallery, or isn't ours after a reinstall.
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    private fun saveToAppFolder(context: Context, source: File, name: String, audio: Boolean): String {
        val dir = context.getExternalFilesDir(if (audio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "media")
        dir.mkdirs()
        val dest = uniqueFile(dir, name)
        if (!source.renameTo(dest)) source.copyTo(dest, overwrite = true)
        return Uri.fromFile(dest).toString()
    }

    // Pending-item pattern: the gallery never indexes a half-written file.
    private fun saveToMediaStore(context: Context, source: File, name: String, mime: String, audio: Boolean): String {
        val resolver = context.contentResolver
        val collection = if (audio) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val folder = if (audio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/Yoink")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Couldn't create a gallery item")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("Couldn't write to the gallery")
            output.use { out -> source.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri.toString()
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        var n = 1
        while (file.exists()) {
            file = File(dir, "${name.substringBeforeLast('.')} ($n).${name.substringAfterLast('.')}")
            n++
        }
        return file
    }

    private fun safeName(title: String): String =
        title.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_").trim().trim('.').take(120).ifEmpty { "video" }

    fun mimeType(ext: String, audio: Boolean): String = when (ext) {
        "mp4" -> "video/mp4"
        "webm" -> if (audio) "audio/webm" else "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus", "ogg" -> "audio/ogg"
        else -> if (audio) "audio/*" else "video/*"
    }
}
