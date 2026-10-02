package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arfa_zuha.phonecleaner.ms321.data.ApkFileItem
import com.arfa_zuha.phonecleaner.ms321.data.AppSortOption
import com.arfa_zuha.phonecleaner.ms321.data.InstalledAppItem
import com.arfa_zuha.phonecleaner.ms321.data.UninstallerTab
import com.arfa_zuha.phonecleaner.ms321.viewmodel.UninstallerUiState
import com.arfa_zuha.phonecleaner.ms321.viewmodel.UninstallerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUninstallerScreen(
    uiState: UninstallerUiState,
    viewModel: UninstallerViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh list when user returns from System Uninstall Dialog
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.scanAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Filtered & Sorted Data
    val filteredApps = remember(uiState.installedApps, uiState.searchQuery, uiState.sortOption, uiState.showSystemApps) {
        uiState.installedApps.filter { app ->
            (uiState.showSystemApps || !app.isSystemApp) &&
                    (app.appName.contains(uiState.searchQuery, ignoreCase = true) ||
                            app.packageName.contains(uiState.searchQuery, ignoreCase = true))
        }.sortedWith { a1, a2 ->
            when (uiState.sortOption) {
                AppSortOption.SIZE -> a2.sizeBytes.compareTo(a1.sizeBytes)
                AppSortOption.NAME -> a1.appName.compareTo(a2.appName, ignoreCase = true)
                AppSortOption.DATE -> a2.installTimeMillis.compareTo(a1.installTimeMillis)
            }
        }
    }

    val filteredApks = remember(uiState.apkFiles, uiState.searchQuery, uiState.sortOption) {
        uiState.apkFiles.filter { apk ->
            apk.fileName.contains(uiState.searchQuery, ignoreCase = true) ||
                    (apk.packageName?.contains(uiState.searchQuery, ignoreCase = true) == true)
        }.sortedWith { a1, a2 ->
            when (uiState.sortOption) {
                AppSortOption.SIZE -> a2.sizeBytes.compareTo(a1.sizeBytes)
                AppSortOption.NAME -> a1.fileName.compareTo(a2.fileName, ignoreCase = true)
                AppSortOption.DATE -> a2.dateModified.compareTo(a1.dateModified)
            }
        }
    }

    val selectedApps = filteredApps.filter { it.isSelected }
    val selectedAppsCount = selectedApps.size
    val selectedAppsSize = selectedApps.sumOf { it.sizeBytes }

    val selectedApks = filteredApks.filter { it.isSelected }
    val selectedApksCount = selectedApks.size
    val selectedApksSize = selectedApks.sumOf { it.sizeBytes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "App & APK Uninstaller",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Batch uninstall unwanted apps & clean leftover APK files",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Switcher (Installed Apps vs Leftover APKs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                title = "📱 Installed (${uiState.installedApps.size})",
                isSelected = uiState.selectedTab == UninstallerTab.INSTALLED_APPS,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setTab(UninstallerTab.INSTALLED_APPS) }
            )
            TabButton(
                title = "📦 Leftover APKs (${uiState.apkFiles.size})",
                isSelected = uiState.selectedTab == UninstallerTab.APK_FILES,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.setTab(UninstallerTab.APK_FILES) }
            )
        }

        // Search Bar & Filter Options
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name or package...", fontSize = 11.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        // Sort Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = uiState.sortOption == AppSortOption.SIZE,
                    onClick = { viewModel.setSortOption(AppSortOption.SIZE) },
                    label = { Text("Size ↓", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = uiState.sortOption == AppSortOption.NAME,
                    onClick = { viewModel.setSortOption(AppSortOption.NAME) },
                    label = { Text("Name A-Z", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = uiState.sortOption == AppSortOption.DATE,
                    onClick = { viewModel.setSortOption(AppSortOption.DATE) },
                    label = { Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (uiState.selectedTab == UninstallerTab.INSTALLED_APPS) {
                FilterChip(
                    selected = uiState.showSystemApps,
                    onClick = { viewModel.toggleSystemApps(!uiState.showSystemApps) },
                    label = { Text(if (uiState.showSystemApps) "System ON" else "+ System", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Status Notification Alert
        AnimatedVisibility(
            visible = uiState.statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(text = msg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // List Content
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                    Text("Scanning Applications...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.selectedTab == UninstallerTab.INSTALLED_APPS) {
                    if (filteredApps.isEmpty()) {
                        EmptyStateView(message = "No matching installed apps found.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps) { app ->
                                InstalledAppCard(
                                    app = app,
                                    viewModel = viewModel,
                                    onToggle = { viewModel.toggleAppSelection(app.packageName) }
                                )
                            }
                        }
                    }
                } else {
                    if (filteredApks.isEmpty()) {
                        EmptyStateView(message = "No leftover APK installer files found.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApks) { apk ->
                                ApkFileCard(
                                    apk = apk,
                                    viewModel = viewModel,
                                    onToggle = { viewModel.toggleApkSelection(apk.path) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Batch Action Bottom Bar
        if (uiState.selectedTab == UninstallerTab.INSTALLED_APPS && selectedAppsCount > 0) {
            Button(
                onClick = { viewModel.uninstallSelectedApps(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Uninstall Selected ($selectedAppsCount ${if (selectedAppsCount == 1) "App" else "Apps"} • ${viewModel.formatBytes(selectedAppsSize)})",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (uiState.selectedTab == UninstallerTab.APK_FILES && selectedApksCount > 0) {
            Button(
                onClick = { viewModel.deleteSelectedApks() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete Selected APKs ($selectedApksCount ${if (selectedApksCount == 1) "File" else "Files"} • ${viewModel.formatBytes(selectedApksSize)})",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InstalledAppCard(
    app: InstalledAppItem,
    viewModel: UninstallerViewModel,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isSelected)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Far Left Checkbox
            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.9f)
            )

            // 2. Icon adjacent to Checkbox
            AsyncAppIcon(
                packageName = app.packageName,
                appName = app.appName,
                fallbackDrawable = app.icon,
                modifier = Modifier.size(38.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Middle Info Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "v${app.versionName} • ${viewModel.formatBytes(app.sizeBytes)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 4. Far Right Selection Toggle Button
            Button(
                onClick = onToggle,
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (app.isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (app.isSelected) Color.White else MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (app.isSelected) "✓ Selected" else "Uninstall",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ApkFileCard(
    apk: ApkFileItem,
    viewModel: UninstallerViewModel,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (apk.isSelected)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Far Left Checkbox
            Checkbox(
                checked = apk.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.9f)
            )

            // 2. Icon adjacent to Checkbox
            AsyncAppIcon(
                packageName = apk.packageName ?: "",
                appName = apk.fileName,
                fallbackDrawable = apk.icon,
                modifier = Modifier.size(38.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Middle Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = apk.fileName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${viewModel.formatBytes(apk.sizeBytes)} • ${apk.path}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 4. Far Right Selection Toggle Button
            Button(
                onClick = onToggle,
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (apk.isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (apk.isSelected) Color.White else MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (apk.isSelected) "✓ Selected" else "Delete",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AsyncAppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier,
    fallbackDrawable: Drawable? = null
) {
    val context = LocalContext.current
    var imageBitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = fallbackDrawable ?: if (packageName.isNotEmpty()) {
                    context.packageManager.getApplicationIcon(packageName)
                } else null

                if (drawable != null) {
                    val bmp = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    imageBitmap = bmp.asImageBitmap()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = appName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
