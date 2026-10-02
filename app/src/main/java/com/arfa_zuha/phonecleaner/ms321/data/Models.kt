package com.arfa_zuha.phonecleaner.ms321.data

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val cacheBytes: Long,
    val imagesBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val documentsBytes: Long
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
}

data class LargeFileItem(
    val id: Long,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateModified: Long
)

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val packageSize: Long,
    val isSystemApp: Boolean,
    val lastTimeUsed: Long = 0L,
    val firstInstallTime: Long = 0L,
    val isGame: Boolean = false
)
