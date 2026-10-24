package io.github.hishamhashmi100.yoink.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.hishamhashmi100.yoink.download.Quality

enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED }

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
)
