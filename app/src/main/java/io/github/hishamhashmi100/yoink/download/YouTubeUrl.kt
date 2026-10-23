package io.github.hishamhashmi100.yoink.download

object YouTubeUrl {

    // youtu.be/<id>, /watch?v=<id>, /shorts/<id>, /live/<id>, /embed/<id> on any youtube.com host.
    private val VIDEO_ID = Regex(
        """(?:youtu\.be/|youtube(?:-nocookie)?\.com/(?:watch\?(?:[^#\s]*&)?v=|shorts/|live/|embed/|v/))([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])""",
        RegexOption.IGNORE_CASE,
    )

    fun extractId(text: String?): String? = text?.let { VIDEO_ID.find(it)?.groupValues?.get(1) }

    /** A clean URL without `si=`, playlist or timestamp parameters. */
    fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

    fun thumbnailUrl(videoId: String) = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}
