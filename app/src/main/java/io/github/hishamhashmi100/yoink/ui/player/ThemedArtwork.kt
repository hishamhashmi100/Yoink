package io.github.hishamhashmi100.yoink.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import io.github.hishamhashmi100.yoink.data.Appearance
import io.github.hishamhashmi100.yoink.ui.theme.colorScheme
import java.io.File
import kotlin.math.max

/**
 * Android draws the media player (notification shade, lock screen) itself and takes its colors from the
 * cover picture, so apps can't theme it directly. Handing it a copy of the thumbnail recolored in the
 * theme's colors makes the player come out in those colors instead.
 */
object ThemedArtwork {

    // Square, like the player shows it; big enough to stay sharp on high-density screens.
    private const val SIZE = 720

    /** A file URI for the themed cover. Does disk work, so call it off the main thread. */
    fun uri(context: Context, thumbnailPath: String?, appearance: Appearance): Uri? = runCatching {
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val (shadow, highlight) = duotone(context, appearance)
        // Named after the two colors, so a new theme makes new covers.
        val theme = "%08x%08x".format(shadow, highlight)
        dir.listFiles()?.filterNot { it.name.startsWith(theme) }?.forEach { it.delete() }

        val name = thumbnailPath?.let { File(it).nameWithoutExtension } ?: "none"
        val file = File(dir, "${theme}_$name.png")
        if (!file.exists()) {
            val bitmap = thumbnailPath?.let { recolor(it, shadow, highlight) } ?: gradient(shadow, highlight)
            // PNG: JPEG blotches the smooth duotone.
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        Uri.fromFile(file)
    }.getOrNull()

    /** The darkest and lightest colors of the duotone, both from the theme's primary color. */
    private fun duotone(context: Context, appearance: Appearance): Pair<Int, Int> {
        val dark = colorScheme(context, appearance.colorSource, dark = true)
        val light = colorScheme(context, appearance.colorSource, dark = false)
        // Lifted toward white so the picture keeps its contrast once the player darkens it.
        return dark.onPrimary.toArgb() to blend(light.primaryContainer.toArgb(), 0xFFFFFFFF.toInt(), 0.45f)
    }

    /**
     * Maps the picture's brightness onto shadow → highlight, so it stays recognizable in the theme's color.
     * The whole 16:9 thumbnail sits in the middle of a square, so the player's square crop doesn't cut it.
     */
    private fun recolor(path: String, shadow: Int, highlight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val source = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null

        val out = gradient(shadow, blend(shadow, highlight, 0.35f))
        val scale = SIZE.toFloat() / source.width
        val height = source.height * scale
        val top = (SIZE - height) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(gradientMap(shadow, highlight))
        }
        Canvas(out).drawBitmap(source, null, RectF(0f, top, SIZE.toFloat(), top + height), paint)
        source.recycle()
        return out
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        fun channel(shift: Int) = ((from shr shift and 0xFF) + ((to shr shift and 0xFF) - (from shr shift and 0xFF)) * amount).toInt()
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** A plain gradient in the theme's colors: the cover without a thumbnail, and the frame around one. */
    private fun gradient(shadow: Int, highlight: Int): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), highlight, shadow, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)
        return out
    }

    /** out = shadow + luminance × (highlight − shadow), per channel. */
    private fun gradientMap(shadow: Int, highlight: Int): ColorMatrix {
        fun row(channel: Int): FloatArray {
            val from = (shadow shr channel and 0xFF).toFloat()
            val span = ((highlight shr channel and 0xFF) - (shadow shr channel and 0xFF)) / 255f
            return floatArrayOf(span * 0.299f, span * 0.587f, span * 0.114f, 0f, from)
        }
        return ColorMatrix(row(16) + row(8) + row(0) + floatArrayOf(0f, 0f, 0f, 1f, 0f))
    }
}
