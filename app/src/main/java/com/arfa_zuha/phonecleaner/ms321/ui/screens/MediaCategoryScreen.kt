package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper
import com.arfa_zuha.phonecleaner.ms321.data.MediaFile
import com.arfa_zuha.phonecleaner.ms321.data.MediaFolder
import com.arfa_zuha.phonecleaner.ms321.data.MediaType
import com.arfa_zuha.phonecleaner.ms321.viewmodel.FileManagerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DocumentStyle(
    val icon: ImageVector,
    val color: Color,
    val badgeLabel: String
)

fun getDocumentStyle(fileName: String, mediaType: MediaType): DocumentStyle {
    val ext = fileName.substringAfterLast(".", "").lowercase()
    return when {
        ext == "pdf" || (mediaType == MediaType.DOCUMENT && fileName.endsWith(".pdf", ignoreCase = true)) ->
            DocumentStyle(Icons.Default.PictureAsPdf, Color(0xFFEF4444), "PDF")
        ext in listOf("doc", "docx") ->
            DocumentStyle(Icons.Default.Description, Color(0xFF2563EB), "DOC")
        ext in listOf("xls", "xlsx", "csv") ->
            DocumentStyle(Icons.Default.TableChart, Color(0xFF10B981), "XLS")
        ext in listOf("ppt", "pptx") ->
            DocumentStyle(Icons.Default.Slideshow, Color(0xFFF97316), "PPT")
        ext in listOf("txt", "json", "xml", "html") ->
            DocumentStyle(Icons.Default.Article, Color(0xFF8B5CF6), "TXT")
        ext in listOf("zip", "rar", "7z") || mediaType == MediaType.ZIP ->
            DocumentStyle(Icons.Default.FolderZip, Color(0xFFD97706), "ZIP")
        ext == "apk" || mediaType == MediaType.APK ->
            DocumentStyle(Icons.Default.Android, Color(0xFF22C55E), "APK")
        else ->
            DocumentStyle(Icons.Default.InsertDriveFile, Color(0xFF0284C7), ext.uppercase().take(4).ifEmpty { "DOC" })
    }
}

fun formatFolderBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.getDefault(), "%.2f GB", gb)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCategoryScreen(
    viewModel: FileManagerViewModel,
    onNavigateBack: () -> Unit
) {
    val currentMediaType by viewModel.currentMediaType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isFolderView by viewModel.isFolderView.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()

    var selectedFile by remember { mutableStateOf<MediaFile?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isListView by remember { mutableStateOf(false) }

    val isMultiSelecting = selectedFileIds.isNotEmpty()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentMediaType) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadFiles(currentMediaType)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cancel selection mode on back press
    BackHandler(enabled = isMultiSelecting) {
        viewModel.clearFileSelection()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val activity = context as? Activity
    
    val handleFileClick: (MediaFile) -> Unit = { file ->
        if (isMultiSelecting) {
            viewModel.toggleFileSelection(file.id)
        } else {
            val showSheet = {
                selectedFile = file
                showBottomSheet = true
            }
            if (activity != null) {
                InterstitialAdHelper.showFileManagerAdWithCooldown(activity, showSheet)
            } else {
                showSheet()
            }
        }
    }
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val title = when (currentMediaType) {
        MediaType.IMAGE -> "Images"
        MediaType.VIDEO -> "Videos"
        MediaType.AUDIO -> "Audio"
        MediaType.DOCUMENT -> "Documents"
        MediaType.APK -> "APKs"
        MediaType.ZIP -> "ZIP Files"
        MediaType.DOWNLOAD -> "Downloads"
        MediaType.RECENT -> "Recent Files"
    }

    // Filter files and folders based on searchQuery
    val filteredAllFiles = remember(allFiles, searchQuery) {
        if (searchQuery.isBlank()) allFiles
        else allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
    }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.folderName.contains(searchQuery, ignoreCase = true) }
    }

    val activeFiles = remember(selectedFolder, filteredAllFiles, folders) {
        if (selectedFolder != null) {
            folders.find { it.folderName == selectedFolder }?.files ?: emptyList()
        } else {
            filteredAllFiles
        }
    }

    val selectedFiles = remember(selectedFileIds, activeFiles) {
        activeFiles.filter { it.id in selectedFileIds }
    }
    val selectedTotalBytes = remember(selectedFiles) {
        selectedFiles.sumOf { it.size }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelecting) {
                        Text(
                            text = "${selectedFileIds.size} Selected (${formatFolderBytes(selectedTotalBytes)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    } else if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search ${selectedFolder ?: title}...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Text(
                            text = selectedFolder ?: title,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isMultiSelecting) {
                            viewModel.clearFileSelection()
                        } else if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else if (selectedFolder != null) {
                            viewModel.toggleViewMode()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isMultiSelecting) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isMultiSelecting) {
                        IconButton(onClick = { viewModel.selectAllFiles(activeFiles) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                    } else {
                        // Search Action Button
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Grid / List View Toggle Switch Button
                        IconButton(onClick = { isListView = !isListView }) {
                            Icon(
                                imageVector = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Toggle Grid/List View"
                            )
                        }

                        // Album / All Files Toggle
                        if (currentMediaType in listOf(MediaType.IMAGE, MediaType.VIDEO, MediaType.AUDIO) && selectedFolder == null) {
                            TextButton(onClick = { viewModel.toggleViewMode() }) {
                                Text(
                                    text = if (isFolderView) "All $title" else "Albums",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isMultiSelecting) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isMultiSelecting) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.shareMultipleFiles(selectedFiles) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share (${selectedFileIds.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.deleteSelectedFiles(selectedFiles) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete (${selectedFileIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedFolder != null) {
                    val folderData = folders.find { it.folderName == selectedFolder }
                    val rawFolderFiles = folderData?.files ?: emptyList()
                    val filesInFolder = if (searchQuery.isBlank()) rawFolderFiles
                    else rawFolderFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    if (isListView) {
                        MediaList(
                            files = filesInFolder,
                            selectedFileIds = selectedFileIds,
                            onFileClick = handleFileClick,
                            onFileLongClick = { file ->
                                viewModel.toggleFileSelection(file.id)
                            },
                            imageLoader = imageLoader
                        )
                    } else {
                        MediaGrid(
                            files = filesInFolder,
                            selectedFileIds = selectedFileIds,
                            onFileClick = handleFileClick,
                            onFileLongClick = { file ->
                                viewModel.toggleFileSelection(file.id)
                            },
                            imageLoader = imageLoader
                        )
                    }
                } else if (isFolderView) {
                    if (isListView) {
                        FolderList(folders = filteredFolders, onFolderClick = { viewModel.selectFolder(it) }, imageLoader = imageLoader)
                    } else {
                        FolderGrid(folders = filteredFolders, onFolderClick = { viewModel.selectFolder(it) }, imageLoader = imageLoader)
                    }
                } else {
                    if (isListView) {
                        MediaList(
                            files = filteredAllFiles,
                            selectedFileIds = selectedFileIds,
                            onFileClick = handleFileClick,
                            onFileLongClick = { file ->
                                viewModel.toggleFileSelection(file.id)
                            },
                            imageLoader = imageLoader
                        )
                    } else {
                        MediaGrid(
                            files = filteredAllFiles,
                            selectedFileIds = selectedFileIds,
                            onFileClick = handleFileClick,
                            onFileLongClick = { file ->
                                viewModel.toggleFileSelection(file.id)
                            },
                            imageLoader = imageLoader
                        )
                    }
                }
            }
        }

        if (showBottomSheet && selectedFile != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                dragHandle = null,
                contentWindowInsets = { WindowInsets.navigationBars }
            ) {
                MediaViewerActionContent(
                    file = selectedFile!!,
                    imageLoader = imageLoader,
                    onShare = {
                        viewModel.shareFile(selectedFile!!.uri)
                        showBottomSheet = false
                    },
                    onDelete = {
                        viewModel.deleteFile(selectedFile!!)
                        showBottomSheet = false
                    },
                    onOpen = {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(selectedFile!!.uri, "*/*")
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaList(
    files: List<MediaFile>,
    selectedFileIds: Set<Long>,
    onFileClick: (MediaFile) -> Unit,
    onFileLongClick: (MediaFile) -> Unit,
    imageLoader: ImageLoader
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files) { file ->
            val isSelected = file.id in selectedFileIds
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedFileIds.isNotEmpty()) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onFileClick(file) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (file.mediaType == MediaType.IMAGE || file.mediaType == MediaType.VIDEO) {
                            AsyncImage(
                                model = file.uri,
                                imageLoader = imageLoader,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val docStyle = remember(file.name, file.mediaType) { getDocumentStyle(file.name, file.mediaType) }
                            Icon(
                                imageVector = docStyle.icon,
                                contentDescription = null,
                                tint = docStyle.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatFolderBytes(file.size)} • ${formatter.format(Date(file.dateModified))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = file.folderName,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedFileIds.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    files: List<MediaFile>,
    selectedFileIds: Set<Long>,
    onFileClick: (MediaFile) -> Unit,
    onFileLongClick: (MediaFile) -> Unit,
    imageLoader: ImageLoader
) {
    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No files found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files) { file ->
            val isSelected = file.id in selectedFileIds
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .combinedClickable(
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
            ) {
                if (file.mediaType == MediaType.IMAGE || file.mediaType == MediaType.VIDEO) {
                    AsyncImage(
                        model = file.uri,
                        imageLoader = imageLoader,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Size Badge Overlay on Top-Left Corner of Photo/Video Tile
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatFolderBytes(file.size),
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else if (file.mediaType == MediaType.AUDIO) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = file.name,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatFolderBytes(file.size),
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    val docStyle = remember(file.name, file.mediaType) { getDocumentStyle(file.name, file.mediaType) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(docStyle.color.copy(alpha = 0.08f))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(docStyle.color)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = docStyle.badgeLabel,
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(docStyle.color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = docStyle.icon,
                                contentDescription = null,
                                tint = docStyle.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = file.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Checkbox Badge Overlay on Selected Grid Item
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (selectedFileIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun FolderGrid(folders: List<MediaFolder>, onFolderClick: (String) -> Unit, imageLoader: ImageLoader) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders) { folder ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder.folderName) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (folder.thumbnailUri != null) {
                        AsyncImage(
                            model = folder.thumbnailUri,
                            imageLoader = imageLoader,
                            contentDescription = folder.folderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp).align(Alignment.Center)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatFolderBytes(folder.totalSizeBytes),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = folder.folderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${folder.fileCount} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFolderBytes(folder.totalSizeBytes),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FolderList(folders: List<MediaFolder>, onFolderClick: (String) -> Unit, imageLoader: ImageLoader) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No folders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders) { folder ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folder.folderName) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (folder.thumbnailUri != null) {
                            AsyncImage(
                                model = folder.thumbnailUri,
                                imageLoader = imageLoader,
                                contentDescription = folder.folderName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.folderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${folder.fileCount} items • ${formatFolderBytes(folder.totalSizeBytes)}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FastVideoPlayer(
    uri: android.net.Uri,
    modifier: Modifier = Modifier
) {
    var isLoadingVideo by remember { mutableStateOf(true) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(uri)
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)

                    setOnPreparedListener { mediaPlayer ->
                        isLoadingVideo = false
                        mediaPlayer.start()
                    }

                    setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            isLoadingVideo = true
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            isLoadingVideo = false
                        }
                        false
                    }

                    setOnErrorListener { _, _, _ ->
                        isLoadingVideo = false
                        false
                    }
                }
            }
        )

        if (isLoadingVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(54.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Preparing Video...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AudioPlayerFrame(
    file: MediaFile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1) }
    var mediaPlayerState by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file.uri) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(context, file.uri)
                prepareAsync()
                setOnPreparedListener { mp ->
                    duration = if (mp.duration > 0) mp.duration else 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayerState = player

        onDispose {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayerState?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPosition = mp.currentPosition
                    }
                } catch (e: Exception) { }
            }
            kotlinx.coroutines.delay(500)
        }
    }

    fun formatMs(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
    }

    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Audio Track",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = file.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val progressFraction = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPosition),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatMs(duration),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .clickable {
                    mediaPlayerState?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                mp.pause()
                                isPlaying = false
                            } else {
                                mp.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
            tonalElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
fun DocumentViewerFrame(
    file: MediaFile,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val docStyle = remember(file.name, file.mediaType) { getDocumentStyle(file.name, file.mediaType) }

    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Badge(containerColor = docStyle.color) {
            Text(
                text = "${docStyle.badgeLabel} FILE",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(docStyle.color.copy(alpha = 0.25f))
                .border(2.dp, docStyle.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = docStyle.icon,
                contentDescription = file.name,
                tint = docStyle.color,
                modifier = Modifier.size(52.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = file.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Size: ${file.size / 1024} KB",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = docStyle.color)
        ) {
            Icon(docStyle.icon, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Document", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
    }
}

@Composable
fun MediaViewerActionContent(
    file: MediaFile,
    imageLoader: ImageLoader,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    var isPlayingVideo by remember(file) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Frame Container for Images, Videos, Audios, AND Documents!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (file.mediaType == MediaType.AUDIO) {
                AudioPlayerFrame(
                    file = file,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (file.mediaType == MediaType.VIDEO && isPlayingVideo) {
                FastVideoPlayer(
                    uri = file.uri,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (file.mediaType == MediaType.IMAGE || file.mediaType == MediaType.VIDEO) {
                AsyncImage(
                    model = file.uri,
                    imageLoader = imageLoader,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (file.mediaType == MediaType.VIDEO) {
                                isPlayingVideo = true
                            }
                        }
                )
                if (file.mediaType == MediaType.VIDEO) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center)
                            .clickable { isPlayingVideo = true },
                        tonalElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            } else {
                DocumentViewerFrame(
                    file = file,
                    onOpen = onOpen,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        // File Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${file.size / 1024} KB • ${formatter.format(Date(file.dateModified))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share Button
            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }

            // Delete Button
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
