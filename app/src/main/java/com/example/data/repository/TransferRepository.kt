package com.example.data.repository

import com.example.data.local.TransferDao
import com.example.data.model.TransferRecord
import kotlinx.coroutines.flow.Flow

class TransferRepository(private val dao: TransferDao) {
    val allTransfers: Flow<List<TransferRecord>> = dao.getAllTransfers()

    fun searchTransfers(query: String): Flow<List<TransferRecord>> =
        dao.searchTransfers(query)

    suspend fun insertTransfer(record: TransferRecord): Long =
        dao.insertTransfer(record)

    suspend fun deleteTransfer(id: Long) =
        dao.deleteTransferById(id)

    suspend fun clearHistory() =
        dao.clearAll()

    suspend fun getAnalytics(): Triple<Int, Int, Int> {
        val total = dao.getCount()
        val completed = dao.getCompletedCount()
        val failed = dao.getFailedCount()
        return Triple(total, completed, failed)
    }
}
