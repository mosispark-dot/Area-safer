package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateFile
import com.arfa_zuha.phonecleaner.ms321.data.DuplicateGroup
import com.arfa_zuha.phonecleaner.ms321.data.FileCategory
import com.arfa_zuha.phonecleaner.ms321.utils.DuplicateFinder
import com.arfa_zuha.phonecleaner.ms321.viewmodel.DuplicateViewModel
import java.io.File
import java.util.Locale

fun launchSystemMediaPlayer(context: android.content.Context, file: DuplicateFile) {
    try {
        val mimeType = when (file.category) {
            FileCategory.VIDEO -> "video/*"
            FileCategory.AUDIO -> "audio/*"
            FileCategory.IMAGE -> "image/*"
            FileCategory.DOCUMENT -> "application/*"
        }
        val chooserTitle = when (file.category) {
            FileCategory.VIDEO -> "Play Video with"
            FileCategory.AUDIO -> "Play Audio with"
            FileCategory.IMAGE -> "View Image with"
            FileCategory.DOCUMENT -> "Open Document with"
        }

        val contentUri: Uri? = when {
            file.uri != Uri.EMPTY && file.uri.scheme == "content" -> file.uri
            file.path.isNotEmpty() && File(file.path).exists() -> {
                try {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(file.path)
                    )
                } catch (e: Exception) {
                    Uri.fromFile(File(file.path))
                }
            }
            !file.previewUrl.isNullOrEmpty() -> Uri.parse(file.previewUrl)
            else -> file.uri
        }

        if (contentUri != null && contentUri != Uri.EMPTY) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateScreen(
    viewModel: DuplicateViewModel,
    onBackClick: () -> Unit = {}
) {
    val currentCategory by viewModel.currentCategory.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewFile by remember { mutableStateOf<DuplicateFile?>(null) }

    // Auto-rescan when returning from granting storage permissions or avoiding re-scans
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentCategory?.let { category ->
                    if (viewModel.duplicateGroups.value.isEmpty() && !viewModel.isLoading.value) {
                        viewModel.scanDuplicates(category)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Keep screen on during scanning
    DisposableEffect(isLoading) {
        val activity = generateSequence(context) { if (it is android.content.ContextWrapper) it.baseContext else null }
            .firstOrNull { it is android.app.Activity } as? android.app.Activity
            
        if (isLoading) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back press to return to main category selection
    BackHandler(enabled = currentCategory != null) {
        viewModel.resetCategory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val mainTitle = if (currentCategory == null) "Duplicate Files Finder" else "${currentCategory!!.label} Duplicates"
                        val subTitle = if (currentCategory == null) "Find & remove exact duplicate media clones" else "Review side-by-side & delete clone copies"
                        Text(text = mainTitle, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(text = subTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentCategory != null) {
                            viewModel.resetCategory()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (currentCategory == null) {
                DuplicateCategories(
                    onCategoryClick = { category ->
                        viewModel.scanDuplicates(category)
                    }
                )
            } else {
                DuplicateResultsView(
                    viewModel = viewModel,
                    onPreviewFile = { file -> previewFile = file }
                )
            }
        }
    }

    // Full-Screen Image / Video / Audio Preview Dialog
    previewFile?.let { file ->
        ImagePreviewDialog(
            file = file,
            viewModel = viewModel,
            onDismiss = { previewFile = null }
        )
    }
}

@Composable
fun DuplicateCategories(onCategoryClick: (FileCategory) -> Unit) {
    val context = LocalContext.current
    val isAllFilesPermissionGranted = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Storage Permission Request Banner (Android 11+)
        if (!isAllFilesPermissionGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Full Storage Access",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Grant permission to scan all hidden duplicates on device",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hero Scan Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Duplicate Cleaner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan media categories to find duplicate photos, videos & docs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Select Media Category to Scan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        CategoryCard(
            title = "Duplicate Images",
            subtitle = "Find photos, screenshots & WhatsApp clones",
            icon = Icons.Default.Image,
            color = Color(0xFF10B981),
            onClick = { onCategoryClick(FileCategory.IMAGE) }
        )
        CategoryCard(
            title = "Duplicate Videos",
            subtitle = "Find duplicate video recordings & clips",
            icon = Icons.Default.VideoLibrary,
            color = Color(0xFF0284C7),
            onClick = { onCategoryClick(FileCategory.VIDEO) }
        )
        CategoryCard(
            title = "Duplicate Audios",
            subtitle = "Clean duplicate music & voice notes",
            icon = Icons.Default.AudioFile,
            color = Color(0xFFF59E0B),
            onClick = { onCategoryClick(FileCategory.AUDIO) }
        )
        CategoryCard(
            title = "Duplicate Documents",
            subtitle = "Find PDF & document copies",
            icon = Icons.AutoMirrored.Filled.InsertDriveFile,
            color = Color(0xFF8B5CF6),
            onClick = { onCategoryClick(FileCategory.DOCUMENT) }
        )
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Scan",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DuplicateResultsView(
    viewModel: DuplicateViewModel,
    onPreviewFile: (DuplicateFile) -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val scanProgressPercent by viewModel.scanProgressPercent.collectAsState()
    val scanPhaseMessage by viewModel.scanPhaseMessage.collectAsState()

    val rawGroups by viewModel.duplicateGroups.collectAsState()

    val allDuplicateFiles = remember(rawGroups) { rawGroups.flatMap { it.files } }
    val selectedCloneFiles = remember(rawGroups) { allDuplicateFiles.filter { it.isSelected } }
    val totalSelectedBytes = remember(selectedCloneFiles) { selectedCloneFiles.sumOf { it.size } }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        CircularProgressIndicator(
                            progress = { scanProgressPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "$scanProgressPercent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = scanPhaseMessage.ifEmpty { "Scanning storage for duplicates..." },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    LinearProgressIndicator(
                        progress = { scanProgressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    Text(
                        text = "Sky Clean AI is analyzing MD5 signatures & storage files...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else if (rawGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Duplicates Found!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Keep Best Shots Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF2F9))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep Best Shots (${rawGroups.size} Groups)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                    Checkbox(
                        checked = selectedCloneFiles.size == allDuplicateFiles.size - rawGroups.size,
                        onCheckedChange = { viewModel.selectAllDuplicates(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF6B7280),
                            uncheckedColor = Color(0xFF6B7280),
                            checkmarkColor = Color.White
                        )
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 70.dp)
                ) {
                    items(rawGroups) { group ->
                        DuplicateGroupCard(
                            group = group,
                            viewModel = viewModel,
                            onPreviewFile = onPreviewFile
                        )
                    }
                }
            }

            // Bottom Action Bar: Share & Delete Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share Button
                OutlinedButton(
                    onClick = { viewModel.shareSelectedFiles(context) },
                    enabled = selectedCloneFiles.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp), tint = Color(0xFF374151))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
                }

                // Delete Button
                Button(
                    onClick = { viewModel.deleteSelectedFiles() },
                    enabled = selectedCloneFiles.isNotEmpty(),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444), // Smart Modern Red
                        disabledContainerColor = Color(0xFFFCA5A5)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete (${selectedCloneFiles.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    viewModel: DuplicateViewModel,
    onPreviewFile: (DuplicateFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val titleName = group.files.firstOrNull()?.name ?: "Unknown"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titleName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${group.files.size} Copies",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            group.files.take(3).forEach { file ->
                DuplicateImageItem(
                    file = file,
                    viewModel = viewModel,
                    onPreviewClick = { onPreviewFile(file) },
                    modifier = Modifier.weight(1f).aspectRatio(0.7f)
                )
            }
            val emptySlots = 3 - minOf(group.files.size, 3)
            if (emptySlots in 1..2) {
                for (slot in 0 until emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DuplicateImageItem(
    file: DuplicateFile,
    viewModel: DuplicateViewModel,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileExists = remember(file.path, file.uri) {
        (file.path.isNotEmpty() && File(file.path).exists()) || file.uri != Uri.EMPTY
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                launchSystemMediaPlayer(context, file)
            }
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
    ) {
        when (file.category) {
            FileCategory.IMAGE -> {
                val imageModel: Any = remember(file.uri, file.path, file.previewUrl) {
                    when {
                        file.uri != Uri.EMPTY -> file.uri
                        file.path.isNotEmpty() && File(file.path).exists() -> File(file.path)
                        !file.previewUrl.isNullOrEmpty() -> file.previewUrl
                        else -> file.path
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .size(300) // Downsample to speed up scroll
                        .crossfade(true)
                        .build(),
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            FileCategory.VIDEO -> {
                if (fileExists) {
                    val imageLoader = remember {
                        coil.ImageLoader.Builder(context)
                            .components { add(coil.decode.VideoFrameDecoder.Factory()) }
                            .build()
                    }
                    val videoModel: Any = remember(file.uri, file.path) {
                        if (file.path.isNotEmpty() && File(file.path).exists()) File(file.path) else file.uri
                    }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoModel)
                            .size(300) // Downsample to speed up scroll
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            FileCategory.AUDIO -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0FDF4))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF16A34A))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = file.name.substringAfterLast(".", "AUDIO").uppercase(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = file.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14532D),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
            FileCategory.DOCUMENT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3E8FF))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF9333EA))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = file.name.substringAfterLast(".", "DOC").uppercase(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = file.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF581C87),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Checkbox Top Right for Keep / Delete Selection
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(if (file.isSelected) Color(0xFF6B7280) else Color.Black.copy(alpha = 0.45f))
                .border(1.5.dp, Color.White, CircleShape)
                .clickable { viewModel.toggleSelection(file) },
            contentAlignment = Alignment.Center
        ) {
            if (file.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Top Left Preview Eye Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    launchSystemMediaPlayer(context, file)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Preview",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // Bottom Info Bar displaying File Size
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Size", color = Color.White, fontSize = 9.5.sp)
            Text(text = viewModel.formatBytes(file.size), color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FastAudioPlayer(
    file: DuplicateFile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val mp = MediaPlayer()
        try {
            if (file.path.isNotEmpty() && File(file.path).exists()) {
                mp.setDataSource(file.path)
            } else if (file.uri != Uri.EMPTY) {
                mp.setDataSource(context, file.uri)
            }
            mp.prepareAsync()
            mp.setOnPreparedListener { player ->
                duration = player.duration.coerceAtLeast(1).toFloat()
                mediaPlayer = player
            }
            mp.setOnCompletionListener {
                isPlaying = false
                currentPosition = 0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                mp.stop()
                mp.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && mediaPlayer != null) {
            currentPosition = mediaPlayer?.currentPosition?.toFloat() ?: 0f
            kotlinx.coroutines.delay(200L)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = file.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Slider(
                value = currentPosition,
                onValueChange = { pos ->
                    currentPosition = pos
                    mediaPlayer?.seekTo(pos.toInt())
                },
                valueRange = 0f..duration,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAudioTime(currentPosition.toLong()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = formatAudioTime(duration.toLong()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatAudioTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun ImagePreviewDialog(
    file: DuplicateFile,
    viewModel: DuplicateViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageModel: Any = remember(file.uri, file.path, file.previewUrl) {
        when {
            file.uri != Uri.EMPTY -> file.uri
            file.path.isNotEmpty() && File(file.path).exists() -> File(file.path)
            !file.previewUrl.isNullOrEmpty() -> file.previewUrl
            else -> file.path
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleLabel = when (file.category) {
                        FileCategory.VIDEO -> "Video Preview"
                        FileCategory.AUDIO -> "Audio Player Preview"
                        else -> "Full Image View"
                    }
                    Text(
                        text = titleLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Interactive Media Player or Image Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when (file.category) {
                        FileCategory.AUDIO -> {
                            FastAudioPlayer(
                                file = file,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = file.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // System External Player Launcher Button for Videos & Audios
                if (file.category == FileCategory.VIDEO || file.category == FileCategory.AUDIO) {
                    Button(
                        onClick = {
                            launchSystemMediaPlayer(context, file)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (file.category == FileCategory.VIDEO) Icons.Default.PlayCircleOutline else Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnText = if (file.category == FileCategory.VIDEO) "Play Video in Phone Player" else "Play in System Audio Player"
                        Text(btnText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = file.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Location: ${file.path}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Size: ${viewModel.formatBytes(file.size)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Toggle Selection Status in Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(
                            checked = file.isSelected,
                            onCheckedChange = { viewModel.toggleSelection(file) }
                        )
                        Text(
                            text = if (file.isSelected) "Marked for Deletion" else "Keep Original",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (file.isSelected) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
