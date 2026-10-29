package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppShortcutItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val gradientColors: List<Color>,
    val category: String,
    val badgeText: String? = null,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 📱 Full-Screen App Launcher Style "All Shortcuts" Tab
 * Displays all non-repeating application shortcuts with search, category filtering,
 * home-screen style app icons, spring animations, and instant navigation.
 */
@Composable
fun AllShortcutsTabScreen(
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onQuickSplit: () -> Unit,
    onQuickConvert: () -> Unit,
    onViewGoals: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onBillsClick: () -> Unit,
    onReminderClick: () -> Unit,
    onAdjustBudget: () -> Unit,
    onOpenStreak: () -> Unit,
    onOpenSettingsSubScreen: (SettingsSubScreen) -> Unit,
    hasBillDueToday: Boolean = false,
    currentStreak: Int = 1,
    selectedLanguage: String = "English",
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val allShortcuts = remember(hasBillDueToday, currentStreak, selectedLanguage) {
        listOf(
            // 💸 Money Flow
            AppShortcutItem(
                id = "add_expense",
                title = "Add Expense",
                subtitle = "Log daily spend",
                icon = AppIcons.TrendingDown,
                iconTint = Color(0xFFEF4444),
                gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                category = "Money Flow",
                isPrimary = true,
                onClick = onAddExpense
            ),
            AppShortcutItem(
                id = "add_income",
                title = "Add Income",
                subtitle = "Record revenue",
                icon = AppIcons.TrendingUp,
                iconTint = Color(0xFF10B981),
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                category = "Money Flow",
                isPrimary = true,
                onClick = onAddIncome
            ),
            AppShortcutItem(
                id = "split_bill",
                title = "Split Bill",
                subtitle = "Calculate group shares",
                icon = AppIcons.CallSplit,
                iconTint = Color(0xFF6366F1),
                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
                category = "Money Flow",
                isPrimary = true,
                onClick = onQuickSplit
            ),
            AppShortcutItem(
                id = "convert_currency",
                title = "Currency Convert",
                subtitle = "Live exchange rates",
                icon = AppIcons.CurrencyExchange,
                iconTint = Color(0xFF0EA5E9),
                gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                category = "Money Flow",
                isPrimary = true,
                onClick = onQuickConvert
            ),

            // 📊 Planning & Budgets
            AppShortcutItem(
                id = "transactions_ledger",
                title = "Transactions",
                subtitle = "Full ledger & history",
                icon = AppIcons.ReceiptLong,
                iconTint = Color(0xFF8B5CF6),
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)),
                category = "Planning",
                onClick = onNavigateToExpenses
            ),
            AppShortcutItem(
                id = "analytics_reports",
                title = "Analytics",
                subtitle = "Charts & breakdown",
                icon = AppIcons.PieChart,
                iconTint = Color(0xFF10B981),
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                category = "Planning",
                onClick = onNavigateToAnalytics
            ),
            AppShortcutItem(
                id = "savings_goals",
                title = "Savings Goals",
                subtitle = "Track financial targets",
                icon = AppIcons.Savings,
                iconTint = Color(0xFFF59E0B),
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                category = "Planning",
                onClick = onViewGoals
            ),
            AppShortcutItem(
                id = "bills_manager",
                title = "Bills & Due Dates",
                subtitle = "Upcoming recurring bills",
                icon = AppIcons.Receipt,
                iconTint = Color(0xFFF97316),
                gradientColors = listOf(Color(0xFFF97316), Color(0xFFEA580C)),
                category = "Planning",
                badgeText = if (hasBillDueToday) "Due Today" else null,
                onClick = onBillsClick
            ),
            AppShortcutItem(
                id = "bill_reminders",
                title = "Reminders",
                subtitle = "Custom notifications",
                icon = AppIcons.NotificationsActive,
                iconTint = Color(0xFFEC4899),
                gradientColors = listOf(Color(0xFFEC4899), Color(0xFFDB2777)),
                category = "Planning",
                onClick = onReminderClick
            ),
            AppShortcutItem(
                id = "monthly_budget",
                title = "Monthly Budget",
                subtitle = "Adjust limits & alerts",
                icon = AppIcons.AccountBalanceWallet,
                iconTint = Color(0xFF3B82F6),
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB)),
                category = "Planning",
                onClick = onAdjustBudget
            ),
            AppShortcutItem(
                id = "financial_health_studio",
                title = "Health Studio",
                subtitle = "Diagnostic score & simulator",
                icon = AppIcons.HealthAndSafety,
                iconTint = Color(0xFF10B981),
                gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                category = "Planning",
                badgeText = "Studio",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.FinancialHealthStudio) }
            ),
            AppShortcutItem(
                id = "net_worth_assets",
                title = "Net Worth & Assets",
                subtitle = "Asset allocation & ratios",
                icon = AppIcons.AccountBalance,
                iconTint = Color(0xFF3B82F6),
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                category = "Planning",
                badgeText = "Portfolio",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.NetWorthAssets) }
            ),

            // 🧮 Tools & Utilities
            AppShortcutItem(
                id = "calculations_hub",
                title = "Calculations",
                subtitle = "Math & finance tools",
                icon = AppIcons.Calculate,
                iconTint = Color(0xFF14B8A6),
                gradientColors = listOf(Color(0xFF14B8A6), Color(0xFF0D9488)),
                category = "Tools",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.Calculations) }
            ),
            AppShortcutItem(
                id = "categories_tags",
                title = "Categories & Tags",
                subtitle = "Custom icons & colors",
                icon = AppIcons.Category,
                iconTint = Color(0xFFA855F7),
                gradientColors = listOf(Color(0xFFA855F7), Color(0xFF9333EA)),
                category = "Tools",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.CategoriesTags) }
            ),
            AppShortcutItem(
                id = "daily_streak",
                title = "Daily Streak",
                subtitle = "$currentStreak days active",
                icon = AppIcons.LocalFireDepartment,
                iconTint = Color(0xFFFF5722),
                gradientColors = listOf(Color(0xFFFF5722), Color(0xFFE64A19)),
                category = "Tools",
                badgeText = "$currentStreak 🔥",
                onClick = onOpenStreak
            ),
            AppShortcutItem(
                id = "export_data",
                title = "Export Reports",
                subtitle = "Download CSV / PDF",
                icon = AppIcons.FileDownload,
                iconTint = Color(0xFF64748B),
                gradientColors = listOf(Color(0xFF64748B), Color(0xFF475569)),
                category = "Tools",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.Export) }
            ),
            AppShortcutItem(
                id = "backup_restore",
                title = "Backup & Restore",
                subtitle = "Safe local data storage",
                icon = AppIcons.CloudSync,
                iconTint = Color(0xFF0284C7),
                gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
                category = "Tools",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.BackupRestore) }
            ),
            AppShortcutItem(
                id = "privacy_shield",
                title = "Privacy & Security",
                subtitle = "PIN & balance shield",
                icon = AppIcons.Security,
                iconTint = Color(0xFFE11D48),
                gradientColors = listOf(Color(0xFFE11D48), Color(0xFFBE123C)),
                category = "Tools",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.Privacy) }
            ),
            AppShortcutItem(
                id = "app_updates",
                title = "Updates & Specs",
                subtitle = "Real-time timeline",
                icon = AppIcons.Timeline,
                iconTint = Color(0xFFEAB308),
                gradientColors = listOf(Color(0xFFEAB308), Color(0xFFCA8A04)),
                category = "Tools",
                badgeText = "v1.29",
                onClick = { onOpenSettingsSubScreen(SettingsSubScreen.AboutApp) }
            )
        )
    }

    val categories = remember { listOf("All", "Money Flow", "Planning", "Tools") }

    val filteredShortcuts = remember(searchQuery, selectedCategory, allShortcuts) {
        allShortcuts.filter { item ->
            val matchesCategory = (selectedCategory == "All" || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Surface(
                color = SleekSurface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = AppIcons.ArrowBack,
                                contentDescription = "Back",
                                tint = SleekTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Quick Shortcuts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${allShortcuts.size} one-tap tools available",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = SleekPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Launcher",
                                fontSize = SleekSizes.textCaption,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search shortcuts (e.g. Split, Goal, Bills)...", fontSize = SleekSizes.textBodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(
                                AppIcons.Search,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(SleekSizes.iconMedium)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        AppIcons.Close,
                                        contentDescription = "Clear",
                                        tint = SleekTextSecondary,
                                        modifier = Modifier.size(SleekSizes.iconSmall)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = SleekShapes.lg,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekSurfaceVariant,
                            unfocusedContainerColor = SleekSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    // Category Pill Tabs
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories, key = { it }) { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = SleekSizes.textBodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SleekSurfaceVariant,
                                    labelColor = SleekTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SleekBorder,
                                    selectedBorderColor = SleekPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredShortcuts.isEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                AppIcons.SearchOff,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No shortcuts found for \"$searchQuery\"",
                                color = SleekTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(filteredShortcuts, key = { it.id }) { shortcut ->
                    HomeScreenAppIconTile(
                        item = shortcut,
                        modifier = Modifier.testTag("app_shortcut_${shortcut.id}")
                    )
                }
            }

            item(span = { GridItemSpan(4) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 🎨 Home Screen Style App Icon Tile
 * Resembles modern mobile launcher icons with rounded squircle, ambient glow,
 * spring scale micro-interaction, haptics, and notification badge.
 */
@Composable
fun HomeScreenAppIconTile(
    item: AppShortcutItem,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tile_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(SleekShapes.lg)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                item.onClick()
            }
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val isDark = isDarkModeActive
            val iconBgBrush = if (isDark) {
                Brush.radialGradient(
                    listOf(
                        item.gradientColors.first().copy(alpha = 0.38f),
                        item.gradientColors.last().copy(alpha = 0.16f),
                        Color(0xFF0F172A)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        item.gradientColors.first().copy(alpha = 0.22f),
                        item.gradientColors.last().copy(alpha = 0.10f)
                    )
                )
            }
            val rimColor = if (isDark) item.gradientColors.first().copy(alpha = 0.55f) else item.gradientColors.first().copy(alpha = 0.38f)
            val iconColor = if (isDark) item.gradientColors.first().copy(alpha = 0.98f) else item.iconTint

            // Squircle app icon container with tactile depth elevation
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(elevation = if (isDark) 5.dp else 2.5.dp, shape = RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconBgBrush)
                    .border(
                        BorderStroke(1.5.dp, rimColor),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notification or feature badge
            if (item.badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(item.gradientColors.first())
                        .border(1.5.dp, SleekSurface, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.badgeText,
                        color = Color.White,
                        fontSize = SleekSizes.textMicro,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            fontSize = SleekSizes.textCaption,
            color = SleekTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.subtitle,
            fontSize = SleekSizes.textMicro,
            color = SleekTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
