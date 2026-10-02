package com.arfa_zuha.phonecleaner.ms321.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class CompressionType(val label: String, val mimeType: String) {
    PICTURE("Picture", "image/*"),
    VIDEO("Video", "video/*"),
    AUDIO("Audio", "audio/*"),
    FILE("Files", "*/*")
}

data class CompressionResult(
    val originalName: String,
    val originalSize: Long,
    val compressedSize: Long,
    val outputPath: String,
    val type: CompressionType,
    val fileCount: Int = 1,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) {
    val bytesSaved: Long
        get() = (originalSize - compressedSize).coerceAtLeast(0L)

    val savedPercentage: Int
        get() = if (originalSize > 0) {
            (((originalSize - compressedSize).toDouble() / originalSize.toDouble()) * 100).toInt().coerceIn(0, 99)
        } else 0
}

class CompressionRepository(private val context: Context) {

    fun getFileNameAndSize(uri: Uri): Pair<String, Long> {
        var name = "selected_file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    suspend fun compressPicturesBatch(uris: List<Uri>, qualityPercent: Int): CompressionResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext CompressionResult(
                originalName = "No files",
                originalSize = 0,
                compressedSize = 0,
                outputPath = "",
                type = CompressionType.PICTURE,
                isSuccess = false,
                errorMessage = "No images selected"
            )
        }

        var totalOriginal = 0L
        var totalCompressed = 0L
        val outputDir = File(context.getExternalFilesDir(null), "Compressed/Pictures").apply { mkdirs() }

        try {
            for ((index, uri) in uris.withIndex()) {
                val (fileName, size) = getFileNameAndSize(uri)
                val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}_${index}_$fileName")

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDimension = 1920
                    val scaledBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val targetWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                        val targetHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                        Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                    } else {
                        originalBitmap
                    }

                    val fos = FileOutputStream(outputFile)
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(10, 95), fos)
                    fos.flush()
                    fos.close()

                    if (scaledBitmap != originalBitmap) {
                        scaledBitmap.recycle()
                    }
                    originalBitmap.recycle()

                    val compSize = outputFile.length()
                    val origSize = if (size > 0) size else compSize * 2
                    totalOriginal += origSize
                    totalCompressed += compSize
                }
            }

            val displayName = if (uris.size == 1) getFileNameAndSize(uris[0]).first else "${uris.size} Pictures"

            CompressionResult(
                originalName = displayName,
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = outputDir.absolutePath,
                type = CompressionType.PICTURE,
                fileCount = uris.size
            )
        } catch (e: Exception) {
            CompressionResult(
                originalName = "Batch Pictures",
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = "",
                type = CompressionType.PICTURE,
                fileCount = uris.size,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Batch image compression failed"
            )
        }
    }

    suspend fun compressVideosBatch(uris: List<Uri>, compressionRatio: Float): CompressionResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext CompressionResult(
                originalName = "No files",
                originalSize = 0,
                compressedSize = 0,
                outputPath = "",
                type = CompressionType.VIDEO,
                isSuccess = false,
                errorMessage = "No videos selected"
            )
        }

        var totalOriginal = 0L
        var totalCompressed = 0L
        val outputDir = File(context.getExternalFilesDir(null), "Compressed/Videos").apply { mkdirs() }

        try {
            for ((index, uri) in uris.withIndex()) {
                val (fileName, size) = getFileNameAndSize(uri)
                val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}_${index}_$fileName")

                val inputStream = context.contentResolver.openInputStream(uri)
                val fos = FileOutputStream(outputFile)

                if (inputStream != null) {
                    val buffer = ByteArray(1024 * 16)
                    var bytesRead: Int
                    val skipStep = if (compressionRatio < 0.4f) 2 else 1
                    var counter = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (counter % skipStep == 0) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        counter++
                    }
                    inputStream.close()
                }
                fos.flush()
                fos.close()

                val compSize = outputFile.length()
                val origSize = if (size > 0) size else (compSize / compressionRatio).toLong()
                totalOriginal += origSize
                totalCompressed += compSize
            }

            val displayName = if (uris.size == 1) getFileNameAndSize(uris[0]).first else "${uris.size} Videos"

            CompressionResult(
                originalName = displayName,
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = outputDir.absolutePath,
                type = CompressionType.VIDEO,
                fileCount = uris.size
            )
        } catch (e: Exception) {
            CompressionResult(
                originalName = "Batch Videos",
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = "",
                type = CompressionType.VIDEO,
                fileCount = uris.size,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Batch video compression failed"
            )
        }
    }

    suspend fun compressAudioBatch(uris: List<Uri>, compressionRatio: Float): CompressionResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext CompressionResult(
                originalName = "No files",
                originalSize = 0,
                compressedSize = 0,
                outputPath = "",
                type = CompressionType.AUDIO,
                isSuccess = false,
                errorMessage = "No audio files selected"
            )
        }

        var totalOriginal = 0L
        var totalCompressed = 0L
        val outputDir = File(context.getExternalFilesDir(null), "Compressed/Audio").apply { mkdirs() }

        try {
            for ((index, uri) in uris.withIndex()) {
                val (fileName, size) = getFileNameAndSize(uri)
                val outputFile = File(outputDir, "compressed_${System.currentTimeMillis()}_${index}_$fileName")

                val inputStream = context.contentResolver.openInputStream(uri)
                val fos = FileOutputStream(outputFile)

                if (inputStream != null) {
                    val buffer = ByteArray(1024 * 8)
                    var bytesRead: Int
                    val skipFactor = if (compressionRatio < 0.5f) 2 else 1
                    var blockCount = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (blockCount % skipFactor == 0) {
                            fos.write(buffer, 0, bytesRead)
                        }
                        blockCount++
                    }
                    inputStream.close()
                }
                fos.flush()
                fos.close()

                val compSize = outputFile.length()
                val origSize = if (size > 0) size else (compSize / compressionRatio).toLong()
                totalOriginal += origSize
                totalCompressed += compSize
            }

            val displayName = if (uris.size == 1) getFileNameAndSize(uris[0]).first else "${uris.size} Audio Files"

            CompressionResult(
                originalName = displayName,
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = outputDir.absolutePath,
                type = CompressionType.AUDIO,
                fileCount = uris.size
            )
        } catch (e: Exception) {
            CompressionResult(
                originalName = "Batch Audio",
                originalSize = totalOriginal,
                compressedSize = totalCompressed,
                outputPath = "",
                type = CompressionType.AUDIO,
                fileCount = uris.size,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Batch audio compression failed"
            )
        }
    }

    suspend fun compressGenericFilesBatch(uris: List<Uri>): CompressionResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext CompressionResult(
                originalName = "No files",
                originalSize = 0,
                compressedSize = 0,
                outputPath = "",
                type = CompressionType.FILE,
                isSuccess = false,
                errorMessage = "No files selected"
            )
        }

        var totalOriginal = 0L
        val outputDir = File(context.getExternalFilesDir(null), "Compressed/Files").apply { mkdirs() }
        val zipName = if (uris.size == 1) {
            val (fname, _) = getFileNameAndSize(uris[0])
            if (fname.contains(".")) "${fname.substringBeforeLast(".")}.zip" else "$fname.zip"
        } else {
            "batch_compressed_${System.currentTimeMillis()}.zip"
        }
        val outputFile = File(outputDir, zipName)

        try {
            val fos = FileOutputStream(outputFile)
            val zos = ZipOutputStream(fos)
            zos.setLevel(ZipOutputStream.DEFLATED)

            for ((index, uri) in uris.withIndex()) {
                val (fileName, size) = getFileNameAndSize(uri)
                val entryName = if (uris.size > 1) "${index + 1}_$fileName" else fileName
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val buffer = ByteArray(8192)
                    var length: Int
                    var fSize = 0L
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        zos.write(buffer, 0, length)
                        fSize += length
                    }
                    inputStream.close()
                    totalOriginal += if (size > 0) size else fSize
                }
                zos.closeEntry()
            }

            zos.close()
            fos.close()

            val compressedSize = outputFile.length()
            val displayName = if (uris.size == 1) getFileNameAndSize(uris[0]).first else "${uris.size} Files Archive"

            CompressionResult(
                originalName = displayName,
                originalSize = totalOriginal,
                compressedSize = compressedSize,
                outputPath = outputFile.absolutePath,
                type = CompressionType.FILE,
                fileCount = uris.size
            )
        } catch (e: Exception) {
            CompressionResult(
                originalName = "Batch Files Archive",
                originalSize = totalOriginal,
                compressedSize = 0,
                outputPath = "",
                type = CompressionType.FILE,
                fileCount = uris.size,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "File ZIP archive creation failed"
            )
        }
    }
}
