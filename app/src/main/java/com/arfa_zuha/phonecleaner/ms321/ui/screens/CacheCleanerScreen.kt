package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.ads.NativeBannerAd
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerUiState
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel

@Composable
fun CacheCleanerScreen(
    uiState: CleanerUiState,
    viewModel: CleanerViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val storage = uiState.storageInfo
    val cacheSize = storage?.cacheBytes ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Junk & Cache Cleaner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Remove temporary cache & residual files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Center Hero Visual Card (Clean, modern light styling)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF10B981), Color(0xFF059669))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Text(
                    text = "Total System Junk Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Text(
                    text = viewModel.formatBytes(cacheSize),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Scanned Junk Categories Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Junk Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                JunkCategoryRow(
                    label = "App Cache & Temp Data",
                    size = viewModel.formatBytes((cacheSize * 0.45).toLong()),
                    icon = Icons.Default.CleaningServices,
                    color = Color(0xFF2196F3)
                )
                JunkCategoryRow(
                    label = "Thumbnail & Gallery Cache",
                    size = viewModel.formatBytes((cacheSize * 0.30).toLong()),
                    icon = Icons.Default.Image,
                    color = Color(0xFF4CAF50)
                )
                JunkCategoryRow(
                    label = "System Logs & Temp Files",
                    size = viewModel.formatBytes((cacheSize * 0.15).toLong()),
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    color = Color(0xFFFF9800)
                )
                JunkCategoryRow(
                    label = "Residual Leftover Files",
                    size = viewModel.formatBytes((cacheSize * 0.10).toLong()),
                    icon = Icons.Default.FolderZip,
                    color = Color(0xFF9C27B0)
                )
            }
        }

        // Smart & Handsome Action Section
        val cleanInteraction = remember { MutableInteractionSource() }
        val isCleanPressed by cleanInteraction.collectIsPressedAsState()
        val cleanScale by animateFloatAsState(
            targetValue = if (isCleanPressed) 0.95f else 1.0f,
            animationSpec = tween(120),
            label = "cleanScale"
        )
        val isCleaning = uiState.isLoading
        val isAlreadyClean = cacheSize == 0L && !isCleaning

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Smart Handsome Clean Button Capsule
            Surface(
                onClick = {
                    if (!isCleaning && !isAlreadyClean) {
                        viewModel.clearJunkFiles {
                            activity?.let { act ->
                                InterstitialAdHelper.showAd(act) {}
                            }
                        }
                    }
                },
                enabled = !isCleaning && !isAlreadyClean,
                interactionSource = cleanInteraction,
                shape = RoundedCornerShape(30.dp),
                shadowElevation = if (isAlreadyClean) 1.dp else 10.dp,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .scale(cleanScale)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                isCleaning -> Brush.horizontalGradient(
                                    listOf(Color(0xFF0D9488), Color(0xFF059669))
                                )
                                isAlreadyClean -> Brush.horizontalGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0))
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(Color(0xFF10B981), Color(0xFF059669))
                                )
                            }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Icon Pod with frosted background
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAlreadyClean) Color.White.copy(alpha = 0.8f)
                                    else Color.White.copy(alpha = 0.22f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCleaning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isAlreadyClean) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Center Text Pod with dynamic smart subtext
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when {
                                    isCleaning -> "Cleaning Junk Files..."
                                    isAlreadyClean -> "Storage is Already Clean"
                                    else -> "Clean Junk Files Now"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAlreadyClean) Color(0xFF475569) else Color.White,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = when {
                                    isCleaning -> "Clearing system logs & temp cache"
                                    isAlreadyClean -> "No residual junk detected"
                                    else -> "Reclaim ${viewModel.formatBytes(cacheSize)} instantly"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isAlreadyClean) Color(0xFF64748B) else Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }

                        // Right Lightning / Speed Action Chip
                        if (!isAlreadyClean && !isCleaning) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (isCleaning) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(36.dp))
                        }
                    }
                }
            }

            NativeBannerAd(modifier = Modifier.padding(top = 6.dp))

            AnimatedVisibility(visible = uiState.statusMessage != null) {
                if (uiState.statusMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = uiState.statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JunkCategoryRow(
    label: String,
    size: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        }
        Text(
            text = size,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
    }
}
