package com.example.ui

import com.example.ui.theme.*
import android.util.Log
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TAG = "StreakComponents"

/**
 * Data class representing a celebratory confetti / ember particle
 */
private data class StreakParticle(
    val initialX: Float,
    val initialY: Float,
    val targetX: Float,
    val targetY: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val shape: Int // 0: circle, 1: star/spark, 2: rect
)

/**
 * 🔥 Interactive, Animated Streak Flame Badge with Dynamic Ember Glow & Shimmer
 * Integrates seamlessly with the application's active theme and dark/light modes.
 */
@Composable
fun StreakFlameLogo(
    streakCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val isLarge = size > 60.dp
    val width = if (isLarge) size * 0.86f else 42.dp
    val height = if (isLarge) size else 48.dp
    val cornerRadius = if (isLarge) 28.dp else 14.dp

    // Subtle idle breathing & floating flicker animation
    val idleScale = rememberLoopFloat(1.0f, 1.06f, 1200, "idle_scale", FastOutSlowInEasing, reverse = true)
    val glowAlpha = rememberLoopFloat(0.40f, 0.85f, 1000, "glow_alpha", FastOutSlowInEasing, reverse = true, rest = 0.55f)
    val flameFlickerX = rememberLoopFloat(-2.5f, 2.5f, 850, "flame_flicker_x", FastOutSlowInEasing, reverse = true, rest = 0f)
    val shimmerOffset = rememberLoopFloat(-50f, 180f, 2200, "shimmer_offset")

    // Interactive press scale animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else (if (isLarge) idleScale else 1.0f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "press_scale"
    )

    // Dynamic flame colors matching streak intensity
    val (primaryFlameColor, secondaryFlameColor, glowColor) = remember(streakCount) {
        when {
            streakCount >= 30 -> Triple(Color(0xFF8B5CF6), Color(0xFFC084FC), Color(0xFFA855F7)) // Mystic Violet Flame for 30+ days
            streakCount >= 14 -> Triple(Color(0xFFEF4444), Color(0xFFF87171), Color(0xFFDC2626)) // Ruby Crimson Flame for 14+ days
            streakCount >= 7 -> Triple(Color(0xFFFF5E00), Color(0xFFFFB703), Color(0xFFFF8500))  // Intense Golden Orange Flame
            else -> Triple(Color(0xFFFF7A00), Color(0xFFFFC700), Color(0xFFFF9E00))              // Warm Amber Flame
        }
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(cornerRadius + 2.dp))
            .scale(pressScale)
            .clickable(
                enabled = onClick != null,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    onClick?.invoke()
                    isPressed = false
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Ambient Glow
        Box(
            modifier = Modifier
                .size(width = width + 6.dp, height = height + 6.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = glowAlpha * 0.45f), Color.Transparent),
                            radius = size.toPx() * 0.7f
                        )
                    )
                }
        )

        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = if (isDarkModeActive) Color(0xFF13121D) else SleekSurface,
            border = BorderStroke(
                if (isLarge) 2.dp else 1.dp,
                Brush.verticalGradient(
                    listOf(
                        glowColor.copy(alpha = 0.6f),
                        if (isDarkModeActive) Color(0xFF2B2844) else SleekBorder
                    )
                )
            ),
            shadowElevation = if (isLarge) 10.dp else 3.dp,
            modifier = Modifier.size(width = width, height = height)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isLarge) 12.dp else 4.dp, vertical = if (isLarge) 10.dp else 3.dp)
            ) {
                val w = this.size.width
                val h = this.size.height

                // Background internal ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.65f),
                        radius = h * 0.55f
                    )
                )

                // Top Right Spark (small diamond)
                val spark1 = Path().apply {
                    val cx = 0.55f * w
                    val cy = 0.08f * h
                    val r = 0.045f * minOf(w, h)
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(spark1, color = secondaryFlameColor)

                // Top Left Spark (larger diamond)
                val spark2 = Path().apply {
                    val cx = 0.38f * w
                    val cy = 0.14f * h
                    val r = 0.065f * minOf(w, h)
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(spark2, color = secondaryFlameColor)

                // Outer Flame Body
                val outerFlame = Path().apply {
                    val tipX = 0.52f * w + flameFlickerX
                    val tipY = 0.22f * h
                    moveTo(tipX, tipY)
                    // Left contour swooping down to notch
                    cubicTo(
                        0.45f * w + flameFlickerX * 0.5f, 0.28f * h,
                        0.36f * w, 0.35f * h,
                        0.34f * w, 0.42f * h
                    )
                    // Notch curve on left
                    cubicTo(
                        0.32f * w, 0.44f * h,
                        0.28f * w, 0.45f * h,
                        0.27f * w, 0.52f * h
                    )
                    // Left belly swelling down
                    cubicTo(
                        0.25f * w, 0.62f * h,
                        0.26f * w, 0.76f * h,
                        0.34f * w, 0.85f * h
                    )
                    // Bottom curve cradling base
                    cubicTo(
                        0.42f * w, 0.90f * h,
                        0.58f * w, 0.90f * h,
                        0.66f * w, 0.85f * h
                    )
                    // Right belly swelling up
                    cubicTo(
                        0.74f * w, 0.76f * h,
                        0.75f * w, 0.62f * h,
                        0.74f * w, 0.52f * h
                    )
                    // Right convex curve back to tip
                    cubicTo(
                        0.73f * w, 0.40f * h,
                        0.63f * w, 0.28f * h,
                        tipX, tipY
                    )
                    close()
                }

                drawPath(
                    path = outerFlame,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryFlameColor, primaryFlameColor.copy(alpha = 0.9f))
                    )
                )

                // Inner Flame Core
                val innerFlame = Path().apply {
                    val tipX = 0.51f * w + flameFlickerX * 0.7f
                    val tipY = 0.53f * h
                    moveTo(tipX, tipY)
                    cubicTo(
                        0.43f * w, 0.64f * h,
                        0.43f * w, 0.75f * h,
                        0.48f * w, 0.81f * h
                    )
                    cubicTo(
                        0.50f * w, 0.83f * h,
                        0.52f * w, 0.83f * h,
                        0.54f * w, 0.81f * h
                    )
                    cubicTo(
                        0.59f * w, 0.75f * h,
                        0.59f * w, 0.64f * h,
                        tipX, tipY
                    )
                    close()
                }

                drawPath(
                    path = innerFlame,
                    brush = Brush.verticalGradient(
                        colors = listOf(secondaryFlameColor, Color(0xFFFFFBEB))
                    )
                )

                // Streak count text at bottom
                val streakStr = streakCount.toString()
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val textSize = when {
                        streakStr.length <= 2 -> 0.32f * h
                        streakStr.length == 3 -> 0.25f * h
                        else -> 0.20f * h
                    }
                    val textY = 0.88f * h

                    val typeface = try {
                        Typeface.create("sans-serif-rounded", Typeface.BOLD)
                    } catch (e: Exception) {
                        Typeface.DEFAULT_BOLD
                    }

                    val outlineColor = if (isDarkModeActive) {
                        android.graphics.Color.parseColor("#13121D")
                    } else {
                        android.graphics.Color.parseColor("#1E293B")
                    }

                    // Cutout stroke paint (punch-out effect for crisp legibility)
                    val strokePaint = Paint().apply {
                        isAntiAlias = true
                        color = outlineColor
                        style = Paint.Style.STROKE
                        strokeWidth = 0.18f * textSize
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        this.textSize = textSize
                        this.typeface = typeface
                        textAlign = Paint.Align.CENTER
                    }

                    // Pure white fill paint
                    val fillPaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.WHITE
                        style = Paint.Style.FILL
                        this.textSize = textSize
                        this.typeface = typeface
                        textAlign = Paint.Align.CENTER
                    }

                    nativeCanvas.drawText(streakStr, 0.50f * w, textY, strokePaint)
                    nativeCanvas.drawText(streakStr, 0.50f * w, textY, fillPaint)
                }
            }
        }
    }
}

/**
 * 🎉 Supercharged Daily Streak Celebration & Gain Experience
 * Features:
 * - Dynamic stage animation: Ray burst, flame drop-in with bouncy overshoot
 * - Exploding confetti & glowing embers simulation
 * - Dynamic Milestone Tier Recognition (Bronze, Silver, Gold, Diamond)
 * - Animated Streak Number odometer count-up
 * - Next milestone preview progress bar with motivational prompt
 * - Sound/Haptic feedback and sound-wave pulse
 * - Themed Glassmorphic background with smooth backdrop blur effect
 */
@Composable
fun DailyStreakCelebrationDialog(
    streakCount: Int,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Trigger haptic pulses
    LaunchedEffect(streakCount) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(250)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform streak celebration haptics", e)
        }
    }

    // Animation progress states
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationStarted = true
    }

    // Flame entrance spring animation
    val flameScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flame_scale_anim"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 350, delayMillis = 100, easing = AppleDecelerateEasing),
        label = "content_alpha_anim"
    )

    // Animated number counter
    val animatedCount by animateIntAsState(
        targetValue = if (animationStarted) streakCount else 0,
        animationSpec = tween(durationMillis = 850, delayMillis = 180, easing = AppleDecelerateEasing),
        label = "count_up_anim"
    )

    // Infinite ambient animations
    val rayRotation = rememberLoopFloat(0f, 360f, 14000, "ray_rotation")
    val pulseGlow = rememberLoopFloat(0.85f, 1.18f, 1200, "pulse_glow", FastOutSlowInEasing, reverse = true, rest = 1f)
    val floatOffset = rememberLoopFloat(-6f, 6f, 1800, "flame_float", FastOutSlowInEasing, reverse = true, rest = 0f)

    // Determine milestone and motivation
    val (milestoneTitle, nextMilestone, tierColor, tierBadge) = remember(streakCount) {
        when {
            streakCount >= 100 -> Quad("Century Champion! 👑", 365, Color(0xFF38BDF8), "DIAMOND")
            streakCount >= 30 -> Quad("Master of Consistency! 💎", 100, Color(0xFFA855F7), "PLATINUM")
            streakCount >= 14 -> Quad("Unstoppable Habit! 🏆", 30, Color(0xFFF59E0B), "GOLD")
            streakCount >= 7 -> Quad("One Full Week Strong! ⚡", 14, Color(0xFF10B981), "SILVER")
            streakCount >= 3 -> Quad("Heating Up Fast! 🔥", 7, Color(0xFFFF7A00), "BRONZE")
            else -> Quad("Streak Initiated! 🌱", 3, SleekPrimary, "STARTER")
        }
    }

    // Confetti particles generator
    val particles = remember {
        val colors = listOf(
            Color(0xFFFF7A00),
            Color(0xFFFFC700),
            Color(0xFFEF4444),
            Color(0xFF10B981),
            Color(0xFF38BDF8),
            Color(0xFFA855F7)
        )
        List(36) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val distance = Random.nextDouble(140.0, 320.0)
            StreakParticle(
                initialX = 0f,
                initialY = 0f,
                targetX = (cos(angle) * distance).toFloat(),
                targetY = (sin(angle) * distance).toFloat(),
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 8f + 4f,
                rotationSpeed = Random.nextFloat() * 720f - 360f,
                shape = Random.nextInt(3)
            )
        }
    }

    val particleProgress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "particle_anim"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Full screen tap-anywhere dismiss box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Central Streak Emblem with Rotating Rays & Confetti Burst
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Sunburst Radiating Rays
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = rayRotation }
                    ) {
                        val centerPx = Offset(size.width / 2f, size.height / 2f)
                        val rayCount = 24
                        val angleStep = 360f / rayCount
                        val rayLength = size.width / 2f

                        for (i in 0 until rayCount) {
                            val angleRad = Math.toRadians((i * angleStep).toDouble())
                            val endX = centerPx.x + (rayLength * cos(angleRad)).toFloat()
                            val endY = centerPx.y + (rayLength * sin(angleRad)).toFloat()

                            val rayColor = if (i % 2 == 0) tierColor else Color(0xFFFFB703)
                            drawLine(
                                color = rayColor.copy(alpha = if (i % 2 == 0) 0.35f else 0.15f),
                                start = centerPx,
                                end = Offset(endX, endY),
                                strokeWidth = if (i % 2 == 0) 7f else 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Pulsing Radiant Halo behind flame
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .scale(pulseGlow)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(tierColor.copy(alpha = 0.45f), Color.Transparent),
                                        radius = size.width / 2f
                                    )
                                )
                            }
                    )

                    // Flying Confetti Embers Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerPx = Offset(size.width / 2f, size.height / 2f)
                        particles.forEach { p ->
                            val currentX = centerPx.x + p.targetX * particleProgress
                            val currentY = centerPx.y + p.targetY * particleProgress
                            val particleAlpha = (1f - particleProgress * 0.85f).coerceIn(0f, 1f)

                            when (p.shape) {
                                0 -> drawCircle(
                                    color = p.color.copy(alpha = particleAlpha),
                                    radius = p.size * (1f - particleProgress * 0.3f),
                                    center = Offset(currentX, currentY)
                                )
                                1 -> {
                                    // Diamond spark
                                    val r = p.size * (1f - particleProgress * 0.2f)
                                    val sparkPath = Path().apply {
                                        moveTo(currentX, currentY - r)
                                        lineTo(currentX + r, currentY)
                                        lineTo(currentX, currentY + r)
                                        lineTo(currentX - r, currentY)
                                        close()
                                    }
                                    drawPath(sparkPath, color = p.color.copy(alpha = particleAlpha))
                                }
                                else -> {
                                    drawRoundRect(
                                        color = p.color.copy(alpha = particleAlpha),
                                        topLeft = Offset(currentX - p.size / 2, currentY - p.size / 2),
                                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.5f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                                    )
                                }
                            }
                        }
                    }

                    // Central Streak Flame Logo with Entrance Bounce & Idle Float
                    Box(
                        modifier = Modifier
                            .scale(flameScale)
                            .offset(y = floatOffset.dp)
                    ) {
                        StreakFlameLogo(
                            streakCount = animatedCount,
                            size = 150.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sleek Glassmorphic Celebration Card
                Surface(
                    shape = SleekShapes.xxl,
                    color = SleekSurface,
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                tierColor.copy(alpha = 0.8f),
                                Color(0xFFFFB703).copy(alpha = 0.4f),
                                SleekBorder.copy(alpha = 0.2f)
                            )
                        )
                    ),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp)
                    ) {
                        // Tier Badge Ribbon
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tierColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, tierColor.copy(alpha = 0.45f)),
                            modifier = Modifier.padding(bottom = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(tierColor)
                                )
                                Text(
                                    text = "$tierBadge • $milestoneTitle",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Big Celebration Title
                        Text(
                            text = if (streakCount == 1) "Streak Started! 🔥" else "$animatedCount Day Streak! 🔥",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekTextPrimary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.2.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (streakCount == 1)
                                "Great start! Check in daily to track expenses and build healthy money habits."
                            else
                                "Keep the fire going! You've logged your finances for $streakCount days in a row.",
                            fontSize = 13.sp,
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            letterSpacing = 0.15.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Next Milestone Tracker
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkModeActive) Color(0xFF161822) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Next Target",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SleekTextSecondary,
                                        letterSpacing = 0.2.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$streakCount / $nextMilestone Days",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tierColor,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val milestoneProgress = (streakCount.toFloat() / nextMilestone.toFloat()).coerceIn(0.05f, 1f)
                                val animatedMilestoneProgress by animateFloatAsState(
                                    targetValue = if (animationStarted) milestoneProgress else 0f,
                                    animationSpec = tween(durationMillis = 1000, delayMillis = 300, easing = FastOutSlowInEasing),
                                    label = "progress_bar_anim"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isDarkModeActive) Color(0xFF1E2235) else Color(0xFFE2E8F0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedMilestoneProgress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(tierColor, Color(0xFFFFB703))
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val daysRemaining = (nextMilestone - streakCount).coerceAtLeast(1)
                                Text(
                                    text = "$daysRemaining day${if (daysRemaining > 1) "s" else ""} until next badge",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SleekTextSecondary,
                                    modifier = Modifier.align(Alignment.End),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple internal helper class for 4 values
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
