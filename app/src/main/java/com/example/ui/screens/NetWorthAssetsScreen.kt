package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AssetClass
import com.example.data.AssetClassBreakdown
import com.example.data.NetWorthSummary
import com.example.ui.AppIcons
import com.example.ui.FinanceViewModel
import com.example.ui.theme.*
import java.util.Locale

/**
 * 💼 NET WORTH & ASSET ALLOCATION BREAKDOWN STUDIO
 * Provides institutional-grade personal balance sheet analytics:
 * - Net Worth: Total Assets vs Liabilities
 * - Canvas Donut Asset Allocation Chart
 * - Liquidity & Debt Solvency Ratios
 * - Account-level multi-currency aggregation
 * - Rebalancing recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthAssetsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()

    val netWorthSummary = remember(accounts, selectedCurrencyCode) {
        viewModel.getNetWorthSummary(selectedCurrencyCode)
    }

    var selectedAssetClass by remember { mutableStateOf<AssetClass?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Net Worth & Assets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "Asset allocation & personal balance sheet",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = SleekTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SleekSurface
                )
            )
        },
        containerColor = SleekBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Net Worth Card
            NetWorthHeroCard(
                summary = netWorthSummary,
                currencySymbol = currencySymbol
            )

            // 2. Interactive Asset Allocation Donut Chart
            AssetAllocationDonutCard(
                summary = netWorthSummary,
                currencySymbol = currencySymbol,
                selectedClass = selectedAssetClass,
                onSelectClass = { cls ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedAssetClass = if (selectedAssetClass == cls) null else cls
                }
            )

            // 3. Financial Solvency & Liquidity Ratios
            SolvencyRatiosCard(summary = netWorthSummary)

            // 4. Grouped Asset Breakdown & Account Lists
            Text(
                text = "Portfolio Breakdown by Class",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )

            netWorthSummary.breakdowns.forEach { breakdown ->
                val isSelected = selectedAssetClass == breakdown.assetClass
                AssetClassCard(
                    breakdown = breakdown,
                    currencySymbol = currencySymbol,
                    isHighlighted = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedAssetClass = if (isSelected) null else breakdown.assetClass
                    }
                )
            }

            // 5. Actionable Advice
            if (netWorthSummary.healthAdvice.isNotEmpty()) {
                Text(
                    text = "Portfolio Recommendations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    netWorthSummary.healthAdvice.forEach { tip ->
                        Surface(
                            shape = SleekShapes.md,
                            color = SleekSurface,
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "💡", fontSize = 18.sp)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NetWorthHeroCard(
    summary: NetWorthSummary,
    currencySymbol: String
) {
    Card(
        shape = SleekShapes.xl,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL NET WORTH",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (summary.netWorth >= 0) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (summary.netWorth >= 0) "Solvent" else "Net Deficit",
                        color = if (summary.netWorth >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%s%s%,.2f",
                    if (summary.netWorth >= 0) "" else "-",
                    currencySymbol,
                    kotlin.math.abs(summary.netWorth)
                ),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = SleekTextPrimary
            )

            // Two-column Assets vs Liabilities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Assets pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = SleekShapes.md,
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Gross Assets",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, summary.totalAssets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Liabilities pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = SleekShapes.md,
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Liabilities",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, summary.totalLiabilities),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetAllocationDonutCard(
    summary: NetWorthSummary,
    currencySymbol: String,
    selectedClass: AssetClass?,
    onSelectClass: (AssetClass) -> Unit
) {
    Card(
        shape = SleekShapes.xl,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Asset Class Allocation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
                Text(
                    text = "Tap slices to filter",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekTextSecondary
                )
            }

            // Custom Canvas Donut Chart
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val nonZeroBreakdowns = remember(summary) {
                    summary.breakdowns.filter { it.totalAmount > 0 }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 26.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcSize = Size(diameter, diameter)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    if (nonZeroBreakdowns.isEmpty()) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        var startAngle = -90f
                        val totalVal = summary.totalAssets + summary.totalLiabilities
                        val safeTotal = if (totalVal > 0) totalVal else 1.0

                        nonZeroBreakdowns.forEach { breakdown ->
                            val sweep = ((breakdown.totalAmount / safeTotal) * 360f).toFloat().coerceAtLeast(4f)
                            val isHighlighted = selectedClass == null || selectedClass == breakdown.assetClass
                            val arcColor = Color(breakdown.assetClass.colorHex).copy(
                                alpha = if (isHighlighted) 1.0f else 0.25f
                            )

                            drawArc(
                                color = arcColor,
                                startAngle = startAngle,
                                sweepAngle = sweep - 2f, // slight gap between slices
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (selectedClass == breakdown.assetClass) strokeWidth * 1.25f else strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweep
                        }
                    }
                }

                // Center Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedClass != null) {
                        val sel = summary.breakdowns.firstOrNull { it.assetClass == selectedClass }
                        Text(
                            text = selectedClass.displayName.take(10),
                            fontSize = 11.sp,
                            color = SleekTextSecondary
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", sel?.percentageOfAssets ?: 0.0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(selectedClass.colorHex)
                        )
                    } else {
                        Text(
                            text = "Net Worth",
                            fontSize = 11.sp,
                            color = SleekTextSecondary
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, summary.netWorth),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }
                }
            }

            // Legend Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                summary.breakdowns.forEach { b ->
                    val isSelected = selectedClass == b.assetClass
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectClass(b.assetClass) }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(b.assetClass.colorHex))
                        )
                        Text(
                            text = b.assetClass.name.take(4),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(b.assetClass.colorHex) else SleekTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolvencyRatiosCard(summary: NetWorthSummary) {
    Card(
        shape = SleekShapes.lg,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Key Balance Sheet Health Ratios",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )

            // Ratio 1: Liquidity Ratio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Liquidity Ratio (Cash / Gross Assets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", summary.liquidityRatio * 100),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                }
                LinearProgressIndicator(
                    progress = { summary.liquidityRatio.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFF3B82F6).copy(alpha = 0.15f)
                )
            }

            // Ratio 2: Debt-to-Asset Ratio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Debt-to-Asset Ratio (Liabilities / Assets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", summary.debtToAssetRatio * 100),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.debtToAssetRatio > 0.40) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
                LinearProgressIndicator(
                    progress = { summary.debtToAssetRatio.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (summary.debtToAssetRatio > 0.40) Color(0xFFEF4444) else Color(0xFF10B981),
                    trackColor = Color(0xFFEF4444).copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun AssetClassCard(
    breakdown: AssetClassBreakdown,
    currencySymbol: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val classColor = Color(breakdown.assetClass.colorHex)

    Surface(
        onClick = onClick,
        shape = SleekShapes.lg,
        color = if (isHighlighted) classColor.copy(alpha = 0.08f) else SleekSurface,
        border = BorderStroke(
            1.dp,
            if (isHighlighted) classColor.copy(alpha = 0.5f) else SleekBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(classColor)
                    )
                    Column {
                        Text(
                            text = breakdown.assetClass.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Text(
                            text = "${breakdown.accountsCount} accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, breakdown.totalAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = classColor
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%% of portfolio", breakdown.percentageOfAssets),
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekTextSecondary
                    )
                }
            }

            // Expanded Account list if highlighted or selected
            AnimatedVisibility(visible = isHighlighted && breakdown.accounts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = SleekBorder)
                    breakdown.accounts.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.account.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SleekBg
                                ) {
                                    Text(
                                        text = item.account.type,
                                        fontSize = 10.sp,
                                        color = SleekTextSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, item.convertedBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
