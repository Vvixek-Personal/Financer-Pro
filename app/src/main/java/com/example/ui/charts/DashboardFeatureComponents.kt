package com.example.ui

import com.example.ui.theme.*
import android.widget.Toast
import com.example.data.Money
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Expense
import com.example.data.SavingsGoal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * ⚡ Quick Action Hub for Home Screen
 * 1-tap shortcuts for immediate financial operations
 */
@Composable
fun DashboardQuickActionHub(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onQuickSplit: () -> Unit,
    onQuickConvert: () -> Unit,
    onViewGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = SleekShapes.xl,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                icon = AppIcons.TrendingDown,
                label = "Expense",
                tintColor = ExpenseRed,
                bgColor = ExpenseRedBg,
                onClick = onAddExpense,
                testTag = "home_quick_add_expense"
            )

            QuickActionButton(
                icon = AppIcons.TrendingUp,
                label = "Income",
                tintColor = IncomeGreen,
                bgColor = IncomeGreenBg,
                onClick = onAddIncome,
                testTag = "home_quick_add_income"
            )

            QuickActionButton(
                icon = AppIcons.CallSplit,
                label = "Split",
                tintColor = SleekPrimary,
                bgColor = SleekPrimaryContainer.copy(alpha = 0.35f),
                onClick = onQuickSplit,
                testTag = "home_quick_split_bill"
            )

            QuickActionButton(
                icon = AppIcons.CurrencyExchange,
                label = "Convert",
                tintColor = SleekPrimary,
                bgColor = SleekPrimaryContainer.copy(alpha = 0.35f),
                onClick = onQuickConvert,
                testTag = "home_quick_convert"
            )

            QuickActionButton(
                icon = AppIcons.Savings,
                label = "Goals",
                tintColor = WarningOrange,
                bgColor = WarningOrange.copy(alpha = 0.12f),
                onClick = onViewGoals,
                testTag = "home_quick_goals"
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tintColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .springPressEffect(scaleDown = 0.92f)
            .clip(SleekShapes.chip)
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(SleekShapes.chip)
                .background(bgColor)
                .border(BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)), SleekShapes.chip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(SleekSizes.iconMedium)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = SleekSizes.textCaption,
            fontWeight = FontWeight.Normal,
            color = SleekTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 📅 Safe-to-Spend Today / Daily Spending Allowance Widget
 * Calculates the exact safe daily spending budget based on remaining monthly budget and remaining days.
 */
@Composable
fun DailySpendingAllowanceWidget(
    monthlyBudget: Double,
    currentMonthExpense: Double,
    todayExpense: Double,
    currencySymbol: String,
    onAdjustBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = remember { Calendar.getInstance() }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val currentDay = remember { cal.get(Calendar.DAY_OF_MONTH) }
    val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)

    val remainingMonthlyBudget = (monthlyBudget - currentMonthExpense).coerceAtLeast(0.0)
    val safeDailyAllowance = if (monthlyBudget > 0) {
        (remainingMonthlyBudget / remainingDays).coerceAtLeast(0.0)
    } else 0.0

    val remainingToday = (safeDailyAllowance - todayExpense).coerceAtLeast(0.0)
    val isOverToday = monthlyBudget > 0 && todayExpense > safeDailyAllowance
    val todayProgress = if (safeDailyAllowance > 0) {
        (todayExpense / safeDailyAllowance).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_daily_allowance_card")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(SleekPrimaryContainer.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Today,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Daily spending pace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "$remainingDays days remaining this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (monthlyBudget > 0) {
                    Surface(
                        shape = SleekShapes.chip,
                        color = if (isOverToday) ExpenseRedBg else IncomeGreenBg,
                        border = BorderStroke(1.dp, if (isOverToday) ExpenseRed.copy(alpha = 0.3f) else IncomeGreen.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (isOverToday) "Over target" else "On track",
                            color = if (isOverToday) ExpenseRed else IncomeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    TextButton(
                        onClick = onAdjustBudget,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Set budget", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (monthlyBudget > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Safe to spend today",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, safeDailyAllowance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isOverToday) "Over budget by" else "Remaining for today",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverToday) ExpenseRed else SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isOverToday) {
                                String.format(Locale.getDefault(), "+%s%,.2f", currencySymbol, todayExpense - safeDailyAllowance)
                            } else {
                                String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, remainingToday)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverToday) ExpenseRed else IncomeGreen
                        )
                    }
                }

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { todayProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(SleekShapes.chip),
                        color = if (isOverToday) ExpenseRed else SleekPrimary,
                        trackColor = SleekBorder.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spent: ${String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, todayExpense)}",
                            fontSize = 12.sp,
                            color = SleekTextSecondary
                        )
                        Text(
                            text = "Target: ${String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, safeDailyAllowance)}/day",
                            fontSize = 12.sp,
                            color = SleekTextSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = "Configure a monthly budget target to unlock smart daily spending allowances and stay on track.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }
        }
    }
}

/**
 * 🧾 Upcoming Bills Widget for Home Screen
 * Displays upcoming or overdue bills with 1-tap "Pay Now" action
 */
@Composable
fun UpcomingBillsDashboardWidget(
    bills: List<BillEntry>,
    currencySymbol: String,
    onPayBill: (BillEntry) -> Unit,
    onViewAllBills: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_upcoming_bills_widget")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(WarningOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Receipt,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Upcoming bills",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = if (bills.isNotEmpty()) "${bills.size} pending due" else "No pending dues",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = onViewAllBills,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("View all", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            if (bills.isEmpty()) {
                Surface(
                    shape = SleekShapes.chip,
                    color = IncomeGreenBg,
                    border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "All caught up. No pending bills.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bills.take(3).forEach { bill ->
                        val isDueToday = bill.dueDate == todayStr
                        Surface(
                            shape = SleekShapes.chip,
                            color = if (isDueToday) WarningOrange.copy(alpha = 0.08f) else SleekBg,
                            border = BorderStroke(1.dp, if (isDueToday) WarningOrange.copy(alpha = 0.35f) else SleekBorder),
                            modifier = Modifier.fillMaxWidth()
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isDueToday) WarningOrange.copy(alpha = 0.15f) else SleekSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Payment,
                                            contentDescription = null,
                                            tint = if (isDueToday) WarningOrange else SleekPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = bill.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SleekTextPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isDueToday) "Due today" else "Due ${bill.dueDate}",
                                                fontSize = 12.sp,
                                                color = if (isDueToday) WarningOrange else SleekTextSecondary,
                                                fontWeight = if (isDueToday) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, bill.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SleekTextPrimary
                                    )

                                    Button(
                                        onClick = { onPayBill(bill) },
                                        shape = SleekShapes.chip,
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🎯 Savings Goals Spotlight Widget for Home Screen
 * Displays the top active savings goal with progress & 1-tap quick deposit
 */
@Composable
fun SavingsGoalSpotlightWidget(
    goals: List<SavingsGoal>,
    currencySymbol: String,
    onQuickDeposit: (SavingsGoal) -> Unit,
    onViewAllGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (goals.isEmpty()) return

    val topGoal = remember(goals) {
        goals.firstOrNull { it.currentAmount < it.targetAmount } ?: goals.first()
    }
    val progressFraction = remember(topGoal) {
        if (topGoal.targetAmount > 0) {
            (topGoal.currentAmount / topGoal.targetAmount).toFloat().coerceIn(0f, 1f)
        } else 0f
    }
    val percentInt = remember(progressFraction) { (progressFraction * 100).roundToInt() }

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_savings_goal_spotlight")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(WarningOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Savings,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = topGoal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Savings target • $percentInt% saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = onViewAllGoals,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("All goals", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Saved so far",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, topGoal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = WarningOrange
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Goal target",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, topGoal.targetAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(SleekShapes.chip),
                color = WarningOrange,
                trackColor = SleekBorder.copy(alpha = 0.5f)
            )

            // Quick Deposit Button
            FilledTonalButton(
                onClick = { onQuickDeposit(topGoal) },
                shape = SleekShapes.chip,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = WarningOrange.copy(alpha = 0.12f),
                    contentColor = WarningOrange
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(AppIcons.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deposit to goal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

/**
 * 📊 Monthly Top Spending Categories Widget
 * Visual breakdown of top categories this month with progress bars
 */
@Composable
fun TopSpendingCategoriesWidget(
    expensesThisMonth: List<Expense>,
    categoryIcons: Map<String, String>,
    currencySymbol: String,
    onNavigateToAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (expensesThisMonth.isEmpty()) return

    val totalSpent = remember(expensesThisMonth) {
        expensesThisMonth.sumOf { it.amount }.coerceAtLeast(1.0)
    }

    val topCategories = remember(expensesThisMonth) {
        expensesThisMonth
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                cat to sum
            }
            .sortedByDescending { it.second }
            .take(4)
    }

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_top_categories_widget")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(SleekPrimaryContainer.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Category,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Top spending this month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Category breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToAnalytics,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Analytics", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topCategories.forEachIndexed { index, (category, amount) ->
                    val pct = ((amount / totalSpent) * 100).coerceIn(0.0, 100.0)
                    val catColor = if (index == 0) ExpenseRed else SleekPrimary

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = SleekTextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "(${pct.roundToInt()}%)",
                                    fontSize = 12.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (pct / 100f).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(SleekShapes.chip),
                            color = catColor,
                            trackColor = SleekBorder.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 💡 Daily Smart Financial Insight / Health Card
 * Contextual tips and feedback computed from real financial data
 */
@Composable
fun DailyFinancialInsightWidget(
    thisMonthIncome: Double,
    thisMonthExpense: Double,
    monthlyBudget: Double,
    streakCount: Int,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(false) }
    if (isDismissed) return

    val insightMessage = remember(thisMonthIncome, thisMonthExpense, monthlyBudget, streakCount) {
        when {
            monthlyBudget > 0 && thisMonthExpense > monthlyBudget -> {
                "Monthly budget exceeded by ${currencySymbol}${String.format(Locale.getDefault(), "%,.0f", thisMonthExpense - monthlyBudget)}. Review discretionary categories to rebalance."
            }
            monthlyBudget > 0 && (thisMonthExpense / monthlyBudget) >= 0.85 -> {
                "You have used ${((thisMonthExpense / monthlyBudget) * 100).roundToInt()}% of your budget for this month."
            }
            thisMonthIncome > 0 && (thisMonthIncome - thisMonthExpense) > 0 -> {
                val savingsRate = (((thisMonthIncome - thisMonthExpense) / thisMonthIncome) * 100).roundToInt()
                "Net savings rate is currently $savingsRate% of monthly income."
            }
            streakCount >= 3 -> {
                "Tracking streak active: $streakCount consecutive days logged."
            }
            else -> {
                "Log transactions regularly to maintain clear visibility into your cash flow."
            }
        }
    }

    Surface(
        shape = SleekShapes.card,
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(SleekShapes.chip)
                    .background(SleekPrimaryContainer.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Lightbulb,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = insightMessage,
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextPrimary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { isDismissed = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Dismiss",
                    tint = SleekTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * ⚡ Quick Deposit Dialog for Home Screen
 */
@Composable
fun QuickGoalDepositDialog(
    goal: SavingsGoal,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmDeposit: (Double) -> Unit
) {
    var depositText by remember { mutableStateOf("500") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Deposit to ${goal.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                Text(
                    text = "Target: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", goal.targetAmount)} | Saved: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", goal.currentAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )

                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Deposit Amount") },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("100", "500", "1000", "2000").forEach { preset ->
                        FilledTonalButton(
                            onClick = { depositText = preset },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+$preset", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SleekTextSecondary)
                    }

                    Button(
                        onClick = {
                            val amt = depositText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirmDeposit(amt)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deposit", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * ⚡ Quick Split Bill Dialog directly accessible on Home Screen
 */
@Composable
fun QuickSplitBillDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onRecordExpense: (Double, String) -> Unit
) {
    var billText by remember { mutableStateOf("1200") }
    var tipPercent by remember { mutableIntStateOf(10) }
    var peopleCount by remember { mutableIntStateOf(3) }

    val bill = billText.toDoubleOrNull() ?: 0.0
    val tip = (bill * tipPercent) / 100.0
    val total = bill + tip
    val perPerson = if (peopleCount > 0) total / peopleCount else 0.0

    Dialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Quick Split Bill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                // Summary result box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Per Person Share", fontSize = 11.sp, color = SleekTextSecondary)
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.2f", currencySymbol, perPerson),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                        Text(
                            text = "Total: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", total)} (Tip: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", tip)})",
                            fontSize = 11.sp,
                            color = SleekTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = billText,
                    onValueChange = { billText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Total Bill Amount") },
                    leadingIcon = { Text(currencySymbol, fontWeight = FontWeight.Bold, color = SleekPrimary, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // People counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split between:", style = MaterialTheme.typography.bodyMedium, color = SleekTextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (peopleCount > 1) peopleCount-- },
                            modifier = Modifier.size(32.dp).background(SleekBg, CircleShape)
                        ) {
                            Icon(AppIcons.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                        }
                        Text("$peopleCount People", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(
                            onClick = { peopleCount++ },
                            modifier = Modifier.size(32.dp).background(SleekBg, CircleShape)
                        ) {
                            Icon(AppIcons.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Tip Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 5, 10, 15, 20).forEach { tip ->
                        FilterChip(
                            selected = tipPercent == tip,
                            onClick = { tipPercent = tip },
                            label = { Text("$tip% Tip", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        onRecordExpense(perPerson, "Split bill ($peopleCount people)")
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(AppIcons.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record My Share as Expense")
                }
            }
        }
    }
}

/**
 * 💱 Quick Currency Convert Dialog directly accessible on Home Screen
 */
@Composable
fun QuickCurrencyConvertDialog(
    currencyCode: String,
    onDismiss: () -> Unit,
    onOpenFullSettings: () -> Unit
) {
    var amountText by remember { mutableStateOf("100") }
    var targetCode by remember { mutableStateOf(if (currencyCode == "USD") "EUR" else "USD") }

    val parsedMinor = remember(amountText, currencyCode) {
        Money.parse(amountText, currencyCode) ?: 0L
    }
    val targetCurrency = remember(targetCode) { CurrencyManager.getByCode(targetCode) }
    val sourceCurrency = remember(currencyCode) { CurrencyManager.getByCode(currencyCode) }

    val conversionResult = remember(parsedMinor, currencyCode, targetCode) {
        Money.tryConvert(parsedMinor, currencyCode, targetCode)
    }

    Dialog(onDismissRequest = onDismiss) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💱 Quick Converter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${targetCurrency.flag} ${targetCurrency.name}", fontSize = 12.sp, color = SleekTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        when (conversionResult) {
                            is Money.ConversionResult.Success -> {
                                Text(
                                    text = Money.format(conversionResult.amountMinor, targetCode),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                                Text(
                                    text = if (conversionResult.timestamp > 0L) {
                                        "${Money.format(parsedMinor, currencyCode)} (Live rate: ${String.format(Locale.US, "%.4f", conversionResult.rateUsed)})"
                                    } else {
                                        "${Money.format(parsedMinor, currencyCode)} (Reference estimate — refresh rates for current value)"
                                    },
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                            }
                            is Money.ConversionResult.MissingRate -> {
                                Text(
                                    text = "Rate unavailable",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                            }
                            is Money.ConversionResult.InvalidRate -> {
                                Text(
                                    text = "Invalid exchange rate",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                            }
                            is Money.ConversionResult.InvalidCurrency -> {
                                Text(
                                    text = "Unsupported currency",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                            }
                            Money.ConversionResult.Overflow -> {
                                Text(
                                    text = "Amount is too large to convert",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount in $currencyCode") },
                    leadingIcon = { Text(sourceCurrency.symbol, fontWeight = FontWeight.Bold, color = SleekPrimary, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick target currency chips
                Text("Convert into:", fontSize = 12.sp, color = SleekTextSecondary, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("USD", "EUR", "GBP", "INR", "AED", "JPY").filter { it != currencyCode }.take(4).forEach { code ->
                        val item = CurrencyManager.getByCode(code)
                        FilterChip(
                            selected = targetCode == code,
                            onClick = { targetCode = code },
                            label = { Text("${item.flag} $code", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenFullSettings()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("100+ More", fontSize = 12.sp, color = SleekPrimary)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
     * 🎯 Interactive Savings Goal Mini-Carousel Widget
     */
@Composable
fun SavingsGoalsMiniCarouselWidget(
    goals: List<SavingsGoal>,
    currencySymbol: String,
    onQuickDeposit: (SavingsGoal, Double) -> Unit,
    onViewAllGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (goals.isEmpty()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = modifier
                .fillMaxWidth()
                .clickable { onViewAllGoals() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = "Set Financial Goals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Track targets, deadlines & allocations",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                    }
                }
                Button(
                    onClick = onViewAllGoals,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Create", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Savings,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Active Savings Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            }
            TextButton(onClick = onViewAllGoals) {
                Text("View All", color = SleekPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(goals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val pct = (progress * 100).toInt()

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .width(240.dp)
                        .testTag("savings_goal_carousel_card_\${goal.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(goal.iconTag.ifEmpty { "🎯" }, fontSize = 16.sp)
                                }
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(110.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(String.format("%s%,.0f", currencySymbol, goal.currentAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Text(String.format("of %s%,.0f", currencySymbol, goal.targetAmount), style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFF59E0B),
                                trackColor = SleekBorder.copy(alpha = 0.5f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onQuickDeposit(goal, 50.0) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                            ) {
                                Text("+50", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                            OutlinedButton(
                                onClick = { onQuickDeposit(goal, 100.0) },
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                            ) {
                                Text("+100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ⚡ Smart Budget Health Gauge Ring Widget
 */
@Composable
fun SmartBudgetHealthGaugeWidget(
    monthlyBudget: Double,
    currentMonthExpense: Double,
    currencySymbol: String,
    onUpdateBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (monthlyBudget <= 0) return

    val spentPct = ((currentMonthExpense / monthlyBudget) * 100).coerceAtLeast(0.0)
    val remaining = monthlyBudget - currentMonthExpense
    val isExceeded = remaining < 0
    val progressFraction = (currentMonthExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)
    val animatedProgressFraction by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = appTween(800),
        label = "budget_gauge_sweep"
    )

    val gaugeColor = when {
        isExceeded -> ExpenseRed
        spentPct >= 80 -> WarningOrange
        else -> IncomeGreen
    }

    val statusText = when {
        isExceeded -> "Budget exceeded"
        spentPct >= 90 -> "Critical (90%+ spent)"
        spentPct >= 80 -> "Warning (80%+ spent)"
        else -> "On track"
    }

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_budget_health_gauge")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(gaugeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Speed,
                            contentDescription = null,
                            tint = gaugeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Budget pace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = gaugeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = onUpdateBudget,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Adjust", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Spent this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format("%s%,.2f", currencySymbol, currentMonthExpense),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = String.format("Limit: %s%,.2f (%d%%)", currencySymbol, monthlyBudget, spentPct.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        drawArc(
                            color = SleekBorder,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        drawArc(
                            color = gaugeColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgressFraction,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${spentPct.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Used",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🌟 Dynamic Financial Health Score & Diagnostics Widget
 * Calculates 0-100 real-time score from Savings Rate, Budget Adherence, Bill Promptness, and Liquidity
 */
@Composable
fun FinancialHealthScoreWidget(
    incomeThisMonth: Double,
    expenseThisMonth: Double,
    monthlyBudget: Double,
    overdueBillsCount: Int,
    currencySymbol: String,
    onExploreBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDiagnostics by remember { mutableStateOf(false) }

    // Dynamic score calculation
    val savingsRatio = if (incomeThisMonth > 0) ((incomeThisMonth - expenseThisMonth) / incomeThisMonth).coerceIn(0.0, 1.0) else 0.4
    val budgetScore = if (monthlyBudget > 0) {
        val spentRatio = expenseThisMonth / monthlyBudget
        when {
            spentRatio <= 0.7 -> 1.0
            spentRatio <= 0.9 -> 0.8
            spentRatio <= 1.0 -> 0.6
            else -> 0.2
        }
    } else 0.7

    val billScore = if (overdueBillsCount == 0) 1.0 else (1.0 - overdueBillsCount * 0.25).coerceAtLeast(0.0)
    val overallScore = ((savingsRatio * 40) + (budgetScore * 40) + (billScore * 20)).roundToInt().coerceIn(15, 99)

    val (badgeText, badgeColor, tipText) = when {
        overallScore >= 80 -> Triple("Excellent", IncomeGreen, "Consistent savings and spending within limits.")
        overallScore >= 65 -> Triple("Stable", SleekPrimary, "Balanced spending with active monthly savings.")
        overallScore >= 45 -> Triple("Moderate", WarningOrange, "Review discretionary spending to preserve budget margin.")
        else -> Triple("Action needed", ExpenseRed, "Expenses or overdue dues exceed recommended targets.")
    }

    val animatedScore by animateIntAsState(
        targetValue = overallScore,
        animationSpec = appTween(750, AppleDecelerateEasing),
        label = "health_score"
    )

    val sweepProgress by animateFloatAsState(
        targetValue = overallScore / 100f,
        animationSpec = appTween(850, AppleDecelerateEasing),
        label = "gauge_sweep"
    )

    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_financial_health_score")
    ) {
        Column(
            modifier = Modifier.padding(SleekSpacing.cardPadding),
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
                            .size(36.dp)
                            .clip(SleekShapes.chip)
                            .background(badgeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.HealthAndSafety,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Financial wellness",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Health diagnostic",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = SleekShapes.chip,
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$animatedScore",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor
                        )
                        Text(
                            text = " / 100",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        drawArc(
                            color = SleekBorder,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        drawArc(
                            color = badgeColor,
                            startAngle = 135f,
                            sweepAngle = 270f * sweepProgress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Icon(
                        imageVector = AppIcons.AutoGraph,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = SleekBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDiagnostics = !expandedDiagnostics },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expandedDiagnostics) "Hide details" else "View breakdown",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekPrimary
                )
                Icon(
                    imageVector = if (expandedDiagnostics) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expandedDiagnostics,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthPillarRow("Savings efficiency", "${(savingsRatio * 100).toInt()}% net saved", if (savingsRatio >= 0.2) IncomeGreen else WarningOrange)
                    HealthPillarRow("Budget utilization", if (monthlyBudget > 0) "${((expenseThisMonth / monthlyBudget) * 100).toInt()}% utilized" else "No limit set", if (budgetScore >= 0.8) IncomeGreen else ExpenseRed)
                    HealthPillarRow("Bill punctuality", if (overdueBillsCount == 0) "100% on time" else "$overdueBillsCount overdue bill(s)", if (overdueBillsCount == 0) IncomeGreen else ExpenseRed)
                }
            }
        }
    }
}
@Composable
private fun HealthPillarRow(title: String, value: String, indicatorColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SleekShapes.chip)
            .background(SleekBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(indicatorColor))
            Text(title, style = MaterialTheme.typography.bodySmall, color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Normal)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = indicatorColor)
    }
}
