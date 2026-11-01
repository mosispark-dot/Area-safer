package com.arfa_zuha.phonecleaner.ms321.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.R
import com.arfa_zuha.phonecleaner.ms321.data.AppUsageRepository
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
    onNavigateToPaywall: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val storage = uiState.storageInfo

    // Smooth orbital animations matching reference video
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")

    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot"
    )

    val counterOrbitRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_orbit_rot"
    )

    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Clean Top Header with Hamburger Menu + Branding Logo + VIP PRO Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Navigation Drawer Menu Toggle
                Surface(
                    onClick = onOpenDrawer,
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ai_cleaner_icon),
                        contentDescription = "Phone Cleaner Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column {
                    Text(
                        text = "Phone Cleaner",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Storage & Memory Optimizer",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Circular VIP Premium Crown Icon Button (Directly opens Paywall)
            val proInteraction = remember { MutableInteractionSource() }
            val isProPressed by proInteraction.collectIsPressedAsState()
            val proScale by animateFloatAsState(
                targetValue = if (isProPressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "pro_scale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = proScale; scaleY = proScale }
                    .size(38.dp)
                    .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0xFFFFB300))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFE082),
                                Color(0xFFFFB300),
                                Color(0xFFFF8F00)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = proInteraction,
                        indication = null,
                        onClick = onNavigateToPaywall
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "VIP Premium",
                    tint = Color(0xFF261C02),
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        // Top Banner Ad (Using AdConfig testing/production AdMob ID)
        com.arfa_zuha.phonecleaner.ms321.ads.NativeBannerAd()

        // Smart Clean Hero Animation Widget (Smart, Compact & Decent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // Central Animated Dial with Orbiting Glowing Arcs (Compact & Balanced)
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Primary Glowing Arc Rotating Clockwise
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .rotate(orbitRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF38BDF8).copy(alpha = 0.25f),
                                Color(0xFF38BDF8),
                                Color(0xFF10B981),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 145f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 2. Secondary Glowing Arc Counter-Rotating
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .rotate(counterOrbitRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF10B981).copy(alpha = 0.2f),
                                Color(0xFF10B981),
                                Color(0xFF38BDF8),
                                Color.Transparent
                            )
                        ),
                        startAngle = 180f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. Central Clean Light Core Circle
                Box(
                    modifier = Modifier
                        .size(114.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF0FDF4).copy(alpha = 0.95f),
                                    Color(0xFFE0F2FE).copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Junk Files",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Inner Clean Button (Smart, compact & handsome)
                        val cleanInteraction = remember { MutableInteractionSource() }
                        val isCleanPressed by cleanInteraction.collectIsPressedAsState()
                        val cleanScale by animateFloatAsState(
                            targetValue = if (isCleanPressed) 0.92f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "inner_clean_scale"
                        )

                        Surface(
                            onClick = onNavigateToCleaner,
                            interactionSource = cleanInteraction,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 1.5.dp,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(88.dp)
                                .height(30.dp)
                                .graphicsLayer { scaleX = cleanScale; scaleY = cleanScale }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clean",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Medium Rectangle Ad with Professional Card Boundaries (Using AdConfig testing/production AdMob ID)
        com.arfa_zuha.phonecleaner.ms321.ads.MediumRectangleAd(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        )

        val sixMonthsAgo = remember { System.currentTimeMillis() - (180L * 24L * 60L * 60L * 1000L) }
        val context = LocalContext.current
        val hasUsagePermission = remember(context) { AppUsageRepository.hasUsagePermission(context) }
        val unusedAppsCount = remember(uiState.installedApps, sixMonthsAgo, hasUsagePermission) {
            if (!hasUsagePermission) 0
            else uiState.installedApps.count { app ->
                !app.isSystemApp &&
                        app.lastTimeUsed != -1L &&
                        (app.firstInstallTime <= 0L || app.firstInstallTime < sixMonthsAgo) &&
                        (app.lastTimeUsed <= 0L || app.lastTimeUsed < sixMonthsAgo)
            }
        }

        // Section 1: Cleaner & Optimizer Tools
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Cleaner & Performance Tools",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Row 1: Junk Cleaner & Duplicate Files
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Junk Cleaner",
                    icon = Icons.Default.CleaningServices,
                    accentColor = Color(0xFFEA580C), // Soft Warm Coral
                    onClick = onNavigateToCleaner
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Duplicate Files",
                    icon = Icons.Default.CopyAll,
                    accentColor = Color(0xFF2563EB), // Clean Royal Blue
                    onClick = onNavigateToDuplicates
                )
            }

            // Row 2: Photo/Media Compressor & Battery Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Compressions",
                    icon = Icons.Default.FolderZip,
                    accentColor = Color(0xFF7C3AED), // Soft Modern Purple
                    onClick = onNavigateToCompression
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery Info",
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = Color(0xFF0891B2), // Clean Cyan
                    onClick = onNavigateToBattery
                )
            }
        }

        // Section 2: Management & Storage Tools
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Management & Storage",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Row 3: App Usage & File Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "App Usage",
                    icon = Icons.Default.BarChart,
                    accentColor = Color(0xFF059669), // Soft Emerald Green
                    onClick = onNavigateToAppUsage
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "File Manager",
                    icon = Icons.Default.FolderSpecial,
                    accentColor = Color(0xFFD97706), // Soft Amber Gold
                    onClick = onNavigateToFileManager
                )
            }

            // Row 4: APK Uninstaller & Unused Apps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "APK Uninstaller",
                    icon = Icons.Default.InstallMobile,
                    accentColor = Color(0xFFDC2626), // Soft Rose Red
                    onClick = onNavigateToUninstaller
                )

                InteractiveFeatureCard(
                    modifier = Modifier.weight(1f),
                    title = "Unused Apps",
                    icon = Icons.Default.PhoneAndroid,
                    accentColor = Color(0xFF0D9488), // Soft Teal Mint
                    onClick = onNavigateToApps
                )
            }
        }

        // Section 3: Storage Breakdown in ONE Unified Light Color
        if (storage != null) {
            val unifiedLightColor = Color(0xFF60A5FA) // Completely light pastel blue

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Breakdown",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            tint = unifiedLightColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    CategoryProgressRow(
                        label = "Images & Photos",
                        size = viewModel.formatBytes(storage.imagesBytes),
                        fraction = 0.25f,
                        icon = Icons.Default.PhotoLibrary,
                        accentColor = unifiedLightColor,
                        onClick = { onNavigateToMediaCategory(MediaType.IMAGE) }
                    )
                    CategoryProgressRow(
                        label = "Videos & Media",
                        size = viewModel.formatBytes(storage.videosBytes),
                        fraction = 0.35f,
                        icon = Icons.Default.VideoLibrary,
                        accentColor = unifiedLightColor,
                        onClick = { onNavigateToMediaCategory(MediaType.VIDEO) }
                    )
                    CategoryProgressRow(
                        label = "Audio & Music",
                        size = viewModel.formatBytes(storage.audioBytes),
                        fraction = 0.10f,
                        icon = Icons.Default.LibraryMusic,
                        accentColor = unifiedLightColor,
                        onClick = { onNavigateToMediaCategory(MediaType.AUDIO) }
                    )
                    CategoryProgressRow(
                        label = "Documents",
                        size = viewModel.formatBytes(storage.documentsBytes),
                        fraction = 0.10f,
                        icon = Icons.Default.Description,
                        accentColor = unifiedLightColor,
                        onClick = { onNavigateToMediaCategory(MediaType.DOCUMENT) }
                    )
                    CategoryProgressRow(
                        label = "Junk & Temp Files",
                        size = viewModel.formatBytes(storage.cacheBytes),
                        fraction = 0.05f,
                        icon = Icons.Default.DeleteSweep,
                        accentColor = unifiedLightColor,
                        onClick = onNavigateToCleaner
                    )
                }
            }
        }
    }
}

/**
 * Smart, Light & Professional Feature Card
 * Features:
 * - Centered 52dp soft-tint pastel squircle badge with crisp 26dp colored icon
 * - Clean subtle border (no harsh neons)
 * - Centered bold, legible title with neutral subtitle
 * - Tactile spring bounce feedback
 */
@Composable
fun InteractiveFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feature_card_scale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Centered Soft Pastel Circle Pod with Crisp Colored Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Centered Title
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Optional Subtitle (if provided)
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Backward compatibility overload
 */
@Composable
fun InteractiveFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    InteractiveFeatureCard(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        accentColor = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

/**
 * Backward compatibility overload
 */
@Composable
fun InteractiveFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    InteractiveFeatureCard(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = icon,
        accentColor = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

/**
 * Backward compatibility overload for InteractiveFeatureCardWithDrawable
 */
@Composable
fun InteractiveFeatureCardWithDrawable(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    drawableRes: Int,
    onClick: () -> Unit
) {
    InteractiveFeatureCard(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.CleaningServices,
        accentColor = Color(0xFFEA580C),
        onClick = onClick
    )
}

@Composable
fun CategoryProgressRow(
    label: String,
    size: String,
    fraction: Float,
    icon: ImageVector = Icons.Default.Folder,
    accentColor: Color = Color(0xFF60A5FA),
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 2.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Section",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = size,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.12f)
        )
    }
}
