package com.arfa_zuha.phonecleaner.ms321.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.R
import com.arfa_zuha.phonecleaner.ms321.data.MediaType
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerUiState
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel

@Composable
fun DashboardScreen(
    uiState: CleanerUiState,
    viewModel: CleanerViewModel,
    onNavigateToCleaner: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToCompression: () -> Unit = {},
    onNavigateToBattery: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToUninstaller: () -> Unit = {},
    onNavigateToAppUsage: () -> Unit = {},
    onNavigateToFileManager: () -> Unit = {},
    onNavigateToMediaCategory: (MediaType) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {}
) {
    val storage = uiState.storageInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Normal Sized AI Cleaner Branding Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ai_cleaner_icon),
                        contentDescription = "AI Cleaner Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column {
                    Text(
                        text = "Sky Clean AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Storage & Memory Optimizer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Premium PRO Badge Button
            Surface(
                onClick = onNavigateToPaywall,
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Go Premium",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PRO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }

        // Display the banner ad prominently at the top
        com.arfa_zuha.phonecleaner.ms321.ads.NativeBannerAd()

        // Smart & Compact Storage Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "SYSTEM SECURE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = storage?.let { "${it.usedPercentage.toInt()}% Used" } ?: "Analyzing...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Compact Circular Storage Dial
                    Box(
                        modifier = Modifier.size(105.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { storage?.let { it.usedPercentage / 100f } ?: 0f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = storage?.let { viewModel.formatBytes(it.freeBytes) } ?: "0 GB",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Free Available",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (storage != null) {
                        Text(
                            text = "${viewModel.formatBytes(storage.usedBytes)} used of ${viewModel.formatBytes(storage.totalBytes)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sleek Smart Clean Button
                    Button(
                        onClick = onNavigateToCleaner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Smart Clean Now",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        val sixMonthsAgo = androidx.compose.runtime.remember { System.currentTimeMillis() - (180L * 24L * 60L * 60L * 1000L) }
        val context = androidx.compose.ui.platform.LocalContext.current
        val hasUsagePermission = androidx.compose.runtime.remember(context) { com.arfa_zuha.phonecleaner.ms321.data.AppUsageRepository.hasUsagePermission(context) }
        val unusedAppsCount = androidx.compose.runtime.remember(uiState.installedApps, sixMonthsAgo, hasUsagePermission) {
            if (!hasUsagePermission) 0
            else uiState.installedApps.count { app ->
                !app.isSystemApp &&
                        app.lastTimeUsed != -1L &&
                        (app.firstInstallTime <= 0L || app.firstInstallTime < sixMonthsAgo) &&
                        (app.lastTimeUsed <= 0L || app.lastTimeUsed < sixMonthsAgo)
            }
        }

        // Section 1: Advanced Tools Group
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Advanced Tools",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Row 1: Compressions & Battery Optimizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Compressions",
                    subtitle = "Reduce media size",
                    icon = Icons.Default.FolderZip,
                    onClick = onNavigateToCompression
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery Info",
                    subtitle = "Power & health status",
                    icon = Icons.Default.BatteryChargingFull,
                    onClick = onNavigateToBattery
                )
            }

            // Row 2: AI Cleaner & File Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InteractiveFeatureCardWithDrawable(
                    modifier = Modifier.weight(1f),
                    title = "AI Cleaner",
                    subtitle = "Smart junk scan",
                    drawableRes = R.drawable.ai_cleaner_icon,
                    onClick = onNavigateToCleaner
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "File Manager",
                    subtitle = "Browse categories",
                    icon = Icons.Default.FolderSpecial,
                    onClick = onNavigateToFileManager
                )
            }

            // Row 3: App Usage & Duplicate Files
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "App Usage",
                    subtitle = "Screen time & stats",
                    icon = Icons.Default.BarChart,
                    onClick = onNavigateToAppUsage
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Duplicate Files",
                    subtitle = "Find & remove clones",
                    icon = Icons.Default.CopyAll,
                    onClick = onNavigateToDuplicates
                )
            }
        }

        // Section 2: Interactive Features Group
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Interactive Features",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Interactive Clickable Feature Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "APK Uninstaller",
                    subtitle = "Clean installer files",
                    icon = Icons.Default.InstallMobile,
                    onClick = onNavigateToUninstaller
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Unused Apps & Games",
                    subtitle = if (unusedAppsCount > 0) "$unusedAppsCount Unused (6+ Mo)" else "${uiState.installedApps.size} Apps & Games",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = onNavigateToApps
                )
            }
        }

        // Medium Rectangle AdMob Ad below APK Uninstaller & Unused Apps
        com.arfa_zuha.phonecleaner.ms321.ads.MediumRectangleAd(modifier = Modifier.padding(vertical = 4.dp))

        // Storage Breakdown Card
        if (storage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(imageVector = Icons.Default.DonutLarge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    CategoryProgressRow(
                        label = "Images & Photos",
                        size = viewModel.formatBytes(storage.imagesBytes),
                        fraction = 0.25f,
                        onClick = { onNavigateToMediaCategory(MediaType.IMAGE) }
                    )
                    CategoryProgressRow(
                        label = "Videos & Media",
                        size = viewModel.formatBytes(storage.videosBytes),
                        fraction = 0.35f,
                        onClick = { onNavigateToMediaCategory(MediaType.VIDEO) }
                    )
                    CategoryProgressRow(
                        label = "Audio & Music",
                        size = viewModel.formatBytes(storage.audioBytes),
                        fraction = 0.10f,
                        onClick = { onNavigateToMediaCategory(MediaType.AUDIO) }
                    )
                    CategoryProgressRow(
                        label = "Documents",
                        size = viewModel.formatBytes(storage.documentsBytes),
                        fraction = 0.10f,
                        onClick = { onNavigateToMediaCategory(MediaType.DOCUMENT) }
                    )
                    CategoryProgressRow(
                        label = "Junk & Temp Files",
                        size = viewModel.formatBytes(storage.cacheBytes),
                        fraction = 0.05f,
                        onClick = onNavigateToCleaner
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun InteractiveFeatureCardWithDrawable(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    drawableRes: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoryProgressRow(
    label: String,
    size: String,
    fraction: Float,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Section",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = size, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
