package com.arfa_zuha.phonecleaner.ms321.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateFile
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateGroup
import com.arfa_zuha.phonecleaner.ms321.data.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object DuplicateFinder {

    suspend fun findDuplicates(
        context: Context,
        category: FileCategory,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        onProgress(10, "Initializing deep video & media scanner for ${category.label}...")
        val allFiles = mutableListOf<DuplicateFile>()
        val existingPaths = HashSet<String>()

        // 1. Single-pass MediaStore Content Query
        try {
            onProgress(20, "Querying MediaStore for ${category.label} files...")
            val uri: Uri
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.SIZE
            )
            val selection: String?
            val selectionArgs: Array<String>?

            when (category) {
                FileCategory.IMAGE -> {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    selection = null
                    selectionArgs = null
                }
                FileCategory.VIDEO -> {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    selection = null
                    selectionArgs = null
                }
                FileCategory.AUDIO -> {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    selection = null
                    selectionArgs = null
                }
                FileCategory.DOCUMENT -> {
                    uri = MediaStore.Files.getContentUri("external")
                    val mimeTypes = arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain"
                    )
                    selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN (" + mimeTypes.joinToString(",") { "?" } + ")"
                    selectionArgs = mimeTypes
                }
            }

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else 0L
                    val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "Unknown" else "Unknown"
                    val path = if (dataColumn != -1) cursor.getString(dataColumn) ?: "" else ""
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L

                    val ext = name.substringAfterLast(".", "").lowercase()
                    val isValidForCategory = when (category) {
                        FileCategory.IMAGE -> ext in listOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
                        FileCategory.VIDEO -> ext in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "m4v", "ts")
                        FileCategory.AUDIO -> ext in listOf("mp3", "m4a", "wav", "aac", "ogg", "flac", "wma", "opus")
                        FileCategory.DOCUMENT -> ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
                    }

                    if (isValidForCategory && size > 100L) {
                        val fileUri = if (id > 0) Uri.withAppendedPath(uri, id.toString()) else if (path.isNotEmpty()) Uri.fromFile(File(path)) else Uri.EMPTY
                        if (path.isNotEmpty()) existingPaths.add(path)
                        allFiles.add(
                            DuplicateFile(
                                id = if (id > 0) id else path.hashCode().toLong(),
                                name = name,
                                path = path,
                                size = size,
                                uri = fileUri,
                                category = category,
                                isSelected = false,
                                previewUrl = fileUri.toString()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        yield()

        // 2. Deep File System Storage Scan (DCIM, Movies, Downloads, WhatsApp, Telegram)
        onProgress(35, "Deep scanning device folders for ${category.label} copies...")
        try {
            val rootDir = Environment.getExternalStorageDirectory()
            scanDirectoryRecursively(rootDir, category, allFiles, existingPaths, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        yield()

        val rawDuplicateGroups = mutableListOf<DuplicateGroup>()
        val assignedPaths = HashSet<String>()

        // 3. Strategy A: Fast Multi-Point Cryptographic Hash Verification
        onProgress(60, "Computing multi-point signatures for ${category.label} files...")
        val sizeGrouped = allFiles.groupBy { it.size }
        val sizeCandidateList = sizeGrouped.filter { it.value.size > 1 && it.key > 200L }

        try {
            coroutineScope {
                val hashTasks = sizeCandidateList.map { (_, candidateFiles) ->
                    async(Dispatchers.IO) {
                        val hashedPairs = candidateFiles.map { file ->
                            val hash = computeMultiPointFileHash(context, file)
                            Pair(hash, file)
                        }
                        val hashGroups = hashedPairs.groupBy { it.first }
                        for ((hash, pairList) in hashGroups) {
                            if (pairList.size > 1 && hash.isNotEmpty()) {
                                val matchedFiles = pairList.map { it.second }.toMutableList()
                                synchronized(rawDuplicateGroups) {
                                    rawDuplicateGroups.add(DuplicateGroup("hash_$hash", matchedFiles.first().size, matchedFiles))
                                    for (f in matchedFiles) {
                                        if (f.path.isNotEmpty()) assignedPaths.add(f.path)
                                    }
                                }
                            }
                        }
                    }
                }
                hashTasks.awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        yield()

        // 4. Strategy B: Normalized Name & Exact Size Matching across Different Folders
        onProgress(80, "Matching copy clones across camera, WhatsApp & downloads...")
        val unassignedFiles = allFiles.filter { it.path.isEmpty() || it.path !in assignedPaths }

        val exactNameAndSizeGrouped = unassignedFiles.groupBy { file ->
            val ext = file.name.substringAfterLast(".", "").lowercase()
            val cleanName = normalizeFileName(file.name)
            "${category.name}_${file.size}_${cleanName}_$ext"
        }

        for ((key, files) in exactNameAndSizeGrouped) {
            if (key.isNotEmpty() && files.size > 1) {
                val matched = files.filter { it.path.isEmpty() || it.path !in assignedPaths }
                if (matched.size > 1) {
                    val distinctFiles = matched.distinctBy { if (it.path.isNotEmpty()) it.path else it.id.toString() }
                    if (distinctFiles.size > 1) {
                        rawDuplicateGroups.add(DuplicateGroup(key, distinctFiles.first().size, distinctFiles.toMutableList()))
                        for (f in distinctFiles) {
                            if (f.path.isNotEmpty()) assignedPaths.add(f.path)
                        }
                    }
                }
            }
        }

        yield()

        // 5. Strategy C: Exact Byte Size Matching for Videos & Audio (> 500 KB)
        onProgress(90, "Verifying identical byte sizes for video copies...")
        val remainingUnassigned = allFiles.filter { it.path.isEmpty() || it.path !in assignedPaths }
        val exactSizeGroups = remainingUnassigned.groupBy { it.size }.filter { it.key > 500 * 1024L && it.value.size > 1 }

        for ((size, files) in exactSizeGroups) {
            val distinctFiles = files.distinctBy { if (it.path.isNotEmpty()) it.path else it.id.toString() }
            if (distinctFiles.size > 1) {
                rawDuplicateGroups.add(DuplicateGroup("size_exact_$size", size, distinctFiles.toMutableList()))
                for (f in distinctFiles) {
                    if (f.path.isNotEmpty()) assignedPaths.add(f.path)
                }
            }
        }

        yield()

        // 6. Enforce 100% Unique File & Group Deduction
        onProgress(98, "Building unique duplicate groups...")
        val finalUniqueGroups = mutableListOf<DuplicateGroup>()
        val globallyUsedPaths = HashSet<String>()

        for (group in rawDuplicateGroups) {
            val uniqueFilesForGroup = group.files
                .distinctBy { if (it.path.isNotEmpty()) it.path else it.id.toString() }
                .filter { it.path.isEmpty() || it.path !in globallyUsedPaths }

            if (uniqueFilesForGroup.size > 1) {
                uniqueFilesForGroup.forEachIndexed { index, file ->
                    file.isSelected = index != 0
                }
                globallyUsedPaths.addAll(uniqueFilesForGroup.map { it.path }.filter { it.isNotEmpty() })
                finalUniqueGroups.add(group.copy(files = uniqueFilesForGroup))
            }
        }

        // Removed deceptive fallback

        onProgress(100, "Scan Complete. Found ${finalUniqueGroups.size} unique duplicate groups.")
        return@withContext finalUniqueGroups.sortedByDescending { it.size }
    }

    private fun scanDirectoryRecursively(
        dir: File,
        category: FileCategory,
        allFiles: MutableList<DuplicateFile>,
        existingPaths: HashSet<String>,
        depth: Int
    ) {
        if (depth > 12) return
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    scanDirectoryRecursively(file, category, allFiles, existingPaths, depth + 1)
                } else if (file.name == ".Statuses" || file.name == ".nomedia" || file.name == ".WhatsApp") {
                    scanDirectoryRecursively(file, category, allFiles, existingPaths, depth + 1)
                }
            } else if (file.isFile && file.length() > 200L) {
                val path = file.absolutePath
                if (!existingPaths.contains(path)) {
                    val ext = file.extension.lowercase()
                    val isMatch = when (category) {
                        FileCategory.IMAGE -> ext in listOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
                        FileCategory.VIDEO -> ext in listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv", "m4v", "ts")
                        FileCategory.AUDIO -> ext in listOf("mp3", "m4a", "wav", "aac", "ogg", "flac", "wma", "opus")
                        FileCategory.DOCUMENT -> ext in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
                    }

                    if (isMatch) {
                        existingPaths.add(path)
                        allFiles.add(
                            DuplicateFile(
                                id = file.hashCode().toLong(),
                                name = file.name,
                                path = path,
                                size = file.length(),
                                uri = Uri.fromFile(file),
                                category = category,
                                isSelected = false,
                                previewUrl = Uri.fromFile(file).toString()
                            )
                        )
                    }
                }
            }
        }
    }

    private fun computeMultiPointFileHash(context: Context, file: DuplicateFile): String {
        try {
            val md = MessageDigest.getInstance("MD5")
            var inputStream: InputStream? = null

            if (file.path.isNotEmpty()) {
                val f = File(file.path)
                if (f.exists() && f.canRead()) {
                    inputStream = FileInputStream(f)
                }
            }

            if (inputStream == null && file.uri != Uri.EMPTY) {
                try {
                    inputStream = context.contentResolver.openInputStream(file.uri)
                } catch (e: Exception) {
                    // ignore
                }
            }

            if (inputStream != null) {
                val fileSize = file.size
                val buffer = ByteArray(64 * 1024)

                val readStart = inputStream.read(buffer, 0, buffer.size)
                if (readStart > 0) {
                    md.update(buffer, 0, readStart)
                }

                if (fileSize > 256 * 1024) {
                    try {
                        val middleOffset = fileSize / 2
                        inputStream.skip(middleOffset - readStart)
                        val readMiddle = inputStream.read(buffer, 0, buffer.size)
                        if (readMiddle > 0) {
                            md.update(buffer, 0, readMiddle)
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                inputStream.close()
                val digest = md.digest()
                return digest.joinToString("") { "%02x".format(it) } + "_s${file.size}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "size_hash_${file.size}_${normalizeFileName(file.name)}"
    }

    private fun normalizeFileName(name: String): String {
        val base = name.substringBeforeLast(".")
        return base
            .lowercase()
            .replace(Regex("(?i)\\s*\\(copy\\)"), "")
            .replace(Regex("(?i)\\s*\\(\\d+\\)"), "")
            .replace(Regex("(?i)[_\\-]*(copy|duplicate|backup|wa|whatsapp|\\d{1,4})$"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }


}
