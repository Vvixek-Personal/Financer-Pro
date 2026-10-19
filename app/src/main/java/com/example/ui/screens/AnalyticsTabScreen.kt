package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Account
import com.example.data.Expense
import com.example.data.Money
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun AnalyticsTab(
    viewModel: FinanceViewModel,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onExpenseClick: (Expense) -> Unit = {}
) {
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val statsCurrencyCode by viewModel.statsCurrencyCode.collectAsStateWithLifecycle()
    val statsCurrencySymbol by viewModel.statsCurrencySymbol.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences(com.example.data.AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val timeFilters = listOf("7D", "1M", "6M", "1Y", "All")
    var selectedTimeFilter by remember {
        val savedPreset = sharedPrefs.getString("analytics_time_filter", "7D") ?: "7D"
        val effectivePreset = if (savedPreset == "30D") "1M" else savedPreset
        mutableStateOf(if (effectivePreset in timeFilters || effectivePreset == "Custom") effectivePreset else "7D")
    }
    var customStartDateMs by remember { mutableStateOf<Long?>(null) }
    var customEndDateMs by remember { mutableStateOf<Long?>(null) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var analyticsMode by rememberSaveable { mutableStateOf(AnalyticsViewMode.QUICK_STATS) }

    var inspectionMetric by remember { mutableStateOf<MetricInspectionData?>(null) }
    var activeCategoryDrillDown by remember { mutableStateOf<CategoryTrendSummary?>(null) }

    val periodDateRange = remember(selectedTimeFilter, allExpenses, customStartDateMs, customEndDateMs) {
        val customStart = customStartDateMs
        val customEnd = customEndDateMs
        if (selectedTimeFilter == "Custom" && customStart != null && customEnd != null) {
            Pair(customStart, customEnd)
        } else {
            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val end = endCal.timeInMillis
            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = when (selectedTimeFilter) {
                "7D" -> startCal.apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
                "1M" -> startCal.apply { add(Calendar.MONTH, -1) }.timeInMillis
                "6M" -> startCal.apply { add(Calendar.MONTH, -6) }.timeInMillis
                "1Y" -> startCal.apply { add(Calendar.YEAR, -1) }.timeInMillis
                else -> {
                    val minExpenseDate = allExpenses.minOfOrNull { it.date } ?: startCal.timeInMillis
                    minOf(minExpenseDate, startCal.timeInMillis)
                }
            }
            Pair(start, end)
        }
    }

    val filteredPeriodExpenses = remember(allExpenses, periodDateRange) {
        val (start, end) = periodDateRange
        allExpenses.filter { it.date in start..end }
    }

    val prevDateRange = remember(periodDateRange) {
        AnalyticsCalculator.computePreviousDateRange(periodDateRange.first, periodDateRange.second)
    }

    val previousPeriodExpenses = remember(allExpenses, prevDateRange) {
        val (pStart, pEnd) = prevDateRange
        allExpenses.filter { it.date in pStart..pEnd }
    }

    val prevPeriodLabel = remember(selectedTimeFilter, prevDateRange) {
        when (selectedTimeFilter) {
            "7D" -> "prev 7 days"
            "1M" -> "prev month"
            "6M" -> "prev 6 mos"
            "1Y" -> "prev year"
            else -> "prev period"
        }
    }

    val comparisonSummary = remember(filteredPeriodExpenses, previousPeriodExpenses, statsCurrencyCode, prevPeriodLabel) {
        AnalyticsCalculator.computeComparison(filteredPeriodExpenses, previousPeriodExpenses, statsCurrencyCode, prevPeriodLabel)
    }

    val categoryTrends = remember(filteredPeriodExpenses, previousPeriodExpenses, statsCurrencyCode) {
        AnalyticsCalculator.computeCategoryTrends(filteredPeriodExpenses, previousPeriodExpenses, statsCurrencyCode)
    }

    val spendingPatterns = remember(filteredPeriodExpenses, statsCurrencyCode) {
        AnalyticsCalculator.computeSpendingPatterns(filteredPeriodExpenses, statsCurrencyCode)
    }

    val budgetPerformance = remember(filteredPeriodExpenses, budgets, statsCurrencyCode) {
        AnalyticsCalculator.computeBudgetPerformance(filteredPeriodExpenses, budgets, statsCurrencyCode)
    }

    val periodDays = remember(periodDateRange) {
        val (start, end) = periodDateRange
        val diffMs = (end - start).coerceAtLeast(86400000L)
        (diffMs / (24 * 3600 * 1000L)).toInt().coerceAtLeast(1)
    }
    val dailyAvg = if (periodDays > 0) comparisonSummary.currentSpent / periodDays else 0.0

    val dateRangeLabel = remember(periodDateRange, selectedTimeFilter) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val startStr = sdf.format(Date(periodDateRange.first))
        val endStr = sdf.format(Date(periodDateRange.second))
        if (selectedTimeFilter == "All") "All Time" else "$startStr – $endStr"
    }

    val categoryColors = remember {
        mapOf(
            "Food" to Color(0xFFF97316),
            "Travel" to Color(0xFF06B6D4),
            "Rent" to Color(0xFF8B5CF6),
            "Utilities" to Color(0xFFEC4899),
            "Entertainment" to Color(0xFF3B82F6),
            "Shopping" to Color(0xFF10B981),
            "Home" to Color(0xFFF59E0B),
            "Bills" to Color(0xFFEF4444),
            "Persons" to Color(0xFF6366F1),
            "Salary" to Color(0xFF10B981),
            "Freelance" to Color(0xFF06B6D4),
            "Investments" to Color(0xFF8B5CF6),
            "Gifts" to Color(0xFFEC4899),
            "Others" to Color(0xFF64748B)
        )
    }

    if (showCustomDateRangePicker) {
        CustomDateRangePickerDialog(
            initialStartMs = periodDateRange.first,
            initialEndMs = periodDateRange.second,
            onDismiss = { showCustomDateRangePicker = false },
            onApplyRange = { start, end ->
                customStartDateMs = start
                customEndDateMs = end
                selectedTimeFilter = "Custom"
                showCustomDateRangePicker = false
            }
        )
    }

    val metricModalData = inspectionMetric
    if (metricModalData != null) {
        MetricInspectionModal(
            data = metricModalData,
            onDismiss = { inspectionMetric = null },
            onExpenseClick = onExpenseClick
        )
    }

    val drillDownItem = activeCategoryDrillDown
    if (drillDownItem != null) {
        CategoryDrillDownDialog(
            item = drillDownItem,
            currencySymbol = currencySymbol,
            onDismiss = { activeCategoryDrillDown = null },
            onExpenseClick = onExpenseClick
        )
    }

    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.5.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    .clip(CircleShape)
                                    .background(SleekPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(AppIcons.FileDownload, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(20.dp))
                            }
                            Text("Export Report", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SleekTextPrimary)
                        }
                        IconButton(onClick = { showExportDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Text("Export analytics data for $selectedTimeFilter ($dateRangeLabel):", fontSize = 13.sp, color = SleekTextSecondary)

                    Button(
                        onClick = {
                            DataExporter.sharePdfReport(
                                context = context,
                                expenses = filteredPeriodExpenses,
                                dateRangeStr = "$selectedTimeFilter ($dateRangeLabel)",
                                typeFilterStr = "All Transactions",
                                categoryFilterStr = "All Categories",
                                amountSaved = (comparisonSummary.currentIncome - comparisonSummary.currentSpent).coerceAtLeast(0.0),
                                monthsOverBudget = 0,
                                includeDetailedTxns = true
                            )
                            showExportDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF Document", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            DataExporter.exportToCSV(
                                context = context,
                                expenses = filteredPeriodExpenses,
                                dateRangeStr = "$selectedTimeFilter ($dateRangeLabel)",
                                typeFilterStr = "All Transactions",
                                categoryFilterStr = "All Categories"
                            )
                            showExportDialog = false
                        },
                        border = BorderStroke(1.dp, SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.TableChart, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV Spreadsheet", color = SleekPrimary, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            DataExporter.exportToJson(
                                context = context,
                                expenses = filteredPeriodExpenses,
                                dateRangeStr = "$selectedTimeFilter ($dateRangeLabel)",
                                typeFilterStr = "All Transactions",
                                categoryFilterStr = "All Categories"
                            )
                            showExportDialog = false
                        },
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Code, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export JSON Document", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            DataExporter.shareImageReport(
                                context = context,
                                expenses = filteredPeriodExpenses,
                                dateRangeStr = "Period ($selectedTimeFilter)",
                                typeFilterStr = "All Transactions",
                                categoryFilterStr = "All Categories"
                            )
                            showExportDialog = false
                        },
                        border = BorderStroke(1.dp, SleekBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Photo, contentDescription = null, tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Graphic Summary", color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SleekPrimaryContainer)
                    .clickable { onProfileClick() },
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

            Text(
                text = "Analytics",
                style = MaterialTheme.typography.titleLarge,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            IconButton(
                onClick = { showExportDialog = true },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = AppIcons.FileDownload,
                    contentDescription = "Export Report",
                    tint = SleekTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        InteractiveAnalyticsHeader(
            currentMode = analyticsMode,
            onModeChange = { analyticsMode = it },
            selectedTimeFilter = selectedTimeFilter,
            onTimeFilterChange = { tf ->
                selectedTimeFilter = tf
                if (tf != "Custom") {
                    sharedPrefs.edit().putString("analytics_time_filter", tf).apply()
                }
            },
            timeFilterPresets = timeFilters,
            onCustomDateRangeClick = { showCustomDateRangePicker = true },
            onExportClick = { showExportDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = AppIcons.DateRange,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = dateRangeLabel,
                    fontSize = 11.sp,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${filteredPeriodExpenses.size} transactions",
                fontSize = 11.sp,
                color = SleekTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val hasMultiCurrency = remember(filteredPeriodExpenses, statsCurrencyCode) {
            filteredPeriodExpenses.any { !it.currencyCode.equals(statsCurrencyCode, ignoreCase = true) }
        }
        if (hasMultiCurrency) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SleekPrimaryContainer.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.CurrencyExchange, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "💱 Multi-currency values converted & normalized to $statsCurrencyCode for exact calculations.",
                        fontSize = 10.5.sp,
                        color = SleekTextPrimary
                    )
                }
            }
        }

        AnimatedContent(
            targetState = analyticsMode,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                 slideInVertically(initialOffsetY = { 20 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                (fadeOut(animationSpec = tween(150)))
            },
            label = "AnalyticsModeTransition"
        ) { mode ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (mode) {
                    AnalyticsViewMode.QUICK_STATS -> {
                        InteractiveScorecardRow(
                            comparison = comparisonSummary,
                            currencySymbol = currencySymbol,
                            onMetricClick = { inspectionMetric = it },
                            currentExpenses = filteredPeriodExpenses,
                            previousExpenses = previousPeriodExpenses,
                            dateRangeLabel = dateRangeLabel
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekSurface)
                                .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Activity", fontSize = 10.sp, color = SleekTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${filteredPeriodExpenses.size} txs",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(SleekBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Daily Avg", fontSize = 10.sp, color = SleekTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currencySymbol%,.0f".format(dailyAvg),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(SleekBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Savings Rate", fontSize = 10.sp, color = SleekTextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${comparisonSummary.currentSavingsRate.roundToInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (comparisonSummary.currentSavingsRate >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }

                        CashFlowWaterfallCard(
                            comparison = comparisonSummary,
                            currencySymbol = currencySymbol
                        )

                        CategoryTrendsSection(
                            categoryTrends = categoryTrends.take(5),
                            currencySymbol = currencySymbol,
                            categoryColors = categoryColors,
                            onCategoryClick = { activeCategoryDrillDown = it }
                        )

                        NetWorthOverTimeChartCard(
                            allExpenses = filteredPeriodExpenses,
                            accounts = accounts,
                            selectedTimeFilter = selectedTimeFilter,
                            currencySymbol = currencySymbol
                        )

                        AnalyticsPeriodTransactionsCard(
                            expenses = filteredPeriodExpenses,
                            selectedTimeFilter = selectedTimeFilter,
                            dateRangeLabel = dateRangeLabel,
                            currencySymbol = currencySymbol,
                            categoryIcons = categoryIcons,
                            onExpenseClick = onExpenseClick
                        )
                    }

                    AnalyticsViewMode.DEEP_ANALYTICS -> {
                        CategoryTrendsSection(
                            categoryTrends = categoryTrends,
                            currencySymbol = currencySymbol,
                            categoryColors = categoryColors,
                            onCategoryClick = { activeCategoryDrillDown = it }
                        )

                        BudgetPerformanceSection(
                            budgetPerformance = budgetPerformance,
                            currencySymbol = currencySymbol
                        )

                        AccountWiseAnalyticsSection(
                            accounts = accounts,
                            expenses = filteredPeriodExpenses,
                            currencySymbol = currencySymbol
                        )

                        SpendingPatternDetectionSection(
                            pattern = spendingPatterns,
                            currencySymbol = currencySymbol
                        )

                        TagSpendingBarChart(
                            expenses = filteredPeriodExpenses,
                            categoryColors = categoryColors,
                            periodLabel = "$selectedTimeFilter ($dateRangeLabel)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnalyticsPeriodTransactionsCard(
                            expenses = filteredPeriodExpenses,
                            selectedTimeFilter = selectedTimeFilter,
                            dateRangeLabel = dateRangeLabel,
                            currencySymbol = currencySymbol,
                            categoryIcons = categoryIcons,
                            onExpenseClick = onExpenseClick
                        )
                    }

                    AnalyticsViewMode.TRENDS_CASHFLOW -> {
                        SpendingTrendsScreen(
                            expenses = allExpenses,
                            currencySymbol = currencySymbol,
                            categoryColors = categoryColors,
                            onExpenseClick = onExpenseClick
                        )

                        CashFlowWaterfallCard(
                            comparison = comparisonSummary,
                            currencySymbol = currencySymbol
                        )

                        NetWorthOverTimeChartCard(
                            allExpenses = filteredPeriodExpenses,
                            accounts = accounts,
                            selectedTimeFilter = selectedTimeFilter,
                            currencySymbol = currencySymbol
                        )

                        AnalyticsPeriodTransactionsCard(
                            expenses = filteredPeriodExpenses,
                            selectedTimeFilter = selectedTimeFilter,
                            dateRangeLabel = dateRangeLabel,
                            currencySymbol = currencySymbol,
                            categoryIcons = categoryIcons,
                            onExpenseClick = onExpenseClick
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekTextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AnalyticsPeriodTransactionsCard(
    expenses: List<Expense>,
    selectedTimeFilter: String,
    dateRangeLabel: String = "",
    currencySymbol: String,
    categoryIcons: Map<String, String>,
    onExpenseClick: (Expense) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember(selectedTimeFilter) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_period_transactions_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transactions in Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = if (dateRangeLabel.isNotBlank()) "$selectedTimeFilter • $dateRangeLabel • ${expenses.size} records" else "$selectedTimeFilter • ${expenses.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekPrimaryContainer.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${expenses.size} total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found for $selectedTimeFilter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekTextSecondary
                    )
                }
            } else {
                val sorted = remember(expenses) { expenses.sortedByDescending { it.date } }
                val displayCount = if (isExpanded) sorted.size else minOf(6, sorted.size)
                val visibleItems = remember(sorted, displayCount) { sorted.take(displayCount) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleItems.forEach { exp ->
                        val isExpense = exp.type != "INCOME" && exp.category != "Locked Savings"
                        val amountColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)
                        val sign = if (isExpense) "-" else "+"
                        val emoji = getCategoryEmoji(exp.category, categoryIcons)
                        val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(exp.date))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekNeutralLight.copy(alpha = 0.45f),
                            border = BorderStroke(0.5.dp, SleekBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExpenseClick(exp)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(amountColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji.ifBlank { if (isExpense) "💸" else "💰" }, fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(
                                            text = if (!exp.note.isNullOrBlank()) exp.note else exp.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SleekTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${exp.category} • $dateStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SleekTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "$sign$currencySymbol%,.2f".format(exp.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                            }
                        }
                    }

                    if (sorted.size > 6) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isExpanded = !isExpanded
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isExpanded) "Show Less" else "View All (${sorted.size})",
                                color = SleekPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthOverTimeChartCard(
    allExpenses: List<Expense>,
    accounts: List<Account>,
    selectedTimeFilter: String,
    currencySymbol: String,
    currencyCode: String = "INR"
) {
    val totalAccountAssets = remember(accounts) {
        accounts.sumOf { if (it.type == "CREDIT") -it.balance else it.balance }
    }

    val sortedExpenses = remember(allExpenses) { allExpenses.sortedBy { it.date } }

    val dataPoints = remember(sortedExpenses, totalAccountAssets, selectedTimeFilter, currencyCode) {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val periodStartMs = when (selectedTimeFilter) {
            "7D" -> now - 7 * dayMs
            "1M" -> now - 30 * dayMs
            "6M" -> now - 180 * dayMs
            "1Y" -> now - 365 * dayMs
            else -> sortedExpenses.firstOrNull()?.date ?: (now - 30 * dayMs)
        }
        if (sortedExpenses.isEmpty()) {
            listOf(
                Pair(periodStartMs, totalAccountAssets),
                Pair(now, totalAccountAssets)
            )
        } else {
            val points = mutableListOf<Pair<Long, Double>>()
            val totalIncome = sortedExpenses.realIncome(currencyCode)
            val totalExpense = sortedExpenses.realExpense(currencyCode)
            val initialNetWorth = if (accounts.isNotEmpty()) {
                totalAccountAssets - (totalIncome - totalExpense)
            } else {
                0.0
            }

            var runningFlow = initialNetWorth
            val firstDate = minOf(periodStartMs, (sortedExpenses.firstOrNull()?.date ?: now) - dayMs)
            points.add(Pair(firstDate, initialNetWorth))

            val step = (sortedExpenses.size / 7).coerceAtLeast(1)
            sortedExpenses.chunked(step).take(7).forEach { chunk ->
                val date = chunk.last().date
                chunk.forEach { e ->
                    val convertedMinor = Money.convert(e.amountMinor, e.currencyCode, currencyCode)
                    val converted = Money.toDouble(convertedMinor, currencyCode)
                    if (e.type == "INCOME") runningFlow += converted else runningFlow -= converted
                }
                points.add(Pair(date, runningFlow))
            }
            if (points.size < 2) {
                points.add(0, Pair(periodStartMs, initialNetWorth))
            }
            points
        }
    }

    val currentNetWorth = dataPoints.lastOrNull()?.second ?: totalAccountAssets
    val startingNetWorth = dataPoints.firstOrNull()?.second ?: (currentNetWorth * 0.9)
    val changeAmount = currentNetWorth - startingNetWorth
    val changePct = if (startingNetWorth != 0.0) (changeAmount / startingNetWorth) * 100 else 0.0

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val activePoint = selectedPointIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()

    val animEnabled = LocalAnimationsEnabled.current
    var animationPlayed by remember { mutableStateOf(!animEnabled) }

    val chartAnimProgress by animateFloatAsState(
        targetValue = if (animationPlayed || !animEnabled) 1f else 0f,
        animationSpec = appTween(durationMs = 850, easing = AppleDecelerateEasing),
        label = "net_worth_chart_anim"
    )

    LaunchedEffect(dataPoints, animEnabled) {
        if (animEnabled) {
            animationPlayed = false
            kotlinx.coroutines.delay(30)
            animationPlayed = true
        } else {
            animationPlayed = true
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Net Worth Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Accumulated assets minus liabilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (changeAmount >= 0) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (changeAmount >= 0) AppIcons.ArrowUpward else AppIcons.ArrowDownward,
                            contentDescription = null,
                            tint = if (changeAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${if (changeAmount >= 0) "+" else ""}${String.format(Locale.US, "%.1f", changePct)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (changeAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = activePoint?.let {
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            sdf.format(Date(it.first))
                        } ?: "Current Value",
                        fontSize = 11.sp,
                        color = SleekTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", activePoint?.second ?: currentNetWorth)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = SleekTextPrimary
                    )
                }

                if (selectedPointIndex != null) {
                    TextButton(
                        onClick = { selectedPointIndex = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reset view", fontSize = 11.sp, color = SleekPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val lineGraphColor = Color(0xFF10B981)
            val values = dataPoints.map { it.second }
            val minVal = (values.minOrNull() ?: 0.0) * 0.95
            val maxVal = ((values.maxOrNull() ?: 1000.0) * 1.05).coerceAtLeast(minVal + 100.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX).roundToInt().coerceIn(0, dataPoints.size - 1)
                                selectedPointIndex = index
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val pointsCount = dataPoints.size

                    if (pointsCount < 2) return@Canvas

                    val stepX = w / (pointsCount - 1)
                    val coords = dataPoints.mapIndexed { idx, pt ->
                        val x = idx * stepX
                        val normalizedY = (((pt.second - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)) * chartAnimProgress
                        val y = h - (normalizedY * (h - 20.dp.toPx())) - 10.dp.toPx()
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.size - 1) {
                            val p1 = coords[i]
                            val p2 = coords[i + 1]
                            val cx = (p1.x + p2.x) / 2f
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(coords.last().x, h)
                        lineTo(coords.first().x, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineGraphColor.copy(alpha = 0.35f),
                                lineGraphColor.copy(alpha = 0.02f)
                            )
                        )
                    )

                    drawPath(
                        path = path,
                        color = lineGraphColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    coords.forEachIndexed { idx, point ->
                        val isSelected = selectedPointIndex == idx || (selectedPointIndex == null && idx == coords.size - 1)
                        if (isSelected) {
                            drawCircle(
                                color = lineGraphColor.copy(alpha = 0.25f),
                                radius = 10.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineGraphColor,
                                radius = 6.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekPrimaryContainer.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Peak Net Worth", fontSize = 10.sp, color = SleekTextSecondary)
                    Text("$currencySymbol${String.format(Locale.US, "%,.0f", maxVal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SleekBorder))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Assets", fontSize = 10.sp, color = SleekTextSecondary)
                    Text("$currencySymbol${String.format(Locale.US, "%,.0f", totalAccountAssets)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }
    }
}
