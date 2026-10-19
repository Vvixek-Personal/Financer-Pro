package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom Canvas component that renders an interactive pie/donut chart showing
 * total expenses broken down by category.
 */
@Composable
fun CategoryExpensePieChart(
    categoryExpenses: Map<String, Double>,
    categoryColors: Map<String, Color>,
    modifier: Modifier = Modifier,
    chartRadius: Dp = 210.dp,
    sliceBorderColor: Color = SleekSurface
) {
    var isDonutMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val animEnabled = LocalAnimationsEnabled.current
    var animationPlayed by remember { mutableStateOf(!animEnabled) }

    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed || !animEnabled) 1f else 0f,
        animationSpec = appTween(durationMs = 800, easing = AppleDecelerateEasing),
        label = "pie_chart_sweep_anim"
    )

    LaunchedEffect(categoryExpenses, animEnabled) {
        if (animEnabled) {
            animationPlayed = false
            kotlinx.coroutines.delay(30)
            animationPlayed = true
        } else {
            animationPlayed = true
        }
    }

    val totalExpense = remember(categoryExpenses) {
        categoryExpenses.values.sum()
    }

    // Filter out categories with 0 or negative expense
    val validExpenses = remember(categoryExpenses) {
        categoryExpenses.filter { it.value > 0 }
    }

    // Distinct, vibrant palette for pie/donut chart sections
    val chartSlicePalette = remember {
        listOf(
            Color(0xFFF97316), // Vivid Orange
            Color(0xFF06B6D4), // Bright Cyan
            Color(0xFF8B5CF6), // Purple / Violet
            Color(0xFFEC4899), // Hot Pink
            Color(0xFF10B981), // Emerald Green
            Color(0xFF3B82F6), // Royal Blue
            Color(0xFFF59E0B), // Amber Gold
            Color(0xFF6366F1), // Indigo
            Color(0xFF14B8A6), // Teal
            Color(0xFFE11D48), // Crimson Rose
            Color(0xFF84CC16), // Lime Green
            Color(0xFF0284C7), // Sky Blue
            Color(0xFFA855F7), // Deep Purple
            Color(0xFFD97706), // Warm Coral
            Color(0xFF059669), // Mint Green
            Color(0xFFD946EF), // Fuchsia
            Color(0xFF22C55E), // Fresh Green
            Color(0xFFEAB308), // Sunflower
            Color(0xFF64748B), // Slate Blue
            Color(0xFF38BDF8)  // Light Sky
        )
    }

    val resolvedCategoryColors = remember(validExpenses, categoryColors) {
        val map = mutableMapOf<String, Color>()
        val usedColors = mutableSetOf<Color>()
        
        validExpenses.keys.forEachIndexed { index, cat ->
            val colorFromMap = categoryColors[cat]
            // If categoryColors has a preset color and it hasn't been assigned yet, use it; otherwise pick next unused from palette
            val assignedColor = if (colorFromMap != null && colorFromMap != SleekPrimary && !usedColors.contains(colorFromMap)) {
                colorFromMap
            } else {
                chartSlicePalette.firstOrNull { !usedColors.contains(it) } 
                    ?: chartSlicePalette[index % chartSlicePalette.size]
            }
            map[cat] = assignedColor
            usedColors.add(assignedColor)
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_expense_pie_chart_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Chart Controls Row (Pie vs Donut Toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Expense Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (selectedCategory == null) "Tap slice or category to inspect" else "Inspecting $selectedCategory",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekPrimary,
                    fontSize = SleekSizes.textCaption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pie vs Donut Style Toggle Pill
            Row(
                modifier = Modifier
                    .clip(SleekShapes.xl)
                    .background(SleekPrimaryContainer.copy(alpha = 0.2f))
                    .border(1.dp, SleekBorder, SleekShapes.xl)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(SleekShapes.lg)
                        .background(if (!isDonutMode) SleekPrimary else Color.Transparent)
                        .clickable { isDonutMode = false }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AppIcons.PieChart,
                            contentDescription = "Pie Chart",
                            tint = if (!isDonutMode) Color.White else SleekTextSecondary,
                            modifier = Modifier.size(SleekSizes.iconMicro)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Pie",
                            fontSize = SleekSizes.textCaption,
                            fontWeight = FontWeight.Bold,
                            color = if (!isDonutMode) Color.White else SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(SleekShapes.lg)
                        .background(if (isDonutMode) SleekPrimary else Color.Transparent)
                        .clickable { isDonutMode = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AppIcons.DonutLarge,
                            contentDescription = "Donut Chart",
                            tint = if (isDonutMode) Color.White else SleekTextSecondary,
                            modifier = Modifier.size(SleekSizes.iconMicro)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Donut",
                            fontSize = SleekSizes.textCaption,
                            fontWeight = FontWeight.Bold,
                            color = if (isDonutMode) Color.White else SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (totalExpense <= 0 || validExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .height(chartRadius)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category expense records found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextSecondary
                )
            }
        } else {
            // Interactive Custom Canvas Pie Chart
            Box(
                modifier = Modifier
                    .size(chartRadius)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pie_chart_canvas")
                        .pointerInput(validExpenses, totalExpense) {
                            detectTapGestures { offset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val dist = sqrt(dx * dx + dy * dy)
                                val maxR = min(size.width, size.height) / 2f

                                if (dist <= maxR * 1.1f && totalExpense > 0) {
                                    // Calculate tap angle in degrees (-180..180)
                                    val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    // Normalize so 0 is top (-90 degrees)
                                    var normAngle = angleDeg + 90f
                                    if (normAngle < 0f) normAngle += 360f

                                    var accumulatedAngle = 0f
                                    for ((cat, sum) in validExpenses) {
                                        val sweep = ((sum / totalExpense) * 360f).toFloat()
                                        if (normAngle >= accumulatedAngle && normAngle < accumulatedAngle + sweep) {
                                            selectedCategory = if (selectedCategory == cat) null else cat
                                            break
                                        }
                                        accumulatedAngle += sweep
                                    }
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val minDim = min(canvasWidth, canvasHeight)
                    val baseRadius = (minDim / 2f) * 0.82f
                    val strokeWidth = minDim * 0.18f
                    val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)

                    var startAngle = -90f

                    validExpenses.forEach { (cat, amount) ->
                        val sweepAngle = ((amount / totalExpense) * 360f).toFloat() * animProgress
                        val color = resolvedCategoryColors[cat] ?: chartSlicePalette[0]
                        val isSelected = (cat == selectedCategory)

                        // If selected, explode slice slightly outwards along its bisecting angle
                        val explosionShift = if (isSelected) 14.dp.toPx() else 0f
                        val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                        val shiftX = (cos(midAngleRad) * explosionShift).toFloat()
                        val shiftY = (sin(midAngleRad) * explosionShift).toFloat()

                        withTransform({
                            translate(left = shiftX, top = shiftY)
                        }) {
                            if (isDonutMode) {
                                // Draw Donut Ring Arc
                                val padding = (minDim - (baseRadius * 2f)) / 2f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 1.2f, // slight gap
                                    useCenter = false,
                                    topLeft = Offset(padding, padding),
                                    size = Size(baseRadius * 2f, baseRadius * 2f),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                if (isSelected) {
                                    // Highlight outer ring stroke
                                    drawArc(
                                        color = Color.White,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle - 1.2f,
                                        useCenter = false,
                                        topLeft = Offset(padding - 2.dp.toPx(), padding - 2.dp.toPx()),
                                        size = Size((baseRadius * 2f) + 4.dp.toPx(), (baseRadius * 2f) + 4.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            } else {
                                // Draw Solid Pie Slice
                                val padding = (minDim - (baseRadius * 2f)) / 2f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 0.4f,
                                    useCenter = true,
                                    topLeft = Offset(padding, padding),
                                    size = Size(baseRadius * 2f, baseRadius * 2f),
                                    style = Fill
                                )
                                // Slice border divider line for crisp separation
                                drawArc(
                                    color = sliceBorderColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(padding, padding),
                                    size = Size(baseRadius * 2f, baseRadius * 2f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                if (isSelected) {
                                    // Outer contour highlight on selected slice
                                    drawArc(
                                        color = Color.White,
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle - 0.4f,
                                        useCenter = true,
                                        topLeft = Offset(padding - 2.dp.toPx(), padding - 2.dp.toPx()),
                                        size = Size((baseRadius * 2f) + 4.dp.toPx(), (baseRadius * 2f) + 4.dp.toPx()),
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                }
                            }
                        }

                        startAngle += sweepAngle
                    }
                }

                // Center Label overlay for Donut Mode or General Center Summary
                if (isDonutMode || selectedCategory != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize(0.52f)
                            .clip(CircleShape)
                            .background(SleekSurface.copy(alpha = 0.95f))
                            .padding(4.dp)
                    ) {
                        if (selectedCategory != null) {
                            val selAmount = validExpenses[selectedCategory] ?: 0.0
                            val pct = if (totalExpense > 0) (selAmount / totalExpense) * 100 else 0.0
                            Text(
                                text = selectedCategory ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = resolvedCategoryColors[selectedCategory] ?: chartSlicePalette[0],
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "₹%,.0f", selAmount),
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", pct),
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Total Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekTextSecondary
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "₹%,.0f", totalExpense),
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Slice Inspector Banner
            AnimatedVisibility(
                visible = selectedCategory != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                selectedCategory?.let { cat ->
                    val catAmount = validExpenses[cat] ?: 0.0
                    val catPct = if (totalExpense > 0) (catAmount / totalExpense) * 100 else 0.0
                    val catColor = resolvedCategoryColors[cat] ?: chartSlicePalette[0]

                    Card(
                        colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.12f)),
                        border = BorderStroke(1.5.dp, catColor),
                        shape = SleekShapes.md,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { selectedCategory = null }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f%%", catPct)} of total spending",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SleekTextSecondary,
                                        fontSize = SleekSizes.textCaption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "₹%,.2f", catAmount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap to clear",
                                    fontSize = SleekSizes.textMicro,
                                    color = catColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Category Legend Breakdown Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                validExpenses.forEach { (cat, sum) ->
                    val catColor = resolvedCategoryColors[cat] ?: chartSlicePalette[0]
                    val catPct = (sum / totalExpense) * 100
                    val isSelected = (cat == selectedCategory)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) catColor.copy(alpha = 0.15f) else SleekSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) catColor else SleekBorder
                        ),
                        shape = SleekShapes.md,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Icon(
                                        AppIcons.CheckCircle,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(SleekSizes.iconMicro)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "₹%,.2f", sum),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(SleekShapes.xs)
                                        .background(catColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f%%", catPct),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = SleekSizes.textCaption,
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
    }
}
