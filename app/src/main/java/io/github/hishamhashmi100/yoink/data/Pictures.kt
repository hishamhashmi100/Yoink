package io.github.hishamhashmi100.yoink.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Pictures for "Your picture": Yoink keeps its own copy of the photo the user picks, sized for the screen, so it
 * keeps working if the original is moved or deleted and loads quickly behind every screen.
 */
object Pictures {

    data class Imported(val path: String, val color: Int)

    private fun dir(context: Context) = File(context.filesDir, "backgrounds")

    /** Copies the picked photo (turned the right way up) and finds its average color. Null if it can't be read. */
    suspend fun import(context: Context, uri: Uri): Imported? = withContext(Dispatchers.IO) {
        runCatching {
            val metrics = context.resources.displayMetrics
            val target = max(metrics.widthPixels, metrics.heightPixels).coerceAtMost(2400)
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                var sample = 1
                while (max(info.size.width, info.size.height) / (sample * 2) >= target) sample *= 2
                decoder.setTargetSampleSize(sample)
                // A software bitmap, so it can be saved and its colors read.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val file = File(dir(context).apply { mkdirs() }, "${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            // Shrinking to one pixel averages the whole picture.
            val dot = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
            val color = dot.getPixel(0, 0) or 0xFF000000.toInt()
            dot.recycle()
            bitmap.recycle()
            Imported(file.absolutePath, color)
        }.getOrNull()
    }

    /** Deletes copies that nothing uses any more (not the current look, nor a saved theme). */
    suspend fun cleanup(context: Context, inUse: Set<String>) = withContext(Dispatchers.IO) {
        dir(context).listFiles().orEmpty().filter { it.absolutePath !in inUse }.forEach { it.delete() }
    }
}
