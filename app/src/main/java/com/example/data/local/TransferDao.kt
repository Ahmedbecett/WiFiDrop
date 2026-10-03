package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TransferRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferRecord>>

    @Query("SELECT * FROM transfer_history WHERE fileName LIKE '%' || :query || '%' OR peerDeviceName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTransfers(query: String): Flow<List<TransferRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(record: TransferRecord): Long

    @Query("DELETE FROM transfer_history WHERE id = :id")
    suspend fun deleteTransferById(id: Long)

    @Query("DELETE FROM transfer_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transfer_history")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM transfer_history WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM transfer_history WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int
}
