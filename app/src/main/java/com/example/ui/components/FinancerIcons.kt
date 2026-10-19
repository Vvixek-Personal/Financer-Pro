package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Financer Design System - Cohesive Icon Suite & Micro-Interaction Guidelines
 * Designed to match the design specifications across:
 * 1. Bottom Navigation
 * 2. Sidebar / Settings Navigation
 * 4. Dashboard / Home Summary Badges
 * 5. Transactions Controls
 * 6. Analytics Charts
 * 7. Budget Controls
 * 8. Goals / Savings Badges
 * 9. Bills / Reminders Controls
 * 10. Transaction Categories
 * 11. General Actions & Controls
 * 12. States & Feedback Badges
 *
 * Fully adaptive in both Dark Mode (deep navy glass + radiant luminescence)
 * and Light Mode (crisp soft pastel surfaces + vibrant glyphs).
 * Implements the 0.95 scale tap physics on press (80-150ms spring).
 */
object FinancerIcons {

    // 1. Bottom Navigation
    object BottomNav {
        val Home: ImageVector = AppIcons.Home
        val Transactions: ImageVector = AppIcons.Autorenew
        val Analytics: ImageVector = AppIcons.BarChart
        val Goals: ImageVector = AppIcons.TrackChanges
        val More: ImageVector = AppIcons.MoreHoriz
    }

    // 2. Sidebar / Settings Navigation
    object Sidebar {
        val Dashboard: ImageVector = AppIcons.Home
        val Transactions: ImageVector = AppIcons.ReceiptLong
        val Analytics: ImageVector = AppIcons.BarChart
        val Goals: ImageVector = AppIcons.TrackChanges
        val Budget: ImageVector = AppIcons.PieChart
        val Bills: ImageVector = AppIcons.Description
        val Settings: ImageVector = AppIcons.Settings
        val Appearance: ImageVector = AppIcons.WaterDrop
        val Language: ImageVector = AppIcons.Language
        val Data: ImageVector = AppIcons.Storage
    }

    // 4. Dashboard / Home Badges
    object Dashboard {
        val TotalBalance: ImageVector = AppIcons.AccountBalanceWallet
        val Income: ImageVector = AppIcons.ArrowUpward
        val Expense: ImageVector = AppIcons.ArrowDownward
        val Savings: ImageVector = AppIcons.Savings
        val QuickAdd: ImageVector = AppIcons.Add
    }

    // 5. Transactions Controls
    object Transactions {
        val Add: ImageVector = AppIcons.Add
        val Edit: ImageVector = AppIcons.Edit
        val Delete: ImageVector = AppIcons.Delete
        val Search: ImageVector = AppIcons.Search
        val Filter: ImageVector = AppIcons.FilterList
    }

    // 6. Analytics Charts
    object Analytics {
        val Chart: ImageVector = AppIcons.BarChart
        val PieChart: ImageVector = AppIcons.PieChart
        val LineChart: ImageVector = AppIcons.ShowChart
        val Insights: ImageVector = AppIcons.Lightbulb
        val Export: ImageVector = AppIcons.FileDownload
    }

    // 7. Budget Controls
    object Budget {
        val Add: ImageVector = AppIcons.Add
        val Edit: ImageVector = AppIcons.Edit
        val Category: ImageVector = AppIcons.GridView
        val Progress: ImageVector = AppIcons.DonutLarge
        val Limit: ImageVector = AppIcons.Shield
    }

    // 8. Goals / Savings Badges
    object Goals {
        val AddGoal: ImageVector = AppIcons.TrackChanges
        val Edit: ImageVector = AppIcons.Edit
        val Progress: ImageVector = AppIcons.DonutLarge
        val Achieved: ImageVector = AppIcons.EmojiEvents
        val History: ImageVector = AppIcons.History
    }

    // 9. Bills / Reminders Controls
    object Bills {
        val AddBill: ImageVector = AppIcons.Add
        val Recurring: ImageVector = AppIcons.Repeat
        val Due: ImageVector = AppIcons.CalendarMonth
        val Paid: ImageVector = AppIcons.CheckCircle
        val History: ImageVector = AppIcons.History
    }

    // 10. Transaction Categories
    object Categories {
        val Food: ImageVector = AppIcons.Restaurant
        val Transport: ImageVector = AppIcons.DirectionsCar
        val Shopping: ImageVector = AppIcons.ShoppingBag
        val Health: ImageVector = AppIcons.Favorite
        val Entertainment: ImageVector = AppIcons.SportsEsports
    }

    // 11. General Actions & Controls
    object Actions {
        val Save: ImageVector = AppIcons.Save
        val Share: ImageVector = AppIcons.Share
        val Download: ImageVector = AppIcons.Download
        val Refresh: ImageVector = AppIcons.Refresh
        val More: ImageVector = AppIcons.MoreHoriz
    }

    // 12. States & Feedback Badges
    object States {
        val Success: ImageVector = AppIcons.CheckCircle
        val Error: ImageVector = AppIcons.Cancel
        val Info: ImageVector = AppIcons.Info
        val Warning: ImageVector = AppIcons.Warning
        val EmptyState: ImageVector = AppIcons.HourglassEmpty
    }

    // Theme Palette Colors for Badges
    object Palette {
        val IncomeGreen = Color(0xFF10B981)
        val ExpenseRed = Color(0xFFEF4444)
        val BalanceBlue = Color(0xFF2563EB)
        val SavingsTeal = Color(0xFF0D9488)
        val QuickAddCyan = Color(0xFF06B6D4)
        val WarningAmber = Color(0xFFF59E0B)
        val InfoBlue = Color(0xFF3B82F6)
        val PurpleAccent = Color(0xFF8B5CF6)
    }
}

/**
 * Micro-interaction Modifier:
 * Scales down to 0.95 on press with smooth bouncy spring (80-150ms).
 */
@Composable
fun Modifier.financerIconPress(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    scaleDown: Float = 0.95f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconPressScale"
    )
    return this
        .scale(animatedScale)
}

/**
 * Adaptive Icon Badge Component supporting both Dark and Light modes.
 * Features:
 * - Ambient gradient background
 * - Luminous outer border
 * - 0.95 scale tap feedback
 * - Perfect accessibility contrast
 */
@Composable
fun FinancerBadgeIcon(
    icon: ImageVector,
    contentDescription: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = CircleShape,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "financerBadgeScale"
    )

    val isDark = isDarkModeActive

    // Ambient background brush
    val backgroundBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.28f),
                accentColor.copy(alpha = 0.10f),
                Color(0xFF0D111A)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.16f),
                accentColor.copy(alpha = 0.08f)
            )
        )
    }

    // Outer luminous rim
    val borderColor = if (isDark) {
        accentColor.copy(alpha = 0.45f)
    } else {
        accentColor.copy(alpha = 0.28f)
    }

    val iconTint = if (isDark) {
        accentColor.copy(alpha = 0.95f)
    } else {
        accentColor
    }

    Box(
        modifier = modifier
            .size(badgeSize)
            .scale(scale)
            .clip(shape)
            .background(backgroundBrush)
            .border(BorderStroke(1.2.dp, borderColor), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
