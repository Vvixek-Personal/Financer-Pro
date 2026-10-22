package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R

/**
 * Clean & Structured Sidebar Drawer Architecture
 * Encapsulates the complete Navigation Drawer UI, section groups, search filtering,
 * user profile header, quick shortcuts, and settings routing.
 */
@Composable
fun SidebarSettingsTile(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBgColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = SleekTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class SidebarMenuItemData(
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val titleKey: String,
    val subtitleKey: String,
    val onClick: () -> Unit
)

@Composable
fun SidebarGroupCard(
    sectionTitle: String,
    items: List<SidebarMenuItemData>,
    selectedLanguage: String
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = LanguageManager.tr(sectionTitle, selectedLanguage).lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = SleekShapes.card,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(SleekShapes.chip)
                                .background(SleekBg)
                                .border(1.dp, SleekBorder, SleekShapes.chip),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.titleKey,
                                tint = SleekPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageManager.tr(item.titleKey, selectedLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextPrimary,
                                fontSize = 14.sp,
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = LanguageManager.tr(item.subtitleKey, selectedLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp,
                                softWrap = true
                            )
                        }
                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (index < items.size - 1) {
                        HorizontalDivider(
                            color = SleekBorder,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SidebarDrawerContent(
    viewModel: FinanceViewModel,
    onCloseDrawer: () -> Unit,
    onOpenSettingsScreen: (SettingsSubScreen) -> Unit,
    onChangePasswordClick: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val isPrivacyMode by viewModel.privacyModeEnabled.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val allSections = remember(selectedLanguage) {
        listOf(
            "ACCOUNT & PROFILE" to listOf(
                SidebarMenuItemData(
                    icon = AppIcons.Person,
                    iconColor = Color(0xFF2563EB),
                    iconBgColor = Color(0xFFEFF6FF),
                    titleKey = "Profile",
                    subtitleKey = "View and edit personal profile details",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.PersonalData)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Star,
                    iconColor = Color(0xFFEA580C),
                    iconBgColor = Color(0xFFFFEDD5),
                    titleKey = "Badges and Milestone",
                    subtitleKey = "Daily streaks, badges and achievements",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.BadgesAndMilestones)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.HealthAndSafety,
                    iconColor = Color(0xFF10B981),
                    iconBgColor = Color(0xFFD1FAE5),
                    titleKey = "Financial Health Studio",
                    subtitleKey = "Diagnostic score, 4 pillars & what-if simulator",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.FinancialHealthStudio)
                    }
                )
            ),
            "PREFERENCES & APPEARANCE" to listOf(
                SidebarMenuItemData(
                    icon = AppIcons.Palette,
                    iconColor = Color(0xFF8B5CF6),
                    iconBgColor = Color(0xFFF3E8FF),
                    titleKey = "Appearance & Theme",
                    subtitleKey = "Light/Dark mode, color palettes & custom accent",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Appearance)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Translate,
                    iconColor = Color(0xFF2563EB),
                    iconBgColor = Color(0xFFEFF6FF),
                    titleKey = "Language",
                    subtitleKey = "App localization, Hindi, English & 9+ languages",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Language)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Payments,
                    iconColor = Color(0xFF10B981),
                    iconBgColor = Color(0xFFD1FAE5),
                    titleKey = "Currency & Rates",
                    subtitleKey = "100+ currencies, live conversion & stats currency",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Currency)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Receipt,
                    iconColor = Color(0xFF0D9488),
                    iconBgColor = Color(0xFFCCFBF1),
                    titleKey = "Transaction Preferences",
                    subtitleKey = "Input defaults, remember category & safe delete",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Transactions)
                    }
                )
            ),
            "FINANCIAL MANAGEMENT" to listOf(
                SidebarMenuItemData(
                    icon = AppIcons.ReceiptLong,
                    iconColor = Color(0xFFD97706),
                    iconBgColor = Color(0xFFFEF3C7),
                    titleKey = "Bills & Reminders",
                    subtitleKey = "Configure recurring bills, alerts & due dates",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Bills)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.PieChart,
                    iconColor = Color(0xFF0284C7),
                    iconBgColor = Color(0xFFE0F2FE),
                    titleKey = "Budgets",
                    subtitleKey = "Monthly limits, warning thresholds & indicators",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Budgets)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Savings,
                    iconColor = Color(0xFFF59E0B),
                    iconBgColor = Color(0xFFFEF3C7),
                    titleKey = "Savings Goals",
                    subtitleKey = "Goal layouts, milestone celebrations & auto-gap",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.SavingsGoals)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Calculate,
                    iconColor = Color(0xFF6366F1),
                    iconBgColor = Color(0xFFEEF2FF),
                    titleKey = "Financial Calculators",
                    subtitleKey = "Currency converter, split bill, tip & percentage tools",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Calculations)
                    }
                )
            ),
            "CATEGORIES & DATA" to listOf(
                SidebarMenuItemData(
                    icon = AppIcons.Category,
                    iconColor = Color(0xFF16A34A),
                    iconBgColor = Color(0xFFDCFCE7),
                    titleKey = "Categories & Tags",
                    subtitleKey = "Manage categories, tags and custom groups",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.CategoriesTags)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.FileDownload,
                    iconColor = Color(0xFF0284C7),
                    iconBgColor = Color(0xFFE0F2FE),
                    titleKey = "Export Statements",
                    subtitleKey = "Export analytics, stats, CSV & PDF reports",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Export)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Storage,
                    iconColor = Color(0xFF10B981),
                    iconBgColor = Color(0xFFD1FAE5),
                    titleKey = "Data Management",
                    subtitleKey = "Storage usage, clear cache, reset app data",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.DataManagement)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.CloudSync,
                    iconColor = Color(0xFF0284C7),
                    iconBgColor = Color(0xFFE0F2FE),
                    titleKey = "Backup & Restore",
                    subtitleKey = "Local Only or Online Cloud Sync, 1-click restore",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.BackupRestore)
                    }
                )
            ),
            "SECURITY & ABOUT" to listOf(
                SidebarMenuItemData(
                    icon = AppIcons.Lock,
                    iconColor = Color(0xFF8B5CF6),
                    iconBgColor = Color(0xFFF3E8FF),
                    titleKey = "Password & Security",
                    subtitleKey = "App lock, passcode, biometric security",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Security)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.VisibilityOff,
                    iconColor = Color(0xFF6366F1),
                    iconBgColor = Color(0xFFE0E7FF),
                    titleKey = "Privacy Settings",
                    subtitleKey = "Blur sensitive amounts, hide balances & privacy mode",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.Privacy)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Help,
                    iconColor = Color(0xFF0284C7),
                    iconBgColor = Color(0xFFE0F2FE),
                    titleKey = "Help & Support FAQ",
                    subtitleKey = "Categorized answers for app features & troubleshooting",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.HelpSupport)
                    }
                ),
                SidebarMenuItemData(
                    icon = AppIcons.Info,
                    iconColor = Color(0xFFDB2777),
                    iconBgColor = Color(0xFFFCE7F3),
                    titleKey = "What's New & About",
                    subtitleKey = "Version info, new features and updates",
                    onClick = {
                        onCloseDrawer()
                        onOpenSettingsScreen(SettingsSubScreen.AboutApp)
                    }
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
        // Top Close & App Brand Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_modern),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column {
                    Text(
                        text = "Finance Tracker Pro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = LanguageManager.tr("Settings", selectedLanguage),
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekBorder.copy(alpha = 0.4f))
                        .bouncyPress()
                ) {
                    Icon(
                        imageVector = if (isFullscreen) AppIcons.FullscreenExit else AppIcons.Fullscreen,
                        contentDescription = if (isFullscreen) "Collapse Sidebar" else "Expand Fullscreen",
                        tint = SleekTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekBorder.copy(alpha = 0.4f))
                        .bouncyPress()
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close Drawer",
                        tint = SleekTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // 1. User Profile Top Section Hero Card
        val initials = userName?.split(" ")?.mapNotNull { it.firstOrNull()?.uppercaseChar() }?.joinToString("")?.take(2)?.ifBlank { "U" } ?: "U"

        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onCloseDrawer()
                    onOpenSettingsScreen(SettingsSubScreen.PersonalData)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(SleekPrimary, SleekPrimaryContainer, SleekPrimary)
                            )
                        )
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(SleekPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_default_profile_avatar),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName?.takeIf { it.isNotBlank() } ?: LanguageManager.tr("User", selectedLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🔥 $currentStreak " + LanguageManager.tr("Day Streak", selectedLanguage),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🛡️ " + LanguageManager.tr("100% Offline", selectedLanguage),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SleekBorder.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = "View Profile Page",
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar for Quick Filtering Settings
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = LanguageManager.tr("Search settings...", selectedLanguage),
                    fontSize = 13.5.sp,
                    color = SleekTextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = null,
                    tint = SleekTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Clear",
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface,
                focusedBorderColor = SleekPrimary,
                unfocusedBorderColor = SleekBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = LanguageManager.tr("QUICK ACTIONS", selectedLanguage),
            style = MaterialTheme.typography.labelSmall,
            color = SleekTextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = {
                    onCloseDrawer()
                    onOpenSettingsScreen(SettingsSubScreen.Currency)
                },
                shape = RoundedCornerShape(12.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.Payments, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Text(LanguageManager.tr("Currency", selectedLanguage), style = MaterialTheme.typography.labelMedium, color = SleekTextPrimary)
                }
            }
            Surface(
                onClick = {
                    onCloseDrawer()
                    onOpenSettingsScreen(SettingsSubScreen.Export)
                },
                shape = RoundedCornerShape(12.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.FileDownload, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                    Text(LanguageManager.tr("Export", selectedLanguage), style = MaterialTheme.typography.labelMedium, color = SleekTextPrimary)
                }
            }
            Surface(
                onClick = {
                    onCloseDrawer()
                    onOpenSettingsScreen(SettingsSubScreen.Security)
                },
                shape = RoundedCornerShape(12.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.Lock, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                    Text(LanguageManager.tr("Security", selectedLanguage), style = MaterialTheme.typography.labelMedium, color = SleekTextPrimary)
                }
            }
            Surface(
                onClick = {
                    onCloseDrawer()
                    onOpenSettingsScreen(SettingsSubScreen.Calculations)
                },
                shape = RoundedCornerShape(12.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.Calculate, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                    Text(LanguageManager.tr("Calculators", selectedLanguage), style = MaterialTheme.typography.labelMedium, color = SleekTextPrimary)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Calculate total matched items for search empty state
        val totalMatched = allSections.sumOf { (_, items) ->
            if (searchQuery.isBlank()) items.size else items.count {
                val title = LanguageManager.tr(it.titleKey, selectedLanguage)
                val sub = LanguageManager.tr(it.subtitleKey, selectedLanguage)
                title.contains(searchQuery, ignoreCase = true) || sub.contains(searchQuery, ignoreCase = true)
            }
        }

        if (totalMatched == 0 && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = LanguageManager.tr("No matching settings", selectedLanguage),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = LanguageManager.tr("Try searching for a different keyword or category", selectedLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Render Filtered Sections
        allSections.forEach { (sectionTitle, items) ->
            val filteredItems = if (searchQuery.isBlank()) {
                items
            } else {
                items.filter {
                    val title = LanguageManager.tr(it.titleKey, selectedLanguage)
                    val sub = LanguageManager.tr(it.subtitleKey, selectedLanguage)
                    title.contains(searchQuery, ignoreCase = true) ||
                    sub.contains(searchQuery, ignoreCase = true)
                }
            }

            if (filteredItems.isNotEmpty()) {
                SidebarGroupCard(
                    sectionTitle = sectionTitle,
                    items = filteredItems,
                    selectedLanguage = selectedLanguage
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = SleekBorder)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Footer: Version & Credits
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (" + LanguageManager.tr("Stable & Secure", selectedLanguage) + ")",
                style = MaterialTheme.typography.labelMedium,
                color = SleekTextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Created with",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextSecondary
                )
                Icon(
                    imageVector = AppIcons.Favorite,
                    contentDescription = "Love",
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "by Vivek",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
