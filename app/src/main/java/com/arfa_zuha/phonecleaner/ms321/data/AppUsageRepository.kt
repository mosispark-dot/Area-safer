package com.arfa_zuha.phonecleaner.ms321.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.arfa_zuha.phonecleaner.ms321.data.AppUsageItem
import com.arfa_zuha.phonecleaner.ms321.data.UsageSummary
import com.arfa_zuha.phonecleaner.ms321.data.UsageTimeFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object AppUsageRepository {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun getAppUsageStats(context: Context, timeFrame: UsageTimeFrame): UsageSummary = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        when (timeFrame) {
            UsageTimeFrame.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            UsageTimeFrame.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
            }
            UsageTimeFrame.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            UsageTimeFrame.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
            }
        }
        val startTime = calendar.timeInMillis

        val usageStatsList: List<UsageStats>? = if (hasUsagePermission(context) && usageStatsManager != null) {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
        } else {
            null
        }

        val rawAppMap = mutableMapOf<String, Long>()
        var totalUsageTime = 0L

        if (!usageStatsList.isNullOrEmpty()) {
            for (stats in usageStatsList) {
                if (stats.totalTimeInForeground > 1000L) {
                    val pkg = stats.packageName
                    val current = rawAppMap[pkg] ?: 0L
                    rawAppMap[pkg] = current + stats.totalTimeInForeground
                }
            }
        }

        val resultItems = mutableListOf<AppUsageItem>()

        if (rawAppMap.isNotEmpty()) {
            totalUsageTime = rawAppMap.values.sum()

            for ((pkg, usageTime) in rawAppMap) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    // Exclude launcher system background core services
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && usageTime < 30000L) {
                        continue
                    }
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val percentage = if (totalUsageTime > 0) (usageTime.toFloat() / totalUsageTime.toFloat()) * 100f else 0f

                    resultItems.add(
                        AppUsageItem(
                            packageName = pkg,
                            appName = label,
                            usageTimeMillis = usageTime,
                            usagePercentage = percentage,
                            lastUsedTimeMillis = endTime - (usageTime / 2),
                            icon = icon
                        )
                    )
                } catch (e: Exception) {
                    // App uninstalled or unavailable
                }
            }
        }

        // Sort items by highest screen usage time
        val sortedList = resultItems.sortedByDescending { it.usageTimeMillis }

        if (sortedList.isNotEmpty()) {
            val top = sortedList.first()
            return@withContext UsageSummary(
                totalTimeMillis = totalUsageTime,
                topApp = top,
                appUsageList = sortedList
            )
        } else {
            return@withContext UsageSummary(totalTimeMillis = 0L, topApp = null, appUsageList = emptyList())
        }
    }

}
