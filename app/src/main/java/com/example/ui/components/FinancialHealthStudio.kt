package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppIcons
import com.example.ui.FinanceViewModel
import com.example.ui.HealthScoreBreakdown
import com.example.ui.theme.*

/**
 * 🧠 FINANCIAL HEALTH SCORE STUDIO
 * Provides an interactive 0-100 diagnostic score with 4 pillars,
 * actionable insights, and an interactive "What-If" simulator.
 */
@Composable
fun FinancialHealthStudioDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onNavigateToBudget: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()

    var simulatedSavingsBoost by remember { mutableFloatStateOf(0f) }
    var simulatedExpenseCut by remember { mutableFloatStateOf(0f) }

    val baselineScore = remember { viewModel.getFinancialHealthScore(0.0, 0.0) }
    val currentHealth = remember(simulatedSavingsBoost, simulatedExpenseCut) {
        viewModel.getFinancialHealthScore(
            simulatedMonthlySavingsBoost = simulatedSavingsBoost.toDouble(),
            simulatedExpenseCut = simulatedExpenseCut.toDouble()
        )
    }

    val animatedScoreRatio by animateFloatAsState(
        targetValue = (currentHealth.overallScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "healthScoreProgress"
    )

    val ratingColor = Color(currentHealth.ratingColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = SleekShapes.xxl,
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🧠", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Financial Health Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "On-device wellness diagnostics",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                // Central Gauge Card
                Surface(
                    shape = SleekShapes.xl,
                    color = SleekBg,
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = ratingColor.copy(alpha = 0.15f),
                                    style = Stroke(width = 12.dp.toPx())
                                )
                                drawArc(
                                    color = ratingColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedScoreRatio,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${currentHealth.overallScore}",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ratingColor
                                )
                                Text(
                                    text = "/ 100",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ratingColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = currentHealth.ratingTitle.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ratingColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        if (simulatedSavingsBoost > 0 || simulatedExpenseCut > 0) {
                            val ptsGain = currentHealth.overallScore - baselineScore.overallScore
                            Text(
                                text = if (ptsGain >= 0) "📈 +$ptsGain points gained from simulated habits!" else "$ptsGain points",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                // 4 Pillars Breakdown
                Text(
                    text = "Core Wellness Pillars",
                    fontSize = SleekSizes.textBody,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    HealthPillarItem(
                        icon = "🎯",
                        title = "Budget Discipline",
                        weight = "30% weight",
                        score = currentHealth.budgetScore,
                        subtitle = "Consistency staying within monthly limits"
                    )
                    HealthPillarItem(
                        icon = "🛡️",
                        title = "Emergency Runway",
                        weight = "25% weight",
                        score = currentHealth.runwayScore,
                        subtitle = "Covers ${"%.1f".format(currentHealth.runwayMonths)} months of average living expenses"
                    )
                    HealthPillarItem(
                        icon = "💰",
                        title = "Savings Velocity",
                        weight = "25% weight",
                        score = currentHealth.savingsScore,
                        subtitle = "${currentHealth.savingsRatePct}% income allocated to goals"
                    )
                    HealthPillarItem(
                        icon = "⚖️",
                        title = "Expense Stability",
                        weight = "20% weight",
                        score = currentHealth.stabilityScore,
                        subtitle = "Day-to-day spending volatility control"
                    )
                }

                // Interactive "What-If" Simulator
                Surface(
                    shape = SleekShapes.lg,
                    color = SleekPrimaryContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🧪", fontSize = 18.sp)
                            Text(
                                text = "What-If Financial Simulator",
                                fontSize = SleekSizes.textBodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                        }

                        // Slider 1: Save More
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Monthly Goal Contribution Boost:",
                                    fontSize = SleekSizes.textCaption,
                                    color = SleekTextSecondary
                                )
                                Text(
                                    text = "+%s%,.0f".format(currencySymbol, simulatedSavingsBoost),
                                    fontSize = SleekSizes.textCaption,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                            }
                            Slider(
                                value = simulatedSavingsBoost,
                                onValueChange = {
                                    simulatedSavingsBoost = it
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                valueRange = 0f..1000f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = SleekPrimary,
                                    activeTrackColor = SleekPrimary
                                )
                            )
                        }

                        // Slider 2: Cut Spend
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cut Discretionary Spend:",
                                    fontSize = SleekSizes.textCaption,
                                    color = SleekTextSecondary
                                )
                                Text(
                                    text = "-%s%,.0f".format(currencySymbol, simulatedExpenseCut),
                                    fontSize = SleekSizes.textCaption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Slider(
                                value = simulatedExpenseCut,
                                onValueChange = {
                                    simulatedExpenseCut = it
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                valueRange = 0f..500f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF10B981)
                                )
                            )
                        }

                        if (simulatedSavingsBoost > 0 || simulatedExpenseCut > 0) {
                            TextButton(
                                onClick = {
                                    simulatedSavingsBoost = 0f
                                    simulatedExpenseCut = 0f
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Reset Simulator", fontSize = 11.sp, color = SleekTextSecondary)
                            }
                        }
                    }
                }

                // Actionable Insights
                if (currentHealth.insights.isNotEmpty()) {
                    Text(
                        text = "Actionable Steps",
                        fontSize = SleekSizes.textBody,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        currentHealth.insights.forEach { insight ->
                            Surface(
                                shape = SleekShapes.md,
                                color = SleekBg,
                                border = BorderStroke(1.dp, SleekBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = insight,
                                    fontSize = SleekSizes.textCaption,
                                    color = SleekTextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigateToBudget()
                        },
                        shape = SleekShapes.md,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Manage Budget", fontSize = SleekSizes.textCaption)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToGoals()
                        },
                        shape = SleekShapes.md,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Boost Goals", fontSize = SleekSizes.textCaption, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthPillarItem(
    icon: String,
    title: String,
    weight: String,
    score: Int,
    subtitle: String
) {
    val barColor = when {
        score >= 80 -> Color(0xFF10B981)
        score >= 60 -> Color(0xFF6366F1)
        score >= 40 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Surface(
        shape = SleekShapes.lg,
        color = SleekBg,
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = icon, fontSize = 16.sp)
                    Text(
                        text = title,
                        fontSize = SleekSizes.textBodySmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "• $weight",
                        fontSize = SleekSizes.textMicro,
                        color = SleekTextSecondary
                    )
                }

                Text(
                    text = "$score / 100",
                    fontSize = SleekSizes.textBodySmall,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = SleekBorder.copy(alpha = 0.5f)
            )

            Text(
                text = subtitle,
                fontSize = SleekSizes.textMicro,
                color = SleekTextSecondary
            )
        }
    }
}
