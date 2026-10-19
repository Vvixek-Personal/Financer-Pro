package com.example.ui

import com.example.ui.theme.*
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class TrendTimeframe {
    WEEKLY,
    MONTHLY
}

enum class TrendChartStyle {
    CURVE,
    BARS
}

data class TrendPeriodData(
    val id: String,
    val title: String,
    val shortLabel: String,
    val subLabel: String,
    val startDateMs: Long,
    val endDateMs: Long,
    val totalSpent: Double,
    val totalIncome: Double,
    val count: Int,
    val topCategory: String?,
    val expenses: List<Expense>,
    val changePercent: Double? = null // Change compared to previous period
)

/**
 * Full-featured Spending Trends View embedded within the Analytics Tab.
 * Leverages native Jetpack Compose Canvas drawing for smooth, interactive
 * weekly and monthly trend visualizations with scrubbing and drill-down insights.
 */
@Composable
fun SpendingTrendsScreen(
    expenses: List<Expense>,
    currencySymbol: String,
    categoryColors: Map<String, Color>,
    modifier: Modifier = Modifier,
    onExpenseClick: (Expense) -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var selectedTimeframe by remember { mutableStateOf(TrendTimeframe.WEEKLY) }
    var selectedChartStyle by remember { mutableStateOf(TrendChartStyle.CURVE) }
    var selectedRangeWeeks by remember { mutableIntStateOf(8) } // 4, 8, 12, 26 weeks
    var selectedRangeMonths by remember { mutableIntStateOf(6) } // 6, 12 months

    // Aggregate trends according to the selected timeframe
    val trendData = remember(expenses, selectedTimeframe, selectedRangeWeeks, selectedRangeMonths) {
        val nonIncome = expenses.filter { it.type != "INCOME" && it.category != "Locked Savings" }
        val incomeList = expenses.filter { it.type == "INCOME" && it.category != "Goal Withdrawal" }

        when (selectedTimeframe) {
            TrendTimeframe.WEEKLY -> calculateWeeklyTrends(nonIncome, incomeList, selectedRangeWeeks)
            TrendTimeframe.MONTHLY -> calculateMonthlyTrends(nonIncome, incomeList, selectedRangeMonths)
        }
    }

    var selectedIndex by remember(trendData) {
        mutableIntStateOf(if (trendData.isNotEmpty()) trendData.lastIndex else -1)
    }

    val selectedPeriod = remember(trendData, selectedIndex) {
        if (selectedIndex in trendData.indices) trendData[selectedIndex] else trendData.lastOrNull()
    }

    // High-level stats over the aggregated series
    val avgSpending = remember(trendData) {
        if (trendData.isNotEmpty()) trendData.map { it.totalSpent }.average() else 0.0
    }
    val peakPeriod = remember(trendData) {
        trendData.maxByOrNull { it.totalSpent }
    }
    val lowestPeriod = remember(trendData) {
        trendData.filter { it.totalSpent > 0 }.minByOrNull { it.totalSpent } ?: trendData.minByOrNull { it.totalSpent }
    }
    val totalPeriodSpent = remember(trendData) {
        trendData.sumOf { it.totalSpent }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trends_screen")
    ) {
        // Top Header & Mode Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SleekSurface)
                            .border(1.dp, SleekBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Back to Overview",
                            tint = SleekTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Spending Trends",
                        fontSize = SleekSizes.textTitleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedTimeframe == TrendTimeframe.WEEKLY)
                            "Weekly trend over last $selectedRangeWeeks weeks"
                        else
                            "Monthly trend over last $selectedRangeMonths months",
                        fontSize = SleekSizes.textBodySmall,
                        color = SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Chart Style Toggle (Spline vs Bar)
            Row(
                modifier = Modifier
                    .clip(SleekShapes.md)
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder, SleekShapes.md)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedChartStyle = TrendChartStyle.CURVE
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(SleekShapes.sm)
                        .background(if (selectedChartStyle == TrendChartStyle.CURVE) SleekPrimary else Color.Transparent)
                ) {
                    Icon(
                        imageVector = AppIcons.ShowChart,
                        contentDescription = "Spline Curve",
                        tint = if (selectedChartStyle == TrendChartStyle.CURVE) Color.White else SleekTextSecondary,
                        modifier = Modifier.size(SleekSizes.iconSmall)
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedChartStyle = TrendChartStyle.BARS
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(SleekShapes.sm)
                        .background(if (selectedChartStyle == TrendChartStyle.BARS) SleekPrimary else Color.Transparent)
                ) {
                    Icon(
                        imageVector = AppIcons.BarChart,
                        contentDescription = "Bars",
                        tint = if (selectedChartStyle == TrendChartStyle.BARS) Color.White else SleekTextSecondary,
                        modifier = Modifier.size(SleekSizes.iconSmall)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Control: Weekly vs Monthly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SleekShapes.lg)
                .background(SleekSurface)
                .border(1.dp, SleekBorder, SleekShapes.lg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SleekShapes.md)
                    .background(if (selectedTimeframe == TrendTimeframe.WEEKLY) SleekPrimary else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTimeframe = TrendTimeframe.WEEKLY
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.CalendarViewWeek,
                        contentDescription = null,
                        tint = if (selectedTimeframe == TrendTimeframe.WEEKLY) Color.White else SleekTextSecondary,
                        modifier = Modifier.size(SleekSizes.iconSmall)
                    )
                    Text(
                        text = "Weekly Trends",
                        fontSize = SleekSizes.textBodyMedium,
                        fontWeight = if (selectedTimeframe == TrendTimeframe.WEEKLY) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTimeframe == TrendTimeframe.WEEKLY) Color.White else SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SleekShapes.md)
                    .background(if (selectedTimeframe == TrendTimeframe.MONTHLY) SleekPrimary else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTimeframe = TrendTimeframe.MONTHLY
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.CalendarMonth,
                        contentDescription = null,
                        tint = if (selectedTimeframe == TrendTimeframe.MONTHLY) Color.White else SleekTextSecondary,
                        modifier = Modifier.size(SleekSizes.iconSmall)
                    )
                    Text(
                        text = "Monthly Trends",
                        fontSize = SleekSizes.textBodyMedium,
                        fontWeight = if (selectedTimeframe == TrendTimeframe.MONTHLY) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTimeframe == TrendTimeframe.MONTHLY) Color.White else SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time Range Duration Filter Chips (e.g. 4W, 8W, 12W or 6M, 12M)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Window:",
                fontSize = SleekSizes.textBodySmall,
                fontWeight = FontWeight.Medium,
                color = SleekTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (selectedTimeframe == TrendTimeframe.WEEKLY) {
                listOf(4 to "4 Weeks", 8 to "8 Weeks", 12 to "12 Weeks", 26 to "26 Weeks").forEach { (weeks, label) ->
                    val isSelected = selectedRangeWeeks == weeks
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRangeWeeks = weeks
                        },
                        label = { Text(label, fontSize = SleekSizes.textCaption, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPrimaryContainer,
                            selectedLabelColor = SleekPrimary,
                            containerColor = SleekSurface,
                            labelColor = SleekTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) SleekPrimary else SleekBorder
                        ),
                        shape = SleekShapes.md
                    )
                }
            } else {
                listOf(6 to "6 Months", 12 to "12 Months", 24 to "2 Years").forEach { (months, label) ->
                    val isSelected = selectedRangeMonths == months
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRangeMonths = months
                        },
                        label = { Text(label, fontSize = SleekSizes.textCaption, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPrimaryContainer,
                            selectedLabelColor = SleekPrimary,
                            containerColor = SleekSurface,
                            labelColor = SleekTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) SleekPrimary else SleekBorder
                        ),
                        shape = SleekShapes.md
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 📈 MAIN CUSTOM CANVAS CHART CARD
        // ==========================================
        Card(
            shape = SleekShapes.xxl,
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trend_canvas_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Inspected Period Info Header
                selectedPeriod?.let { cur ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = cur.title,
                                fontSize = SleekSizes.textBodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$currencySymbol%,.2f".format(cur.totalSpent),
                                    fontSize = SleekSizes.textHeadline,
                                    fontWeight = FontWeight.Black,
                                    color = SleekTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                cur.changePercent?.let { pct ->
                                    val isDrop = pct <= 0
                                    val badgeBg = if (isDrop) IncomeGreenBg else ExpenseRedBg
                                    val badgeText = if (isDrop) IncomeGreen else ExpenseRed
                                    Row(
                                        modifier = Modifier
                                            .clip(SleekShapes.sm)
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDrop) AppIcons.TrendingDown else AppIcons.TrendingUp,
                                            contentDescription = null,
                                            tint = badgeText,
                                            modifier = Modifier.size(SleekSizes.iconMicro)
                                        )
                                        Text(
                                            text = "%+.1f%%".format(pct),
                                            fontSize = SleekSizes.textCaption,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Average spending indicator pill
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Avg per ${if (selectedTimeframe == TrendTimeframe.WEEKLY) "week" else "month"}",
                                fontSize = SleekSizes.textCaption,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol%,.0f".format(avgSpending),
                                fontSize = SleekSizes.textBody,
                                fontWeight = FontWeight.Bold,
                                color = SavingGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas Drawing Chart
                if (trendData.isEmpty() || trendData.all { it.totalSpent == 0.0 }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(SleekShapes.lg)
                            .background(SleekBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Timeline,
                                contentDescription = null,
                                tint = SleekTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No spending records found in this window",
                                fontSize = 13.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }
                } else {
                    TrendsCustomCanvas(
                        data = trendData,
                        selectedIndex = selectedIndex,
                        onSelectIndex = { idx ->
                            if (idx != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = idx
                            }
                        },
                        chartStyle = selectedChartStyle,
                        avgValue = avgSpending,
                        currencySymbol = currencySymbol,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive scrubber prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.TouchApp,
                            contentDescription = null,
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Tap or drag to scrub points",
                            fontSize = 11.sp,
                            color = SleekTextSecondary
                        )
                    }

                    // Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SleekPrimary)
                            )
                            Text("Spending", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(2.dp)
                                    .background(SavingGold)
                            )
                            Text("Average", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 📊 KEY TREND METRICS TILES
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Peak Spending Card
            Card(
                shape = SleekShapes.xl,
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.NorthEast,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(SleekSizes.iconMicro)
                            )
                        }
                        Text("Peak", fontSize = SleekSizes.textCaption, color = SleekTextSecondary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$currencySymbol%,.0f".format(peakPeriod?.totalSpent ?: 0.0),
                        fontSize = SleekSizes.textBodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = peakPeriod?.shortLabel ?: "—",
                        fontSize = SleekSizes.textMicro,
                        color = SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Lowest Spending Card
            Card(
                shape = SleekShapes.xl,
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.SouthEast,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(SleekSizes.iconMicro)
                            )
                        }
                        Text("Frugal", fontSize = SleekSizes.textCaption, color = SleekTextSecondary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$currencySymbol%,.0f".format(lowestPeriod?.totalSpent ?: 0.0),
                        fontSize = SleekSizes.textBodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = lowestPeriod?.shortLabel ?: "—",
                        fontSize = SleekSizes.textMicro,
                        color = SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Total Series Spending
            Card(
                shape = SleekShapes.xl,
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.AllInclusive,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(SleekSizes.iconMicro)
                            )
                        }
                        Text("Total", fontSize = SleekSizes.textCaption, color = SleekTextSecondary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$currencySymbol%,.0f".format(totalPeriodSpent),
                        fontSize = SleekSizes.textBodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${trendData.size} ${if (selectedTimeframe == TrendTimeframe.WEEKLY) "wks" else "mos"}",
                        fontSize = SleekSizes.textMicro,
                        color = SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 🔎 SELECTED PERIOD BREAKDOWN & DRILLDOWN
        // ==========================================
        selectedPeriod?.let { period ->
            Card(
                shape = SleekShapes.xxl,
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trend_period_drilldown_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "${period.title} Breakdown",
                                fontSize = SleekSizes.textSubhead,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${period.count} transactions recorded (${period.subLabel})",
                                fontSize = SleekSizes.textBodySmall,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(SleekShapes.sm)
                                .background(SleekPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$currencySymbol%,.2f".format(period.totalSpent),
                                fontSize = SleekSizes.textBodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (period.expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expense entries for this specific period.",
                                fontSize = SleekSizes.textBodySmall,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Category pills for this period
                        val categorySums = remember(period.expenses) {
                            period.expenses.groupBy { it.category }
                                .mapValues { it.value.sumOf { e -> e.amount } }
                                .toList()
                                .sortedByDescending { it.second }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categorySums, key = { it.first }) { (cat, amount) ->
                                val catColor = categoryColors[cat] ?: SleekPrimary
                                Row(
                                    modifier = Modifier
                                        .clip(SleekShapes.md)
                                        .background(catColor.copy(alpha = 0.15f))
                                        .border(1.dp, catColor.copy(alpha = 0.4f), SleekShapes.md)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Text(
                                        text = cat,
                                        fontSize = SleekSizes.textCaption,
                                        fontWeight = FontWeight.Medium,
                                        color = SleekTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$currencySymbol%,.0f".format(amount),
                                        fontSize = SleekSizes.textCaption,
                                        fontWeight = FontWeight.Bold,
                                        color = catColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Transactions List preview (Top 5)
                        Text(
                            text = "Transactions in this period:",
                            fontSize = SleekSizes.textBodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        period.expenses.take(5).forEach { exp ->
                            val sdf = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(SleekShapes.md)
                                    .clickable { onExpenseClick(exp) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    val catColor = categoryColors[exp.category] ?: SleekPrimary
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = exp.category.take(1).uppercase(),
                                            fontSize = SleekSizes.textBodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = catColor
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (!exp.note.isNullOrBlank()) exp.note else exp.category,
                                            fontSize = SleekSizes.textBodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = SleekTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${exp.category} • ${sdf.format(Date(exp.date))}",
                                            fontSize = SleekSizes.textCaption,
                                            color = SleekTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "-$currencySymbol%,.2f".format(exp.amount),
                                    fontSize = SleekSizes.textBodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }

                        if (period.expenses.size > 5) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+ ${period.expenses.size - 5} more transactions in this period",
                                fontSize = SleekSizes.textCaption,
                                color = SleekPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Canvas Drawing Component for Spending Trends.
 * Supports both smooth Cubic-Bezier Spline curves with gradient area fill
 * and modern rounded bar charts, with interactive touch scrubber and milestone grid lines.
 */
@Composable
fun TrendsCustomCanvas(
    data: List<TrendPeriodData>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    chartStyle: TrendChartStyle,
    avgValue: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animState = remember { Animatable(0f) }

    LaunchedEffect(data, chartStyle) {
        animState.snapTo(0f)
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }
    animationProgress = animState.value

    val isDark = isDarkModeActive
    val primaryColor = SleekPrimary
    val primaryContainer = SleekPrimaryContainer
    val borderColor = SleekBorder
    val textSecondaryColor = SleekTextSecondary

    val density = LocalDensity.current

    // Touch scrubber state
    var touchX by remember { mutableFloatStateOf(-1f) }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures(
                        onTap = { offset ->
                            val count = data.size
                            if (count > 0) {
                                val paddingStart = 45f
                                val paddingEnd = 20f
                                val plotWidth = (size.width - paddingStart - paddingEnd).coerceAtLeast(1f)
                                val stepX = if (count > 1) plotWidth / (count - 1) else plotWidth
                                val relativeX = (offset.x - paddingStart).coerceIn(0f, plotWidth)
                                val idx = (relativeX / stepX).roundToInt().coerceIn(0, count - 1)
                                onSelectIndex(idx)
                                touchX = offset.x
                            }
                        }
                    )
                }
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchX = offset.x
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            touchX = change.position.x
                            val count = data.size
                            if (count > 0) {
                                val paddingStart = 45f
                                val paddingEnd = 20f
                                val plotWidth = (size.width - paddingStart - paddingEnd).coerceAtLeast(1f)
                                val relativeX = (change.position.x - paddingStart).coerceIn(0f, plotWidth)
                                val idx = (relativeX / stepX(count, plotWidth)).roundToInt().coerceIn(0, count - 1)
                                onSelectIndex(idx)
                            }
                        },
                        onDragEnd = {
                            touchX = -1f
                        }
                    )
                }
        ) {
            val count = data.size
            if (count == 0) return@Canvas

            val paddingStart = 50f
            val paddingEnd = 25f
            val paddingTop = 30f
            val paddingBottom = 40f

            val plotWidth = (size.width - paddingStart - paddingEnd).coerceAtLeast(10f)
            val plotHeight = (size.height - paddingTop - paddingBottom).coerceAtLeast(10f)

            val maxSpend = (data.maxOfOrNull { it.totalSpent } ?: 100.0).coerceAtLeast(50.0)
            // Round ceiling to neat visual increments
            val yCeiling = calculateYCeiling(maxSpend)

            // 1. Draw horizontal dashed grid lines and Y-axis labels
            val gridSteps = 4
            val textPaint = Paint().apply {
                color = if (isDark) android.graphics.Color.parseColor("#9CA3AF") else android.graphics.Color.parseColor("#6B7280")
                textSize = density.run { 10.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            for (i in 0..gridSteps) {
                val fraction = i.toFloat() / gridSteps
                val y = paddingTop + plotHeight * (1f - fraction)
                val milestoneValue = yCeiling * fraction

                // Grid line
                drawLine(
                    color = borderColor.copy(alpha = if (i == 0) 0.8f else 0.35f),
                    start = Offset(paddingStart, y),
                    end = Offset(size.width - paddingEnd, y),
                    strokeWidth = if (i == 0) 1.5f else 1.0f,
                    pathEffect = if (i > 0) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) else null
                )

                // Label
                val labelText = if (milestoneValue >= 1000) {
                    "$currencySymbol%.0fk".format(milestoneValue / 1000)
                } else {
                    "$currencySymbol%.0f".format(milestoneValue)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    paddingStart - 10f,
                    y + 4f,
                    textPaint
                )
            }

            // 2. Draw Average spending guideline
            if (avgValue > 0 && avgValue <= yCeiling) {
                val avgY = paddingTop + plotHeight * (1f - (avgValue / yCeiling).toFloat()).coerceIn(0f, 1f)
                drawLine(
                    color = Color(0xFFD97706).copy(alpha = 0.7f),
                    start = Offset(paddingStart, avgY),
                    end = Offset(size.width - paddingEnd, avgY),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // 3. Compute Coordinates for each data item
            val stepX = if (count > 1) plotWidth / (count - 1) else plotWidth
            val points = data.mapIndexed { index, item ->
                val x = paddingStart + index * stepX
                val normalizedY = (item.totalSpent / yCeiling).coerceIn(0.0, 1.0).toFloat()
                val animatedY = normalizedY * animationProgress
                val y = paddingTop + plotHeight * (1f - animatedY)
                Offset(x, y)
            }

            // 4. Render Chart according to selected style
            if (chartStyle == TrendChartStyle.CURVE) {
                drawSplineCurve(
                    points = points,
                    paddingTop = paddingTop,
                    plotHeight = plotHeight,
                    paddingStart = paddingStart,
                    primaryColor = primaryColor,
                    isDark = isDark
                )
            } else {
                drawRoundedBars(
                    points = points,
                    data = data,
                    paddingTop = paddingTop,
                    plotHeight = plotHeight,
                    stepX = stepX,
                    primaryColor = primaryColor,
                    selectedIndex = selectedIndex
                )
            }

            // 5. Draw Bottom X-Axis Labels (Week or Month short names)
            val labelInterval = when {
                count <= 8 -> 1
                count <= 14 -> 2
                else -> 4
            }
            val xLabelPaint = Paint().apply {
                color = if (isDark) android.graphics.Color.parseColor("#9CA3AF") else android.graphics.Color.parseColor("#4B5563")
                textSize = density.run { 10.sp.toPx() }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            data.forEachIndexed { index, item ->
                if (index % labelInterval == 0 || index == count - 1 || index == selectedIndex) {
                    val x = points[index].x
                    val isCurrent = index == selectedIndex
                    xLabelPaint.isFakeBoldText = isCurrent
                    xLabelPaint.color = if (isCurrent) {
                        primaryColor.toArgb()
                    } else {
                        if (isDark) android.graphics.Color.parseColor("#9CA3AF") else android.graphics.Color.parseColor("#4B5563")
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        item.shortLabel,
                        x,
                        size.height - 12f,
                        xLabelPaint
                    )
                }
            }

            // 6. Draw Selected Index Highlights (Vertical Scrubber Line & Glowing Node)
            if (selectedIndex in points.indices) {
                val selectedPoint = points[selectedIndex]

                // Vertical indicator line
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(selectedPoint.x, paddingTop),
                    end = Offset(selectedPoint.x, paddingTop + plotHeight),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Concentric circles at the selected data node
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 14f,
                    center = selectedPoint
                )
                drawCircle(
                    color = if (isDark) Color(0xFF1E2028) else Color.White,
                    radius = 7.5f,
                    center = selectedPoint
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5f,
                    center = selectedPoint
                )

                // Value floating pill tag above selected point
                val valStr = "$currencySymbol%,.0f".format(data[selectedIndex].totalSpent)
                val bubblePaint = Paint().apply {
                    color = primaryColor.toArgb()
                    textSize = density.run { 10.sp.toPx() }
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val bubbleY = (selectedPoint.y - 14f).coerceAtLeast(paddingTop + 10f)

                drawContext.canvas.nativeCanvas.drawText(
                    valStr,
                    selectedPoint.x,
                    bubbleY,
                    bubblePaint
                )
            }
        }
    }
}

private fun stepX(count: Int, plotWidth: Float): Float {
    return if (count > 1) plotWidth / (count - 1) else plotWidth
}

/**
 * Draws smooth Cubic Bezier Spline with glowing stroke and soft vertical gradient area.
 */
private fun DrawScope.drawSplineCurve(
    points: List<Offset>,
    paddingTop: Float,
    plotHeight: Float,
    paddingStart: Float,
    primaryColor: Color,
    isDark: Boolean
) {
    if (points.isEmpty()) return

    val strokePath = Path()
    val fillPath = Path()

    strokePath.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, paddingTop + plotHeight)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]

        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

        strokePath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )
        fillPath.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p1.x, p1.y
        )
    }

    fillPath.lineTo(points.last().x, paddingTop + plotHeight)
    fillPath.close()

    // 1. Draw gradient area under the curve
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                primaryColor.copy(alpha = 0.02f)
            ),
            startY = paddingTop,
            endY = paddingTop + plotHeight
        )
    )

    // 2. Draw smooth stroke curve
    drawPath(
        path = strokePath,
        color = primaryColor,
        style = Stroke(
            width = 3.5f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 3. Draw small anchor dots at every data point
    points.forEach { pt ->
        drawCircle(
            color = if (isDark) Color(0xFF1B1D25) else Color.White,
            radius = 3.5f,
            center = pt
        )
        drawCircle(
            color = primaryColor,
            radius = 2.5f,
            center = pt
        )
    }
}

/**
 * Draws modern rounded bars with active selection highlight.
 */
private fun DrawScope.drawRoundedBars(
    points: List<Offset>,
    data: List<TrendPeriodData>,
    paddingTop: Float,
    plotHeight: Float,
    stepX: Float,
    primaryColor: Color,
    selectedIndex: Int
) {
    val barWidth = (stepX * 0.65f).coerceIn(12f, 32f)

    points.forEachIndexed { index, pt ->
        val isSelected = index == selectedIndex
        val barHeight = (paddingTop + plotHeight) - pt.y
        val left = pt.x - barWidth / 2f
        val top = pt.y

        val barColor = if (isSelected) {
            primaryColor
        } else {
            primaryColor.copy(alpha = 0.45f)
        }

        drawRoundRect(
            color = barColor,
            topLeft = Offset(left, top),
            size = Size(barWidth, barHeight.coerceAtLeast(3f)),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}

/**
 * Calculates a friendly ceiling milestone for Y axis.
 */
private fun calculateYCeiling(maxVal: Double): Float {
    if (maxVal <= 0) return 100f
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(maxVal))).toFloat()
    val ratio = (maxVal / magnitude).toFloat()

    val factor = when {
        ratio <= 1.0f -> 1.0f
        ratio <= 2.0f -> 2.0f
        ratio <= 2.5f -> 2.5f
        ratio <= 5.0f -> 5.0f
        else -> 10.0f
    }
    return factor * magnitude * 1.15f
}

/**
 * Aggregates transactions into discrete calendar weeks (Monday - Sunday).
 */
private fun calculateWeeklyTrends(
    expenses: List<Expense>,
    incomes: List<Expense>,
    numWeeks: Int
): List<TrendPeriodData> {
    val result = mutableListOf<TrendPeriodData>()
    val sdfDate = SimpleDateFormat("MMM d", Locale.getDefault())
    val sdfFull = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    val now = Calendar.getInstance()

    // Align to the end of the current week (Sunday 23:59:59)
    val curCal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    // Generate buckets going back numWeeks
    val buckets = mutableListOf<Triple<Long, Long, String>>()
    for (i in (numWeeks - 1) downTo 0) {
        val weekCal = (curCal.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, -i)
        }
        val endMs = weekCal.timeInMillis

        val startCal = (weekCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis
        val label = if (i == 0) "This Wk" else if (i == 1) "Last Wk" else "W-${i}"
        buckets.add(Triple(startMs, endMs, label))
    }

    var prevSpent: Double? = null

    buckets.forEachIndexed { index, (startMs, endMs, label) ->
        val weekExpenses = expenses.filter { it.date in startMs..endMs }
        val weekIncomes = incomes.filter { it.date in startMs..endMs }

        val spent = weekExpenses.sumOf { it.amount }
        val inc = weekIncomes.sumOf { it.amount }
        val topCat = weekExpenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key

        val prevW = prevSpent
        val changePct = if (prevW != null && prevW > 0) {
            ((spent - prevW) / prevW) * 100.0
        } else null
        prevSpent = spent

        val startStr = sdfDate.format(Date(startMs))
        val endStr = sdfDate.format(Date(endMs))

        result.add(
            TrendPeriodData(
                id = "week_$index",
                title = "$startStr – $endStr",
                shortLabel = label,
                subLabel = "$startStr to $endStr",
                startDateMs = startMs,
                endDateMs = endMs,
                totalSpent = spent,
                totalIncome = inc,
                count = weekExpenses.size,
                topCategory = topCat,
                expenses = weekExpenses,
                changePercent = changePct
            )
        )
    }

    return result
}

/**
 * Aggregates transactions into discrete calendar months.
 */
private fun calculateMonthlyTrends(
    expenses: List<Expense>,
    incomes: List<Expense>,
    numMonths: Int
): List<TrendPeriodData> {
    val result = mutableListOf<TrendPeriodData>()
    val sdfShort = SimpleDateFormat("MMM", Locale.getDefault())
    val sdfTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val curCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val buckets = mutableListOf<Triple<Long, Long, String>>()
    for (i in (numMonths - 1) downTo 0) {
        val cal = (curCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -i)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val endMs = cal.timeInMillis

        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = startCal.timeInMillis
        val shortName = sdfShort.format(startCal.time)
        val title = sdfTitle.format(startCal.time)
        buckets.add(Triple(startMs, endMs, shortName))
    }

    var prevSpent: Double? = null

    buckets.forEachIndexed { index, (startMs, endMs, shortName) ->
        val monthExpenses = expenses.filter { it.date in startMs..endMs }
        val monthIncomes = incomes.filter { it.date in startMs..endMs }

        val spent = monthExpenses.sumOf { it.amount }
        val inc = monthIncomes.sumOf { it.amount }
        val topCat = monthExpenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key

        val prevM = prevSpent
        val changePct = if (prevM != null && prevM > 0) {
            ((spent - prevM) / prevM) * 100.0
        } else null
        prevSpent = spent

        val monthTitle = sdfTitle.format(Date(startMs))

        result.add(
            TrendPeriodData(
                id = "month_$index",
                title = monthTitle,
                shortLabel = shortName,
                subLabel = monthTitle,
                startDateMs = startMs,
                endDateMs = endMs,
                totalSpent = spent,
                totalIncome = inc,
                count = monthExpenses.size,
                topCategory = topCat,
                expenses = monthExpenses,
                changePercent = changePct
            )
        )
    }

    return result
}
