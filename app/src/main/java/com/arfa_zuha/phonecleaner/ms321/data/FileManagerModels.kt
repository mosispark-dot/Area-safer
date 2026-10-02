package com.arfa_zuha.phonecleaner.ms321.data

import android.net.Uri

enum class MediaType {
    IMAGE, VIDEO, AUDIO, DOCUMENT, APK, ZIP, DOWNLOAD, RECENT
}

data class MediaFile(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val dateModified: Long,
    val folderName: String,
    val mediaType: MediaType
)

data class MediaFolder(
    val folderName: String,
    val fileCount: Int,
    val files: List<MediaFile>,
    val thumbnailUri: Uri?,
    val totalSizeBytes: Long = files.sumOf { it.size }
)
