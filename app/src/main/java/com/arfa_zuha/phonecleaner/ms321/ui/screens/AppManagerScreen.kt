package com.arfa_zuha.phonecleaner.ms321.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.arfa_zuha.phonecleaner.ms321.data.AppInfoItem
import com.arfa_zuha.phonecleaner.ms321.data.AppUsageRepository
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerUiState
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppCategoryFilter(val label: String) {
    UNUSED_6_MONTHS("Unused (6+ Mo)"),
    ALL("All Apps"),
    GAMES("Games"),
    LARGE("Large (>100MB)"),
    SYSTEM("System Apps")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    uiState: CleanerUiState,
    viewModel: CleanerViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var selectedCategory by remember { mutableStateOf(AppCategoryFilter.UNUSED_6_MONTHS) }
    var searchQuery by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(AppUsageRepository.hasUsagePermission(context)) }

    // Auto refresh when returning from Usage Access Settings or App Details
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
                hasPermission = AppUsageRepository.hasUsagePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sixMonthsAgo = remember { System.currentTimeMillis() - (180L * 24L * 60L * 60L * 1000L) }

    // Logic to detect if an app or game is unused for 6+ months (Requires Usage Access permission granted & valid lastTimeUsed)
    val isUnusedFor6Months = { app: AppInfoItem ->
        hasPermission &&
                !app.isSystemApp &&
                app.lastTimeUsed != -1L &&
                (app.firstInstallTime <= 0L || app.firstInstallTime < sixMonthsAgo) &&
                (app.lastTimeUsed <= 0L || app.lastTimeUsed < sixMonthsAgo)
    }

    val allApps = uiState.installedApps

    // List of apps & games unused for 6+ months
    val unusedApps = remember(allApps, sixMonthsAgo, hasPermission) {
        allApps.filter { isUnusedFor6Months(it) }
    }
    val unusedTotalSize = remember(unusedApps) { unusedApps.sumOf { it.packageSize } }

    // Filter apps based on category filter and search query
    val filteredApps = remember(allApps, selectedCategory, searchQuery, sixMonthsAgo, hasPermission) {
        allApps.filter { app ->
            val matchesCategory = when (selectedCategory) {
                AppCategoryFilter.ALL -> !app.isSystemApp
                AppCategoryFilter.UNUSED_6_MONTHS -> isUnusedFor6Months(app)
                AppCategoryFilter.GAMES -> app.isGame && !app.isSystemApp
                AppCategoryFilter.LARGE -> app.packageSize > 100 * 1024 * 1024L && !app.isSystemApp
                AppCategoryFilter.SYSTEM -> app.isSystemApp
            }

            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Unused Apps & Games",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${unusedApps.size} Unused in 6+ Months • ${viewModel.formatBytes(unusedTotalSize)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            // Action Button Flush at Bottom (sits directly above/with bottom navigation buttons)
            if (selectedCategory == AppCategoryFilter.UNUSED_6_MONTHS && unusedApps.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Button(
                            onClick = {
                                val firstApp = unusedApps.firstOrNull()
                                if (firstApp != null) {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${firstApp.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Review & Clean Unused Apps (${unusedApps.size} • ${viewModel.formatBytes(unusedTotalSize)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Permission Banner if Usage Access Permission is Missing
            if (!hasPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usage Access Permission Required",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Enable permission to detect 6+ month unused apps & games.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Grant", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Compact Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppCategoryFilter.entries.forEach { category ->
                    val badgeCount = if (category == AppCategoryFilter.UNUSED_6_MONTHS) " (${unusedApps.size})" else ""
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            if (category == AppCategoryFilter.UNUSED_6_MONTHS && !hasPermission) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                selectedCategory = category
                            }
                        },
                        label = { Text("${category.label}$badgeCount", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            if (category == AppCategoryFilter.UNUSED_6_MONTHS) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(13.dp))
                            } else if (category == AppCategoryFilter.GAMES) {
                                Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(13.dp))
                            }
                        },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            // Compact Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or package...", fontSize = 10.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            )

            // Native AdMob Banner Ad
            com.arfa_zuha.phonecleaner.ms321.ads.NativeBannerAd(modifier = Modifier.padding(vertical = 2.dp))

            // List View Content with Maximum Vertical Space
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No apps found matching selected criteria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps) { app ->
                        AppItemCard(
                            app = app,
                            viewModel = viewModel,
                            sixMonthsAgo = sixMonthsAgo,
                            hasPermission = hasPermission,
                            onAppClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${app.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItemCard(
    app: AppInfoItem,
    viewModel: CleanerViewModel,
    sixMonthsAgo: Long,
    hasPermission: Boolean,
    onAppClick: () -> Unit
) {
    val isUnused6Months = hasPermission &&
            !app.isSystemApp &&
            app.lastTimeUsed != -1L &&
            (app.firstInstallTime <= 0L || app.firstInstallTime < sixMonthsAgo) &&
            (app.lastTimeUsed <= 0L || app.lastTimeUsed < sixMonthsAgo)

    Card(
        onClick = onAppClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnused6Months)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isUnused6Months)
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Icon placed close to top-left corner
            AsyncAppIcon(
                packageName = app.packageName,
                appName = app.appName,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(42.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Middle: App Title placed close to icon, Version/Package Name, and Red Unused 6+ Text right below it
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // App Title + Game/System Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = app.appName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (app.isGame) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GAME",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (app.isSystemApp) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SYSTEM",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Version & Package Name
                Text(
                    text = "v${app.versionName} • ${app.packageName}",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Red Color Unused 6+ Status Text directly below version & package
                if (isUnused6Months) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (app.lastTimeUsed > 0L) {
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                "Unused 6+ Mo (Last: ${dateFormat.format(Date(app.lastTimeUsed))})"
                            } else {
                                "Unused 6+ Mo (Never opened)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (app.lastTimeUsed > 0L) {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val isToday = android.text.format.DateUtils.isToday(app.lastTimeUsed)
                    val dateStr = if (isToday) "Today (${dateFormat.format(Date(app.lastTimeUsed))})" else dateFormat.format(Date(app.lastTimeUsed))
                    Text(
                        text = "Last used: $dateStr",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                } else if (app.lastTimeUsed == -1L || !hasPermission) {
                    Text(
                        text = "Last used: Usage Access Required",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                } else {
                    Text(
                        text = "Last used: Unknown",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Package Size & Action Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(
                    text = viewModel.formatBytes(app.packageSize),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onAppClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUnused6Months) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isUnused6Months) Color.White else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isUnused6Months) "Uninstall" else "Manage",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
