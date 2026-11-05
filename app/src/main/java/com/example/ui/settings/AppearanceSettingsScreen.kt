package com.example.ui

import androidx.activity.compose.BackHandler

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppearanceScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isFollowDeviceColors by viewModel.isFollowDeviceColors.collectAsStateWithLifecycle()
    val themeIndex by viewModel.themeIndex.collectAsStateWithLifecycle()
    val customThemeHue by viewModel.customThemeHue.collectAsStateWithLifecycle()
    val textSize by viewModel.textSizeOption.collectAsStateWithLifecycle()
    val isCompact by viewModel.isCompactLayout.collectAsStateWithLifecycle()
    val isAnim by viewModel.isAnimationEnabled.collectAsStateWithLifecycle()
    val fontFamilyChoice by viewModel.fontFamily.collectAsStateWithLifecycle()
    val isHaptics by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val isHighContrast by viewModel.highContrastMode.collectAsStateWithLifecycle()
    val isCardGlow by viewModel.cardGlowAnimation.collectAsStateWithLifecycle()
    val showCents by viewModel.showCents.collectAsStateWithLifecycle()
    val confirmExit by viewModel.confirmBeforeExit.collectAsStateWithLifecycle()
    val quickAdd by viewModel.quickAddEnabled.collectAsStateWithLifecycle()
    val showTags by viewModel.showTransactionTags.collectAsStateWithLifecycle()
    val longPressEdit by viewModel.longPressToEdit.collectAsStateWithLifecycle()

    var showCustomHuePicker by remember { mutableStateOf(false) }
    var tempHue by remember(customThemeHue) { mutableStateOf(customThemeHue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Appearance", onBack, subtitle = "Theme, text size, layout and motion")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 1. Top Mode Selector Segmented Control (Light / Dark / Device)
            Surface(
                shape = CircleShape,
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Triple("light", "Light", AppIcons.LightMode),
                        Triple("dark", "Dark", AppIcons.DarkMode),
                        Triple("device", "Device", AppIcons.Tv)
                    ).forEach { (modeKey, label, icon) ->
                        val isSelected = themeMode.equals(modeKey, ignoreCase = true) || (modeKey == "device" && themeMode.equals("system", ignoreCase = true))
                        Surface(
                            onClick = { viewModel.updateThemeMode(modeKey) },
                            shape = CircleShape,
                            color = if (isSelected) SleekPrimary else Color.Transparent,
                            contentColor = if (isSelected) onAccentColor else SleekTextPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 4x4 Theme Swatches Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 4) {
                            val index = row * 4 + col
                            val isSelected = themeIndex == index
                            val preview = getThemePalettePreview(index, customThemeHue)

                            Card(
                                onClick = {
                                    viewModel.updateTheme(index)
                                    if (index == 15) {
                                        showCustomHuePicker = true
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkModeActive) Color(0xFF23252B) else Color(0xFFF0F2F5)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) SleekPrimary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(76.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index < 15) {
                                        Box(
                                            modifier = Modifier.size(46.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(color = preview.topLeft, startAngle = 180f, sweepAngle = 90f, useCenter = true)
                                                drawArc(color = preview.topRight, startAngle = 270f, sweepAngle = 90f, useCenter = true)
                                                drawArc(color = preview.bottomRight, startAngle = 0f, sweepAngle = 90f, useCenter = true)
                                                drawArc(color = preview.bottomLeft, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                                            }

                                             if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(18.dp)
                                                        .background(SleekPrimary, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = AppIcons.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.size(46.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Edit,
                                                contentDescription = "Custom Hue",
                                                tint = SleekTextPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(18.dp)
                                                        .background(SleekPrimary, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = AppIcons.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
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
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SleekBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Follow Device Colors Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Follow device colors",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SleekTextPrimary
                )
                Switch(
                    checked = isFollowDeviceColors,
                    onCheckedChange = { viewModel.toggleFollowDeviceColors(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1967D2)
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("TEXT SIZE PREFERENCE", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.data.UiPrefs.textSizeLabels.forEach { option ->
                        val selected = textSize == option
                        Surface(
                            onClick = {
                                if (textSize != option) {
                                    com.example.data.UiPrefs.setTextSize(viewModel.appSettingsManager, option)
                                    // Rebuild the activity so the new font scale is applied everywhere.
                                    (context as? android.app.Activity)?.recreate()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) SleekPrimary.copy(alpha = 0.14f) else SleekSurface,
                            border = BorderStroke(1.dp, if (selected) SleekPrimary else SleekBorder)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Aa",
                                    fontSize = (11 * com.example.data.UiPrefs.scaleForLabel(option)).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) SleekPrimary else SleekTextPrimary
                                )
                                Text(
                                    text = option,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (selected) SleekPrimary else SleekTextSecondary
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Scales all app text on top of your device font size. Applies instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("LAYOUT & ANIMATIONS", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Compact Dashboard Layout", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Displays tighter card padding for more data visibility", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = isCompact, onCheckedChange = { viewModel.toggleCompactLayout(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Fluid Animations", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Smooth transitions and fluid budget indicators", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = isAnim, onCheckedChange = { viewModel.toggleAnimationEnabled(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Card Glow & Shimmer Effects", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Lively glowing borders on cards and active milestones", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = isCardGlow, onCheckedChange = { viewModel.toggleCardGlow(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Haptic Touch Feedback", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Tactile micro-vibrations on button taps & operations", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = isHaptics, onCheckedChange = { viewModel.toggleHaptics(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("High Contrast Mode", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Sharpened borders and high-visibility content edges", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = isHighContrast, onCheckedChange = { viewModel.toggleHighContrast(it) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("TYPOGRAPHY & FONT STYLE", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Sans Serif" to "Modern Crisp (Default)",
                        "Rounded / Casual" to "Warm & Friendly (SF Pro Rounded / Sans fallback)",
                        "Tech Monospace" to "Fintech & Terminal",
                        "Classic Serif" to "Editorial & Elegant"
                    ).forEach { (fontKey, fontDesc) ->
                        val isSelected = fontFamilyChoice == fontKey || (fontFamilyChoice == "Default" && fontKey == "Sans Serif")
                        Surface(
                            onClick = { viewModel.updateFontFamily(fontKey) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else SleekSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = fontKey,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SleekPrimary else SleekTextPrimary,
                                        fontFamily = getFontFamilyForChoice(fontKey),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = fontDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SleekTextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = AppIcons.CheckCircle,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("FINANCIAL DISPLAY", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppearanceSwitchRow(
                        title = "Show Cents in Amounts",
                        subtitle = "Off rounds every amount to whole units across the app",
                        checked = showCents,
                        onCheckedChange = { viewModel.toggleShowCents(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)
                    AppearanceSwitchRow(
                        title = "Confirm Before Exiting",
                        subtitle = "Asks before the back button leaves the app from the dashboard",
                        checked = confirmExit,
                        onCheckedChange = { viewModel.toggleConfirmBeforeExit(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)
                    AppearanceSwitchRow(
                        title = "Show Fast Add Button",
                        subtitle = "Keeps the floating add button visible over the dashboard",
                        checked = quickAdd,
                        onCheckedChange = { viewModel.toggleQuickAdd(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("TRANSACTION LIST", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AppearanceSwitchRow(
                        title = "Show Tags on Rows",
                        subtitle = "Displays transaction tags inline in the recent activity list",
                        checked = showTags,
                        onCheckedChange = { viewModel.toggleShowTransactionTags(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)
                    AppearanceSwitchRow(
                        title = "Long-Press to Edit",
                        subtitle = "Hold a transaction to open it instead of tapping",
                        checked = longPressEdit,
                        onCheckedChange = { viewModel.toggleLongPressToEdit(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    if (showCustomHuePicker) {
        AlertDialog(
            onDismissRequest = { showCustomHuePicker = false },
            title = { Text("Custom Theme Color Accent", color = SleekTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.hsv(tempHue, 0.75f, 0.65f), CircleShape)
                            .border(1.dp, SleekBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hue: ${tempHue.toInt()}°", color = SleekTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tempHue,
                        onValueChange = {
                            tempHue = it
                            viewModel.updateCustomThemeHue(it)
                        },
                        valueRange = 0f..360f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomHuePicker = false }) {
                    Text("Done", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * Compact, theme-aware switch row used by the Appearance screen's option groups.
 * Kept local so the design language of every row in this screen stays identical.
 */
@Composable
private fun AppearanceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
