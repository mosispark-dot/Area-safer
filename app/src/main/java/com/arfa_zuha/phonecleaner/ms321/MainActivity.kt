package com.arfa_zuha.phonecleaner.ms321

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.ui.components.AppNavigationDrawer
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppManagerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppUninstallerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.AppUsageScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.BatteryOptimizerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.CacheCleanerScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.CompressionScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.DashboardScreen
import com.arfa_zuha.phonecleaner.ms321.ui.screens.DuplicateScreen
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
import kotlinx.coroutines.launch

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

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

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

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = currentScreen == Screen.Dashboard,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                                drawerContainerColor = Color.White
                            ) {
                                AppNavigationDrawer(
                                    currentScreen = currentScreen,
                                    onNavigate = { screen ->
                                        coroutineScope.launch { drawerState.close() }
                                        if (screen == Screen.Cleaner) {
                                            com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                                currentScreen = Screen.Cleaner
                                            }
                                        } else {
                                            currentScreen = screen
                                        }
                                    },
                                    onOpenPaywall = {
                                        coroutineScope.launch { drawerState.close() }
                                        showPaywall = true
                                    },
                                    onRateUs = {
                                        coroutineScope.launch { drawerState.close() }
                                        openPlayStore(context)
                                    },
                                    onShareApp = {
                                        coroutineScope.launch { drawerState.close() }
                                        shareApp(context)
                                    },
                                    onPrivacyPolicy = {
                                        coroutineScope.launch { drawerState.close() }
                                        openPrivacyPolicy(context)
                                    },
                                    onFeedback = {
                                        coroutineScope.launch { drawerState.close() }
                                        sendFeedback(context)
                                    }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            bottomBar = {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    color = Color.White,
                                    shadowElevation = 14.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. HOME BUTTON (Large, highly visible round button + Home text)
                                        RoundBottomNavItem(
                                            title = "Home",
                                            icon = Icons.Default.Home,
                                            selected = currentScreen == Screen.Dashboard,
                                            accentColor = Color(0xFF2563EB),
                                            onClick = { currentScreen = Screen.Dashboard }
                                        )

                                        // 2. JUNK BUTTON (Large, highly visible round button + Junk text)
                                        RoundBottomNavItem(
                                            title = "Junk",
                                            icon = Icons.Default.CleaningServices,
                                            selected = currentScreen == Screen.Cleaner,
                                            accentColor = Color(0xFF10B981),
                                            onClick = {
                                                com.arfa_zuha.phonecleaner.ms321.ads.InterstitialAdHelper.showAd(this@MainActivity) {
                                                    currentScreen = Screen.Cleaner
                                                }
                                            }
                                        )

                                        // 3. FILES BUTTON (Large, highly visible round button + Files text)
                                        RoundBottomNavItem(
                                            title = "Files",
                                            icon = Icons.Default.Folder,
                                            selected = currentScreen == Screen.FileManager,
                                            accentColor = Color(0xFF2563EB),
                                            onClick = { currentScreen = Screen.FileManager }
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
                                        onNavigateToPaywall = { showPaywall = true },
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
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

    private fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    private fun shareApp(context: Context) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Free up storage and clean junk files with Phone Cleaner: https://play.google.com/store/apps/details?id=${context.packageName}")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Phone Cleaner").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openPrivacyPolicy(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendFeedback(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@phonecleaner.app")
                putExtra(Intent.EXTRA_SUBJECT, "Phone Cleaner Feedback & Support")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Reusable Round Circular Bottom Navigation Item
 * Matches the exact styling of the top Navigation Menu Button (CircleShape, soft border, clean centered icon)
 * with a clean simple title text placed underneath (Home, Junk, Files).
 */
@Composable
private fun RowScope.RoundBottomNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accentColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = tween(120),
        label = "round_item_scale"
    )

    val activeBg = if (accentColor == Color(0xFF10B981)) Color(0xFFD1FAE5) else Color(0xFFDBEAFE)

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Round Circular Button Container with high-visibility border & badge
        Surface(
            shape = CircleShape,
            color = if (selected) activeBg else Color(0xFFF8FAFC),
            border = BorderStroke(
                if (selected) 1.8.dp else 1.dp,
                if (selected) accentColor else Color(0xFFCBD5E1)
            ),
            shadowElevation = if (selected) 3.dp else 0.dp,
            modifier = Modifier
                .scale(scale)
                .size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) accentColor else Color(0xFF475569),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Large, crisp, bold title text
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) accentColor else Color(0xFF334155)
        )
    }
}
