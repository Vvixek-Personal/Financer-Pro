package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sleek Interface Theme Colors (Backed by dynamic Compose states)
var activeSleekPrimary by mutableStateOf(Color(0xFFA8B7F2))
var activeSleekPrimaryContainer by mutableStateOf(Color(0xFF30384F))
var activeSleekOnPrimaryContainer by mutableStateOf(Color(0xFFE4E9FF))

val SleekPrimary: Color get() = activeSleekPrimary
val SleekPrimaryContainer: Color get() = activeSleekPrimaryContainer
val SleekOnPrimaryContainer: Color get() = activeSleekOnPrimaryContainer

// Predefined Themes Palette
fun getPresetThemeColors(index: Int, customHue: Float): Triple<Color, Color, Color> {
    return when (index) {
        0 -> Triple(Color(0xFFA8B7F2), Color(0xFF30384F), Color(0xFFE4E9FF)) // Vector Lavender
        1 -> Triple(Color(0xFF5A6370), Color(0xFFE2E8F0), Color(0xFF1E293B)) // Slate Grey
        2 -> Triple(Color(0xFF1A365D), Color(0xFFDBEAFE), Color(0xFF1E3A8A)) // Navy Blue
        3 -> Triple(Color(0xFF2B4C7E), Color(0xFFDCE6F1), Color(0xFF1A3050)) // Steel Blue
        4 -> Triple(Color(0xFF4E6B50), Color(0xFFE2EFE3), Color(0xFF263A28)) // Sage Green
        5 -> Triple(Color(0xFF0D9488), Color(0xFFCCFBF1), Color(0xFF115E59)) // Teal
        6 -> Triple(Color(0xFF16A34A), Color(0xFFDCFCE7), Color(0xFF15803D)) // Forest Green
        7 -> Triple(Color(0xFF65A30D), Color(0xFFECFCCB), Color(0xFF3F6212)) // Olive Green
        8 -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF92400E)) // Gold/Amber
        9 -> Triple(Color(0xFFEA580C), Color(0xFFFFEDD5), Color(0xFF9A3412)) // Bronze/Orange
        10 -> Triple(Color(0xFF854D0E), Color(0xFFFEF3C7), Color(0xFF78350F)) // Brown/Cocoa
        11 -> Triple(Color(0xFFE11D48), Color(0xFFFFE4E6), Color(0xFF9F1239)) // Rose
        12 -> Triple(Color(0xFF9D174D), Color(0xFFFCE7F3), Color(0xFF701A75)) // Plum
        13 -> Triple(Color(0xFF7C3AED), Color(0xFFEDE9FE), Color(0xFF5B21B6)) // Purple
        14 -> Triple(Color(0xFF6366F1), Color(0xFFE0E7FF), Color(0xFF3730A3)) // Lavender
        15 -> { // Dynamic custom HSL/HSV color
            val p = Color.hsv(customHue, 0.75f, 0.65f)
            val pc = Color.hsv(customHue, 0.25f, 0.92f)
            val opc = Color.hsv(customHue, 0.9f, 0.25f)
            Triple(p, pc, opc)
        }
        else -> Triple(Color(0xFF0061A4), Color(0xFFD3E4FF), Color(0xFF001D36))
    }
}

var isDarkModeActive by mutableStateOf(false)
var themeModeState by mutableStateOf("dark") // "light", "dark", "device"
var isFollowDeviceColorsState by mutableStateOf(false)
var isHighContrastActive by mutableStateOf(false)

data class PalettePreviewColors(
    val topLeft: Color,
    val topRight: Color,
    val bottomLeft: Color,
    val bottomRight: Color
)

fun getThemePalettePreview(index: Int, customHue: Float): PalettePreviewColors {
    return when (index) {
        0 -> PalettePreviewColors(Color(0xFF80B3FF), Color(0xFFC2D6F0), Color(0xFF0052CC), Color(0xFFA0B0C0))
        1 -> PalettePreviewColors(Color(0xFFB0C4DE), Color(0xFFE0E6ED), Color(0xFF0052CC), Color(0xFFB8C4D0))
        2 -> PalettePreviewColors(Color(0xFFC6D8FF), Color(0xFFE8EEFF), Color(0xFF335C98), Color(0xFF94ABCF))
        3 -> PalettePreviewColors(Color(0xFFC8D4E6), Color(0xFFE4EBF5), Color(0xFF4A607A), Color(0xFF8A9DB5))
        4 -> PalettePreviewColors(Color(0xFFC5D9DB), Color(0xFFE2ECED), Color(0xFF405659), Color(0xFF8CA1A3))
        5 -> PalettePreviewColors(Color(0xFF80F0E3), Color(0xFFB8F8F0), Color(0xFF007A70), Color(0xFF40C0B0))
        6 -> PalettePreviewColors(Color(0xFFA8E6A3), Color(0xFFD4F5D2), Color(0xFF2E7D32), Color(0xFF81C784))
        7 -> PalettePreviewColors(Color(0xFFC8E0B8), Color(0xFFE4F0DC), Color(0xFF4D6638), Color(0xFF90A880))
        8 -> PalettePreviewColors(Color(0xFFFDE047), Color(0xFFFEF08A), Color(0xFF715A00), Color(0xFFCA8A04))
        9 -> PalettePreviewColors(Color(0xFFFED7AA), Color(0xFFFFEDD5), Color(0xFF9A3412), Color(0xFFFB923C))
        10 -> PalettePreviewColors(Color(0xFFF5D0FE), Color(0xFFFAE8FF), Color(0xFF701A75), Color(0xFFC084FC))
        11 -> PalettePreviewColors(Color(0xFFFECDD3), Color(0xFFFFE4E6), Color(0xFF881337), Color(0xFFFB7185))
        12 -> PalettePreviewColors(Color(0xFFFBCFE8), Color(0xFFFCE7F3), Color(0xFF831843), Color(0xFFF472B6))
        13 -> PalettePreviewColors(Color(0xFFF0ABFC), Color(0xFFFA88FF), Color(0xFF701A75), Color(0xFFE879F9))
        14 -> PalettePreviewColors(Color(0xFFDDD6FE), Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFFA78BFA))
        15 -> {
            val p = Color.hsv(customHue, 0.75f, 0.65f)
            val pc = Color.hsv(customHue, 0.25f, 0.92f)
            val darkPc = Color.hsv(customHue, 0.40f, 0.70f)
            PalettePreviewColors(pc, Color(0xFFE5E7EB), p, darkPc)
        }
        else -> PalettePreviewColors(Color(0xFF80B3FF), Color(0xFFC2D6F0), Color(0xFF0052CC), Color(0xFFA0B0C0))
    }
}

fun updateThemeColors(index: Int, customHue: Float) {
    val (p, pc, opc) = getPresetThemeColors(index, customHue)
    activeSleekPrimary = p
    activeSleekPrimaryContainer = pc
    activeSleekOnPrimaryContainer = opc
}

/** Lightens a colour toward white by [amount] (0..1) — used to keep accent icons readable in dark mode. */
fun lighten(color: Color, amount: Float): Color = Color(
    red = (color.red + (1f - color.red) * amount).coerceIn(0f, 1f),
    green = (color.green + (1f - color.green) * amount).coerceIn(0f, 1f),
    blue = (color.blue + (1f - color.blue) * amount).coerceIn(0f, 1f),
    alpha = color.alpha
)

fun mixPrimaryWithColor(primary: Color, base: Color, fraction: Float): Color {
    return Color(
        red = (primary.red * fraction + base.red * (1f - fraction)).coerceIn(0f, 1f),
        green = (primary.green * fraction + base.green * (1f - fraction)).coerceIn(0f, 1f),
        blue = (primary.blue * fraction + base.blue * (1f - fraction)).coerceIn(0f, 1f),
        alpha = 1.0f
    )
}

val SleekBg: Color get() = if (isDarkModeActive) {
    mixPrimaryWithColor(activeSleekPrimary, Color(0xFF121418), 0.05f)
} else {
    mixPrimaryWithColor(activeSleekPrimary, Color(0xFFF7F8FC), 0.04f)
}

val SleekSurface: Color get() = if (isDarkModeActive) {
    mixPrimaryWithColor(activeSleekPrimary, Color(0xFF1B1E24), 0.07f)
} else {
    Color(0xFFFFFFFF)
}

val SleekSurfaceElevated: Color get() = if (isDarkModeActive) {
    mixPrimaryWithColor(activeSleekPrimary, Color(0xFF242830), 0.09f)
} else {
    Color(0xFFFFFFFF)
}

val SleekSurfaceVariant: Color get() = SleekSurfaceElevated

val SleekBorder: Color get() = if (isHighContrastActive) {
    if (isDarkModeActive) Color(0xFF64748B) else Color(0xFF334155)
} else if (isDarkModeActive) {
    Color(0x1FFFFFFF)
} else {
    Color(0xFFE4E7F0)
}

val SleekTextPrimary: Color get() = if (isDarkModeActive) {
    Color(0xFFF1F2F4)
} else {
    Color(0xFF0F172A)
}

val SleekTextSecondary: Color get() = if (isDarkModeActive) {
    Color(0xFFA6ABB6)
} else {
    Color(0xFF64748B)
}

val SleekGlassBg: Color get() = if (isDarkModeActive) {
    Color(0xFF1B1E24).copy(alpha = 0.94f)
} else {
    Color(0xFFFFFFFF).copy(alpha = 0.88f)
}

val SleekGlassBorder: Color get() = if (isDarkModeActive) {
    Color(0xFFFFFFFF).copy(alpha = 0.08f)
} else {
    Color(0xFF000000).copy(alpha = 0.07f)
}

val SleekNeutralLight: Color get() = if (isDarkModeActive) Color(0xFF2C3242) else Color(0xFFE2E6EE)

// Calm, premium flat surface gradient for hero net balance card
fun getHeroCardGradient(): Brush {
    return if (isDarkModeActive) {
        Brush.linearGradient(
            colors = listOf(SleekSurfaceElevated, SleekSurface),
            start = Offset(0f, 0f),
            end = Offset(0f, 600f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(SleekSurface, SleekSurfaceElevated),
            start = Offset(0f, 0f),
            end = Offset(0f, 600f)
        )
    }
}

// Semantic Alerts (Strict Fintech Palette: green=income, red=expense, amber=warning)
val ExpenseRed = Color(0xFFDC2626)
val ExpenseRedBg: Color get() = if (isDarkModeActive) Color(0xFFDC2626).copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.08f)
val IncomeGreen = Color(0xFF16A34A)
val IncomeGreenBg: Color get() = if (isDarkModeActive) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.08f)
val SavingGold = Color(0xFFD97706)
val WarningOrange = Color(0xFFD97706)
val InfoBlue = Color(0xFF0284C7)

// Backward-compatible aliases for "Sleek Interface" look
val SlateDarkBg: Color get() = SleekBg
val SlateDarkCard: Color get() = SleekSurface
val SlateDarkBorder: Color get() = SleekBorder
val SlateTextPrimary: Color get() = SleekTextPrimary
val SlateTextSecondary: Color get() = SleekTextSecondary
val EmeraldPrimary: Color get() = SleekPrimary
val EmeraldSecondary: Color get() = SleekPrimaryContainer
val AmberTertiary: Color get() = SleekPrimaryContainer
