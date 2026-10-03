package com.example.server

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.TransferDatabase
import com.example.data.model.TransferProgress
import com.example.data.model.TransferRecord
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class TransferClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.MINUTES)
        .readTimeout(60, TimeUnit.MINUTES)
        .build()

    private val isCancelled = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    fun cancel() {
        isCancelled.set(true)
    }

    fun setPaused(paused: Boolean) {
        isPaused.set(paused)
    }

    suspend fun sendFiles(
        targetIp: String,
        targetPort: Int,
        targetName: String,
        uris: List<Uri>,
        progressFlow: MutableStateFlow<TransferProgress>
    ) = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        isPaused.set(false)

        val fileDetails = uris.map { uri ->
            getFileDetails(uri)
        }
        val totalBytes = fileDetails.sumOf { it.second }
        var overallBytesTransferred = 0L

        progressFlow.value = TransferProgress(
            isActive = true,
            isSending = true,
            totalFiles = fileDetails.size,
            currentFileIndex = 0,
            bytesTransferred = 0,
            totalBytes = totalBytes,
            peerDeviceName = targetName,
            statusMessage = "Connecting to $targetName..."
        )

        val overallStartTime = System.currentTimeMillis()

        for (index in fileDetails.indices) {
            if (isCancelled.get()) break
            val (name, size, uri) = fileDetails[index]

            progressFlow.value = progressFlow.value.copy(
                currentFileName = name,
                currentFileIndex = index + 1,
                statusMessage = "Sending $name (${index + 1}/${fileDetails.size})"
            )

            var fileBytesSent = 0L
            val fileStartTime = System.currentTimeMillis()

            try {
                val requestBody = object : RequestBody() {
                    override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

                    override fun contentLength(): Long = size

                    override fun writeTo(sink: BufferedSink) {
                        val input: InputStream? = context.contentResolver.openInputStream(uri)
                        input?.use { stream ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (stream.read(buffer).also { read = it } != -1) {
                                if (isCancelled.get()) break
                                while (isPaused.get()) {
                                    Thread.sleep(200)
                                    if (isCancelled.get()) break
                                }

                                sink.write(buffer, 0, read)
                                fileBytesSent += read
                                val currentOverall = overallBytesTransferred + fileBytesSent

                                val elapsedSec = max(0.001, (System.currentTimeMillis() - overallStartTime) / 1000.0)
                                val speedMb = (currentOverall / (1024.0 * 1024.0)) / elapsedSec
                                val remainingBytes = max(0L, totalBytes - currentOverall)
                                val remainingSec = if (speedMb > 0) (remainingBytes / (speedMb * 1024 * 1024)).toLong() else 0L

                                progressFlow.value = progressFlow.value.copy(
                                    bytesTransferred = currentOverall,
                                    speedMbPerSec = (speedMb * 10).toInt() / 10.0,
                                    timeRemainingSeconds = remainingSec,
                                    isPaused = isPaused.get()
                                )
                            }
                        }
                    }
                }

                val encodedName = URLEncoder.encode(name, "UTF-8")
                val request = Request.Builder()
                    .url("http://$targetIp:$targetPort/api/upload")
                    .header("X-Filename", encodedName)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful

                overallBytesTransferred += size
                val fileElapsedSec = max(0.001, (System.currentTimeMillis() - fileStartTime) / 1000.0)
                val fileSpeedMb = (size / (1024.0 * 1024.0)) / fileElapsedSec

                recordHistory(
                    fileName = name,
                    size = size,
                    type = "SEND",
                    status = if (success) "COMPLETED" else "FAILED",
                    speedMb = (fileSpeedMb * 10).toInt() / 10.0,
                    peer = targetName
                )
                response.close()
            } catch (e: Exception) {
                recordHistory(
                    fileName = name,
                    size = size,
                    type = "SEND",
                    status = if (isCancelled.get()) "CANCELLED" else "FAILED",
                    speedMb = 0.0,
                    peer = targetName
                )
            }
        }

        val completed = !isCancelled.get()
        progressFlow.value = progressFlow.value.copy(
            isActive = false,
            statusMessage = if (completed) "All files sent successfully! ✅" else "Transfer cancelled"
        )
    }

    private fun getFileDetails(uri: Uri): Triple<String, Long, Uri> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        if (size <= 0) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        return Triple(name, size, uri)
    }

    private suspend fun recordHistory(
        fileName: String,
        size: Long,
        type: String,
        status: String,
        speedMb: Double,
        peer: String
    ) {
        try {
            val db = TransferDatabase.getDatabase(context)
            db.transferDao().insertTransfer(
                TransferRecord(
                    fileName = fileName,
                    fileSize = size,
                    formattedSize = FileUtils.formatFileSize(size),
                    peerDeviceName = peer,
                    transferType = type,
                    status = status,
                    transferSpeedMb = speedMb
                )
            )
        } catch (_: Exception) {}
    }
}
