package com.arfa_zuha.phonecleaner.ms321.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UninstallerRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packageMap = mutableMapOf<String, InstalledAppItem>()

        try {
            val packages = pm.getInstalledPackages(0)
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val packageName = pkg.packageName

                // Do not show our own cleaner app in uninstaller
                if (packageName == context.packageName) continue

                // Verify package is actually installed
                try {
                    pm.getPackageInfo(packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = appInfo.loadLabel(pm).toString()

                // Calculate total size including base APK and split APKs
                var size = 0L
                try {
                    if (appInfo.sourceDir != null) {
                        val baseFile = File(appInfo.sourceDir)
                        if (baseFile.exists()) {
                            size += baseFile.length()
                        }
                    }
                    if (appInfo.splitSourceDirs != null) {
                        for (splitDir in appInfo.splitSourceDirs) {
                            val splitFile = File(splitDir)
                            if (splitFile.exists()) {
                                size += splitFile.length()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }

                val installTime = pkg.firstInstallTime

                packageMap[packageName] = InstalledAppItem(
                    packageName = packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    sizeBytes = size,
                    icon = null,
                    isSystemApp = isSystem,
                    installTimeMillis = installTime
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also query launcher activities to guarantee 100% coverage
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in launcherApps) {
                val appInfo = resolveInfo.activityInfo.applicationInfo ?: continue
                val pkgName = appInfo.packageName
                if (pkgName == context.packageName) continue

                if (!packageMap.containsKey(pkgName)) {
                    try {
                        pm.getPackageInfo(pkgName, 0)
                    } catch (e: PackageManager.NameNotFoundException) {
                        continue
                    }

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appName = resolveInfo.loadLabel(pm).toString()
                    var size = 0L
                    try {
                        if (appInfo.sourceDir != null) {
                            val baseFile = File(appInfo.sourceDir)
                            if (baseFile.exists()) {
                                size += baseFile.length()
                            }
                        }
                        if (appInfo.splitSourceDirs != null) {
                            for (splitDir in appInfo.splitSourceDirs) {
                                val splitFile = File(splitDir)
                                if (splitFile.exists()) {
                                    size += splitFile.length()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }

                    packageMap[pkgName] = InstalledAppItem(
                        packageName = pkgName,
                        appName = appName,
                        versionName = "1.0",
                        sizeBytes = size,
                        icon = null,
                        isSystemApp = isSystem,
                        installTimeMillis = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        packageMap.values.sortedByDescending { it.sizeBytes }
    }

    suspend fun scanApkFiles(): List<ApkFileItem> = withContext(Dispatchers.IO) {
        val apkList = mutableListOf<ApkFileItem>()
        val pm = context.packageManager

        try {
            val searchDirs = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStorageDirectory()
            )

            for (dir in searchDirs) {
                if (dir != null && dir.exists() && dir.isDirectory) {
                    findApksInDirectory(dir, apkList, pm)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        apkList.distinctBy { it.path }.sortedByDescending { it.sizeBytes }
    }

    private fun findApksInDirectory(dir: File, resultList: MutableList<ApkFileItem>, pm: PackageManager, depth: Int = 0) {
        if (depth > 3) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                findApksInDirectory(file, resultList, pm, depth + 1)
            } else if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                try {
                    val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
                    val appName = if (archiveInfo != null) {
                        archiveInfo.applicationInfo?.loadLabel(pm)?.toString() ?: file.nameWithoutExtension
                    } else {
                        file.nameWithoutExtension
                    }

                    resultList.add(
                        ApkFileItem(
                            path = file.absolutePath,
                            fileName = file.name,
                            packageName = archiveInfo?.packageName ?: appName,
                            versionName = archiveInfo?.versionName ?: "1.0",
                            sizeBytes = file.length(),
                            icon = null,
                            dateModified = file.lastModified()
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun deleteApkFiles(paths: List<String>): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var count = 0
        var freed = 0L
        for (path in paths) {
            try {
                val f = File(path)
                val len = if (f.exists()) f.length() else 0L
                var deleted = false

                if (f.exists()) {
                    deleted = f.delete()
                }

                // If direct delete returned false (e.g. Scoped Storage), use MediaStore content provider delete
                if (!deleted) {
                    try {
                        val uri = MediaStore.Files.getContentUri("external")
                        val rows = context.contentResolver.delete(
                            uri,
                            "${MediaStore.MediaColumns.DATA}=?",
                            arrayOf(path)
                        )
                        if (rows > 0) {
                            deleted = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Also query by MediaStore file ID if path match failed
                if (!deleted) {
                    try {
                        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
                        val cursor = context.contentResolver.query(
                            MediaStore.Files.getContentUri("external"),
                            projection,
                            "${MediaStore.Files.FileColumns.DATA}=?",
                            arrayOf(path),
                            null
                        )
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                                val deleteUri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString())
                                val countDeleted = context.contentResolver.delete(deleteUri, null, null)
                                if (countDeleted > 0) {
                                    deleted = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Notify media scanner to remove entry from media database
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                } catch (e: Exception) {
                    // ignore
                }

                if (deleted || !f.exists()) {
                    count++
                    freed += len
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Pair(count, freed)
    }
}
