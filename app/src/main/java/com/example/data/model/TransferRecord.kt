package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val timestamp: Long = System.currentTimeMillis(),
    val peerDeviceName: String,
    val transferType: String, // "SEND" or "RECEIVE"
    val status: String,       // "COMPLETED", "FAILED", "CANCELLED"
    val transferSpeedMb: Double = 0.0,
    val filePath: String = ""
)
