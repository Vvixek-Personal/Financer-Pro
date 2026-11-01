package com.example.ui

import com.example.ui.theme.*
import android.util.Log
import com.example.data.*
import com.example.R
import java.util.*
import java.text.*
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.widget.Toast

private const val TAG = "DashboardTabScreen"

// ==========================================
// 1️⃣ DASHBOARD TAB
// ==========================================
@Composable
fun DashboardTab(
    expenses: List<Expense>,
    userName: String?,
    monthlyBudget: Double,
    categoryIcons: Map<String, String> = emptyMap(),
    onUpdateBudget: (Double) -> Unit,
    onUpdateName: (String) -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit = {},
    onNavigateToExpenses: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onProfileClick: () -> Unit,
    onEditExpenseClick: (Expense) -> Unit,
    onOpenSettingsSubScreen: (SettingsSubScreen) -> Unit = {},
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val billShowUpcomingDashboard by viewModel.billShowUpcomingDashboard.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val showStreakDialog by viewModel.showStreakDialog.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val isLocked = isAppLocked && (!appPin.isNullOrBlank() || biometricEnabled)
    val isPrivacyMode by viewModel.privacyModeEnabled.collectAsStateWithLifecycle()
    val isPrivacyRevealed by viewModel.privacyRevealOverride.collectAsStateWithLifecycle()
    val shouldHideBalance = isPrivacyMode && !isPrivacyRevealed

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    val todayExpense = remember(expenses) {
        expenses.filter { it.date in todayStart..todayEnd && it.type != "INCOME" && it.category != "Locked Savings" }
            .sumOf { it.amount }
    }

    var showQuickDepositDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var showQuickSplitDialog by remember { mutableStateOf(false) }
    var showQuickConvertDialog by remember { mutableStateOf(false) }
    var showAdjustBudgetDialog by remember { mutableStateOf(false) }

    if (showStreakDialog && !isLocked) {
        DailyStreakCelebrationDialog(
            streakCount = currentStreak,
            onDismiss = { viewModel.dismissStreakDialog() }
        )
    }

    val (thisMonthRange, lastMonthRange) = remember {
        val cal = Calendar.getInstance()
        val cMonth = cal.get(Calendar.MONTH)
        val cYear = cal.get(Calendar.YEAR)

        val calStart = Calendar.getInstance().apply {
            set(cYear, cMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(cYear, cMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val lmCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val lmMonth = lmCal.get(Calendar.MONTH)
        val lmYear = lmCal.get(Calendar.YEAR)

        val lmStart = Calendar.getInstance().apply {
            set(lmYear, lmMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lmEnd = Calendar.getInstance().apply {
            set(lmYear, lmMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        (calStart.timeInMillis..calEnd.timeInMillis) to (lmStart.timeInMillis..lmEnd.timeInMillis)
    }

    // All-time income, expense, and savings goals totals for Total Net Balance
    val totalAllTimeIncome = remember(expenses, selectedCurrencyCode) { expenses.realIncome(selectedCurrencyCode) }
    val totalAllTimeExpense = remember(expenses, selectedCurrencyCode) { expenses.realExpense(selectedCurrencyCode) }
    val totalSavingsGoalsMoney = remember(savingsGoals) { savingsGoals.totalSavings() }
    val overallTotalNetBalance = remember(expenses, savingsGoals, selectedCurrencyCode) { expenses.netWorth(savingsGoals, selectedCurrencyCode) }

    // Filter current month expenses
    val thisMonthExpenses = remember(expenses, thisMonthRange) {
        expenses.filter {
            it.date in thisMonthRange && it.type != "INCOME" && it.category != "Locked Savings"
        }
    }
    val thisMonthTotal = remember(thisMonthExpenses, selectedCurrencyCode) {
        thisMonthExpenses.realExpense(selectedCurrencyCode)
    }

    // Filter current month incomes
    val thisMonthIncomes = remember(expenses, thisMonthRange) {
        expenses.filter {
            it.date in thisMonthRange && it.type == "INCOME" && it.category != "Goal Withdrawal"
        }
    }
    val thisMonthIncomeTotal = remember(thisMonthIncomes, selectedCurrencyCode) {
        thisMonthIncomes.realIncome(selectedCurrencyCode)
    }

    // Last month expenses
    val lastMonthExpenses = remember(expenses, lastMonthRange) {
        expenses.filter {
            it.date in lastMonthRange && it.type != "INCOME" && it.category != "Locked Savings"
        }
    }
    val lastMonthTotal = remember(lastMonthExpenses, selectedCurrencyCode) {
        lastMonthExpenses.realExpense(selectedCurrencyCode)
    }

    // Difference Calculation
    val diffPct = remember(thisMonthTotal, lastMonthTotal) {
        if (lastMonthTotal > 0) {
            ((thisMonthTotal - lastMonthTotal) / lastMonthTotal) * 100
        } else {
            0.0
        }
    }

    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showBillsScreen by remember { mutableStateOf(false) }
    var showRemindersScreen by remember { mutableStateOf(false) }
    var showSavingGoalsScreen by remember { mutableStateOf(false) }
    var showAllShortcutsScreen by remember { mutableStateOf(false) }
    var showStartupReminder by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showStartupReminder = false
    }

    if (showAllShortcutsScreen) {
        val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
        val hasBillDueToday = viewModel.billsList.any { it.dueDate == todayStr }

        AllShortcutsTabScreen(
            onBack = { showAllShortcutsScreen = false },
            onAddExpense = {
                showAllShortcutsScreen = false
                onAddExpenseClick()
            },
            onAddIncome = {
                showAllShortcutsScreen = false
                onAddIncomeClick()
            },
            onQuickSplit = {
                showAllShortcutsScreen = false
                showQuickSplitDialog = true
            },
            onQuickConvert = {
                showAllShortcutsScreen = false
                showQuickConvertDialog = true
            },
            onViewGoals = {
                showAllShortcutsScreen = false
                showSavingGoalsScreen = true
            },
            onNavigateToExpenses = {
                showAllShortcutsScreen = false
                onNavigateToExpenses()
            },
            onNavigateToAnalytics = {
                showAllShortcutsScreen = false
                onNavigateToAnalytics()
            },
            onBillsClick = {
                showAllShortcutsScreen = false
                showBillsScreen = true
            },
            onReminderClick = {
                showAllShortcutsScreen = false
                showRemindersScreen = true
            },
            onAdjustBudget = {
                showAllShortcutsScreen = false
                showAdjustBudgetDialog = true
            },
            onOpenStreak = {
                showAllShortcutsScreen = false
                viewModel.triggerShowStreakDialog()
            },
            onOpenSettingsSubScreen = { subScreen ->
                showAllShortcutsScreen = false
                onOpenSettingsSubScreen(subScreen)
            },
            hasBillDueToday = hasBillDueToday,
            currentStreak = currentStreak,
            selectedLanguage = selectedLanguage
        )
        return
    }

    // Startup reminder alert dialog removed - shown on-screen in dashboard feed

    if (showSavingGoalsScreen) {
        SavingGoalsFullScreen(
            viewModel = viewModel,
            onBack = { showSavingGoalsScreen = false }
        )
        return
    }

    if (showBillsScreen) {
        BillsFullScreen(
            viewModel = viewModel,
            onBack = { showBillsScreen = false },
            onPayBill = { title: String, amt: Double ->
                viewModel.addExpense(
                    amount = amt,
                    category = "Bills",
                    date = System.currentTimeMillis(),
                    note = "Paid $title",
                    type = "EXPENSE",
                    currencyCode = selectedCurrencyCode
                )
            }
        )
        return
    }

    if (showRemindersScreen) {
        RemindersFullScreen(
            viewModel = viewModel,
            onBack = { showRemindersScreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Header & User Profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                val initials = if (!userName.isNullOrBlank()) {
                    userName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(2)
                } else "U"

                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                Column {
                    val timeOfDay = remember {
                        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                            in 0..11 -> "Good Morning,"
                            in 12..16 -> "Good Afternoon,"
                            else -> "Good Evening,"
                        }
                    }
                    Text(
                        text = timeOfDay,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Text(
                        text = userName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Streak Badge beside profile & Quick Add Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 🧠 Financial Health Score Badge
                val healthScore = remember(expenses, savingsGoals, accounts, monthlyBudget) {
                    viewModel.getFinancialHealthScore(0.0, 0.0)
                }
                Surface(
                    onClick = { onOpenSettingsSubScreen(SettingsSubScreen.FinancialHealthStudio) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(healthScore.ratingColorHex).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(healthScore.ratingColorHex).copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("dashboard_health_score_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "🧠", fontSize = 12.sp)
                        Text(
                            text = "${healthScore.overallScore}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(healthScore.ratingColorHex)
                        )
                    }
                }

                // 🔥 Streak Flame Logo (replaces text pill as requested)
                StreakFlameLogo(
                    streakCount = currentStreak,
                    onClick = { viewModel.triggerShowStreakDialog() },
                    modifier = Modifier.testTag("dashboard_streak_flame_badge")
                )

                IconButton(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .size(48.dp)
                        .tactileDepth(elevation = 5.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SleekPrimary,
                                    mixPrimaryWithColor(SleekPrimary, Color.Black, 0.18f)
                                )
                            )
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .testTag("dashboard_add_expense_fab")
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = "Add Expense",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Balance Card (Minimal, Calm, Flat Fintech Style)
        val animatedBalance = overallTotalNetBalance.toFloat()
        val animatedIncome = totalAllTimeIncome.toFloat()
        val animatedExpense = totalAllTimeExpense.toFloat()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettingsSubScreen(SettingsSubScreen.NetWorthAssets) }
                .testTag("dashboard_hero_balance_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.AccountBalance,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Net balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                        if (isPrivacyMode) {
                            IconButton(
                                onClick = { viewModel.togglePrivacyReveal() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (shouldHideBalance) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = "Toggle privacy reveal",
                                    tint = SleekTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (overallTotalNetBalance >= 0) IncomeGreenBg else ExpenseRedBg
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (overallTotalNetBalance >= 0) "Surplus" else "Deficit",
                            color = if (overallTotalNetBalance >= 0) IncomeGreen else ExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (shouldHideBalance) "$currencySymbol ••••••" else String.format(Locale.getDefault(), "%s%s%,.2f", if (animatedBalance >= 0) "" else "-", currencySymbol, Math.abs(animatedBalance)),
                    style = MaterialTheme.typography.displayMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp
                )

                if (totalSavingsGoalsMoney > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (shouldHideBalance) "Includes $currencySymbol•••• in savings goals" else String.format(Locale.getDefault(), "Includes %s%,.0f in savings goals", currencySymbol, totalSavingsGoalsMoney),
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown row: Income vs Expense capsules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IncomeGreenBg)
                            .border(BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.TrendingUp,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (shouldHideBalance) "$currencySymbol••••" else String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, animatedIncome),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Expense Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ExpenseRedBg)
                            .border(BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.TrendingDown,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (shouldHideBalance) "$currencySymbol••••" else String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, animatedExpense),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDecrease = thisMonthTotal <= lastMonthTotal
                    val pillBg = if (isDecrease) IncomeGreenBg else ExpenseRedBg
                    val pillTextColor = if (isDecrease) IncomeGreen else ExpenseRed
                    val prefixSign = if (isDecrease) "-" else "+"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pillBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.1f%%", prefixSign, Math.abs(diffPct)),
                            color = pillTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "vs last month (%s%,.0f)", currencySymbol, lastMonthTotal),
                        color = SleekTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 💡 1. Daily Smart Financial Insight
        DailyFinancialInsightWidget(
            thisMonthIncome = thisMonthIncomeTotal,
            thisMonthExpense = thisMonthTotal,
            monthlyBudget = monthlyBudget,
            streakCount = currentStreak,
            currencySymbol = currencySymbol
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ⚡ 2. Quick Shortcuts Feed (Non-repeating 1-tap actions + "More" to open All Shortcuts Launcher)
        QuickServicesCategorySection(
            viewModel = viewModel,
            selectedLanguage = selectedLanguage,
            onAddExpense = onAddExpenseClick,
            onAddIncome = onAddIncomeClick,
            onQuickSplit = { showQuickSplitDialog = true },
            onQuickConvert = { showQuickConvertDialog = true },
            onMoreClick = { showAllShortcutsScreen = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🧾 4. Upcoming Bills Widget with inline "Pay Now"
        if (billShowUpcomingDashboard) {
            UpcomingBillsDashboardWidget(
                bills = viewModel.billsList,
                currencySymbol = currencySymbol,
                onPayBill = { bill ->
                    val totalBal = accounts.sumOf { it.balance }
                    if (bill.amount > totalBal && totalBal > 0) {
                        Toast.makeText(context, "Cannot pay: amount exceeds total balance!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addExpense(
                            amount = bill.amount,
                            category = "Bills",
                            date = System.currentTimeMillis(),
                            note = "Paid ${bill.title}",
                            type = "EXPENSE",
                            currencyCode = selectedCurrencyCode
                        )
                        viewModel.billsList.remove(bill)
                        Toast.makeText(context, "Paid ${bill.title} successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                onViewAllBills = { showBillsScreen = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. Smart Budget Health Gauge Ring
        if (monthlyBudget > 0) {
            SmartBudgetHealthGaugeWidget(
                monthlyBudget = monthlyBudget,
                currentMonthExpense = thisMonthTotal,
                currencySymbol = currencySymbol,
                onUpdateBudget = { showAdjustBudgetDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            MonthlyBudgetSnapshotCard(
                monthlyBudget = monthlyBudget,
                currentMonthExpense = thisMonthTotal,
                currencySymbol = currencySymbol,
                onUpdateBudget = onUpdateBudget
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 🌟 Dynamic Financial Health Score & Diagnostics Widget
        val overdueBillsCount = remember(viewModel.billsList.toList()) {
            val nowTime = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            viewModel.billsList.count { bill ->
                try {
                    val parsed = sdf.parse(bill.dueDate)
                    parsed != null && parsed.time < nowTime
                } catch (e: Exception) {
                    false
                }
            }
        }
        FinancialHealthScoreWidget(
            incomeThisMonth = thisMonthIncomeTotal,
            expenseThisMonth = thisMonthTotal,
            monthlyBudget = monthlyBudget,
            overdueBillsCount = overdueBillsCount,
            currencySymbol = currencySymbol,
            onExploreBudget = { showAdjustBudgetDialog = true }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🎯 7. Interactive Savings Goal Mini-Carousel
        if (savingsGoals.isNotEmpty()) {
            SavingsGoalsMiniCarouselWidget(
                goals = savingsGoals,
                currencySymbol = currencySymbol,
                onQuickDeposit = { goal, amount ->
                    viewModel.quickDepositToGoal(goal, amount)
                    Toast.makeText(context, "Deposited \$%.0f to \${goal.name}!".format(amount), Toast.LENGTH_SHORT).show()
                },
                onViewAllGoals = { showSavingGoalsScreen = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 📊 8. Monthly Top Spending Categories Widget
        if (thisMonthExpenses.isNotEmpty()) {
            TopSpendingCategoriesWidget(
                expensesThisMonth = thisMonthExpenses,
                categoryIcons = categoryIcons,
                currencySymbol = currencySymbol,
                onNavigateToAnalytics = onNavigateToAnalytics
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Activity List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent activity",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "See all",
                color = SleekPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onNavigateToExpenses() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activities recorded yet. Tap + to add.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextSecondary
                )
            }
        } else {
            // Show top 3 recent items
            expenses.take(3).forEach { expense ->
                RecentExpenseRow(
                    expense = expense,
                    categoryIcons = categoryIcons,
                    currencySymbol = currencySymbol,
                    onClick = { onEditExpenseClick(expense) }
                )
            }
        }
        Spacer(modifier = Modifier.height(90.dp))
    }

    val targetGoal = showQuickDepositDialog
    if (targetGoal != null) {
        QuickGoalDepositDialog(
            goal = targetGoal,
            currencySymbol = currencySymbol,
            onDismiss = { showQuickDepositDialog = null },
            onConfirmDeposit = { amt ->
                viewModel.quickDepositToGoal(targetGoal, amt)
                showQuickDepositDialog = null
                Toast.makeText(context, "Deposited to ${targetGoal.name} successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showQuickSplitDialog) {
        QuickSplitBillDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showQuickSplitDialog = false },
            onRecordExpense = { amt, note ->
                viewModel.addExpense(
                    amount = amt,
                    category = "Food & Dining",
                    date = System.currentTimeMillis(),
                    note = note,
                    type = "EXPENSE",
                    currencyCode = selectedCurrencyCode
                )
                Toast.makeText(context, "Recorded split expense of $currencySymbol${String.format(Locale.getDefault(), "%,.2f", amt)}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showQuickConvertDialog) {
        QuickCurrencyConvertDialog(
            currencyCode = selectedCurrencyCode,
            onDismiss = { showQuickConvertDialog = false },
            onOpenFullSettings = {
                showQuickConvertDialog = false
                onOpenSettingsSubScreen(SettingsSubScreen.Currency)
            }
        )
    }

    if (showAdjustBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toInt().toString() else "") }
        Dialog(onDismissRequest = { showAdjustBudgetDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Set Monthly Budget Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Your daily spending allowance will automatically adapt based on the days remaining in the month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Budget Target ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdjustBudgetDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }
                        Button(
                            onClick = {
                                val amt = budgetInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    onUpdateBudget(amt)
                                    showAdjustBudgetDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showChangeNameDialog) {
        Dialog(onDismissRequest = { showChangeNameDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                var newName by remember { mutableStateOf(userName ?: "") }
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Update Your Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Your Name", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedLabelColor = SleekPrimary,
                            unfocusedLabelColor = SleekTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangeNameDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.trim().isNotEmpty()) {
                                    onUpdateName(newName.trim())
                                    showChangeNameDialog = false
                                }
                            },
                            enabled = newName.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(110.dp))
    }
}

// ==========================================
// 🎨 QUICK SHORTCUTS & CATEGORY FEED
// ==========================================
@Composable
fun QuickServicesCategorySection(
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    selectedLanguage: String = "English",
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onQuickSplit: () -> Unit,
    onQuickConvert: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_services_category_feed")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Bolt,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = LanguageManager.tr("Shortcuts", selectedLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onMoreClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = LanguageManager.tr("All Tools", selectedLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryFeedTile(
                    icon = AppIcons.TrendingDown,
                    label = LanguageManager.tr("Expense", selectedLanguage),
                    tileColor = ExpenseRed,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.TrendingUp,
                    label = LanguageManager.tr("Income", selectedLanguage),
                    tileColor = IncomeGreen,
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.CallSplit,
                    label = LanguageManager.tr("Split", selectedLanguage),
                    tileColor = Color(0xFF6366F1),
                    onClick = onQuickSplit,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.CurrencyExchange,
                    label = LanguageManager.tr("Convert", selectedLanguage),
                    tileColor = Color(0xFF0EA5E9),
                    onClick = onQuickConvert,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.GridView,
                    label = LanguageManager.tr("More", selectedLanguage),
                    tileColor = SleekPrimary,
                    onClick = onMoreClick,
                    modifier = Modifier.weight(1f),
                    hasBadge = false
                )
            }
        }
    }
}

@Composable
fun CategoryFeedTile(
    icon: ImageVector,
    label: String,
    tileColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tile_scale"
    )

    val isDark = isDarkModeActive
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                tileColor.copy(alpha = 0.38f),
                tileColor.copy(alpha = 0.16f),
                Color(0xFF0F172A)
            ),
            start = Offset(0f, 0f),
            end = Offset(120f, 120f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                tileColor.copy(alpha = 0.18f),
                tileColor.copy(alpha = 0.30f)
            ),
            start = Offset(0f, 0f),
            end = Offset(120f, 120f)
        )
    }
    val rimBorder = if (isDark) tileColor.copy(alpha = 0.65f) else tileColor.copy(alpha = 0.40f)
    val iconTint = if (isDark) tileColor.copy(alpha = 0.99f) else tileColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = if (isDark) 4.dp else 2.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(bgBrush)
                .border(BorderStroke(1.5.dp, rimBorder), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = SleekTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MonthlyBudgetSnapshotCard(
    monthlyBudget: Double,
    currentMonthExpense: Double,
    currencySymbol: String = "₹",
    onUpdateBudget: (Double) -> Unit
) {
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_monthly_budget_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SleekPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.PieChart,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (monthlyBudget > 0) "Current month burn rate" else "No target configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showEditBudgetDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (monthlyBudget > 0) "Adjust" else "Set target",
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            if (monthlyBudget > 0) {
                val spentPct = ((currentMonthExpense / monthlyBudget) * 100).coerceAtLeast(0.0)
                val remaining = monthlyBudget - currentMonthExpense
                val isExceeded = remaining < 0
                val progressFraction = (currentMonthExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)

                val statusColor = when {
                    isExceeded -> ExpenseRed
                    spentPct >= 80 -> WarningOrange
                    else -> IncomeGreen
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Spent this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, currentMonthExpense),
                            style = MaterialTheme.typography.titleLarge,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExceeded) "Exceeded by ${String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, Math.abs(remaining))}"
                                else String.format(Locale.getDefault(), "%.0f%% used", spentPct),
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isExceeded) String.format(Locale.getDefault(), "%s%,.0f remaining", currencySymbol, remaining) else "Limit: ${String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, monthlyBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Clean flat progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekBorder.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekBorder.copy(alpha = 0.25f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set a monthly target to visualize burn rate and track spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showEditBudgetDialog) {
        var inputBudget by remember { mutableStateOf(if (monthlyBudget > 0) String.format(Locale.getDefault(), "%.0f", monthlyBudget) else "") }
        Dialog(onDismissRequest = { showEditBudgetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputBudget,
                        onValueChange = { inputBudget = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Budget Cap ($currencySymbol)", color = SleekTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditBudgetDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }
                        Button(
                            onClick = {
                                val value = inputBudget.toDoubleOrNull() ?: 0.0
                                onUpdateBudget(value)
                                showEditBudgetDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentExpenseRow(
    expense: Expense,
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
    val isIncome = expense.type == "INCOME"
    val catColor = if (isIncome) {
        when (expense.category) {
            "Salary" -> Color(0xFF10B981)
            "Freelance" -> Color(0xFF0D9488)
            "Investments" -> Color(0xFF3B82F6)
            "Gifts" -> Color(0xFFEC4899)
            else -> Color(0xFF10B981)
        }
    } else {
        categoryColors[expense.category] ?: SleekPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, catColor.copy(alpha = 0.25f)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category, categoryIcons),
                    contentDescription = expense.category,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note ?: (if (isIncome) "Income" else "Expense"),
                    style = MaterialTheme.typography.titleSmall,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.category} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }

            Text(
                text = String.format("%s%s%,.2f", if (isIncome) "+" else "-", currencySymbol, expense.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncome) Color(0xFF10B981) else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// ExpensesTab moved to ExpensesTabScreen.kt
// Image1TransactionRow and DateRangePickerDialog moved to ExpensesTabScreen.kt
// AnalyticsStatCard moved to AnalyticsTabScreen.kt
// AnalyticsTab moved to AnalyticsTabScreen.kt

// AnalyticsPeriodTransactionsCard moved to AnalyticsTabScreen.kt

// NetWorthOverTimeChartCard and GraphPoint moved to AnalyticsTabScreen.kt

// ==========================================
// 4️⃣.5️⃣ CATEGORY SELECTOR COMPONENTS
// ==========================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySelectorGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categories: List<String>,
    categoryIcons: Map<String, String> = emptyMap(),
    onAddCustomCategoryClick: () -> Unit,
    onDeleteCustomCategory: ((String) -> Unit)? = null,
    onEditCustomCategory: ((String, String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val emoji = getCategoryEmoji(cat, categoryIcons)
            
            val isDefault = listOf("Food", "Travel", "Rent", "Utilities", "Entertainment", "Shopping", "Persons", "Others", "Salary", "Freelance", "Investments", "Gifts").contains(cat)
            
            var showEditDeleteDialog by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCategorySelected(cat)
                        },
                        onLongClick = {
                            if (!isDefault && (onDeleteCustomCategory != null || onEditCustomCategory != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEditDeleteDialog = true
                            }
                        }
                    ),
                shape = CircleShape,
                color = if (isSelected) SleekPrimary.copy(alpha = 0.2f) else SleekNeutralLight,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) SleekPrimary else SleekBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) SleekPrimary else SleekTextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    
                    if (!isDefault && isSelected) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "Edit Category",
                            tint = SleekTextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showEditDeleteDialog = true }
                        )
                    }
                }
            }
            
            if (showEditDeleteDialog) {
                ManageCategoryDialog(
                    categoryName = cat,
                    onDismiss = { showEditDeleteDialog = false },
                    onDelete = {
                        if (onDeleteCustomCategory != null) {
                            onDeleteCustomCategory(cat)
                        }
                        showEditDeleteDialog = false
                    },
                    onRename = { newName ->
                        if (onEditCustomCategory != null) {
                            onEditCustomCategory(cat, newName)
                        }
                        showEditDeleteDialog = false
                    }
                )
            }
        }
        
        if (onAddCustomCategoryClick != null) {
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAddCustomCategoryClick() },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = "Add Custom Category",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Add Custom",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ManageCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(categoryName) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showConfirmDelete) {
                    Text(
                        text = "Delete Category?",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Are you sure you want to delete \"$categoryName\"? Associated expenses will revert to \"Others\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmDelete = false },
                            border = BorderStroke(1.dp, SleekBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                } else {
                    Text(
                        text = "Manage Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Category Name", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showConfirmDelete = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (newName.trim().isNotEmpty() && newName.trim() != categoryName) {
                                    onRename(newName.trim())
                                }
                                onDismiss()
                            },
                            enabled = newName.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rename", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SleekTextSecondary)
                    }
                }
            }
        }
    }
}

// Note: Add Expense Dialog is modularized in AddTransactionFlowComponents.kt

