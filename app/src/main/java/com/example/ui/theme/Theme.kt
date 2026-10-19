package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

// Picks readable text/icon color for content drawn on top of `background`,
// based on that color's actual luminance — instead of hardcoding onPrimary
// to a fixed theme token (SleekBg/SleekSurface) that has no relation to how
// light or dark the currently selected accent preset actually is. Without
// this, some of the 16 accent presets (and the custom hue slider) produced
// low-contrast or unreadable text on primary-colored buttons/chips.
private fun textColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF1A1C1E) else Color(0xFFF1F1F5)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = when (themeModeState) {
      "dark" -> true
      "light" -> false
      "device" -> isSystemInDarkTheme()
      else -> isDarkModeActive
  },
  dynamicColor: Boolean = isFollowDeviceColorsState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  // isDarkModeActive is the single flag every Sleek* color token reads.
  // Update it BEFORE colorScheme is computed below, in the same composition
  // pass (not via SideEffect, which runs after composition and made colors
  // one frame stale / caused mismatches on toggle and on "device" mode).
  isDarkModeActive = darkTheme
  val onPrimaryColor = textColorFor(SleekPrimary)
  val onSecondaryColor = textColorFor(SleekPrimaryContainer)
  val colorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
    dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
    darkTheme -> darkColorScheme(
      primary = SleekPrimary,
      secondary = SleekPrimaryContainer,
      tertiary = SavingGold,
      background = SleekBg,
      surface = SleekSurface,
      onPrimary = onPrimaryColor,
      onSecondary = onSecondaryColor,
      onBackground = SleekTextPrimary,
      onSurface = SleekTextPrimary,
      outline = SleekBorder
    )
    else -> lightColorScheme(
      primary = SleekPrimary,
      secondary = SleekPrimaryContainer,
      tertiary = SavingGold,
      background = SleekBg,
      surface = SleekSurface,
      onPrimary = onPrimaryColor,
      onSecondary = onSecondaryColor,
      onBackground = SleekTextPrimary,
      onSurface = SleekTextPrimary,
      outline = SleekBorder
    )
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    AdaptiveScalingProvider {
        content()
    }
  }
}
