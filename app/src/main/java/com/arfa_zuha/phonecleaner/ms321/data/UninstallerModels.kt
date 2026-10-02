package com.arfa_zuha.phonecleaner.ms321.data

import android.graphics.drawable.Drawable

enum class UninstallerTab(val label: String) {
    INSTALLED_APPS("Installed Apps"),
    APK_FILES("Leftover APKs")
}

enum class AppSortOption(val label: String) {
    SIZE("Size"),
    NAME("Name"),
    DATE("Date")
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val sizeBytes: Long,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val installTimeMillis: Long = 0L,
    var isSelected: Boolean = false
)

data class ApkFileItem(
    val path: String,
    val fileName: String,
    val packageName: String?,
    val versionName: String?,
    val sizeBytes: Long,
    val icon: Drawable?,
    val dateModified: Long,
    var isSelected: Boolean = false
)
