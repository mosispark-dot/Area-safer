package com.arfa_zuha.phonecleaner.ms321.data

import android.net.Uri

enum class FileCategory(val label: String) {
    IMAGE("Images"),
    VIDEO("Videos"),
    AUDIO("Audios"),
    DOCUMENT("Documents")
}

data class DuplicateFile(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val uri: Uri,
    val category: FileCategory,
    var isSelected: Boolean = false,
    val previewUrl: String? = null
)

data class DuplicateGroup(
    val hash: String,
    val size: Long,
    val files: List<DuplicateFile>
)
