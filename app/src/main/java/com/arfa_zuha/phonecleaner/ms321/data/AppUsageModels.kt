package com.arfa_zuha.phonecleaner.ms321.data

import android.graphics.drawable.Drawable

enum class UsageTimeFrame(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEK("7 Days"),
    MONTH("30 Days")
}

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val usagePercentage: Float,
    val lastUsedTimeMillis: Long,
    val launchCount: Int = 1,
    val icon: Drawable? = null
)

data class UsageSummary(
    val totalTimeMillis: Long,
    val topApp: AppUsageItem?,
    val appUsageList: List<AppUsageItem>
)
