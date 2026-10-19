package com.example.ui

import com.example.ui.theme.*
import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.animation.core.CubicBezierEasing

/** High-end professional easing curves (Apple & Material 3 Motion Specs) */
val AppleDecelerateEasing: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
val SmoothStandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/** True only when the in-app switch is ON and Android's animator scale is not 0. */
val LocalAnimationsEnabled = compositionLocalOf { true }

@Composable
fun systemAnimationsAllowed(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}

fun <T> appSpring(
    enabled: Boolean,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium
): FiniteAnimationSpec<T> =
    if (enabled) spring(dampingRatio, stiffness) else snap()

fun <T> appTween(
    enabled: Boolean,
    durationMs: Int,
    easing: Easing = AppleDecelerateEasing
): FiniteAnimationSpec<T> =
    if (enabled) tween(durationMs, easing = easing) else snap()

@Composable
fun <T> appTween(durationMs: Int, easing: Easing = AppleDecelerateEasing): FiniteAnimationSpec<T> =
    if (LocalAnimationsEnabled.current) tween(durationMs, easing = easing) else snap()

@Composable
fun <T> appSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium
): FiniteAnimationSpec<T> =
    if (LocalAnimationsEnabled.current) spring(dampingRatio, stiffness) else snap()

/** Looping float that holds [rest] (no animation, no frame work) when animations are off. */
@Composable
fun rememberLoopFloat(
    initial: Float,
    target: Float,
    durationMs: Int,
    label: String,
    easing: Easing = LinearEasing,
    reverse: Boolean = false,
    rest: Float = initial
): Float {
    if (!LocalAnimationsEnabled.current) return rest
    val transition = rememberInfiniteTransition(label = label)
    val value by transition.animateFloat(
        initialValue = initial,
        targetValue = target,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = easing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = label
    )
    return value
}

/**
 * High-end tactile touch feedback on interactive buttons and chips.
 * Compresses subtly (0.975f) with low bounce spring physics on tap and snaps back smoothly.
 * Honors [LocalAnimationsEnabled] — stays 1.0f when animations are disabled.
 */
fun Modifier.bouncyClickable(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    enabled: Boolean = true,
    scaleDown: Float = 0.975f,
    onClick: () -> Unit
): Modifier = composed {
    val animationsEnabled = LocalAnimationsEnabled.current
    val actualSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "professional_click_scale"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = actualSource,
            indication = indication,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Spring press scale modifier for Cards and custom surfaces.
 */
fun Modifier.bouncyPress(scaleDown: Float = 0.975f): Modifier = composed {
    val animationsEnabled = LocalAnimationsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "professional_press_scale"
    )
    this.scale(scale)
}

/**
 * Adds high-fidelity tactile depth, elevation drop-shadow, and subtle spring press physics to Buttons and interactive components.
 */
fun Modifier.tactileDepth(
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleDown: Float = 0.98f
): Modifier = composed {
    val animationsEnabled = LocalAnimationsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed && animationsEnabled) 0.5.dp else elevation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactile_elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactile_scale"
    )
    this
        .scale(scale)
        .shadow(elevation = animatedElevation, shape = shape, clip = false)
}

/**
 * Smooth numeric count-up animation for balances, stats, and scores.
 */
@Composable
fun animateAmountFloat(
    targetValue: Float,
    durationMs: Int = 750
): Float {
    val animationsEnabled = LocalAnimationsEnabled.current
    if (!animationsEnabled) return targetValue
    val animVal by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "amount_count_up"
    )
    return animVal
}

/**
 * Dynamic pulsing shimmer gradient brush for cards and banners.
 */
@Composable
fun rememberAnimatedGlowBrush(
    baseColor: Color,
    accentColor: Color,
    durationMs: Int = 2600
): Brush {
    val offset = rememberLoopFloat(
        initial = -200f,
        target = 800f,
        durationMs = durationMs,
        label = "glow_brush_offset",
        reverse = false,
        rest = 0f
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, accentColor, baseColor),
        start = Offset(offset - 250f, offset - 250f),
        end = Offset(offset + 250f, offset + 250f)
    )
}

/**
     * Staggered entrance modifier with slide-up and fade-in physics.
     */
@Composable
fun rememberStaggeredProgress(delayMs: Int = 0, durationMs: Int = 450): Float {
    val animationsEnabled = LocalAnimationsEnabled.current
    if (!animationsEnabled) return 1f
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "staggered_entrance"
    )
    return progress
}

/**
 * Subtle organic breathing pulse animation modifier for hero banners, active badges, and focus elements.
 * Uses gentle 0.992f..1.008f breathing scale over 3600ms with Apple decelerate easing.
 */
fun Modifier.subtleBreathing(
    minScale: Float = 0.992f,
    maxScale: Float = 1.008f,
    durationMs: Int = 3600
): Modifier = composed {
    val scale = rememberLoopFloat(
        initial = minScale,
        target = maxScale,
        durationMs = durationMs,
        label = "subtle_breathing_scale",
        easing = AppleDecelerateEasing,
        reverse = true,
        rest = 1f
    )
    this.scale(scale)
}

/**
 * Lightweight hardware-accelerated celebration particle canvas for milestone, budget success, and savings celebrations.
 * Uses aerodynamic deceleration with soft alpha fade-out for professional confetti feel.
 */
@Composable
fun CelebrationParticleBurst(
    trigger: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 28
) {
    if (!trigger || !LocalAnimationsEnabled.current) return

    val animProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(trigger) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = AppleDecelerateEasing)
        )
    }

    val colors = remember {
        listOf(
            Color(0xFF10B981), // Emerald
            Color(0xFF3B82F6), // Blue
            Color(0xFFF59E0B), // Amber
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4)  // Cyan
        )
    }

    val particles = remember {
        val random = java.util.Random(42)
        List(particleCount) { index ->
            val angle = (index.toFloat() / particleCount.toFloat()) * 2f * Math.PI.toFloat() + (random.nextFloat() * 0.3f - 0.15f)
            val distance = 50f + random.nextFloat() * 110f
            val size = 4f + random.nextFloat() * 5f
            val color = colors[index % colors.size]
            Triple(angle, distance, Pair(size, color))
        }
    }

    if (animProgress.value < 1f) {
        androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
            val progress = animProgress.value
            val alpha = (1f - (progress * progress)).coerceIn(0f, 1f)
            val center = Offset(size.width / 2f, size.height / 2f)

            particles.forEach { (angle, maxDist, sizeAndColor) ->
                // Smooth ease-out flight distance
                val easedDist = maxDist * (1f - (1f - progress) * (1f - progress))
                val px = center.x + kotlin.math.cos(angle) * easedDist
                val py = center.y + kotlin.math.sin(angle) * easedDist + (progress * progress * 32f) // subtle gravity
                val pSize = sizeAndColor.first * (1f - progress * 0.35f)
                drawCircle(
                    color = sizeAndColor.second.copy(alpha = alpha),
                    radius = pSize,
                    center = Offset(px, py)
                )
            }
        }
    }
}

/**
 * Dynamic pulsing glow modifier that subtly animates alpha when animations are enabled.
 */
fun Modifier.pulseGlow(
    glowColor: Color,
    minAlpha: Float = 0.12f,
    maxAlpha: Float = 0.32f,
    durationMs: Int = 2800
): Modifier = composed {
    val alpha = rememberLoopFloat(
        initial = minAlpha,
        target = maxAlpha,
        durationMs = durationMs,
        label = "pulse_glow_alpha",
        easing = AppleDecelerateEasing,
        reverse = true,
        rest = minAlpha
    )
    this.shadow(
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        ambientColor = glowColor.copy(alpha = alpha),
        spotColor = glowColor.copy(alpha = alpha)
    )
}

/**
 * Animated gradient border that smoothly shifts colors along the perimeter.
 */
fun Modifier.animatedGradientBorder(
    colors: List<Color>,
    borderWidth: Dp = 1.2.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    durationMs: Int = 4500
): Modifier = composed {
    val offset = rememberLoopFloat(
        initial = 0f,
        target = 1000f,
        durationMs = durationMs,
        label = "gradient_border_offset",
        easing = LinearEasing,
        reverse = false,
        rest = 0f
    )
    val brush = remember(offset, colors) {
        Brush.linearGradient(
            colors = colors,
            start = Offset(offset, 0f),
            end = Offset(offset + 600f, 600f)
        )
    }
    this.border(width = borderWidth, brush = brush, shape = shape)
}

/**
 * High-precision spring press effect for interactive chips, cards, and icons.
 */
fun Modifier.springPressEffect(
    scaleDown: Float = 0.975f,
    stiffness: Float = Spring.StiffnessMedium
): Modifier = composed {
    val animationsEnabled = LocalAnimationsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = stiffness
        ),
        label = "professional_spring_press_scale"
    )
    this.scale(scale)
}

