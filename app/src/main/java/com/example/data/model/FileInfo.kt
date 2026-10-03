package com.example.data.model

import android.net.Uri

enum class FileCategory(val label: String, val iconName: String) {
    ALL("All", "folder"),
    IMAGE("Images", "image"),
    VIDEO("Videos", "videocam"),
    DOCUMENT("Documents", "description"),
    AUDIO("Audio", "audiotrack"),
    ARCHIVE("Archives", "archive"),
    OTHER("Other", "insert_drive_file")
}

data class FileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val formattedSize: String,
    val mimeType: String,
    val category: FileCategory,
    val lastModified: Long,
    val path: String = "",
    val uri: Uri? = null
)
