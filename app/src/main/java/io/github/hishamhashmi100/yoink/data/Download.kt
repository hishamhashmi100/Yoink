package io.github.hishamhashmi100.yoink.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.hishamhashmi100.yoink.download.Quality

/**
 * WAITING: a playlist video in line, not handed to the downloader yet (see [DownloadRepository.pump]).
 * QUEUED: handed to the downloader, about to start.
 */
enum class DownloadStatus { WAITING, QUEUED, DOWNLOADING, COMPLETED, FAILED }

/** One video in a shared playlist. */
data class PlaylistEntry(val videoId: String, val title: String, val channel: String?, val durationSec: Long?)

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val title: String,
    val channel: String? = null,
    val durationSec: Long? = null,
    val thumbnailPath: String? = null,
    val fileUri: String? = null,
    val mimeType: String? = null,
    val quality: Quality,
    val sizeBytes: Long? = null,
    val status: DownloadStatus,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** The video's actual resolution (its short side in pixels, e.g. 1080), read from the file. */
    val resolution: Int? = null,
    /** Set for videos downloaded as part of a playlist, so they share one notification. */
    val playlistId: String? = null,
    val playlistTitle: String? = null,
)
