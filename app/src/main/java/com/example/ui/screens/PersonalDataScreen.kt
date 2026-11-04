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
    var showComingSoonDialog by remember { mutableStateOf(false) }

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

            // --- LATEST ACHIEVEMENT (COMING SOON CARD) ---
            ComingSoonTrophyCard(
                isDark = isDark,
                onClick = { showComingSoonDialog = true }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- MY ACHIEVEMENTS (COMING SOON PREVIEWS) ---
            MyAchievementsComingSoonSection(
                isDark = isDark,
                onSeeAllClick = { showComingSoonDialog = true }
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

        // --- COMING SOON MODAL ---
        if (showComingSoonDialog) {
            val dialogBg = if (isDark) Color(0xFF131D24) else SleekSurface
            val titleColor = if (isDark) Color.White else SleekTextPrimary
            val textColor = if (isDark) Color(0xFFCBD5E1) else SleekTextSecondary
            val confirmColor = if (isDark) Color(0xFF2DD4BF) else SleekPrimary

            AlertDialog(
                onDismissRequest = { showComingSoonDialog = false },
                containerColor = dialogBg,
                titleContentColor = titleColor,
                textContentColor = textColor,
                icon = {
                    Icon(
                        imageVector = AppIcons.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("Achievements & Milestones", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "We are crafting an inspiring gamified milestone system! Soon you'll unlock tier badges, track multi-month budget streaks, and celebrate financial victories."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showComingSoonDialog = false }) {
                        Text("Awesome", color = confirmColor, fontWeight = FontWeight.Bold)
                    }
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
                if (!profileImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_profile_avatar),
                        contentDescription = "Profile Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
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
 * 🏆 LATEST ACHIEVEMENT CARD (COMING SOON)
 * Replaces the fake achievement card with a polished, high-fidelity Coming Soon banner.
 * Warm golden gradient and glowing trophy halo adapt to both light and dark backgrounds.
 */
@Composable
private fun ComingSoonTrophyCard(
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardBorder = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFFCD34D)
    val cardGradient = if (isDark) {
        listOf(
            Color(0xFF281E0F),
            Color(0xFF1E1710),
            Color(0xFF131A1F)
        )
    } else {
        listOf(
            Color(0xFFFFFBEB),
            Color(0xFFFEF3C7),
            Color(0xFFFFF7ED)
        )
    }
    val titleColor = if (isDark) Color.White else Color(0xFF78350F)
    val subheadColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
    val descColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF92400E).copy(alpha = 0.85f)
    val chipBg = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.18f)
    val chipText = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
    val chevronBg = if (isDark) Color(0xFF281E0F) else Color(0xFFFEF3C7)
    val chevronTint = if (isDark) Color(0xFFF59E0B) else Color(0xFFB45309)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = cardGradient))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trophy Emblem in Golden Halo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = if (isDark) 0.3f else 0.22f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.EmojiEvents,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Texts
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Latest Achievement",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subheadColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = chipBg
                        ) {
                            Text(
                                text = "COMING SOON",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = chipText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Budget Master & Trophies",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Unlock trophies and financial milestones in upcoming updates!",
                        fontSize = 11.sp,
                        color = descColor,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Chevron icon
                Surface(
                    shape = CircleShape,
                    color = chevronBg,
                    border = BorderStroke(1.dp, if (isDark) Color.Transparent else Color(0xFFFDE68A)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = "Details",
                        tint = chevronTint,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 🏅 MY ACHIEVEMENTS (COMING SOON ROW)
 * 5 future badge concepts with "Coming Soon" styling.
 * High contrast on both dark obsidian and bright light surfaces.
 */
@Composable
private fun MyAchievementsComingSoonSection(
    isDark: Boolean,
    onSeeAllClick: () -> Unit
) {
    val titleColor = if (isDark) Color.White else SleekTextPrimary
    val seeAllColor = if (isDark) Color(0xFF94A3B8) else SleekPrimary

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
            Text(
                text = "My Achievements",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = seeAllColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = seeAllColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row of 5 upcoming badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AchievementBadgeItem(
                icon = AppIcons.CalendarToday,
                color = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                title = "Budget\nMaster",
                isDark = isDark
            )
            AchievementBadgeItem(
                icon = AppIcons.TrendingUp,
                color = if (isDark) Color(0xFFA855F7) else Color(0xFF9333EA),
                title = "Expense\nAnalyzer",
                isDark = isDark
            )
            AchievementBadgeItem(
                icon = AppIcons.Star,
                color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                title = "Savings\nStar",
                isDark = isDark
            )
            AchievementBadgeItem(
                icon = AppIcons.LocalFireDepartment,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                title = "Streak\nWarrior",
                isDark = isDark
            )
            AchievementBadgeItem(
                icon = AppIcons.Lock,
                color = if (isDark) Color(0xFF64748B) else Color(0xFF475569),
                title = "More\nSoon...",
                isDark = isDark
            )
        }
    }
}

@Composable
private fun AchievementBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    isDark: Boolean
) {
    val badgeBg = if (isDark) Color(0xFF111E24) else SleekSurface
    val badgeBorder = if (isDark) color.copy(alpha = 0.5f) else color.copy(alpha = 0.35f)
    val badgeTitleColor = if (isDark) Color(0xFF94A3B8) else SleekTextPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(58.dp)
    ) {
        // Hexagon/Rounded Badge Box
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(badgeBg)
                .border(1.2.dp, badgeBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Subtle color tint inside badge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = if (isDark) 0.08f else 0.12f))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = badgeTitleColor,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

/**
 * 🔥 24 DAY STREAK CARD WITH SMOOTH GOLDEN WAVE LINE
 * Live streak count and Canvas bezier wave curve with glowing yellow end dot.
 * Fully visible in both light mode and dark mode.
 */
@Composable
private fun ProfileStreakCard(
    streakDays: Int,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF141715) else SleekSurface
    val cardBorder = if (isDark) Color(0xFF282516) else Color(0xFFFED7AA)
    val flameBg = if (isDark) Color(0xFF381F0C) else Color(0xFFFFEDD5)
    val flameBorder = if (isDark) Color(0xFFF97316).copy(alpha = 0.45f) else Color(0xFFF97316).copy(alpha = 0.6f)
    val flameTint = if (isDark) Color(0xFFFF7A00) else Color(0xFFEA580C)
    val titleColor = if (isDark) Color.White else SleekTextPrimary
    val subtitleColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Flame Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(flameBg)
                    .border(1.dp, flameBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.LocalFireDepartment,
                    contentDescription = "Streak Flame",
                    tint = flameTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Streak Texts
            Column {
                Text(
                    text = "$streakDays Day Streak",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Keep going! 🔥",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Smooth Golden Wave Line Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) {
                val w = size.width
                val h = size.height

                val wavePath = Path().apply {
                    moveTo(0f, h * 0.85f)
                    cubicTo(w * 0.22f, h * 0.95f, w * 0.28f, h * 0.4f, w * 0.48f, h * 0.55f)
                    cubicTo(w * 0.65f, h * 0.7f, w * 0.72f, h * 0.25f, w * 0.85f, h * 0.35f)
                    cubicTo(w * 0.92f, h * 0.4f, w * 0.96f, h * 0.12f, w - 4f, h * 0.15f)
                }

                val strokeGradient = if (isDark) {
                    listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.2f),
                        Color(0xFFF59E0B).copy(alpha = 0.85f),
                        Color(0xFFFDE68A)
                    )
                } else {
                    listOf(
                        Color(0xFFD97706).copy(alpha = 0.25f),
                        Color(0xFFD97706),
                        Color(0xFFF59E0B)
                    )
                }

                // Draw golden stroke
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(colors = strokeGradient),
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Radiant glowing yellow end dot
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDE68A),
                            Color(0xFFF59E0B).copy(alpha = if (isDark) 0.4f else 0.3f),
                            Color.Transparent
                        ),
                        radius = 8.dp.toPx(),
                        center = Offset(w - 4f, h * 0.15f)
                    ),
                    radius = 8.dp.toPx(),
                    center = Offset(w - 4f, h * 0.15f)
                )

                drawCircle(
                    color = if (isDark) Color(0xFFFFFBEB) else Color(0xFFD97706),
                    radius = 3.2.dp.toPx(),
                    center = Offset(w - 4f, h * 0.15f)
                )
            }
        }
    }
}

/**
 * 🧭 NAVIGATION MENU CARD
 * Overall Statistics, My Goals, Export Data, Data Management
 * Perfectly balanced contrast in both light mode and dark mode.
 */
@Composable
private fun ProfileNavigationMenuCard(
    isDark: Boolean,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToDataManagement: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF0F1A20) else SleekSurface
    val cardBorder = if (isDark) Color(0xFF1A2A33) else SleekBorder
    val dividerColor = if (isDark) Color(0xFF182730) else SleekBorder

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Option 1: Overall Statistics
            ProfileNavigationRow(
                icon = AppIcons.Analytics,
                iconTint = if (isDark) Color(0xFFA855F7) else Color(0xFF9333EA),
                iconContainerColor = if (isDark) Color(0xFF281C38) else Color(0xFFF3E8FF),
                title = "Overall Statistics",
                subtitle = "Your complete financial overview",
                isDark = isDark,
                onClick = onNavigateToAnalytics,
                testTag = "profile_nav_statistics"
            )

            HorizontalDivider(
                color = dividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Option 2: My Goals
            ProfileNavigationRow(
                icon = AppIcons.EmojiEvents,
                iconTint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                iconContainerColor = if (isDark) Color(0xFF122C24) else Color(0xFFDCFCE7),
                title = "My Goals",
                subtitle = "Track your savings goals",
                isDark = isDark,
                onClick = onNavigateToGoals,
                testTag = "profile_nav_goals"
            )

            HorizontalDivider(
                color = dividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Option 3: Export Data
            ProfileNavigationRow(
                icon = AppIcons.FileDownload,
                iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                iconContainerColor = if (isDark) Color(0xFF142738) else Color(0xFFE0F2FE),
                title = "Export Data",
                subtitle = "Download your data (CSV/PDF)",
                isDark = isDark,
                onClick = onNavigateToExport,
                testTag = "profile_nav_export"
            )

            HorizontalDivider(
                color = dividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Option 4: Data Management
            ProfileNavigationRow(
                icon = AppIcons.Security,
                iconTint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                iconContainerColor = if (isDark) Color(0xFF341926) else Color(0xFFFFE4E6),
                title = "Data Management",
                subtitle = "Backup, Restore & Renew Data",
                isDark = isDark,
                onClick = onNavigateToDataManagement,
                testTag = "profile_nav_data_management"
            )
        }
    }
}

@Composable
private fun ProfileNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val titleColor = if (isDark) Color.White else SleekTextPrimary
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else SleekTextSecondary
    val chevronTint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleColor
            )
        }

        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = chevronTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * ✏️ EDIT PROFILE DIALOG
 * User can edit their name and pick a new profile avatar from gallery or revert to default.
 * Theme-aware dialog styling for both light and dark modes.
 */
@Composable
private fun EditProfileDialog(
    currentName: String,
    hasCustomPhoto: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }

    val dialogBg = if (isDark) Color(0xFF131D24) else SleekSurface
    val titleColor = if (isDark) Color.White else SleekTextPrimary
    val textColor = if (isDark) Color(0xFFCBD5E1) else SleekTextSecondary
    val fieldTextColor = if (isDark) Color.White else SleekTextPrimary
    val fieldBorder = if (isDark) Color(0xFF334155) else SleekBorder
    val primaryColor = if (isDark) Color(0xFF2DD4BF) else SleekPrimary
    val onPrimaryColor = if (isDark) Color(0xFF0F172A) else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = titleColor,
        textContentColor = textColor,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Edit Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Personalize your display name and profile picture.",
                    fontSize = 13.sp,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Display Name", color = textColor) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = fieldTextColor,
                        unfocusedTextColor = fieldTextColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = fieldBorder,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val pickColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    OutlinedButton(
                        onClick = onPickPhoto,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = pickColor
                        ),
                        border = BorderStroke(1.dp, pickColor.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = AppIcons.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (hasCustomPhoto) {
                        val resetColor = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                        OutlinedButton(
                            onClick = onRemovePhoto,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = resetColor
                            ),
                            border = BorderStroke(1.dp, resetColor.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Avatar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nameText.trim().ifBlank { "Vivek Singh" }
                    onSaveName(trimmed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}

/**
 * Number formatter helper
 */
private fun formatStatNumber(amount: Double): String {
    return if (amount >= 1_000_000) {
        String.format(Locale.US, "%.1fM", amount / 1_000_000)
    } else {
        NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())
    }
}

/**
 * 0️⃣-B BADGES AND MILESTONES SCREEN (Backwards compatibility)
 */
@Composable
fun BadgesAndMilestonesScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Badges and Milestone", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComingSoonConstructionBanner()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * 🏗️ COMING SOON CONSTRUCTION BANNER (Backwards compatibility)
 */
@Composable
fun ComingSoonConstructionBanner(
    modifier: Modifier = Modifier
) {
    val swingAngle = rememberLoopFloat(-12f, 12f, 2200, "swing", reverse = true, rest = 0f)
    val craneHookOffsetY = rememberLoopFloat(0f, 14f, 1800, "craneY", reverse = true, rest = 0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.5.dp, SleekBorder),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val buildingColor = Color(0xFF334155)
                    val windowColor = Color(0xFF94A3B8).copy(alpha = 0.4f)

                    drawRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.05f, h * 0.45f),
                        size = Size(w * 0.18f, h * 0.55f)
                    )
                    for (r in 0..3) {
                        for (c in 0..1) {
                            drawRect(
                                color = windowColor,
                                topLeft = Offset(w * 0.08f + c * 20f, h * 0.5f + r * 22f),
                                size = Size(12f, 14f)
                            )
                        }
                    }

                    drawRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.25f, h * 0.35f),
                        size = Size(w * 0.22f, h * 0.65f)
                    )
                    for (r in 0..4) {
                        for (c in 0..1) {
                            drawRect(
                                color = windowColor,
                                topLeft = Offset(w * 0.28f + c * 24f, h * 0.4f + r * 22f),
                                size = Size(14f, 14f)
                            )
                        }
                    }

                    drawRect(
                        color = buildingColor,
                        topLeft = Offset(w * 0.72f, h * 0.5f),
                        size = Size(w * 0.23f, h * 0.5f)
                    )
                    val scaffoldColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
                    drawLine(
                        color = scaffoldColor,
                        start = Offset(w * 0.72f, h * 0.4f),
                        end = Offset(w * 0.72f, h * 0.95f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = scaffoldColor,
                        start = Offset(w * 0.95f, h * 0.4f),
                        end = Offset(w * 0.95f, h * 0.95f),
                        strokeWidth = 3f
                    )

                    val craneColor = Color(0xFFF59E0B)
                    val craneX = w * 0.55f
                    val craneBaseY = h * 0.95f
                    val craneTopY = h * 0.15f

                    drawLine(
                        color = craneColor,
                        start = Offset(craneX - 6f, craneBaseY),
                        end = Offset(craneX - 6f, craneTopY),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = craneColor,
                        start = Offset(craneX + 6f, craneBaseY),
                        end = Offset(craneX + 6f, craneTopY),
                        strokeWidth = 4f
                    )

                    val jibLeft = craneX - w * 0.2f
                    val jibRight = craneX + w * 0.35f
                    drawLine(
                        color = craneColor,
                        start = Offset(jibLeft, craneTopY),
                        end = Offset(jibRight, craneTopY),
                        strokeWidth = 5f
                    )

                    val cableHookX = craneX + w * 0.2f
                    val cableStartY = craneTopY
                    val cableLength = h * 0.35f + craneHookOffsetY

                    val swingRad = Math.toRadians(swingAngle.toDouble())
                    val endHookX = (cableHookX + Math.sin(swingRad) * cableLength).toFloat()
                    val endHookY = (cableStartY + Math.cos(swingRad) * cableLength).toFloat()

                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(cableHookX, cableStartY),
                        end = Offset(endHookX, endHookY),
                        strokeWidth = 2f
                    )

                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 12f,
                        center = Offset(endHookX, endHookY + 12f)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Construction,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coming soon",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Badges and Milestone",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We are currently constructing your Badges and Milestones hub! Soon you will earn unlockable badges for daily savings streaks, financial milestones, and smart budgeting achievements.",
                fontSize = 13.sp,
                color = SleekTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
