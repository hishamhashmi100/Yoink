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

    @Insert
    suspend fun insert(download: Download): Long

    @Update
    suspend fun update(download: Download)

    @Query("UPDATE downloads SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: DownloadStatus, error: String? = null)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)
}
