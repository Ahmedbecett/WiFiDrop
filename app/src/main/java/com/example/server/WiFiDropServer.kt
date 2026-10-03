package com.example.server

import android.content.Context
import android.os.PowerManager
import com.example.data.local.TransferDatabase
import com.example.data.model.NearbyDevice
import com.example.data.model.TransferProgress
import com.example.data.model.TransferRecord
import com.example.util.FileUtils
import com.example.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class WiFiDropServer(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newFixedThreadPool(8)
    private val isRunning = AtomicBoolean(false)
    private var wakeLock: PowerManager.WakeLock? = null

    // State flows
    private val _isServerActive = MutableStateFlow(false)
    val isServerActive: StateFlow<Boolean> = _isServerActive.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _securityPin = MutableStateFlow("482731")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isPinRequired = MutableStateFlow(true)
    val isPinRequired: StateFlow<Boolean> = _isPinRequired.asStateFlow()

    private val _askBeforeReceiving = MutableStateFlow(false)
    val askBeforeReceiving: StateFlow<Boolean> = _askBeforeReceiving.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients: StateFlow<List<String>> = _connectedClients.asStateFlow()

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress: StateFlow<TransferProgress> = _transferProgress.asStateFlow()

    private val _nearbyDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val nearbyDevices: StateFlow<List<NearbyDevice>> = _nearbyDevices.asStateFlow()

    private val authenticatedIps = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val nearbyDevicesMap = ConcurrentHashMap<String, NearbyDevice>()

    private var discoveryJob: Job? = null
    private var isCancelledTransfer = AtomicBoolean(false)

    fun setPinRequired(required: Boolean) {
        _isPinRequired.value = required
        if (!required) authenticatedIps.clear()
    }

    fun setAskBeforeReceiving(ask: Boolean) {
        _askBeforeReceiving.value = ask
    }

    fun regeneratePin(): String {
        val pin = (100000..999999).random().toString()
        _securityPin.value = pin
        authenticatedIps.clear()
        return pin
    }

    fun setServerPort(port: Int) {
        if (_isServerActive.value) {
            stopServer()
            _serverPort.value = port
            startServer()
        } else {
            _serverPort.value = port
        }
    }

    fun startServer(): Boolean {
        if (isRunning.get()) return true

        try {
            val port = _serverPort.value
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            _isServerActive.value = true

            // Acquire WakeLock to keep transfer reliable
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WiFiDrop::TransferLock")?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout per session
            }

            executor.execute {
                acceptConnections()
            }

            startDeviceDiscovery()
            return true
        } catch (e: Exception) {
            isRunning.set(false)
            _isServerActive.value = false
            return false
        }
    }

    fun stopServer() {
        isRunning.set(false)
        _isServerActive.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        discoveryJob?.cancel()
        discoveryJob = null
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        _connectedClients.value = emptyList()
    }

    fun cancelActiveTransfer() {
        isCancelledTransfer.set(true)
        _transferProgress.value = _transferProgress.value.copy(
            isActive = false,
            statusMessage = "Cancelled"
        )
    }

    private fun acceptConnections() {
        while (isRunning.get()) {
            try {
                val socket = serverSocket?.accept() ?: break
                executor.execute {
                    handleClient(socket)
                }
            } catch (e: Exception) {
                if (!isRunning.get()) break
            }
        }
    }

    private fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress.hostAddress ?: "Unknown"
        try {
            val inputStream = BufferedInputStream(socket.getInputStream())
            val outputStream = BufferedOutputStream(socket.getOutputStream())

            val requestLine = readLine(inputStream) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val uriWithQuery = parts[1]
            val path = uriWithQuery.substringBefore("?")
            val queryString = if (uriWithQuery.contains("?")) uriWithQuery.substringAfter("?") else ""

            // Read HTTP headers
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(inputStream) ?: break
                if (line.isEmpty()) break
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    headers[line.substring(0, colonIdx).trim().lowercase()] = line.substring(colonIdx + 1).trim()
                }
            }

            // Track client connection
            val userAgent = headers["user-agent"] ?: "Browser"
            val clientLabel = parseClientLabel(userAgent, clientIp)
            if (!_connectedClients.value.contains(clientLabel)) {
                _connectedClients.value = _connectedClients.value + clientLabel
            }

            // Route handling
            when {
                path == "/" -> {
                    serveWebClient(outputStream)
                }
                path == "/api/status" -> {
                    serveStatusApi(outputStream, clientIp)
                }
                path == "/api/verify-pin" && method == "POST" -> {
                    handleVerifyPin(inputStream, headers, outputStream, clientIp)
                }
                path == "/api/files" -> {
                    serveFilesListApi(outputStream)
                }
                path == "/api/download" -> {
                    handleDownload(queryString, outputStream)
                }
                path == "/api/upload" && method == "POST" -> {
                    handleUpload(inputStream, headers, outputStream, clientLabel)
                }
                path == "/api/progress" -> {
                    serveProgressApi(outputStream)
                }
                path == "/api/cancel" && method == "POST" -> {
                    cancelActiveTransfer()
                    sendJsonResponse(outputStream, "200 OK", "{\"status\":\"cancelled\"}")
                }
                else -> {
                    sendNotFound(outputStream)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun parseClientLabel(userAgent: String, ip: String): String {
        val os = when {
            userAgent.contains("Windows") -> "PC (Windows)"
            userAgent.contains("Macintosh") || userAgent.contains("Mac OS") -> "Mac"
            userAgent.contains("iPhone") || userAgent.contains("iPad") -> "iOS Device"
            userAgent.contains("Android") -> "Android Device"
            userAgent.contains("Linux") -> "Linux PC"
            else -> "Web Browser"
        }
        val browser = when {
            userAgent.contains("Edg") -> "Edge"
            userAgent.contains("Chrome") -> "Chrome"
            userAgent.contains("Firefox") -> "Firefox"
            userAgent.contains("Safari") -> "Safari"
            else -> ""
        }
        return if (browser.isNotEmpty()) "$os · $browser ($ip)" else "$os ($ip)"
    }

    private fun serveWebClient(output: OutputStream) {
        val html = WebClientHtml.getHtml(
            deviceName = NetworkUtils.getDeviceName(),
            pinRequired = _isPinRequired.value
        )
        val bytes = html.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun serveStatusApi(output: OutputStream, clientIp: String) {
        val (total, _, free) = FileUtils.getStorageInfo()
        val isAuth = !_isPinRequired.value || authenticatedIps.contains(clientIp)
        val json = JSONObject().apply {
            put("deviceName", NetworkUtils.getDeviceName())
            put("ip", clientIp)
            put("pinRequired", _isPinRequired.value)
            put("isAuthenticated", isAuth)
            put("totalStorage", FileUtils.formatFileSize(total))
            put("freeStorage", FileUtils.formatFileSize(free))
            put("serverActive", true)
        }
        sendJsonResponse(output, "200 OK", json.toString())
    }

    private fun handleVerifyPin(
        input: InputStream,
        headers: Map<String, String>,
        output: OutputStream,
        clientIp: String
    ) {
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val count = input.read(bodyBytes, read, contentLength - read)
            if (count == -1) break
            read += count
        }
        val body = String(bodyBytes, Charsets.UTF_8)
        val enteredPin = try {
            JSONObject(body).optString("pin", "")
        } catch (_: Exception) { "" }

        if (enteredPin == _securityPin.value) {
            authenticatedIps.add(clientIp)
            sendJsonResponse(output, "200 OK", "{\"success\":true,\"message\":\"Authorized\"}")
        } else {
            sendJsonResponse(output, "401 Unauthorized", "{\"success\":false,\"message\":\"Invalid PIN\"}")
        }
    }

    private fun serveFilesListApi(output: OutputStream) {
        val files = FileUtils.listReceivedFiles(context)
        val jsonArray = JSONArray()
        for (f in files) {
            val item = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("size", f.size)
                put("formattedSize", f.formattedSize)
                put("category", f.category.label)
                put("mimeType", f.mimeType)
                put("downloadUrl", "/api/download?name=" + java.net.URLEncoder.encode(f.name, "UTF-8"))
            }
            jsonArray.put(item)
        }
        sendJsonResponse(output, "200 OK", jsonArray.toString())
    }

    private fun handleDownload(queryString: String, output: OutputStream) {
        val params = parseQuery(queryString)
        val fileName = params["name"] ?: params["id"] ?: ""
        if (fileName.isEmpty()) {
            sendNotFound(output)
            return
        }

        val decodedName = try { URLDecoder.decode(fileName, "UTF-8") } catch (_: Exception) { fileName }
        val targetFile = File(FileUtils.getReceivedDirectory(context), decodedName)

        if (!targetFile.exists() || !targetFile.isFile) {
            sendNotFound(output)
            return
        }

        val fileLength = targetFile.length()
        val mimeType = FileUtils.getMimeType(targetFile)

        val headerString = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $mimeType\r\n" +
                "Content-Length: $fileLength\r\n" +
                "Content-Disposition: attachment; filename=\"${targetFile.name}\"\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Connection: close\r\n\r\n"

        output.write(headerString.toByteArray(Charsets.UTF_8))
        output.flush()

        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L
        val startTime = System.currentTimeMillis()

        _transferProgress.value = TransferProgress(
            isActive = true,
            isSending = true,
            currentFileName = targetFile.name,
            totalFiles = 1,
            currentFileIndex = 1,
            bytesTransferred = 0,
            totalBytes = fileLength,
            peerDeviceName = "PC Browser",
            statusMessage = "Sending ${targetFile.name}"
        )

        FileInputStream(targetFile).use { fis ->
            while (true) {
                if (isCancelledTransfer.get()) break
                val read = fis.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                totalRead += read

                val elapsedSec = max(0.001, (System.currentTimeMillis() - startTime) / 1000.0)
                val speedMb = (totalRead / (1024.0 * 1024.0)) / elapsedSec
                val remainingBytes = fileLength - totalRead
                val remainingSec = if (speedMb > 0) (remainingBytes / (speedMb * 1024 * 1024)).toLong() else 0L

                _transferProgress.value = _transferProgress.value.copy(
                    bytesTransferred = totalRead,
                    speedMbPerSec = (speedMb * 10).toInt() / 10.0,
                    timeRemainingSeconds = remainingSec
                )
            }
            output.flush()
        }

        val completed = totalRead == fileLength
        _transferProgress.value = _transferProgress.value.copy(
            isActive = false,
            statusMessage = if (completed) "Download completed" else "Cancelled"
        )
        recordHistory(
            fileName = targetFile.name,
            size = fileLength,
            type = "SEND",
            status = if (completed) "COMPLETED" else "CANCELLED",
            speedMb = _transferProgress.value.speedMbPerSec,
            peer = "PC Browser",
            path = targetFile.absolutePath
        )
    }

    private fun handleUpload(
        input: InputStream,
        headers: Map<String, String>,
        output: OutputStream,
        clientLabel: String
    ) {
        isCancelledTransfer.set(false)
        val contentType = headers["content-type"] ?: ""
        val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

        if (contentType.contains("multipart/form-data")) {
            val boundary = contentType.substringAfter("boundary=").trim().removeSurrounding("\"")
            handleMultipartUpload(input, boundary, contentLength, output, clientLabel)
        } else {
            // Direct binary octet-stream upload
            val filenameHeader = headers["x-filename"] ?: "received_${System.currentTimeMillis()}.bin"
            val decodedName = try { URLDecoder.decode(filenameHeader, "UTF-8") } catch (_: Exception) { filenameHeader }
            handleDirectUpload(input, decodedName, contentLength, output, clientLabel)
        }
    }

    private fun handleDirectUpload(
        input: InputStream,
        fileName: String,
        contentLength: Long,
        output: OutputStream,
        clientLabel: String
    ) {
        val destFile = getSafeDestinationFile(fileName)
        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L
        val startTime = System.currentTimeMillis()

        _transferProgress.value = TransferProgress(
            isActive = true,
            isSending = false,
            currentFileName = destFile.name,
            totalFiles = 1,
            currentFileIndex = 1,
            bytesTransferred = 0,
            totalBytes = contentLength,
            peerDeviceName = clientLabel,
            statusMessage = "Receiving ${destFile.name}"
        )

        FileOutputStream(destFile).use { fos ->
            while (totalRead < contentLength) {
                if (isCancelledTransfer.get()) break
                val toRead = buffer.size.toLong().coerceAtMost(contentLength - totalRead).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break
                fos.write(buffer, 0, read)
                totalRead += read

                val elapsedSec = max(0.001, (System.currentTimeMillis() - startTime) / 1000.0)
                val speedMb = (totalRead / (1024.0 * 1024.0)) / elapsedSec
                val remainingBytes = contentLength - totalRead
                val remainingSec = if (speedMb > 0) (remainingBytes / (speedMb * 1024 * 1024)).toLong() else 0L

                _transferProgress.value = _transferProgress.value.copy(
                    bytesTransferred = totalRead,
                    speedMbPerSec = (speedMb * 10).toInt() / 10.0,
                    timeRemainingSeconds = remainingSec
                )
            }
            fos.flush()
        }

        val completed = totalRead == contentLength && !isCancelledTransfer.get()
        _transferProgress.value = _transferProgress.value.copy(
            isActive = false,
            statusMessage = if (completed) "Received successfully!" else "Transfer failed/cancelled"
        )

        recordHistory(
            fileName = destFile.name,
            size = totalRead,
            type = "RECEIVE",
            status = if (completed) "COMPLETED" else "CANCELLED",
            speedMb = _transferProgress.value.speedMbPerSec,
            peer = clientLabel,
            path = destFile.absolutePath
        )

        sendJsonResponse(output, "200 OK", "{\"success\":$completed,\"fileName\":\"${destFile.name}\"}")
    }

    private fun handleMultipartUpload(
        input: InputStream,
        boundary: String,
        contentLength: Long,
        output: OutputStream,
        clientLabel: String
    ) {
        val boundaryBytes = "--$boundary".toByteArray(Charsets.ISO_8859_1)
        var totalBytesRead = 0L
        val startTime = System.currentTimeMillis()

        // Read until boundary
        var line = readLine(input)
        var filename = "upload_${System.currentTimeMillis()}.bin"
        var foundFile = false

        while (line != null && !foundFile) {
            if (line.contains("Content-Disposition") && line.contains("filename=")) {
                val rawName = line.substringAfter("filename=").trim().removeSurrounding("\"")
                filename = try { URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                if (filename.isEmpty()) filename = "received_file_${System.currentTimeMillis()}.bin"
                foundFile = true
            }
            line = readLine(input)
        }

        // Skip to file data start (empty line)
        while (line != null && line.isNotEmpty()) {
            line = readLine(input)
        }

        val destFile = getSafeDestinationFile(filename)
        val fileEstimatedSize = max(0L, contentLength - 500)

        _transferProgress.value = TransferProgress(
            isActive = true,
            isSending = false,
            currentFileName = destFile.name,
            totalFiles = 1,
            currentFileIndex = 1,
            bytesTransferred = 0,
            totalBytes = fileEstimatedSize,
            peerDeviceName = clientLabel,
            statusMessage = "Receiving ${destFile.name}"
        )

        FileOutputStream(destFile).use { fos ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                if (isCancelledTransfer.get()) break
                fos.write(buffer, 0, read)
                totalBytesRead += read

                val elapsedSec = max(0.001, (System.currentTimeMillis() - startTime) / 1000.0)
                val speedMb = (totalBytesRead / (1024.0 * 1024.0)) / elapsedSec
                val remainingBytes = max(0L, fileEstimatedSize - totalBytesRead)
                val remainingSec = if (speedMb > 0) (remainingBytes / (speedMb * 1024 * 1024)).toLong() else 0L

                _transferProgress.value = _transferProgress.value.copy(
                    bytesTransferred = totalBytesRead,
                    speedMbPerSec = (speedMb * 10).toInt() / 10.0,
                    timeRemainingSeconds = remainingSec
                )
                if (totalBytesRead >= fileEstimatedSize) break
            }
            fos.flush()
        }

        val completed = !isCancelledTransfer.get()
        _transferProgress.value = _transferProgress.value.copy(
            isActive = false,
            statusMessage = if (completed) "Received successfully!" else "Cancelled"
        )

        recordHistory(
            fileName = destFile.name,
            size = destFile.length(),
            type = "RECEIVE",
            status = if (completed) "COMPLETED" else "CANCELLED",
            speedMb = _transferProgress.value.speedMbPerSec,
            peer = clientLabel,
            path = destFile.absolutePath
        )

        sendJsonResponse(output, "200 OK", "{\"success\":true,\"fileName\":\"${destFile.name}\"}")
    }

    private fun getSafeDestinationFile(desiredName: String): File {
        val dir = FileUtils.getReceivedDirectory(context)
        var file = File(dir, desiredName)
        if (!file.exists()) return file

        val nameWithoutExt = desiredName.substringBeforeLast('.', desiredName)
        val ext = if (desiredName.contains('.')) "." + desiredName.substringAfterLast('.') else ""
        var count = 1
        while (file.exists()) {
            file = File(dir, "$nameWithoutExt ($count)$ext")
            count++
        }
        return file
    }

    private fun recordHistory(
        fileName: String,
        size: Long,
        type: String,
        status: String,
        speedMb: Double,
        peer: String,
        path: String
    ) {
        coroutineScope.launch(Dispatchers.IO) {
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
                        transferSpeedMb = speedMb,
                        filePath = path
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private fun serveProgressApi(output: OutputStream) {
        val progress = _transferProgress.value
        val json = JSONObject().apply {
            put("isActive", progress.isActive)
            put("isSending", progress.isSending)
            put("fileName", progress.currentFileName)
            put("bytesTransferred", progress.bytesTransferred)
            put("totalBytes", progress.totalBytes)
            put("speedMb", progress.speedMbPerSec)
            put("timeRemainingSec", progress.timeRemainingSeconds)
            put("percent", (progress.progressPercent * 100).toInt())
            put("status", progress.statusMessage)
        }
        sendJsonResponse(output, "200 OK", json.toString())
    }

    private fun sendJsonResponse(output: OutputStream, status: String, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val response = "HTTP/1.1 $status\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendNotFound(output: OutputStream) {
        val body = "{\"error\":\"Not Found\"}"
        val response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n$body"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun readLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var c: Int
        while (input.read().also { c = it } != -1) {
            if (c == '\n'.code) break
            if (c != '\r'.code) baos.write(c)
        }
        if (baos.size() == 0 && c == -1) return null
        return baos.toString("UTF-8")
    }

    private fun parseQuery(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                result[pair.substring(0, idx)] = pair.substring(idx + 1)
            }
        }
        return result
    }

    // UDP Auto-Discovery on local subnet
    private fun startDeviceDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = coroutineScope.launch(Dispatchers.IO) {
            // Periodic broadcast
            launch {
                val beaconSocket = DatagramSocket()
                beaconSocket.broadcast = true
                val beaconPort = 8889

                while (isActive && isRunning.get()) {
                    try {
                        val ip = NetworkUtils.getLocalIpAddress(context) ?: "127.0.0.1"
                        val json = JSONObject().apply {
                            put("name", NetworkUtils.getDeviceName())
                            put("ip", ip)
                            put("port", _serverPort.value)
                            put("os", "Android")
                        }.toString()
                        val bytes = json.toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(
                            bytes,
                            bytes.size,
                            InetAddress.getByName("255.255.255.255"),
                            beaconPort
                        )
                        beaconSocket.send(packet)
                    } catch (_: Exception) {}
                    delay(3000)
                }
                try { beaconSocket.close() } catch (_: Exception) {}
            }

            // Beacon receiver
            launch {
                var listenSocket: DatagramSocket? = null
                try {
                    listenSocket = DatagramSocket(8889)
                    val buffer = ByteArray(1024)
                    val packet = DatagramPacket(buffer, buffer.size)

                    while (isActive && isRunning.get()) {
                        listenSocket.receive(packet)
                        val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val myIp = NetworkUtils.getLocalIpAddress(context)
                        val senderIp = packet.address.hostAddress

                        if (senderIp != myIp && data.contains("name")) {
                            try {
                                val obj = JSONObject(data)
                                val name = obj.optString("name", "Unknown Device")
                                val port = obj.optInt("port", 8080)
                                val os = obj.optString("os", "Unknown")
                                val dev = NearbyDevice(
                                    name = name,
                                    ip = senderIp ?: "",
                                    port = port,
                                    osType = os,
                                    lastSeen = System.currentTimeMillis()
                                )
                                nearbyDevicesMap[senderIp ?: ""] = dev
                                _nearbyDevices.value = nearbyDevicesMap.values.toList()
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    try { listenSocket?.close() } catch (_: Exception) {}
                }
            }
        }
    }
}
