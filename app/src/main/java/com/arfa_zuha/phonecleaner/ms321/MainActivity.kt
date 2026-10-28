package com.arfa_zuha.phonecleaner.ms321

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    object Dashboard : Screen("dashboard", "Cleaner", Icons.Default.CleaningServices)
    object Cleaner : Screen("cleaner", "Clean Junk", Icons.Default.CleaningServices)
    object Compression : Screen("compression", "Compress", Icons.Default.FolderZip)
    object Battery : Screen("battery", "Battery", Icons.Default.BatteryChargingFull)
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
                            com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAdWithCooldown(this@MainActivity) {
                                currentScreen = Screen.Dashboard
                            }
                        }
                    }

                    // Automatic Screen View Tracking in Google Analytics
                    LaunchedEffect(currentScreen) {
                        com.arfa_zuha.phonecleaner.ms321.analytics.AnalyticsHelper.logScreenView(currentScreen.route)
                    }

                    Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .navigationBarsPadding(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .align(Alignment.BottomCenter),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.2.dp, Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Home / Dashboard
                                    CustomBottomNavItem(
                                        modifier = Modifier.weight(1f),
                                        title = "Home",
                                        icon = Icons.Default.Home,
                                        selected = currentScreen == Screen.Dashboard,
                                        onClick = { currentScreen = Screen.Dashboard }
                                    )

                                    // Spacer for Center Floating Clean FAB
                                    Spacer(modifier = Modifier.weight(1f))

                                    // 3. Files / Storage Manager
                                    CustomBottomNavItem(
                                        modifier = Modifier.weight(1f),
                                        title = "Files",
                                        icon = Icons.Default.Folder,
                                        selected = currentScreen == Screen.FileManager,
                                        onClick = { currentScreen = Screen.FileManager }
                                    )
                                }
                            }

                            // Center Elevated Smart Cleaner Action Button (FAB)
                            val fabInteraction = remember { MutableInteractionSource() }
                            val isFabPressed by fabInteraction.collectIsPressedAsState()
                            val fabScale by animateFloatAsState(
                                targetValue = if (isFabPressed) 0.88f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "fab_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .graphicsLayer {
                                        scaleX = fabScale
                                        scaleY = fabScale
                                    }
                                    .size(60.dp)
                                    .shadow(elevation = 10.dp, shape = CircleShape, spotColor = Color(0xFF10B981))
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    )
                                    .border(3.dp, Color.White, CircleShape)
                                    .clickable(
                                        interactionSource = fabInteraction,
                                        indication = null
                                    ) {
                                        com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                            currentScreen = Screen.Cleaner
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Smart Clean",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
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
                                onNavigateToCompression = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Compression
                                    }
                                },
                                onNavigateToBattery = {
                                    com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                        currentScreen = Screen.Battery
                                    }
                                },
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

@Composable
private fun CustomBottomNavItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFF10B981) // Emerald Green
    val inactiveColor = Color(0xFF64748B) // Slate Gray

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Smart top active indicator line
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(3.5.dp)
                .clip(CircleShape)
                .background(if (selected) activeColor else Color.Transparent)
        )

        // Icon with soft pill container on active
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(26.dp)
            )
        }

        // Title text
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) activeColor else inactiveColor,
            maxLines = 1
        )
    }
}
