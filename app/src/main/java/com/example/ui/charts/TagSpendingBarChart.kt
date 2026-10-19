package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import java.util.Locale

/**
 * Custom Composable that uses Canvas to draw an interactive bar chart
 * visualizing monthly spending categorized by tags (extracted from category or notes).
 */
@Composable
fun TagSpendingBarChart(
    expenses: List<Expense>,
    categoryColors: Map<String, Color>,
    modifier: Modifier = Modifier,
    periodLabel: String = "This Month",
    chartHeight: Dp = 200.dp
) {
    var selectedTag by remember { mutableStateOf<String?>(null) }
    val animEnabled = LocalAnimationsEnabled.current
    var animationPlayed by remember { mutableStateOf(!animEnabled) }

    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed || !animEnabled) 1f else 0f,
        animationSpec = appTween(durationMs = 750, easing = AppleDecelerateEasing),
        label = "bar_chart_anim"
    )

    LaunchedEffect(expenses, animEnabled) {
        if (animEnabled) {
            animationPlayed = false
            kotlinx.coroutines.delay(30)
            animationPlayed = true
        } else {
            animationPlayed = true
        }
    }

    // Extract tags & calculate tag spending breakdown
    val tagData = remember(expenses) {
        val nonIncome = expenses.filter { it.type != "INCOME" && it.amount > 0 }
        val map = mutableMapOf<String, Double>()
        val countMap = mutableMapOf<String, Int>()

        nonIncome.forEach { expense ->
            val hashtagMatches = Regex("#[a-zA-Z0-9_]+").findAll(expense.note ?: "")
                .map { it.value.lowercase() }
                .distinct()
                .toList()

            val tagsForExpense = if (hashtagMatches.isNotEmpty()) {
                hashtagMatches
            } else {
                listOf("#${expense.category.lowercase().replace(" ", "")}")
            }

            tagsForExpense.forEach { tag ->
                map[tag] = (map[tag] ?: 0.0) + expense.amount
                countMap[tag] = (countMap[tag] ?: 0) + 1
            }
        }

        // Sort tags by spending descending
        map.entries
            .sortedByDescending { it.value }
            .take(7) // Top 7 tags for clean bar rendering
            .map { entry ->
                TagSpendingInfo(
                    tag = entry.key,
                    amount = entry.value,
                    count = countMap[entry.key] ?: 1
                )
            }
    }

    val totalSpending = remember(tagData) { tagData.sumOf { it.amount } }
    val maxTagAmount = remember(tagData) { (tagData.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(1.0) }

    // Palette of vibrant colors for tags
    val defaultTagColors = remember {
        listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF8B5CF6), // Purple
            Color(0xFF06B6D4), // Cyan
            Color(0xFFF97316)  // Orange
        )
    }

    fun getTagColor(tag: String, index: Int): Color {
        val cleanCat = tag.removePrefix("#").replaceFirstChar { it.uppercase() }
        return categoryColors[cleanCat] ?: defaultTagColors[index % defaultTagColors.size]
    }

    val density = LocalDensity.current
    val gridColor = SleekBorder.copy(alpha = 0.6f)
    val axisTextColor = SleekTextSecondary

    Card(
        shape = SleekShapes.xxl,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_spending_bar_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.LocalOffer,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "Spending by Tag",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Breakdown for $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SleekPrimaryContainer,
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Total: ₹${String.format(Locale.getDefault(), "%,.0f", totalSpending)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (tagData.isEmpty()) {
                // Empty state view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .background(SleekBorder.copy(alpha = 0.2f), SleekShapes.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.BarChart,
                            contentDescription = null,
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tagged expenses for $periodLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Interactive Custom Canvas Bar Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tag_bar_chart_canvas")
                            .pointerInput(tagData) {
                                detectTapGestures { tapOffset ->
                                    val width = size.width.toFloat()
                                    val leftPadding = 100f
                                    val rightPadding = 30f
                                    val usableWidth = (width - leftPadding - rightPadding).coerceAtLeast(1f)
                                    val count = tagData.size.coerceAtLeast(1)
                                    val slotWidth = usableWidth / count

                                    if (tagData.isNotEmpty() && tapOffset.x >= leftPadding && tapOffset.x <= width - rightPadding) {
                                        val index = ((tapOffset.x - leftPadding) / slotWidth).toInt().coerceIn(0, tagData.size - 1)
                                        val tappedTag = tagData[index].tag
                                        selectedTag = if (selectedTag == tappedTag) null else tappedTag
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val leftMargin = 110f
                        val bottomMargin = 70f
                        val topMargin = 40f
                        val rightMargin = 30f

                        val chartWidth = (canvasWidth - leftMargin - rightMargin).coerceAtLeast(1f)
                        val chartHeightPx = (canvasHeight - topMargin - bottomMargin).coerceAtLeast(1f)

                        // 1. Draw horizontal background grid lines (0%, 50%, 100%)
                        val gridLevels = listOf(0f, 0.5f, 1f)
                        gridLevels.forEach { level ->
                            val y = topMargin + chartHeightPx * (1f - level)

                            // Grid line
                            drawLine(
                                color = gridColor,
                                start = Offset(leftMargin, y),
                                end = Offset(canvasWidth - rightMargin, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Y-axis label text
                            val labelValue = maxTagAmount * level
                            val labelText = if (labelValue >= 1000) {
                                String.format(Locale.getDefault(), "₹%.1fk", labelValue / 1000)
                            } else {
                                String.format(Locale.getDefault(), "₹%.0f", labelValue)
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                leftMargin - 16f,
                                y + 10f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 26f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                    isAntiAlias = true
                                }
                            )
                        }

                        // 2. Draw Bars for each tag
                        val barCount = tagData.size.coerceAtLeast(1)
                        val slotWidth = chartWidth / barCount
                        val barWidth = (slotWidth * 0.52f).coerceAtMost(56f)

                        tagData.forEachIndexed { index, item ->
                            val tagColor = getTagColor(item.tag, index)
                            val isSelected = selectedTag == item.tag

                            val barHeightRatio = (item.amount / maxTagAmount).toFloat() * animProgress
                            val barHeightPx = (chartHeightPx * barHeightRatio).coerceAtLeast(6f)

                            val xCenter = leftMargin + (index + 0.5f) * slotWidth
                            val barLeft = xCenter - barWidth / 2f
                            val barTop = topMargin + chartHeightPx - barHeightPx

                            // Bar Brush Gradient
                            val barBrush = Brush.verticalGradient(
                                colors = listOf(
                                    tagColor,
                                    tagColor.copy(alpha = if (isSelected) 0.95f else 0.7f)
                                )
                            )

                            // Draw rounded bar
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeightPx),
                                cornerRadius = CornerRadius(12f, 12f)
                            )

                            // Highlight border if selected
                            if (isSelected) {
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(barLeft - 2f, barTop - 2f),
                                    size = Size(barWidth + 4f, barHeightPx + 4f),
                                    cornerRadius = CornerRadius(14f, 14f),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }

                            // Value label on top of bar
                            val valueText = String.format(Locale.getDefault(), "₹%.0f", item.amount)
                            drawContext.canvas.nativeCanvas.drawText(
                                valueText,
                                xCenter,
                                (barTop - 10f).coerceAtLeast(24f),
                                android.graphics.Paint().apply {
                                    color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = isSelected
                                    isAntiAlias = true
                                }
                            )

                            // X-axis tag label text below bar
                            val displayTag = if (item.tag.length > 8) item.tag.take(7) + "…" else item.tag
                            drawContext.canvas.nativeCanvas.drawText(
                                displayTag,
                                xCenter,
                                canvasHeight - 16f,
                                android.graphics.Paint().apply {
                                    color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.GRAY
                                    textSize = 26f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = isSelected
                                    isAntiAlias = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Tag Chip Selector & Legend
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tagData, key = { it.tag }) { item ->
                        val index = tagData.indexOf(item)
                        val color = getTagColor(item.tag, index)
                        val isSelected = selectedTag == item.tag

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTag = if (isSelected) null else item.tag
                            },
                            label = {
                                Text(
                                    text = item.tag,
                                    fontSize = SleekSizes.textBodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.25f),
                                selectedLabelColor = SleekTextPrimary,
                                containerColor = SleekSurface,
                                labelColor = SleekTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = SleekBorder,
                                selectedBorderColor = color
                            )
                        )
                    }
                }

                // Detail Banner when tag is selected
                AnimatedVisibility(
                    visible = selectedTag != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val activeInfo = tagData.find { it.tag == selectedTag }
                    if (activeInfo != null) {
                        val index = tagData.indexOf(activeInfo)
                        val color = getTagColor(activeInfo.tag, index)
                        val pct = if (totalSpending > 0) (activeInfo.amount / totalSpending * 100) else 0.0

                        Card(
                            shape = SleekShapes.lg,
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Label,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(SleekSizes.iconMedium)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = activeInfo.tag,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${activeInfo.count} transaction(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format(Locale.getDefault(), "%,.2f", activeInfo.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", pct)}% of spending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekTextSecondary,
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

private data class TagSpendingInfo(
    val tag: String,
    val amount: Double,
    val count: Int
)
