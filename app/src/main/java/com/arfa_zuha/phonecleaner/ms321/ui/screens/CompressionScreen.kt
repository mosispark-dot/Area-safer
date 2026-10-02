package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.ads.MediumRectangleAd
import com.arfa_zuha.phonecleaner.ms321.data.CompressionType
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CompressionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompressionScreen(
    compressionState: CompressionUiState,
    viewModel: CleanerViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Multiple File Picker Launcher
    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.selectFilesForCompression(uris)
        }
    }

    val selectedCount = compressionState.selectedUris.size
    val estimatedSavedText = viewModel.formatBytes(compressionState.estimatedBytesSaved)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header with Top Back Button
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Media & File Compressor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Compress Pictures, Videos & Files to free up space",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4 Category Tabs (Picture, Video, Audio, Files)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompressionTypeTab(
                type = CompressionType.PICTURE,
                icon = Icons.Default.Image,
                isSelected = compressionState.selectedType == CompressionType.PICTURE,
                modifier = Modifier.weight(1f),
                onSelect = { viewModel.setCompressionType(CompressionType.PICTURE) }
            )
            CompressionTypeTab(
                type = CompressionType.VIDEO,
                icon = Icons.Default.Videocam,
                isSelected = compressionState.selectedType == CompressionType.VIDEO,
                modifier = Modifier.weight(1f),
                onSelect = { viewModel.setCompressionType(CompressionType.VIDEO) }
            )
            CompressionTypeTab(
                type = CompressionType.AUDIO,
                icon = Icons.Default.Audiotrack,
                isSelected = compressionState.selectedType == CompressionType.AUDIO,
                modifier = Modifier.weight(1f),
                onSelect = { viewModel.setCompressionType(CompressionType.AUDIO) }
            )
            CompressionTypeTab(
                type = CompressionType.FILE,
                icon = Icons.Default.FolderZip,
                isSelected = compressionState.selectedType == CompressionType.FILE,
                modifier = Modifier.weight(1f),
                onSelect = { viewModel.setCompressionType(CompressionType.FILE) }
            )
        }

        // Multiple File Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    multipleFilePickerLauncher.launch(compressionState.selectedType.mimeType)
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedCount > 0)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (compressionState.selectedType) {
                        CompressionType.PICTURE -> Icons.Default.AddPhotoAlternate
                        CompressionType.VIDEO -> Icons.Default.VideoCall
                        CompressionType.AUDIO -> Icons.Default.AudioFile
                        CompressionType.FILE -> Icons.Default.FileUpload
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (selectedCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = "$selectedCount ${if (selectedCount == 1) "File" else "Files"} Selected",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "Total Size: ${viewModel.formatBytes(compressionState.totalSizeBytes)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // Selected files chips / preview list (Max 2 shown, rest in +more)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        compressionState.fileNames.take(2).forEach { fname ->
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = fname,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        if (compressionState.fileNames.size > 2) {
                            SuggestionChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "+${compressionState.fileNames.size - 2} more",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { multipleFilePickerLauncher.launch(compressionState.selectedType.mimeType) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Different / More Files")
                    }
                } else {
                    Text(
                        text = "Tap to choose ${compressionState.selectedType.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select one or multiple ${compressionState.selectedType.label.lowercase()} files to compress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Show Medium Rectangle Ad when no file is selected
        if (selectedCount == 0) {
            MediumRectangleAd(modifier = Modifier.padding(top = 10.dp))
        }

        // Compression Level Slider (When file(s) selected)
        if (selectedCount > 0 && compressionState.result == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compression Level",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(
                                    text = "Frees ~$estimatedSavedText",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "${(compressionState.qualityLevel * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Slider(
                        value = compressionState.qualityLevel,
                        onValueChange = { viewModel.setQualityLevel(it) },
                        valueRange = 0.2f..0.9f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max Compression\n(Frees More Space)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "High Quality\n(Original Details)",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive Action Button with Freed Space Indicator
            Button(
                onClick = { viewModel.performCompression() },
                enabled = !compressionState.isCompressing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (compressionState.isCompressing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Compressing $selectedCount ${compressionState.selectedType.label}...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Compress $selectedCount ${if (selectedCount > 1) "${compressionState.selectedType.label} Files" else compressionState.selectedType.label} Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "⚡ Will free up ~$estimatedSavedText of storage space",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Result Card (Success / Error Display)
        AnimatedVisibility(
            visible = compressionState.result != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            compressionState.result?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isSuccess)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (result.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (result.isSuccess) "Compression Completed!" else "Compression Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${result.originalName} (${result.fileCount} ${if (result.fileCount == 1) "file" else "files"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (result.isSuccess) {
                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResultStatBox(
                                    label = "Original",
                                    value = viewModel.formatBytes(result.originalSize)
                                )
                                ResultStatBox(
                                    label = "Compressed",
                                    value = viewModel.formatBytes(result.compressedSize),
                                    isHighlighted = true
                                )
                                ResultStatBox(
                                    label = "Freed Space",
                                    value = viewModel.formatBytes(result.bytesSaved),
                                    isHighlighted = true
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Saved at: ${result.outputPath}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = result.errorMessage ?: "An error occurred during compression.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Button(
                            onClick = {
                                if (activity != null) {
                                    InterstitialAdHelper.showAd(activity) {
                                        viewModel.resetCompression()
                                    }
                                } else {
                                    viewModel.resetCompression()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Compress More Files")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompressionTypeTab(
    type: CompressionType,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = type.label, modifier = Modifier.size(20.dp))
            Text(
                text = type.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun ResultStatBox(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
