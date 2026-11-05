package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating dock navigation bar.
 *
 * Improvements over the previous version:
 *  - The selected tab's label used to be force-coloured `Color.White`, which became invisible in
 *    light mode where the active pill was a light tint. Both the pill and its label now derive
 *    from the accent colour via [onAccentColor], so every one of the 16 accents is readable.
 *  - The dock collapses to icons-only automatically on narrow screens (large font scale + small
 *    width used to overflow / clip the pill), and labels still appear for the active tab when
 *    there is room.
 *  - A subtle accent-coloured glow ring marks the active tab instead of a flat white overlay.
 *  - Haptics respect the app's haptics setting and the animations setting disables the bounce.
 */
@Composable
fun FloatingDockBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    selectedLanguage: String = "English",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current

    val screens = remember {
        listOf(
            Triple(Screen.Dashboard, AppIcons.Dashboard, "Home"),
            Triple(Screen.Expenses, AppIcons.ReceiptLong, "Transactions"),
            Triple(Screen.Analytics, AppIcons.PieChart, "Analytics"),
            Triple(Screen.Calendar, AppIcons.CalendarMonth, "Calendar")
        )
    }

    // Collapse labels when the dock would not fit (narrow phone + large font scale).
    val config = LocalConfiguration.current
    val compactDock = config.screenWidthDp < 400 || config.fontScale > 1.25f

    val dockContainerBg = if (isDarkModeActive) {
        mixPrimaryWithColor(SleekPrimary, Color(0xFF0F1219), 0.10f).copy(alpha = 0.96f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.97f)
    }
    val dockBorderColor = if (isDarkModeActive) {
        SleekPrimary.copy(alpha = 0.30f)
    } else {
        SleekBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = SleekShapes.pill,
            color = dockContainerBg,
            border = BorderStroke(1.dp, dockBorderColor),
            shadowElevation = if (isDarkModeActive) 18.dp else 12.dp,
            tonalElevation = 0.dp,
            modifier = Modifier.clip(SleekShapes.pill)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { (screen, icon, titleKey) ->
                    val isSelected = currentScreen == screen
                    DockTile(
                        icon = icon,
                        title = LanguageManager.tr(titleKey, selectedLanguage),
                        isSelected = isSelected,
                        showLabel = isSelected && !compactDock,
                        testTag = "nav_item_${screen.route}",
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScreenSelected(screen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTile(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    showLabel: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 14.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "tilePadding"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.98f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "tileScale"
    )
    val containerBg by animateColorAsState(
        targetValue = if (isSelected) SleekPrimary else Color.Transparent,
        animationSpec = tween(220, easing = AppleDecelerateEasing),
        label = "containerBgAnim"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .testTag(testTag)
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (animationsEnabled) Modifier.bouncyPress(scaleDown = 0.955f) else Modifier)
            .clip(SleekShapes.pill)
            .background(containerBg)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) SleekPrimary.copy(alpha = 0.9f) else Color.Transparent,
                shape = SleekShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.02f))
                        )
                        isDarkModeActive -> Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.03f))
                        )
                        else -> Brush.linearGradient(
                            colors = listOf(Color(0xFFF1F5F9), Color(0xFFE8EDF5))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = when {
                    isSelected -> onAccentColor
                    isDarkModeActive -> SleekTextSecondary
                    else -> Color(0xFF334155)
                },
                modifier = Modifier.size(if (isSelected) 19.dp else 18.dp)
            )
        }

        AnimatedVisibility(
            visible = showLabel,
            enter = expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(animationSpec = tween(160, easing = AppleDecelerateEasing)),
            exit = shrinkHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(animationSpec = tween(110, easing = FastOutLinearInEasing))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = onAccentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
