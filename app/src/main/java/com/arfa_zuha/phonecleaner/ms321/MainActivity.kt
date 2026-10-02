package com.arfa_zuha.phonecleaner.ms321

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppManagerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppUninstallerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppUsageScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.BatteryOptimizerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.CacheCleanerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.CompressionScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.DashboardScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.DuplicateScreen
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.arfa_zuha.phonecleaner.ms321.ui.screens.FileManagerDashboard
import com.arfa_zuha.phonecleaner.ms321.ui.screens.MediaCategoryScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.OnboardingScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.SplashScreen
import com.arfa_zuha.phonecleaner.ms321.ui.theme.PhoneCleanerTheme
import com.arfa_zuha.phonecleaner.ms321.viewmodel.AppUsageViewModel
import com.arfa_zuha.phonecleaner.ms321.viewmodel.CleanerViewModel
import com.arfa_zuha.phonecleaner.ms321.viewmodel.DuplicateViewModel
import com.arfa_zuha.phonecleaner.ms321.viewmodel.FileManagerViewModel
import com.arfa_zuha.phonecleaner.ms321.viewmodel.UninstallerViewModel

import androidx.activity.compose.BackHandler

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Cleaner : Screen("cleaner", "Cleaner", Icons.Default.CleaningServices)
    object Compression : Screen("compression", "Compress", Icons.Default.FolderZip)
    object Battery : Screen("battery", "Battery Info", Icons.Default.BatteryChargingFull)
    object Duplicates : Screen("duplicates", "Duplicates", Icons.Default.CopyAll)
    object Uninstaller : Screen("uninstaller", "Uninstaller", Icons.Default.InstallMobile)
    object Apps : Screen("apps", "Unused Apps", Icons.Default.PhoneAndroid)
    object AppUsage : Screen("app_usage", "App Usage", Icons.Default.BarChart)
    object FileManager : Screen("file_manager", "Files", Icons.Default.Folder)
    object MediaCategory : Screen("media_category", "Media", Icons.Default.Folder)
}

class MainActivity : ComponentActivity() {
    private val viewModel: CleanerViewModel by viewModels()
    private val duplicateViewModel: DuplicateViewModel by viewModels()
    private val uninstallerViewModel: UninstallerViewModel by viewModels()
    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private val fileManagerViewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneCleanerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val compressionState by viewModel.compressionState.collectAsState()
                val batteryState by viewModel.batteryState.collectAsState()
                val uninstallerState by uninstallerViewModel.uiState.collectAsState()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                val context = LocalContext.current
                val prefs = remember { context.getSharedPreferences("sky_clean_prefs", Context.MODE_PRIVATE) }
                var isOnboardingCompleted by remember { mutableStateOf(prefs.getBoolean("onboarding_completed", false)) }
                var showSplash by remember { mutableStateOf(true) }
                var showPaywall by remember { mutableStateOf(false) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        onOnboardingFinished = {
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                            isOnboardingCompleted = true
                            showPaywall = true // Show paywall right after onboarding
                        }
                    )
                } else if (showPaywall && !com.arfa_zuha.phonecleaner.ms321.billing.BillingHelper.getInstance(context).isPremium.collectAsState().value) {
                    com.arfa_zuha.phonecleaner.ms321.ui.screens.PaywallScreen(
                        onDismiss = { showPaywall = false }
                    )
                } else {
                    BackHandler(enabled = currentScreen != Screen.Dashboard) {
                        if (currentScreen == Screen.MediaCategory) {
                            currentScreen = Screen.FileManager
                        } else {
                            currentScreen = Screen.Dashboard
                        }
                    }

                    val items = listOf(
                        Screen.Dashboard,
                        Screen.Battery,
                        Screen.Compression
                    )

                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items.forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onNavigateToCleaner = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Cleaner
                                    }
                                },
                                onNavigateToApps = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Apps
                                    }
                                },
                                onNavigateToCompression = { currentScreen = Screen.Compression },
                                onNavigateToBattery = { currentScreen = Screen.Battery },
                                onNavigateToDuplicates = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Duplicates
                                    }
                                },
                                onNavigateToUninstaller = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Uninstaller
                                    }
                                },
                                onNavigateToAppUsage = { currentScreen = Screen.AppUsage },
                                onNavigateToFileManager = { currentScreen = Screen.FileManager },
                                onNavigateToMediaCategory = { mediaType ->
                                    fileManagerViewModel.setMediaType(mediaType)
                                    currentScreen = Screen.MediaCategory
                                },
                                onNavigateToPaywall = { showPaywall = true }
                            )
                            Screen.Cleaner -> CacheCleanerScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.Compression -> CompressionScreen(
                                compressionState = compressionState,
                                viewModel = viewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.Battery -> BatteryOptimizerScreen(
                                batteryState = batteryState,
                                viewModel = viewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.Duplicates -> DuplicateScreen(
                                viewModel = duplicateViewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.Uninstaller -> AppUninstallerScreen(
                                uiState = uninstallerState,
                                viewModel = uninstallerViewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.Apps -> AppManagerScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.AppUsage -> AppUsageScreen(
                                viewModel = appUsageViewModel,
                                onBackClick = { currentScreen = Screen.Dashboard }
                            )
                            Screen.FileManager -> FileManagerDashboard(
                                viewModel = fileManagerViewModel,
                                onNavigateBack = { currentScreen = Screen.Dashboard },
                                onCategoryClick = { mediaType ->
                                    fileManagerViewModel.setMediaType(mediaType)
                                    currentScreen = Screen.MediaCategory
                                }
                            )
                            Screen.MediaCategory -> MediaCategoryScreen(
                                viewModel = fileManagerViewModel,
                                onNavigateBack = { currentScreen = Screen.FileManager }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
