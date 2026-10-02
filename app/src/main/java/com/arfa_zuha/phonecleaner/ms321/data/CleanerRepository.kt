package com.arfa_zuha.phonecleaner.ms321.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CleanerRepository(private val context: Context) {

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - freeBytes

        // Calculate cache size safely from context.cacheDir, externalCacheDir, and public junk files
        val cacheSize = calculateJunkSize()

        // Query actual sizes from MediaStore for accurate dashboard pie chart
        var imagesBytes = 0L
        var videosBytes = 0L
        var audioBytes = 0L
        var documentsBytes = 0L
        try {
            val projection = arrayOf("SUM(${android.provider.MediaStore.MediaColumns.SIZE})")
            context.contentResolver.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { if (it.moveToFirst()) imagesBytes = it.getLong(0) }
            context.contentResolver.query(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { if (it.moveToFirst()) videosBytes = it.getLong(0) }
            context.contentResolver.query(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { if (it.moveToFirst()) audioBytes = it.getLong(0) }
            context.contentResolver.query(android.provider.MediaStore.Files.getContentUri("external"), projection, "${android.provider.MediaStore.Files.FileColumns.MIME_TYPE} IN ('application/pdf', 'application/msword', 'text/plain')", null, null)?.use { if (it.moveToFirst()) documentsBytes = it.getLong(0) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        StorageInfo(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            cacheBytes = cacheSize,
            imagesBytes = imagesBytes,
            videosBytes = videosBytes,
            audioBytes = audioBytes,
            documentsBytes = documentsBytes
        )
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        try {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        getDirSize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    private fun calculateJunkSize(): Long {
        var scannedJunk = 0L

        // 1. App Cache Directories
        scannedJunk += getDirSize(context.cacheDir)
        scannedJunk += (context.externalCacheDir?.let { getDirSize(it) } ?: 0L)

        // 2. Scan Public Storage Directories for .tmp, .temp, .log, .thumbnails, .cache files
        try {
            val searchDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStorageDirectory()
            )

            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scannedJunk += scanJunkRecursively(dir, delete = false, depth = 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return scannedJunk
    }

    private fun scanJunkRecursively(dir: File, delete: Boolean, depth: Int): Long {
        if (depth > 4) return 0L
        var junkSize = 0L
        val files = dir.listFiles() ?: return 0L

        for (file in files) {
            if (file.isDirectory) {
                val isJunkFolder = file.name.equals(".thumbnails", ignoreCase = true) ||
                        file.name.equals(".thumb", ignoreCase = true) ||
                        file.name.equals(".cache", ignoreCase = true) ||
                        file.name.equals("cache", ignoreCase = true) ||
                        file.name.equals(".Trash", ignoreCase = true) ||
                        file.name.equals("Trash", ignoreCase = true) ||
                        file.name.equals(".Statuses", ignoreCase = true)

                if (isJunkFolder) {
                    val dirSize = getDirSize(file)
                    junkSize += dirSize
                    if (delete) {
                        deleteDirContents(file)
                        file.delete()
                    }
                } else if (!file.name.startsWith(".")) {
                    junkSize += scanJunkRecursively(file, delete, depth + 1)
                }
            } else if (file.isFile) {
                val name = file.name.lowercase()
                val ext = file.extension.lowercase()
                val isJunkFile = ext in listOf("tmp", "temp", "log", "bak", "chk", "part", "crdownload", "dime", "old") ||
                        (ext == "apk" && file.parentFile?.name != "apk") ||
                        name.startsWith("cache_") || name.endsWith(".cache")

                if (isJunkFile) {
                    val len = file.length()
                    junkSize += len
                    if (delete) {
                        try {
                            file.delete()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }
        return junkSize
    }

    suspend fun clearJunkFiles(): Long = withContext(Dispatchers.IO) {
        var clearedBytes = 0L
        try {
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                clearedBytes += deleteDirContents(cacheDir)
            }
            val extCacheDir = context.externalCacheDir
            if (extCacheDir != null && extCacheDir.exists()) {
                clearedBytes += deleteDirContents(extCacheDir)
            }
            val searchDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            )
            for (dir in searchDirs) {
                if (dir.exists() && dir.isDirectory) {
                    clearedBytes += scanJunkRecursively(dir, delete = true, depth = 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext clearedBytes
    }

    private fun deleteDirContents(dir: File): Long {
        var deletedSize = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            if (file.isDirectory) {
                deletedSize += deleteDirContents(file)
                file.delete()
            } else {
                val length = file.length()
                if (file.delete()) {
                    deletedSize += length
                }
            }
        }
        return deletedSize
    }

    suspend fun getInstalledApps(): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfoItem>()

        val hasPermission = AppUsageRepository.hasUsagePermission(context)
        val usageStatsMap = mutableMapOf<String, Long>()

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            val endTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.MONTH, -12) // Query up to last 12 months
            val startTime = calendar.timeInMillis
            try {
                // 1. Query aggregated usage stats
                val aggregatedStats = usageStatsManager?.queryAndAggregateUsageStats(startTime, endTime)
                aggregatedStats?.forEach { (pkg, stats) ->
                    if (stats.lastTimeUsed > (usageStatsMap[pkg] ?: 0L)) {
                        usageStatsMap[pkg] = stats.lastTimeUsed
                    }
                }

                // 2. Query daily stats to ensure exact recent usage times (e.g. today/yesterday)
                val dailyStats = usageStatsManager?.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                dailyStats?.forEach { stats ->
                    val pkg = stats.packageName
                    val lastUsed = stats.lastTimeUsed
                    if (lastUsed > (usageStatsMap[pkg] ?: 0L)) {
                        usageStatsMap[pkg] = lastUsed
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            @Suppress("DEPRECATION")
            val isGame = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                    (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME)

            val appName = appInfo.loadLabel(pm).toString()
            val versionName = pkg.versionName ?: "1.0"
            
            var size = 0L
            try {
                if (appInfo.sourceDir != null) {
                    size = File(appInfo.sourceDir).length()
                }
            } catch (e: Exception) {
                // ignore
            }
            
            // If permission is not granted, set lastUsed to -1L (unknown) instead of 0L
            val lastUsed = if (hasPermission) (usageStatsMap[pkg.packageName] ?: 0L) else -1L

            appList.add(
                AppInfoItem(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = versionName,
                    packageSize = size,
                    isSystemApp = isSystem,
                    lastTimeUsed = lastUsed,
                    firstInstallTime = pkg.firstInstallTime,
                    isGame = isGame
                )
            )
        }
        appList.sortedByDescending { it.packageSize }
    }
}
