package io.github.hishamhashmi100.yoink.download

import androidx.annotation.StringRes
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.github.hishamhashmi100.yoink.R

enum class Quality(val height: Int?, @StringRes val labelRes: Int?, private val fixedLabel: String?) {
    BEST(null, R.string.quality_best, null),
    P1080(1080, null, "1080p"),
    P720(720, null, "720p"),
    P480(480, null, "480p"),
    AUDIO(null, R.string.quality_audio, null);

    val isAudio: Boolean get() = this == AUDIO

    fun label(resources: android.content.res.Resources): String =
        labelRes?.let(resources::getString) ?: fixedLabel.orEmpty()

    fun applyTo(request: YoutubeDLRequest) {
        when (this) {
            BEST -> {
                request.addOption("-f", "bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            AUDIO -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "192K")
            }
            else -> {
                val h = height!!
                request.addOption("-f", "bv*[height<=$h]+ba/b[height<=$h]/b")
                // Resolution has to come first: with the codec first, a 360p H.264 stream beats 1080p VP9.
                request.addOption("-S", "res:$h,vcodec:h264,acodec:aac")
                request.addOption("--merge-output-format", "mp4")
            }
        }
    }

    companion object {
        fun fromName(name: String?): Quality? = entries.firstOrNull { it.name == name }
    }
}
