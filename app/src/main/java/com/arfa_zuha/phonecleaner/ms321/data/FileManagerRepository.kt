package com.arfa_zuha.phonecleaner.ms321.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerRepository(private val context: Context) {

    suspend fun getCategorySizesMap(): Map<MediaType, Long> = withContext(Dispatchers.IO) {
        val sizeMap = mutableMapOf<MediaType, Long>()
        for (type in MediaType.entries) {
            try {
                val files = getMediaFiles(type)
                sizeMap[type] = files.sumOf { it.size }
            } catch (e: Exception) {
                sizeMap[type] = 0L
            }
        }
        sizeMap
    }

    suspend fun getMediaFiles(mediaType: MediaType): List<MediaFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<MediaFile>()
        val contentResolver: ContentResolver = context.contentResolver

        val uri: Uri
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        when (mediaType) {
            MediaType.IMAGE -> uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.AUDIO -> uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaType.DOCUMENT -> {
                uri = MediaStore.Files.getContentUri("external")
                selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
                selectionArgs = arrayOf("%.pdf", "%.doc%", "%.xls%", "%.ppt%", "%.txt")
            }
            MediaType.APK -> {
                uri = MediaStore.Files.getContentUri("external")
                selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
                selectionArgs = arrayOf("%.apk")
            }
            MediaType.ZIP -> {
                uri = MediaStore.Files.getContentUri("external")
                selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
                selectionArgs = arrayOf("%.zip", "%.rar", "%.7z")
            }
            MediaType.DOWNLOAD -> {
                uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
                selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
                selectionArgs = arrayOf("%/Download/%")
            }
            MediaType.RECENT -> {
                uri = MediaStore.Files.getContentUri("external")
            }
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else System.currentTimeMillis()
                    val data = if (dataColumn != -1) cursor.getString(dataColumn) ?: "" else ""
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: File(data).name else File(data).name
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                    val date = if (dateColumn != -1) cursor.getLong(dateColumn) else 0L
                    val folderName = if (data.isNotEmpty()) File(data).parentFile?.name ?: "Main Storage" else "Main Storage"

                    val contentUri = if (idColumn != -1 && id > 0) ContentUris.withAppendedId(uri, id) else Uri.fromFile(File(data))

                    if (size > 0 || data.isNotEmpty()) {
                        files.add(
                            MediaFile(
                                id = id,
                                name = name.ifEmpty { "File_$id" },
                                path = data,
                                uri = contentUri,
                                size = if (size > 0) size else if (data.isNotEmpty()) File(data).length() else 0L,
                                dateModified = if (date > 0) date * 1000L else System.currentTimeMillis(),
                                folderName = folderName,
                                mediaType = mediaType
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback 1: Direct File System Scanner if MediaStore returns 0 files
        if (files.isEmpty()) {
            files.addAll(scanDirectoryFallback(mediaType))
        }

        // Removed deceptive Fallback 2

        if (mediaType == MediaType.RECENT) {
            return@withContext files.sortedByDescending { it.dateModified }.take(50)
        }

        files.sortedByDescending { it.dateModified }
    }

    private fun scanDirectoryFallback(mediaType: MediaType): List<MediaFile> {
        val resultList = mutableListOf<MediaFile>()
        try {
            val searchDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStorageDirectory()
            )

            val targetExtensions = when (mediaType) {
                MediaType.IMAGE -> setOf("jpg", "jpeg", "png", "webp", "gif", "heic")
                MediaType.VIDEO -> setOf("mp4", "mkv", "webm", "avi", "3gp", "mov")
                MediaType.AUDIO -> setOf("mp3", "m4a", "wav", "aac", "flac", "ogg")
                MediaType.DOCUMENT -> setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
                MediaType.APK -> setOf("apk")
                MediaType.ZIP -> setOf("zip", "rar", "7z")
                MediaType.DOWNLOAD, MediaType.RECENT -> setOf("pdf", "doc", "docx", "jpg", "png", "mp4", "mp3", "apk", "zip")
            }

            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryRecursive(dir, targetExtensions, resultList, mediaType, depth = 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultList
    }

    private fun scanDirectoryRecursive(
        dir: File,
        extensions: Set<String>,
        resultList: MutableList<MediaFile>,
        mediaType: MediaType,
        depth: Int
    ) {
        if (depth > 3 || resultList.size > 200) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectoryRecursive(file, extensions, resultList, mediaType, depth + 1)
            } else if (file.isFile && file.length() > 0) {
                val ext = file.extension.lowercase()
                if (ext in extensions) {
                    val id = file.absolutePath.hashCode().toLong()
                    resultList.add(
                        MediaFile(
                            id = id,
                            name = file.name,
                            path = file.absolutePath,
                            uri = Uri.fromFile(file),
                            size = file.length(),
                            dateModified = file.lastModified(),
                            folderName = file.parentFile?.name ?: "Storage",
                            mediaType = mediaType
                        )
                    )
                }
            }
        }
    }

}
