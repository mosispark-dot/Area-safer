package com.arfa_zuha.phonecleaner.ms321.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arfa_zuha.phonecleaner.ms321.R
import com.arfa_zuha.phonecleaner.ms321.Screen

@Composable
fun AppNavigationDrawer(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    onOpenPaywall: () -> Unit,
    onRateUs: () -> Unit,
    onShareApp: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onFeedback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(290.dp)
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. App Header with Mascot/Logo (Compact & Clean)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF10B981), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ai_cleaner_icon),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column {
                Text(
                    text = "Phone Cleaner",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Storage & Memory Pro",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }

        // 2. VIP PRO Golden Promo Card (Compact Spacing)
        Surface(
            onClick = onOpenPaywall,
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                        )
                    )
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upgrade to VIP PRO",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            text = "Remove ads & unlock features",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

        // 3. Main Navigation & Cleaner Tools Section
        Text(
            text = "CLEANER TOOLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        DrawerMenuItem(
            icon = Icons.Default.CleaningServices,
            title = "Junk & Cache Cleaner",
            accentColor = Color(0xFF10B981),
            isSelected = currentScreen == Screen.Cleaner,
            onClick = { onNavigate(Screen.Cleaner) }
        )

        DrawerMenuItem(
            icon = Icons.Default.Folder,
            title = "Files & Storage Manager",
            accentColor = Color(0xFF3B82F6),
            isSelected = currentScreen == Screen.FileManager,
            onClick = { onNavigate(Screen.FileManager) }
        )

        DrawerMenuItem(
            icon = Icons.Default.FolderZip,
            title = "Media Compression",
            accentColor = Color(0xFF6366F1),
            isSelected = currentScreen == Screen.Compression,
            onClick = { onNavigate(Screen.Compression) }
        )

        DrawerMenuItem(
            icon = Icons.Default.CopyAll,
            title = "Duplicate Files Cleaner",
            accentColor = Color(0xFFF59E0B),
            isSelected = currentScreen == Screen.Duplicates,
            onClick = { onNavigate(Screen.Duplicates) }
        )

        DrawerMenuItem(
            icon = Icons.Default.PhoneAndroid,
            title = "Unused Apps Manager",
            accentColor = Color(0xFF8B5CF6),
            isSelected = currentScreen == Screen.Apps,
            onClick = { onNavigate(Screen.Apps) }
        )

        DrawerMenuItem(
            icon = Icons.Default.BatteryChargingFull,
            title = "Battery Optimizer",
            accentColor = Color(0xFF10B981),
            isSelected = currentScreen == Screen.Battery,
            onClick = { onNavigate(Screen.Battery) }
        )

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

        // 4. App Support, Rate Us, Share & Privacy
        Text(
            text = "APP & SUPPORT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Rate Us Button (Highlighted)
        DrawerActionItem(
            icon = Icons.Default.Star,
            title = "Rate Us 5 Stars ⭐",
            iconTint = Color(0xFFEAB308),
            badgeText = "Love It!",
            onClick = onRateUs
        )

        // Share App Button
        DrawerActionItem(
            icon = Icons.Default.Share,
            title = "Share with Friends",
            iconTint = Color(0xFF3B82F6),
            onClick = onShareApp
        )

        // Feedback & Support
        DrawerActionItem(
            icon = Icons.Default.Email,
            title = "Feedback & Help",
            iconTint = Color(0xFF06B6D4),
            onClick = onFeedback
        )

        // Privacy Policy
        DrawerActionItem(
            icon = Icons.Default.Security,
            title = "Privacy Policy",
            iconTint = Color(0xFF64748B),
            onClick = onPrivacyPolicy
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))

        // 5. Version Info Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Phone Cleaner • v1.0.0",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isSelected) accentColor else accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else Color(0xFF1E293B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DrawerActionItem(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                modifier = Modifier.weight(1f)
            )

            if (badgeText != null) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
