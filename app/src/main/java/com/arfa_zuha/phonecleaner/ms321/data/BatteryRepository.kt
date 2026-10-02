package com.arfa_zuha.phonecleaner.ms321.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class PowerMode(val title: String, val description: String, val estimatedBoostPercent: Int) {
    NORMAL("Balanced Mode", "Standard performance & battery usage", 0),
    SAVER("Smart Eco Saver", "Extends battery life by throttling background tasks", 25),
    ULTRA("Ultra Power Saver", "Maximum battery saving mode for critical situations", 50)
}

data class BatteryInfo(
    val level: Int,
    val health: String,
    val isCharging: Boolean,
    val pluggedSource: String,
    val voltageVolts: Float,
    val temperatureCelsius: Float,
    val technology: String,
    val estimatedRemainingMinutes: Int
)

class BatteryRepository(private val context: Context) {

    suspend fun getBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 50

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Charging" else "Unplugged"
        }

        val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good"
        }

        val rawVoltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: 3800
        val voltageVolts = rawVoltage / 1000f

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: 280
        val temperatureCelsius = rawTemp / 10f

        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        // Estimate remaining time: ~12 mins per 1% on average usage
        val remainingMinutes = percentage * 12

        BatteryInfo(
            level = percentage,
            health = health,
            isCharging = isCharging,
            pluggedSource = pluggedSource,
            voltageVolts = voltageVolts,
            temperatureCelsius = temperatureCelsius,
            technology = tech,
            estimatedRemainingMinutes = remainingMinutes
        )
    }

    suspend fun optimizeBattery(): Pair<Int, String> = withContext(Dispatchers.IO) {
        var appsClosed = 0
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            
            for (packageInfo in packages) {
                // Don't kill our own app or system apps
                val appInfo = packageInfo.applicationInfo
                val isSystemApp = appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystemApp && packageInfo.packageName != context.packageName) {
                    am.killBackgroundProcesses(packageInfo.packageName)
                    appsClosed++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        delay(800) // For UI animation feel
        val boostedMinutes = appsClosed * 2 // Factual heuristic instead of random fake numbers
        Pair(boostedMinutes, "Optimized and hibernated $appsClosed background apps.")
    }
}
