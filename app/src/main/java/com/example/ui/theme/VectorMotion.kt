package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Financer Pro motion tokens adapted from VECTOR's interaction system.
 *
 * Keep transitions short and purposeful: fast feedback for controls, slightly
 * longer movement for navigation and larger surfaces. Respect the app's
 * existing reduced-motion preference at call sites.
 */
object VectorMotion {
    const val instantMillis = 110
    const val fastMillis = 160
    const val standardMillis = 200
    const val deliberateMillis = 280

    val easeOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val easeStandard = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val easeInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val easeIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    val controlTransition = tween<Float>(
        durationMillis = instantMillis,
        easing = easeOut
    )
    val screenTransition = tween<Float>(
        durationMillis = standardMillis,
        easing = easeOut
    )
    val surfaceTransition = tween<Float>(
        durationMillis = deliberateMillis,
        easing = easeStandard
    )
    val navigationSpring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 420f
    )

    /** Use with AnimatedVisibility / animate*AsState when motion is enabled. */
    fun durationFor(reducedMotion: Boolean, normalDuration: Int): Int =
        if (reducedMotion) 0 else normalDuration
}
