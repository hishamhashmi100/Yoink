package io.github.hishamhashmi100.yoink.download

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

/** Length and resolution read from a media file. */
data class MediaInfo(val durationSec: Long?, val resolution: Int?) {

    companion object {
        /** Reads a local file path or a file:// / content:// URI. Returns null if it can't be read. */
        fun read(context: Context, source: String): MediaInfo? {
            val retriever = MediaMetadataRetriever()
            return try {
                val uri = Uri.parse(source)
                when (uri.scheme) {
                    null, "" -> retriever.setDataSource(source)
                    "file" -> retriever.setDataSource(uri.path)
                    else -> retriever.setDataSource(context, uri)
                }
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                MediaInfo(
                    durationSec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.div(1000),
                    // "1080p" means the short side, so a 1080×1920 Short is 1080p too.
                    resolution = if (width != null && height != null && width > 0 && height > 0) minOf(width, height) else null,
                )
            } catch (e: Exception) {
                null
            } finally {
                retriever.release()
            }
        }

        /** "1080p", "720p", "4K"… The nearest standard size, allowing for odd sizes like 1072. */
        fun label(resolution: Int): String {
            val standard = listOf(4320 to "8K", 2160 to "4K", 1440 to "1440p", 1080 to "1080p", 720 to "720p", 480 to "480p", 360 to "360p", 240 to "240p")
            return standard.firstOrNull { (size, _) -> resolution >= size * 0.9 }?.second ?: "${resolution}p"
        }
    }
}
