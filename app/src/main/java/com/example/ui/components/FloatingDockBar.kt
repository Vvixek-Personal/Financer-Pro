package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated Floating Dock Bar inspired by Video Demo:
 * Smooth sliding active pill indicator, glowing circular icon badge with spring scale bounce,
 * and translucent dark navy glass aesthetic.
 */
@Composable
fun FloatingDockBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    selectedLanguage: String = "English",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val screens = remember {
        listOf(
            Triple(Screen.Dashboard, AppIcons.Dashboard, "Home"),
            Triple(Screen.Expenses, AppIcons.ReceiptLong, "Transactions"),
            Triple(Screen.Analytics, AppIcons.PieChart, "Analytics"),
            Triple(Screen.Calendar, AppIcons.CalendarMonth, "Calendar")
        )
    }

    val selectedIndex = remember(currentScreen) {
        screens.indexOfFirst { it.first == currentScreen }.coerceAtLeast(0)
    }

    // Theme-adaptive glass capsule aesthetic
    val dockContainerBg = if (isDarkModeActive) {
        mixPrimaryWithColor(SleekPrimary, Color(0xFF10131C), 0.12f).copy(alpha = 0.92f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.96f)
    }
    val dockBorderColor = if (isDarkModeActive) {
        SleekPrimary.copy(alpha = 0.35f)
    } else {
        SleekBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Container Pill
        Surface(
            shape = SleekShapes.pill,
            color = dockContainerBg,
            border = BorderStroke(1.2.dp, dockBorderColor),
            shadowElevation = if (isDarkModeActive) 16.dp else 10.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.clip(SleekShapes.pill)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEachIndexed { index, (screen, icon, titleKey) ->
                    val isSelected = currentScreen == screen
                    val title = LanguageManager.tr(titleKey, selectedLanguage)

                    AnimatedDockTile(
                        icon = icon,
                        title = title,
                        isSelected = isSelected,
                        testTag = "nav_item_${screen.route}",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onScreenSelected(screen)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedDockTile(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val animatedPaddingHorizontal by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "tilePadding"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "tileScale"
    )

    val activePillBg = SleekPrimary
    val activePillBorder = SleekPrimary.copy(alpha = 0.85f)

    val containerBg by animateColorAsState(
        targetValue = if (isSelected) activePillBg else Color.Transparent,
        animationSpec = tween(240, easing = AppleDecelerateEasing),
        label = "containerBgAnim"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .testTag(testTag)
            .height(SleekSizes.buttonMedium)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .bouncyPress(scaleDown = 0.965f)
            .clip(SleekShapes.pill)
            .background(containerBg)
            .border(
                border = BorderStroke(
                    width = if (isSelected) 1.2.dp else 0.dp,
                    color = if (isSelected) activePillBorder else Color.Transparent
                ),
                shape = SleekShapes.pill
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = animatedPaddingHorizontal, vertical = 6.dp)
    ) {
        // Glowing Icon Badge Circle
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    } else {
                        if (isDarkModeActive) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.04f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF1F5F9),
                                    Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                )
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.9f)
                    } else {
                        if (isDarkModeActive) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) {
                    Color.White
                } else {
                    if (isDarkModeActive) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)
                },
                modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = androidx.compose.animation.expandHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(animationSpec = tween(180, easing = AppleDecelerateEasing)),
            exit = androidx.compose.animation.shrinkHorizontally(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = SleekSizes.textBodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}


