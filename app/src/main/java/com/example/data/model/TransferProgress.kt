package com.example.data.model

data class TransferProgress(
    val isActive: Boolean = false,
    val isSending: Boolean = false,
    val currentFileName: String = "",
    val totalFiles: Int = 0,
    val currentFileIndex: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMbPerSec: Double = 0.0,
    val timeRemainingSeconds: Long = 0L,
    val peerDeviceName: String = "",
    val isPaused: Boolean = false,
    val statusMessage: String = ""
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

data class NearbyDevice(
    val name: String,
    val ip: String,
    val port: Int = 8080,
    val osType: String = "Unknown",
    val lastSeen: Long = System.currentTimeMillis()
)
