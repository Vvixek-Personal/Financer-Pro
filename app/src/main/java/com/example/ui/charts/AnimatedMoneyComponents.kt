package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Animated Money and Financial Component Suite.
 * Provides smooth odometer count-up numbers, animated progress meters,
 * and celebratory goal completion pulses adhering to LocalAnimationsEnabled.
 */

/**
 * Animated odometer-style numeric currency text that smoothly counts up to target amount.
 * Features fast initial acceleration and smooth deceleration curve (EmphasizedDecelerateEasing).
 */
@Composable
fun AnimatedMoneyText(
    amount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    decimals: Int = 2,
    color: Color = SleekTextPrimary,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    durationMs: Int = 650
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val animatedAmount by animateFloatAsState(
        targetValue = if (animationsEnabled) amount.toFloat() else amount.toFloat(),
        animationSpec = if (animationsEnabled) {
            tween(durationMillis = durationMs, easing = EmphasizedDecelerateEasing)
        } else {
            snap()
        },
        label = "animated_money_odometer"
    )

    val displayValue = if (animationsEnabled) animatedAmount.toDouble() else amount
    val prefix = if (displayValue < 0) "-" else ""
    val absVal = Math.abs(displayValue)

    val formatString = if (decimals > 0) {
        "%s%s%,.${decimals}f"
    } else {
        "%s%s%,.0f"
    }

    Text(
        text = String.format(Locale.getDefault(), formatString, prefix, currencySymbol, absVal),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

/**
 * Animated Spring-Filled Progress Bar for budget category limits and spend meters.
 * Uses smooth Apple decelerate easing for natural fluid fill.
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = SleekPrimary,
    trackColor: Color = SleekBorder.copy(alpha = 0.45f),
    durationMs: Int = 600
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val clampedTarget = progress.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = if (animationsEnabled) {
            tween(durationMillis = durationMs, easing = AppleDecelerateEasing)
        } else {
            snap()
        },
        label = "animated_progress_fill"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            color.copy(alpha = 0.85f),
                            color
                        )
                    )
                )
        )
    }
}

/**
 * Savings Goal Celebration Pulse that radiates celebratory rings when a goal reaches 100%.
 */
@Composable
fun SavingsGoalCelebrationPulse(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF10B981),
    content: @Composable BoxScope.() -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current

    val pulseScale by rememberInfiniteTransition(label = "goal_pulse_scale").animateFloat(
        initialValue = 1f,
        targetValue = if (isCompleted && animationsEnabled) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by rememberInfiniteTransition(label = "goal_pulse_alpha").animateFloat(
        initialValue = if (isCompleted && animationsEnabled) 0.5f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted && animationsEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha))
            )
        }

        content()
    }
}
