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

// Picks a readable text/icon color for content drawn on top of `background`, based on that
// color's actual luminance — instead of hardcoding onPrimary to a fixed theme token that has no
// relation to how light or dark the currently selected accent preset actually is. Without this,
// some of the 16 accent presets (and the custom hue slider) produced low-contrast text on
// primary-colored buttons/chips.
fun textColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF10131A) else Color(0xFFFFFFFF)

/** Text/icon color that reads correctly on top of the current accent color (SleekPrimary). */
val onAccentColor: Color
    get() = textColorFor(SleekPrimary)

/** Subtle elevated fill used for chips, inputs and secondary surfaces. */
val SleekInputBg: Color
    get() = if (isDarkModeActive) Color(0xFF1B1F2A).copy(alpha = 0.85f) else Color(0xFFF3F5FA)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = when (themeModeState) {
      "dark" -> true
      "light" -> false
      "device", "system" -> isSystemInDarkTheme()
      // Legacy persisted values from older builds were stored lowercase in
      // FinanceViewModel's init block. Treat anything unrecognised as "follow device" so a
      // stale value can never strand the app in the wrong theme.
      else -> isSystemInDarkTheme()
  },
  dynamicColor: Boolean = isFollowDeviceColorsState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  // isDarkModeActive is the single flag every Sleek* color token reads. Update it BEFORE
  // colorScheme is computed below, in the same composition pass (not via SideEffect, which runs
  // after composition and made colors one frame stale / caused mismatches on toggle and in
  // "device" mode).
  isDarkModeActive = darkTheme

  // Cache the two schemes per (dark, accent) pair. Without this, every recomposition of the
  // theme root — which happens on every drawer toggle and every setting change — allocated a new
  // ColorScheme plus a full MaterialTheme scope.
  val schemeKey = SchemeKey(darkTheme, SleekPrimary.value, SleekPrimaryContainer.value, dynamicColor)
  val colorScheme = if (dynamicColor) {
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
  } else {
    schemeCache.getOrPut(schemeKey) {
      val onPrimaryColor = textColorFor(SleekPrimary)
      val onSecondaryColor = textColorFor(SleekPrimaryContainer)
      if (darkTheme) {
        darkColorScheme(
          primary = SleekPrimary,
          onPrimary = onPrimaryColor,
          primaryContainer = SleekPrimaryContainer,
          onPrimaryContainer = SleekOnPrimaryContainer,
          secondary = SleekPrimaryContainer,
          onSecondary = onSecondaryColor,
          tertiary = SavingGold,
          background = SleekBg,
          onBackground = SleekTextPrimary,
          surface = SleekSurface,
          onSurface = SleekTextPrimary,
          surfaceVariant = SleekSurfaceElevated,
          onSurfaceVariant = SleekTextSecondary,
          outline = SleekBorder,
          outlineVariant = SleekBorder.copy(alpha = 0.6f),
          error = Color(0xFFFF6B6B),
          onError = Color(0xFF2B0A0A),
          errorContainer = Color(0xFF4A1212),
          onErrorContainer = Color(0xFFFFD9D9),
          surfaceTint = SleekPrimary,
          inverseSurface = Color(0xFFF1F3F9),
          inverseOnSurface = Color(0xFF10131A),
        )
      } else {
        lightColorScheme(
          primary = SleekPrimary,
          onPrimary = onPrimaryColor,
          primaryContainer = SleekPrimaryContainer,
          onPrimaryContainer = SleekOnPrimaryContainer,
          secondary = SleekPrimaryContainer,
          onSecondary = onSecondaryColor,
          tertiary = SavingGold,
          background = SleekBg,
          onBackground = SleekTextPrimary,
          surface = SleekSurface,
          onSurface = SleekTextPrimary,
          surfaceVariant = SleekSurfaceElevated,
          onSurfaceVariant = SleekTextSecondary,
          outline = SleekBorder,
          outlineVariant = SleekBorder.copy(alpha = 0.6f),
          error = Color(0xFFB3261E),
          onError = Color.White,
          errorContainer = Color(0xFFF9DEDC),
          onErrorContainer = Color(0xFF410E0B),
          surfaceTint = SleekPrimary,
          inverseSurface = Color(0xFF1B1F2A),
          inverseOnSurface = Color(0xFFF1F3F9),
        )
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography) {
    AdaptiveScalingProvider {
        content()
    }
  }
}

private data class SchemeKey(
    val dark: Boolean,
    val primary: Long,
    val primaryContainer: Long,
    val dynamic: Boolean
)

private val schemeCache = object : LinkedHashMap<SchemeKey, androidx.compose.material3.ColorScheme>(8, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SchemeKey, androidx.compose.material3.ColorScheme>?): Boolean =
        size > 6
}
