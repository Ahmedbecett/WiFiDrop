package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.data.model.FileCategory
import com.example.data.model.FileInfo
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }

    fun getCategory(fileName: String, mimeType: String? = null): FileCategory {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType?.lowercase(Locale.ROOT) ?: ""

        if (mime.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic")) {
            return FileCategory.IMAGE
        }
        if (mime.startsWith("video/") || extension in listOf("mp4", "mkv", "avi", "mov", "flv", "webm", "wmv", "3gp")) {
            return FileCategory.VIDEO
        }
        if (mime.startsWith("audio/") || extension in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma")) {
            return FileCategory.AUDIO
        }
        if (mime.contains("pdf") || mime.contains("document") || mime.contains("text") ||
            extension in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx", "csv", "rtf", "md")
        ) {
            return FileCategory.DOCUMENT
        }
        if (mime.contains("zip") || mime.contains("compressed") || mime.contains("tar") ||
            extension in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
        ) {
            return FileCategory.ARCHIVE
        }
        return FileCategory.OTHER
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    fun getReceivedDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "received")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listReceivedFiles(context: Context): List<FileInfo> {
        val dir = getReceivedDirectory(context)
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { it.isFile }.map { file ->
            val mime = getMimeType(file)
            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                null
            }
            FileInfo(
                id = file.name,
                name = file.name,
                size = file.length(),
                formattedSize = formatFileSize(file.length()),
                mimeType = mime,
                category = getCategory(file.name, mime),
                lastModified = file.lastModified(),
                path = file.absolutePath,
                uri = uri
            )
        }.sortedByDescending { it.lastModified }
    }

    fun getStorageInfo(): Triple<Long, Long, Long> {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        return Triple(totalBytes, usedBytes, freeBytes)
    }

    fun getReceivedFilesSize(context: Context): Long {
        val dir = getReceivedDirectory(context)
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    fun getCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        val extCacheDir = context.externalCacheDir
        var size = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        if (extCacheDir != null) {
            size += extCacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        }
        return size
    }

    fun clearCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openFile(context: Context, fileInfo: FileInfo) {
        try {
            val file = File(fileInfo.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileInfo.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general intent
            val file = File(fileInfo.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    }

    fun shareFile(context: Context, fileInfo: FileInfo) {
        try {
            val file = File(fileInfo.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = fileInfo.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileInfo.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${fileInfo.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun deleteFile(context: Context, fileInfo: FileInfo): Boolean {
        return try {
            val file = File(fileInfo.path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun renameFile(context: Context, fileInfo: FileInfo, newName: String): Boolean {
        return try {
            val file = File(fileInfo.path)
            val target = File(file.parentFile, newName)
            if (file.exists() && !target.exists()) {
                file.renameTo(target)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
