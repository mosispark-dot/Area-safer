package com.arfa_zuha.phonecleaner.ms321.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badgeTag: String,
    val highlightChip: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val lightBgColor: Color
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingFinished: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            title = "Deep Storage Clean",
            description = "Effortlessly scan and clear junk files, app cache, and residual system data to speed up your phone.",
            icon = Icons.Default.CleaningServices,
            badgeTag = "🧹 Smart Junk Cleaner",
            highlightChip = "100% Safe Clean",
            primaryColor = Color(0xFF10B981), // Emerald
            secondaryColor = Color(0xFF38BDF8), // Light Cyan
            lightBgColor = Color(0xFFF0FDF4)
        ),
        OnboardingPageData(
            title = "Photo & Video Compression",
            description = "Reduce large media files without losing visual clarity to reclaim gigabytes of valuable storage.",
            icon = Icons.Default.FolderZip,
            badgeTag = "🗜️ Lossless Compressor",
            highlightChip = "Save Up to 80% Space",
            primaryColor = Color(0xFF3B82F6), // Vibrant Blue
            secondaryColor = Color(0xFF6366F1), // Indigo
            lightBgColor = Color(0xFFEFF6FF)
        ),
        OnboardingPageData(
            title = "Detect Unused Apps",
            description = "Easily find hidden, heavy applications you haven't opened in months and batch uninstall in one tap.",
            icon = Icons.Default.PhoneAndroid,
            badgeTag = "📱 App Insights",
            highlightChip = "Batch Uninstall",
            primaryColor = Color(0xFF8B5CF6), // Purple
            secondaryColor = Color(0xFF06B6D4), // Cyan
            lightBgColor = Color(0xFFF5F3FF)
        ),
        OnboardingPageData(
            title = "Duplicate & File Manager",
            description = "Identify identical photos, duplicate videos, and large archives to free up disk space with zero hassle.",
            icon = Icons.Default.CopyAll,
            badgeTag = "📁 Duplicate Finder",
            highlightChip = "Smart Auto-Select",
            primaryColor = Color(0xFF10B981), // Emerald
            secondaryColor = Color(0xFF3B82F6), // Blue
            lightBgColor = Color(0xFFECFDF5)
        )
    )

    // Infinite rotating transition matching the Dashboard hero animation
    val infiniteTransition = rememberInfiniteTransition(label = "OnboardingOrbit")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween( durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )
    val counterOrbitRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterOrbitRotation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFC) // Clean light modern background matching Dashboard
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Row with Modern Skip Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Phone Cleaner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                if (currentPage < pages.size - 1) {
                    Surface(
                        onClick = onOnboardingFinished,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // 2. Middle Slide with Floating Luminous Arcs & Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> (width * 0.8f).toInt() } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { width -> -(width * 0.8f).toInt() } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally { width -> -(width * 0.8f).toInt() } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { width -> (width * 0.8f).toInt() } + fadeOut(tween(250)))
                    }
                },
                label = "OnboardingSlide",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating Animated Hero Visual (Exact style match to Dashboard Hero)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Clockwise Luminous Arc
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .rotate(orbitRotation)
                        ) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        page.secondaryColor.copy(alpha = 0.25f),
                                        page.secondaryColor,
                                        page.primaryColor,
                                        Color.Transparent
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 140f,
                                useCenter = false,
                                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Inner Counter-Clockwise Complementary Arc
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .rotate(counterOrbitRotation)
                        ) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        page.primaryColor.copy(alpha = 0.2f),
                                        page.primaryColor,
                                        page.secondaryColor,
                                        Color.Transparent
                                    )
                                ),
                                startAngle = 180f,
                                sweepAngle = 110f,
                                useCenter = false,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Central Floating Clean Card
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .shadow(8.dp, CircleShape, spotColor = page.primaryColor.copy(alpha = 0.2f))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White,
                                            page.lightBgColor.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = page.title,
                                tint = page.primaryColor,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Feature Tag Badge
                    Surface(
                        color = page.lightBgColor,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, page.primaryColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = page.badgeTag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = page.primaryColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title Text
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Description Text
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtle Highlight Chip (e.g. "100% Safe Clean")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Text(
                            text = "✓ ${page.highlightChip}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Banner Ad on Onboarding (Uses AdConfig Testing/Production AdMob ID)
            com.arfa_zuha.phonecleaner.ms321.ads.NativeBannerAd(
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 3. Bottom Navigation & Smart Handsome Action Section
            val isLastPage = currentPage == pages.size - 1
            val nextInteraction = remember { MutableInteractionSource() }
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val buttonScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isNextPressed) 0.94f else 1.0f,
                animationSpec = tween(120),
                label = "buttonScale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smooth Animated Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in pages.indices) {
                        val isSelected = i == currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                                )
                        )
                    }
                }

                // Smart Handsome Action Capsule Button
                Surface(
                    onClick = {
                        if (!isLastPage) {
                            currentPage++
                        } else {
                            onOnboardingFinished()
                        }
                    },
                    interactionSource = nextInteraction,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 8.dp,
                    color = Color.Transparent,
                    modifier = Modifier
                        .scale(buttonScale)
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    if (isLastPage) listOf(Color(0xFF10B981), Color(0xFF059669))
                                    else listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                                )
                            )
                            .padding(
                                start = if (isLastPage) 22.dp else 18.dp,
                                end = if (isLastPage) 16.dp else 14.dp,
                                top = 10.dp,
                                bottom = 10.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isLastPage) "Get Started" else "Next",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLastPage) {
                                        Icons.Default.RocketLaunch
                                    } else {
                                        Icons.AutoMirrored.Filled.ArrowForward
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
