package io.github.hishamhashmi100.yoink.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Download>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: Long): Download?

    @Query("SELECT * FROM downloads WHERE status = :status")
    suspend fun getByStatus(status: DownloadStatus): List<Download>

    @Query("UPDATE downloads SET resolution = :resolution WHERE id = :id")
    suspend fun setResolution(id: Long, resolution: Int)

    @Insert
    suspend fun insert(download: Download): Long

    @Insert
    suspend fun insertAll(downloads: List<Download>)

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN (:statuses)")
    suspend fun countWithStatus(statuses: List<DownloadStatus>): Int

    /** The first in line with [status], in the order they were added. */
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt, id LIMIT :limit")
    suspend fun oldestWithStatus(status: DownloadStatus, limit: Int): List<Download>

    @Query("SELECT * FROM downloads WHERE playlistId = :playlistId")
    suspend fun getByPlaylist(playlistId: String): List<Download>

    @Query("SELECT * FROM downloads WHERE status != :status")
    suspend fun getAllExcept(status: DownloadStatus): List<Download>

    @Query("DELETE FROM downloads WHERE status != :status")
    suspend fun deleteAllExcept(status: DownloadStatus)

    @Update
    suspend fun update(download: Download)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: DownloadStatus, error: String? = null)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}
