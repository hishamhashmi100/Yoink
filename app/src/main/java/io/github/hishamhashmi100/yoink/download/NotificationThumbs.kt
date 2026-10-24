package io.github.hishamhashmi100.yoink.download

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Video thumbnails for notifications: the saved file if there is one, otherwise YouTube's, cached. */
object NotificationThumbs {

    private val memory = LruCache<String, Bitmap>(12)

    /** May download the thumbnail, so call it off the main thread. */
    fun load(context: Context, videoId: String, localPath: String? = null): Bitmap? =
        cached(context, videoId, localPath) ?: runCatching { fetch(context, videoId) }.getOrNull()?.let { decode(videoId, it) }

    /** Only what's already on the phone; never touches the network. */
    fun cached(context: Context, videoId: String, localPath: String? = null): Bitmap? {
        memory.get(videoId)?.let { return it }
        val file = localPath?.let(::File)?.takeIf { it.exists() } ?: cacheFile(context, videoId).takeIf { it.exists() }
        return file?.let { decode(videoId, it) }
    }

    private fun cacheFile(context: Context, videoId: String) = File(context.cacheDir, "notification-thumbs/$videoId.jpg")

    private fun fetch(context: Context, videoId: String): File {
        val dest = cacheFile(context, videoId).apply { parentFile?.mkdirs() }
        val connection = URL(YouTubeUrl.thumbnailUrl(videoId)).openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        try {
            connection.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
        return dest
    }

    private fun decode(videoId: String, file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 640) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        return bitmap?.also { memory.put(videoId, it) }
    }
}
