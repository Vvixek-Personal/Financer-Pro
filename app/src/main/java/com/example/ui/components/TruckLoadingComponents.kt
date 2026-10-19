package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * High-fidelity Jetpack Compose implementation of the Uiverse.io delivery truck loading animation
 * originally designed by vinodjangid07.
 *
 * Features:
 * - .truckWrapper: 200dp x 100dp container with clipped overflow.
 * - .truckBody: 130dp width with 1s linear infinite suspension bobbing (0dp -> 3dp -> 0dp).
 * - .truckTires: 130dp width with 24dp rotating wheels at bottom with alloy rims.
 * - .road: #282828 base line with white dashed stripes animating at 1.4s linear infinite.
 * - .lampPost: 90dp lamp post traversing from right to left at 1.4s linear infinite with warm cone illumination.
 * - System-aware: respects [LocalAnimationsEnabled] via [rememberLoopFloat].
 */
@Composable
fun DeliveryTruckLoader(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    caption: String? = null,
    subCaption: String? = null
) {
    // 1s suspension bobbing: 0 -> 3dp -> 0dp
    val truckSuspensionY = rememberLoopFloat(
        initial = 0f,
        target = 3f,
        durationMs = 500,
        label = "truck_suspension",
        easing = LinearEasing,
        reverse = true,
        rest = 0f
    )

    // 1.4s road and lamp post linear scroll: 0px to -350px
    val roadTranslateX = rememberLoopFloat(
        initial = 0f,
        target = -350f,
        durationMs = 1400,
        label = "road_scroll",
        easing = LinearEasing,
        reverse = false,
        rest = 0f
    )

    // Wheel rotation synced with ground movement (approx 700ms full rotation)
    val wheelRotation = rememberLoopFloat(
        initial = 0f,
        target = 360f,
        durationMs = 700,
        label = "wheel_spin",
        easing = LinearEasing,
        reverse = false,
        rest = 0f
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // .truckWrapper (200dp x 100dp, overflow-x: hidden)
        Box(
            modifier = Modifier
                .size(width = (200 * scale).dp, height = (100 * scale).dp)
                .clipToBounds(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background Canvas: Lamp Post, Road & Moving Dashes
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height
                val roadY = h - 6f * scale
                val roadThickness = 2f * scale

                // --- 1. LAMP POST ANIMATION (right: -90%, moving across screen) ---
                // The post starts off-screen to the right and translates by roadTranslateX (-350px)
                val lampPostBaseX = w + 80f * scale + (roadTranslateX * scale)
                val lampPostHeight = 85f * scale
                val lampPostTopY = roadY - lampPostHeight

                // Draw Lamp Post if visible within bounds
                if (lampPostBaseX > -60f * scale && lampPostBaseX < w + 120f * scale) {
                    val postColor = Color(0xFF64748B) // Slate grey post
                    // Vertical pole
                    drawLine(
                        color = postColor,
                        start = Offset(lampPostBaseX, roadY),
                        end = Offset(lampPostBaseX, lampPostTopY + 12f * scale),
                        strokeWidth = 2.5f * scale,
                        cap = StrokeCap.Round
                    )
                    // Curved neck pointing left toward road
                    val neckPath = Path().apply {
                        moveTo(lampPostBaseX, lampPostTopY + 12f * scale)
                        quadraticTo(
                            lampPostBaseX, lampPostTopY,
                            lampPostBaseX - 16f * scale, lampPostTopY + 2f * scale
                        )
                    }
                    drawPath(
                        path = neckPath,
                        color = postColor,
                        style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
                    )
                    // Lamp head / shade
                    drawCircle(
                        color = Color(0xFF334155),
                        radius = 4f * scale,
                        center = Offset(lampPostBaseX - 16f * scale, lampPostTopY + 3f * scale)
                    )
                    // Warm yellow glowing light bulb & downward cone
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = 2.5f * scale,
                        center = Offset(lampPostBaseX - 16f * scale, lampPostTopY + 5f * scale)
                    )
                    // Soft light beam on the asphalt
                    val lightConePath = Path().apply {
                        moveTo(lampPostBaseX - 16f * scale, lampPostTopY + 6f * scale)
                        lineTo(lampPostBaseX - 32f * scale, roadY)
                        lineTo(lampPostBaseX - 2f * scale, roadY)
                        close()
                    }
                    drawPath(
                        path = lightConePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x3DFDE047),
                                Color(0x00FDE047)
                            ),
                            startY = lampPostTopY + 6f * scale,
                            endY = roadY
                        )
                    )
                }

                // --- 2. ROAD BASE (#282828) ---
                drawRoundRect(
                    color = Color(0xFF282828),
                    topLeft = Offset(0f, roadY),
                    size = Size(w, roadThickness),
                    cornerRadius = CornerRadius(3f * scale, 3f * scale)
                )

                // --- 3. MOVING ROAD MARKINGS & DASHES (roadAnimation 1.4s translateX) ---
                // We draw multiple repeating dashes offset by roadTranslateX so the road constantly flows smoothly
                val dashInterval = 140f * scale
                val dashLength = 28f * scale
                val dashThickness = 2.5f * scale

                for (i in -1..4) {
                    val rawX = (i * dashInterval) + (roadTranslateX * scale)
                    // Wrap-around modulo math to keep endless road markings flowing
                    val wrappedX = ((rawX % (dashInterval * 3)) + (dashInterval * 3)) % (dashInterval * 3) - 60f * scale
                    if (wrappedX in -dashLength..(w + dashLength)) {
                        // Road dash mark
                        drawRoundRect(
                            color = Color(0xFFF8FAFC),
                            topLeft = Offset(wrappedX, roadY - 0.5f * scale),
                            size = Size(dashLength, dashThickness),
                            cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale)
                        )
                    }
                }
            }

            // Truck Container + Wheels (Centered horizontally, sitting on road)
            Box(
                modifier = Modifier
                    .width((130 * scale).dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // --- 4. TRUCK BODY (Upper Body with 1s suspension bobbing) ---
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((64 * scale).dp)
                        .padding(bottom = (8 * scale).dp)
                        .offset(y = (truckSuspensionY * scale).dp)
                ) {
                    val truckW = size.width
                    val truckH = size.height

                    // Main Cargo Container (Left/Rear portion: 0 to ~82% width)
                    val cargoW = truckW * 0.68f
                    val cargoH = truckH * 0.88f
                    val cargoTopY = truckH - cargoH - 4f * scale

                    // Cargo Box Background (Sleek dark emerald / teal gradient for fintech theme)
                    val cargoPath = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                left = 0f,
                                top = cargoTopY,
                                right = cargoW,
                                bottom = truckH - 4f * scale,
                                radiusX = 6f * scale,
                                radiusY = 6f * scale
                            )
                        )
                    }
                    drawPath(
                        path = cargoPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A), // Dark slate
                                Color(0xFF1E293B)  // Medium slate
                            )
                        )
                    )
                    // Cargo Box Border Accent
                    drawPath(
                        path = cargoPath,
                        color = Color(0xFF10B981).copy(alpha = 0.6f),
                        style = Stroke(width = 1.5f * scale)
                    )

                    // Vertical cargo ribs/lines
                    for (xStep in 1..3) {
                        val ribX = (cargoW / 4f) * xStep
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(ribX, cargoTopY + 4f * scale),
                            end = Offset(ribX, truckH - 8f * scale),
                            strokeWidth = 1.2f * scale
                        )
                    }

                    // Gold/Emerald luxury badge emblem on container
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 6f * scale,
                        center = Offset(cargoW * 0.5f, cargoTopY + cargoH * 0.45f)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 3.5f * scale,
                        center = Offset(cargoW * 0.5f, cargoTopY + cargoH * 0.45f)
                    )

                    // Cabin (Right/Front portion: cargoW to truckW)
                    val cabStartX = cargoW + 2f * scale
                    val cabW = truckW - cabStartX
                    val cabH = truckH * 0.72f
                    val cabTopY = truckH - cabH - 4f * scale

                    // Cabin silhouette with aerodynamic sloped windshield
                    val cabPath = Path().apply {
                        moveTo(cabStartX, truckH - 4f * scale)
                        lineTo(cabStartX, cabTopY)
                        lineTo(cabStartX + cabW * 0.45f, cabTopY)
                        lineTo(cabStartX + cabW, cabTopY + cabH * 0.45f)
                        lineTo(cabStartX + cabW, truckH - 4f * scale)
                        close()
                    }
                    drawPath(
                        path = cabPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF047857), // Deep emerald
                                Color(0xFF10B981)  // Vibrant emerald
                            )
                        )
                    )
                    drawPath(
                        path = cabPath,
                        color = Color(0xFF34D399),
                        style = Stroke(width = 1.2f * scale)
                    )

                    // Windshield Glass (Ice blue / cyan with glossy reflection)
                    val windshieldPath = Path().apply {
                        moveTo(cabStartX + cabW * 0.15f, cabTopY + 3f * scale)
                        lineTo(cabStartX + cabW * 0.42f, cabTopY + 3f * scale)
                        lineTo(cabStartX + cabW * 0.88f, cabTopY + cabH * 0.44f)
                        lineTo(cabStartX + cabW * 0.15f, cabTopY + cabH * 0.44f)
                        close()
                    }
                    drawPath(
                        path = windshieldPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF38BDF8),
                                Color(0xFF0284C7)
                            )
                        )
                    )

                    // Headlight (Front bumper lamp)
                    drawRoundRect(
                        color = Color(0xFFFDE047), // Yellow glowing headlight
                        topLeft = Offset(truckW - 2.5f * scale, truckH - 18f * scale),
                        size = Size(3.5f * scale, 6f * scale),
                        cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale)
                    )

                    // Headlight forward light beam
                    val headlightBeam = Path().apply {
                        moveTo(truckW + 1f * scale, truckH - 15f * scale)
                        lineTo(truckW + 35f * scale, truckH - 24f * scale)
                        lineTo(truckW + 35f * scale, truckH - 2f * scale)
                        close()
                    }
                    drawPath(
                        path = headlightBeam,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x66FDE047),
                                Color(0x00FDE047)
                            )
                        )
                    )

                    // Front lower bumper (Dark chrome)
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(truckW - 6f * scale, truckH - 10f * scale),
                        size = Size(8f * scale, 6f * scale),
                        cornerRadius = CornerRadius(2f * scale, 2f * scale)
                    )

                    // Rear red taillight
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(-1f * scale, truckH - 16f * scale),
                        size = Size(2.5f * scale, 5f * scale),
                        cornerRadius = CornerRadius(1f * scale, 1f * scale)
                    )
                }

                // --- 5. TRUCK TIRES (svg width 24px, position absolute bottom: 0) ---
                // Padding: left ~12dp, right ~10dp across 130dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (14 * scale).dp, end = (12 * scale).dp, bottom = (0.5f * scale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Rear Wheel (24dp)
                    TruckWheel(wheelRotation = wheelRotation, scale = scale)
                    // Mid-wheel chassis tank connector
                    Box(
                        modifier = Modifier
                            .width((32 * scale).dp)
                            .height((8 * scale).dp)
                            .offset(y = (-8 * scale).dp)
                            .clip(RoundedCornerShape((2 * scale).dp))
                            .background(Color(0xFF334155))
                    )
                    // Front Wheel (24dp)
                    TruckWheel(wheelRotation = wheelRotation, scale = scale)
                }
            }
        }

        // Optional Caption & Subtitle
        if (!caption.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (!subCaption.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subCaption,
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 24dp Truck Wheel with deep tread rubber and rotating alloy spokes.
 */
@Composable
private fun TruckWheel(
    wheelRotation: Float,
    scale: Float
) {
    Canvas(
        modifier = Modifier.size((24 * scale).dp)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(radius, radius)

        // Outer Dark Rubber Tire
        drawCircle(
            color = Color(0xFF1E293B),
            radius = radius,
            center = center
        )

        // Tire Tread Rim
        drawCircle(
            color = Color(0xFF0F172A),
            radius = radius * 0.85f,
            center = center
        )

        // Inner Alloy Rim Base (Silver/Slate)
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = radius * 0.65f,
            center = center
        )

        // Rotating Spokes
        rotate(wheelRotation, pivot = center) {
            // 4-spoke cross design
            val spokeLen = radius * 0.55f
            val spokeWidth = 2f * scale
            val spokeColor = Color(0xFF1E293B)

            drawLine(
                color = spokeColor,
                start = Offset(center.x - spokeLen, center.y),
                end = Offset(center.x + spokeLen, center.y),
                strokeWidth = spokeWidth
            )
            drawLine(
                color = spokeColor,
                start = Offset(center.x, center.y - spokeLen),
                end = Offset(center.x, center.y + spokeLen),
                strokeWidth = spokeWidth
            )
            // Diagonal spokes for rich detail
            val diag = spokeLen * 0.707f
            drawLine(
                color = spokeColor,
                start = Offset(center.x - diag, center.y - diag),
                end = Offset(center.x + diag, center.y + diag),
                strokeWidth = spokeWidth * 0.8f
            )
            drawLine(
                color = spokeColor,
                start = Offset(center.x - diag, center.y + diag),
                end = Offset(center.x + diag, center.y - diag),
                strokeWidth = spokeWidth * 0.8f
            )
        }

        // Center Chrome Hubcap
        drawCircle(
            color = Color(0xFF10B981), // Emerald center cap
            radius = radius * 0.24f,
            center = center
        )
    }
}

/**
 * Fullscreen or overlay Loading Screen displaying the animated delivery truck loader.
 */
@Composable
fun TruckLoadingScreen(
    message: String = "Loading...",
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = SleekShapes.xxl,
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeliveryTruckLoader(
                    scale = 1.15f,
                    caption = message,
                    subCaption = subtitle ?: "Preparing your financial records safely offline"
                )
            }
        }
    }
}

/**
 * Modal Dialog with Delivery Truck Loader for blocking operations
 * (e.g. data restore, encrypted backup creation, database sync).
 */
@Composable
fun TruckLoadingDialog(
    isOpen: Boolean,
    message: String,
    subtitle: String? = null,
    onDismissRequest: () -> Unit = {}
) {
    if (isOpen) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = SleekShapes.xxl,
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DeliveryTruckLoader(
                        scale = 1.1f,
                        caption = message,
                        subCaption = subtitle ?: "Please hold on while processing completes"
                    )
                }
            }
        }
    }
}
