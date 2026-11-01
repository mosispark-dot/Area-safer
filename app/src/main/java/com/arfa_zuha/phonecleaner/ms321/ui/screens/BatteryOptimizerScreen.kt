package com.arfa_zuha.phonecleaner.ms321.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.arfa_zuha.phonecleaner.ms321.viewmodel.BatteryUiState
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel

@Composable
fun BatteryOptimizerScreen(
    batteryState: BatteryUiState,
    viewModel: CleanerViewModel,
    onBackClick: () -> Unit = {}
) {
    val info = batteryState.batteryInfo
    val level = info?.level ?: 50

    // Dynamic Battery Color based on Level
    val batteryColor = when {
        level > 50 -> Color(0xFF10B981) // Emerald Green
        level > 20 -> Color(0xFFF59E0B) // Amber Yellow
        else -> Color(0xFFEF4444)       // Red
    }

    Surface(
        color = Color(0xFFFAFAFC),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBackClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Battery Info & Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Monitor live battery stats",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Hero Battery Dial & Remaining Time Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Live Status Badge (Charging vs Discharging)
                    Badge(
                        containerColor = if (info?.isCharging == true) Color(0xFF0284C7) else batteryColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (info?.isCharging == true) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (info?.isCharging == true) "CHARGING (${info.pluggedSource})" else "DISCHARGING",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Battery Circular Gauge
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { level / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14.dp,
                            color = batteryColor,
                            trackColor = batteryColor.copy(alpha = 0.15f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$level%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Remaining",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Live Battery Hardware Metrics Grid
            Text(
                text = "Battery Hardware Stats",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BatteryMetricCard(
                    title = "Health",
                    value = info?.health ?: "Good",
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricCard(
                    title = "Temperature",
                    value = "${info?.temperatureCelsius ?: 28.0}°C",
                    icon = Icons.Default.Thermostat,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BatteryMetricCard(
                    title = "Voltage",
                    value = "${info?.voltageVolts ?: 3.8} V",
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )
                BatteryMetricCard(
                    title = "Technology",
                    value = info?.technology ?: "Li-ion",
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f)
                )
            }

            // Advanced System Settings Shortcuts
            Text(
                text = "System Power Controls",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val localContext = androidx.compose.ui.platform.LocalContext.current
                
                Button(
                    onClick = { 
                        try {
                            localContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS))
                        } catch (e: Exception) {
                            localContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Power Saver", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { 
                        try {
                            localContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))
                        } catch (e: Exception) {
                            localContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                ) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Display Config", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Battery Care Tips Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "Battery Lifespan Tips",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = "• Keep charge levels between 20% and 80% for long-term health.\n• Avoid using heavy games while charging to prevent overheating.\n• Use Dark Mode on OLED screens to reduce battery drain.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B))
                Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        }
    }
}

