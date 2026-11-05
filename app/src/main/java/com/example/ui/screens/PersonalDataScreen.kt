package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Expense
import com.example.data.SavingsGoal
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * 👑 REDESIGNED PROFILE SCREEN (Theme-Adaptive Luxury Aesthetic)
 * Seamlessly adapts to both Light Mode and Dark Mode with high contrast,
 * full accessibility, real financial metrics, streak curve, and functional navigation.
 */
@Composable
fun PersonalDataScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onNavigateToBadges: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isDark = isDarkModeActive
    val currentName by viewModel.userName.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val savingGoals by viewModel.savingsGoals.collectAsStateWithLifecycle(emptyList())

    var showEditProfileDialog by remember { mutableStateOf(false) }
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    // --- REAL METRICS COMPUTATIONS ---
    val totalBalance = remember(accounts) {
        accounts.sumOf { it.balance }
    }

    val (thisMonthExpenses, thisMonthIncome) = remember(expenses) {
        val cal = Calendar.getInstance()
        val cMonth = cal.get(Calendar.MONTH)
        val cYear = cal.get(Calendar.YEAR)
        val startCal = Calendar.getInstance().apply {
            set(cYear, cMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(cYear, cMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val start = startCal.timeInMillis
        val end = endCal.timeInMillis

        var exp = 0.0
        var inc = 0.0
        for (e in expenses) {
            val t = e.date
            if (t in start..end) {
                if (e.type == "INCOME") inc += e.amount else exp += e.amount
            }
        }
        Pair(exp, inc)
    }

    val savedAmount = remember(thisMonthIncome, thisMonthExpenses, savingGoals) {
        val net = (thisMonthIncome - thisMonthExpenses).coerceAtLeast(0.0)
        val goalSaved = savingGoals.sumOf { it.currentAmount }
        if (net > 0.0) net else goalSaved
    }

    val activeDays = remember(currentStreak) {
        if (currentStreak <= 0) 1 else currentStreak
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateUserProfileImageFromUri(context, uri)
            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic screen background
    val screenBg = if (isDark) Color(0xFF071115) else SleekBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Decorative background wavy ambient lighting (Dark teal in dark mode, gentle accent tint in light mode)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            val w = size.width
            val h = size.height

            val meshPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.42f)
                cubicTo(w * 0.75f, h * 0.85f, w * 0.35f, h * 0.25f, 0f, h * 0.88f)
                close()
            }

            val meshColors = if (isDark) {
                listOf(
                    Color(0xFF0F3138).copy(alpha = 0.65f),
                    Color(0xFF0A2228).copy(alpha = 0.35f),
                    Color.Transparent
                )
            } else {
                listOf(
                    SleekPrimary.copy(alpha = 0.14f),
                    Color(0xFF0D9488).copy(alpha = 0.08f),
                    Color.Transparent
                )
            }

            drawPath(
                path = meshPath,
                brush = Brush.verticalGradient(colors = meshColors)
            )

            // Secondary subtle wave
            val subtlePath = Path().apply {
                moveTo(0f, h * 0.5f)
                cubicTo(w * 0.4f, h * 0.9f, w * 0.7f, h * 0.3f, w, h * 0.65f)
                lineTo(w, 0f)
                lineTo(0f, 0f)
                close()
            }

            val subtleColors = if (isDark) {
                listOf(
                    Color(0xFF144A54).copy(alpha = 0.2f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color(0xFF14B8A6).copy(alpha = 0.09f),
                    Color.Transparent
                )
            }

            drawPath(
                path = subtlePath,
                brush = Brush.linearGradient(colors = subtleColors)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- TOP HEADER: AVATAR, NAME, CROWN, TAGLINE, SETTINGS GEAR ---
            ProfileHeaderSection(
                userName = currentName?.takeIf { it.isNotBlank() } ?: "Vivek Singh",
                profileImageUri = profileImageUri,
                isDark = isDark,
                onAvatarClick = { showEditProfileDialog = true },
                onEditNameClick = { showEditProfileDialog = true },
                onSettingsClick = onOpenSettings,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4 STATS ROW CARD (LEGIT DATA) ---
            ProfileStatsCard(
                currencySymbol = currencySymbol,
                totalBalance = totalBalance,
                thisMonthExpenses = thisMonthExpenses,
                savedAmount = savedAmount,
                activeDays = activeDays,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- LIVE ACHIEVEMENTS (computed from this user's own records) ---
            LiveAchievementsSection(
                isDark = isDark,
                totalTransactions = expenses.size,
                streak = currentStreak,
                goalsTotal = savingGoals.size,
                goalsCompleted = savingGoals.count { it.savedAmount >= it.targetAmount && it.targetAmount > 0 },
                budgetKept = monthlyBudget > 0 && thisMonthExpenses <= monthlyBudget,
                hasBudget = monthlyBudget > 0,
                onSeeAllClick = onNavigateToBadges
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- STREAK CARD WITH SMOOTH GOLDEN WAVE LINE ---
            ProfileStreakCard(
                streakDays = activeDays,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- NAVIGATION ACTIONS (STATISTICS, GOALS, EXPORT, DATA MANAGEMENT) ---
            ProfileNavigationMenuCard(
                isDark = isDark,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToGoals = onNavigateToGoals,
                onNavigateToExport = onNavigateToExport,
                onNavigateToDataManagement = onNavigateToDataManagement
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // --- EDIT PROFILE DIALOG ---
        if (showEditProfileDialog) {
            EditProfileDialog(
                currentName = currentName?.takeIf { it.isNotBlank() } ?: "Vivek Singh",
                hasCustomPhoto = !profileImageUri.isNullOrBlank(),
                isDark = isDark,
                onDismiss = { showEditProfileDialog = false },
                onSaveName = { newName ->
                    viewModel.saveUserName(newName)
                    Toast.makeText(context, "Profile name updated!", Toast.LENGTH_SHORT).show()
                    showEditProfileDialog = false
                },
                onPickPhoto = {
                    showEditProfileDialog = false
                    imagePickerLauncher.launch("image/*")
                },
                onRemovePhoto = {
                    viewModel.removeUserProfileImage(context)
                    Toast.makeText(context, "Default avatar restored", Toast.LENGTH_SHORT).show()
                    showEditProfileDialog = false
                }
            )
        }

    }
}

/**
 * 👤 TOP HEADER SECTION
 * Avatar with interactive pencil badge, user name with crown, tagline, and settings button.
 */
@Composable
private fun ProfileHeaderSection(
    userName: String,
    profileImageUri: String?,
    isDark: Boolean,
    onAvatarClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit
) {
    val iconButtonBg = if (isDark) Color(0xFF132228) else SleekSurface
    val iconButtonBorder = if (isDark) Color(0xFF1E353E) else SleekBorder
    val backIconTint = if (isDark) Color(0xFFCBD5E1) else SleekTextPrimary
    val settingsIconTint = if (isDark) Color(0xFFCBD5E1) else SleekTextPrimary
    val nameColor = if (isDark) Color.White else SleekTextPrimary
    val taglineColor = if (isDark) Color(0xFF94A3B8) else SleekTextSecondary
    val avatarBorder = if (isDark) Color(0xFF2DD4BF).copy(alpha = 0.6f) else SleekPrimary
    val avatarBg = if (isDark) Color(0xFF132228) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (Accessible 48dp touch target)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag("profile_back_button")
        ) {
            Icon(
                imageVector = AppIcons.ArrowBack,
                contentDescription = "Back",
                tint = backIconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Avatar with edit pen badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                shape = CircleShape,
                color = avatarBg,
                border = BorderStroke(2.dp, avatarBorder),
                modifier = Modifier.fillMaxSize()
            ) {
                com.example.ui.components.UserAvatar(
                    userName = userName,
                    imageUri = profileImageUri,
                    size = 72.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Edit Pencil circular badge button
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF1E293B) else SleekPrimary,
                border = BorderStroke(1.5.dp, if (isDark) Color(0xFF475569) else Color.White),
                shadowElevation = if (isDark) 2.dp else 3.dp,
                modifier = Modifier
                    .size(26.dp)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit Avatar",
                    tint = Color.White,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name, Crown, and Tagline
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onEditNameClick() }
            ) {
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Crown Icon Badge 👑
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.16f) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "👑",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = "Edit Name",
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Plan  •  Track  •  Grow",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = taglineColor,
                letterSpacing = 0.3.sp
            )
        }

        // Top Right Settings Gear Button
        Surface(
            shape = CircleShape,
            color = iconButtonBg,
            border = BorderStroke(1.dp, iconButtonBorder),
            shadowElevation = if (isDark) 0.dp else 1.5.dp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onSettingsClick() }
                .testTag("profile_settings_gear_button")
        ) {
            Icon(
                imageVector = AppIcons.Settings,
                contentDescription = "Settings",
                tint = settingsIconTint,
                modifier = Modifier.padding(11.dp)
            )
        }
    }
}

/**
 * 📊 4 STATS ROW CARD (LEGIT DATA)
 * Total Balance, This Month, Saved, Active Days
 */
@Composable
private fun ProfileStatsCard(
    currencySymbol: String,
    totalBalance: Double,
    thisMonthExpenses: Double,
    savedAmount: Double,
    activeDays: Int,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF0F1B21) else SleekSurface
    val cardBorder = if (isDark) Color(0xFF192C35) else SleekBorder
    val dividerColor = if (isDark) Color(0xFF1E2E36) else SleekBorder
    val valueColor = if (isDark) Color.White else SleekTextPrimary
    val labelColor = if (isDark) Color(0xFF94A3B8) else SleekTextSecondary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Stat 1: Total Balance
            StatColumnItem(
                icon = AppIcons.AccountBalanceWallet,
                iconTint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                value = "$currencySymbol${formatStatNumber(totalBalance)}",
                label = "Total Balance",
                valueColor = valueColor,
                labelColor = labelColor,
                modifier = Modifier.weight(1f)
            )

            // Divider
            VerticalStatDivider(dividerColor)

            // Stat 2: This Month
            StatColumnItem(
                icon = AppIcons.TrendingUp,
                iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                value = "$currencySymbol${formatStatNumber(thisMonthExpenses)}",
                label = "This Month",
                valueColor = valueColor,
                labelColor = labelColor,
                modifier = Modifier.weight(1f)
            )

            // Divider
            VerticalStatDivider(dividerColor)

            // Stat 3: Saved
            StatColumnItem(
                icon = AppIcons.Savings,
                iconTint = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                value = "$currencySymbol${formatStatNumber(savedAmount)}",
                label = "Saved",
                valueColor = valueColor,
                labelColor = labelColor,
                modifier = Modifier.weight(1f)
            )

            // Divider
            VerticalStatDivider(dividerColor)

            // Stat 4: Active Days
            StatColumnItem(
                icon = AppIcons.CalendarMonth,
                iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                value = "$activeDays",
                label = "Active Days",
                valueColor = valueColor,
                labelColor = labelColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatColumnItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = labelColor,
            maxLines = 1
        )
    }
}

@Composable
private fun VerticalStatDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(color)
    )
}

/**
 * 🏅 LIVE ACHIEVEMENTS
 * Shows badges computed from the user's real records (transactions, streak, goals, budget).
 * The previous version advertised five badges as "Coming Soon" and never unlocked anything —
 * these all resolve to actual state and link through to the full Badges & Milestones screen.
 */
@Composable
private fun LiveAchievementsSection(
    isDark: Boolean,
    totalTransactions: Int,
    streak: Int,
    goalsTotal: Int,
    goalsCompleted: Int,
    budgetKept: Boolean,
    hasBudget: Boolean,
    onSeeAllClick: () -> Unit
) {
    data class LiveBadge(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val color: Color,
        val label: String,
        val unlocked: Boolean
    )

    val badges = listOf(
        LiveBadge(
            icon = AppIcons.ReceiptLong,
            color = Color(0xFF0284C7),
            label = "First\nRecord",
            unlocked = totalTransactions >= 1
        ),
        LiveBadge(
            icon = AppIcons.LocalFireDepartment,
            color = Color(0xFFF97316),
            label = "Streak\nStarter",
            unlocked = streak >= 3
        ),
        LiveBadge(
            icon = AppIcons.VerifiedUser,
            color = Color(0xFF16A34A),
            label = "Budget\nKeeper",
            unlocked = hasBudget && budgetKept
        ),
        LiveBadge(
            icon = AppIcons.Savings,
            color = Color(0xFFD97706),
            label = "Goal\nSetter",
            unlocked = goalsTotal >= 1
        ),
        LiveBadge(
            icon = AppIcons.EmojiEvents,
            color = Color(0xFF9333EA),
            label = "Goal\nCrusher",
            unlocked = goalsCompleted >= 1
        )
    )
    val unlockedCount = badges.count { it.unlocked }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Achievements",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                Text(
                    text = "$unlockedCount of ${badges.size} unlocked",
                    fontSize = 11.sp,
                    color = SleekTextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) SleekPrimary.copy(alpha = 0.18f) else SleekPrimary.copy(alpha = 0.10f))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "See all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekPrimary
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            badges.forEach { badge ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(58.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (badge.unlocked) {
                                    badge.color.copy(alpha = if (isDark) 0.22f else 0.13f)
                                } else {
                                    SleekTextSecondary.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                1.2.dp,
                                if (badge.unlocked) badge.color.copy(alpha = 0.55f)
                                else SleekBorder,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (badge.unlocked) badge.icon else AppIcons.Lock,
                            contentDescription = null,
                            tint = if (badge.unlocked) badge.color else SleekTextSecondary.copy(alpha = 0.65f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = badge.label,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        textAlign = TextAlign.Center,
                        color = if (badge.unlocked) SleekTextPrimary else SleekTextSecondary,
                        fontWeight = if (badge.unlocked) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 0️⃣-B BADGES AND MILESTONES SCREEN (Backwards compatibility)
 */
// NOTE: BadgesAndMilestonesScreen moved to BadgesMilestonesScreen.kt and now renders a real,
// data-driven achievement system instead of the earlier "Coming soon" construction banner.
// The placeholder banner was removed with it because nothing else referenced it.

// The "Coming soon" crane banner was removed once Badges & Milestones shipped as a real
// screen; it had no remaining callers.

