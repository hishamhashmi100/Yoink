package io.github.hishamhashmi100.yoink.download

object YouTubeUrl {

    // youtu.be/<id>, /watch?v=<id>, /shorts/<id>, /live/<id>, /embed/<id> on any youtube.com host.
    private val VIDEO_ID = Regex(
        """(?:youtu\.be/|youtube(?:-nocookie)?\.com/(?:watch\?(?:[^#\s]*&)?v=|shorts/|live/|embed/|v/))([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])""",
        RegexOption.IGNORE_CASE,
    )

    fun extractId(text: String?): String? = text?.let { VIDEO_ID.find(it)?.groupValues?.get(1) }

    // list=<id> on a playlist page, or on a video opened from a playlist. Watch Later and Liked videos
    // (WL, LL) are too short to match: they're private, so there'd be nothing to download anyway.
    private val PLAYLIST_ID = Regex(
        """(?:youtu\.be|youtube(?:-nocookie)?\.com)/\S*?[?&]list=([A-Za-z0-9_-]{10,})""",
        RegexOption.IGNORE_CASE,
    )

    fun extractPlaylistId(text: String?): String? = text?.let { PLAYLIST_ID.find(it)?.groupValues?.get(1) }

    /**
     * The playlist's address. Mixes (made up by YouTube, ids starting with RD) only open from one of their videos,
     * so [videoId] is kept when known.
     */
    fun playlistUrl(playlistId: String, videoId: String? = null) =
        if (videoId != null) "https://www.youtube.com/watch?v=$videoId&list=$playlistId"
        else "https://www.youtube.com/playlist?list=$playlistId"

    /** A clean URL without `si=`, playlist or timestamp parameters. */
    fun watchUrl(videoId: String) = "https://www.youtube.com/watch?v=$videoId"

    fun thumbnailUrl(videoId: String) = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}
