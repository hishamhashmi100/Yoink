package io.github.hishamhashmi100.yoink.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** A playlist the user made in Yoink (not one shared from YouTube). */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One track in a playlist. A track is in a playlist at most once; [position] keeps the order it was added in. */
@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "downloadId"])
data class PlaylistItem(
    val playlistId: Long,
    val downloadId: Long,
    val position: Long,
)

/** A playlist as the Library lists it: its name, how many tracks, and the first track's thumbnail as its cover. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val count: Int,
    val cover: String?,
    val coverVideoId: String?,
)

@Dao
interface PlaylistDao {

    // Only finished downloads count: a track deleted from the Library disappears from its playlists too.
    @Query(
        """
        SELECT p.id AS id, p.name AS name,
            (SELECT COUNT(*) FROM playlist_items i JOIN downloads d ON d.id = i.downloadId
                WHERE i.playlistId = p.id AND d.status = :completed) AS count,
            (SELECT d.thumbnailPath FROM playlist_items i JOIN downloads d ON d.id = i.downloadId
                WHERE i.playlistId = p.id AND d.status = :completed ORDER BY i.position LIMIT 1) AS cover,
            (SELECT d.videoId FROM playlist_items i JOIN downloads d ON d.id = i.downloadId
                WHERE i.playlistId = p.id AND d.status = :completed ORDER BY i.position LIMIT 1) AS coverVideoId
        FROM playlists p
        ORDER BY p.createdAt DESC
        """,
    )
    fun observeSummaries(completed: DownloadStatus): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observe(id: Long): Flow<Playlist?>

    @Query(
        """
        SELECT d.* FROM playlist_items i JOIN downloads d ON d.id = i.downloadId
        WHERE i.playlistId = :playlistId AND d.status = :completed
        ORDER BY i.position
        """,
    )
    fun observeTracks(playlistId: Long, completed: DownloadStatus): Flow<List<Download>>

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("SELECT IFNULL(MAX(position), -1) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun lastPosition(playlistId: Long): Long

    /** -1 if the track was already in the playlist. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: PlaylistItem): Long

    /** Adds [downloadId] at the end of the playlist. False if it was already there. */
    @Transaction
    suspend fun add(playlistId: Long, downloadId: Long): Boolean =
        insertItem(PlaylistItem(playlistId, downloadId, lastPosition(playlistId) + 1)) != -1L

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND downloadId = :downloadId")
    suspend fun removeItem(playlistId: Long, downloadId: Long)

    /** A deleted download leaves every playlist. */
    @Query("DELETE FROM playlist_items WHERE downloadId = :downloadId")
    suspend fun removeDownload(downloadId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clear(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        clear(id)
        deletePlaylist(id)
    }
}
