package com.example.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Process-wide, Compose-observable UI preferences.
 *
 * These values previously sat only inside SharedPreferences (e.g. "text_size_option",
 * "date_format", "first_day_of_week", "is_compact_layout"). That meant the Appearance and
 * Date & Time screens wrote them, but nothing in the UI ever read them back — the toggles
 * looked like they worked while changing nothing at all.
 *
 * UiPrefs is the single read path for those values:
 *  - [textScale] is consumed by BaseActivity to rebuild the Activity configuration (this is what
 *    actually changes typography app-wide, without restarting the process).
 *  - [compactLayout] and [dateFormat] / [firstDayOfWeek] are consumed by Compose directly.
 *
 * Every setter persists through AppSettingsManager and updates the observable state in the same
 * call, so the UI reacts instantly and the value survives a restart.
 */
object UiPrefs {

    /** Multiplier applied to the user's system font scale. 1f = system default. */
    var textScale by mutableFloatStateOf(1f)
        private set

    /** Tighter paddings / denser rows across scrollable screens. */
    var compactLayout by mutableStateOf(false)
        private set

    /** One of "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd". */
    var dateFormat by mutableStateOf("dd/MM/yyyy")
        private set

    /** One of "Monday", "Sunday", "Saturday". */
    var firstDayOfWeek by mutableStateOf("Monday")
        private set

    /**
     * True when transaction rows should mask their amount. Set from the privacy settings
     * (hide sensitive amounts + privacy mode); read by list rows at draw time.
     */
    var maskRowAmounts by mutableStateOf(false)

    /** Mirrors the "Show cents" appearance setting, read by Money.format on every call. */
    var showCents by mutableStateOf(true)
        private set

    /** True when the user wants a confirmation before leaving the app from the dashboard. */
    var confirmBeforeExit by mutableStateOf(false)
        private set

    /** Show tags inline on transaction rows. */
    var showTransactionTags by mutableStateOf(true)
        private set

    /** Show the floating fast-add button over content screens. */
    var quickAddEnabled by mutableStateOf(true)
        private set

    /** Long-press (instead of tap) opens a transaction. */
    var longPressToEdit by mutableStateOf(true)
        private set

    /** The concrete text-size label the user picked (Normal / Large / ...). */
    var textSizeLabel by mutableStateOf("Normal")
        private set

    /** Bumped whenever the activity is recreated so dependent composables can recompute sizes. */
    var textScaleGeneration by mutableIntStateOf(0)
        private set

    val textSizeLabels = listOf("Small", "Normal", "Large", "Extra Large")

    fun scaleForLabel(label: String): Float = when (label) {
        "Small" -> 0.88f
        "Large", "Large (1.15x)" -> 1.12f
        "Extra Large", "Extra Large (1.25x)" -> 1.24f
        "Medium", "" -> 1f
        else -> 1f
    }

    /** Label for a persisted scale value, so old installs that stored a scale still read correctly. */
    fun labelForScale(scale: Float): String = when {
        scale <= 0.92f -> "Small"
        scale >= 1.2f -> "Extra Large"
        scale >= 1.06f -> "Large"
        else -> "Normal"
    }

    /** Pull every value from the persisted settings snapshot. Safe to call repeatedly. */
    fun syncFrom(settings: AppSettingsState) {
        textSizeLabel = settings.textSize.takeIf { it.isNotBlank() } ?: "Normal"
        textScale = scaleForLabel(textSizeLabel)
        compactLayout = settings.compactLayout
        dateFormat = settings.dateFormat.takeIf { it.isNotBlank() } ?: "dd/MM/yyyy"
        firstDayOfWeek = settings.firstDayOfWeek.takeIf { it.isNotBlank() } ?: "Monday"
        showCents = settings.showCents
        confirmBeforeExit = settings.confirmBeforeExit
        showTransactionTags = settings.showTransactionTags
        quickAddEnabled = settings.quickAddEnabled
        longPressToEdit = settings.longPressToEdit
    }

    fun setShowTransactionTags(manager: AppSettingsManager, enabled: Boolean) {
        showTransactionTags = enabled
        manager.dispatch(AppSettingsIntent.UpdateShowTransactionTags(enabled))
    }

    fun setQuickAdd(manager: AppSettingsManager, enabled: Boolean) {
        quickAddEnabled = enabled
        manager.dispatch(AppSettingsIntent.UpdateQuickAdd(enabled))
    }

    fun setLongPressToEdit(manager: AppSettingsManager, enabled: Boolean) {
        longPressToEdit = enabled
        manager.dispatch(AppSettingsIntent.UpdateLongPressToEdit(enabled))
    }

    fun setShowCents(manager: AppSettingsManager, enabled: Boolean) {
        showCents = enabled
        manager.dispatch(AppSettingsIntent.UpdateShowCents(enabled))
    }

    fun setConfirmBeforeExit(manager: AppSettingsManager, enabled: Boolean) {
        confirmBeforeExit = enabled
        manager.dispatch(AppSettingsIntent.UpdateConfirmBeforeExit(enabled))
    }


    fun setTextSize(manager: AppSettingsManager, label: String) {
        val safe = if (label in textSizeLabels) label else "Normal"
        textSizeLabel = safe
        textScale = scaleForLabel(safe)
        manager.dispatch(AppSettingsIntent.UpdateTextSize(safe))
    }

    fun setCompactLayout(manager: AppSettingsManager, enabled: Boolean) {
        compactLayout = enabled
        manager.dispatch(AppSettingsIntent.UpdateCompactLayout(enabled))
    }

    fun setDateFormat(manager: AppSettingsManager, format: String) {
        dateFormat = format
        manager.dispatch(AppSettingsIntent.UpdateDateFormat(format))
    }

    fun setFirstDayOfWeek(manager: AppSettingsManager, day: String) {
        firstDayOfWeek = day
        manager.dispatch(AppSettingsIntent.UpdateFirstDayOfWeek(day))
    }

    // ---------------------------------------------------------------- formatting

    private val cacheLock = Any()
    private var cachedPattern: String = ""
    private var cachedFormatter: SimpleDateFormat? = null

    fun formatter(): SimpleDateFormat {
        val pattern = dateFormat
        synchronized(cacheLock) {
            val existing = cachedFormatter
            if (existing != null && cachedPattern == pattern) return existing
            val fresh = try {
                SimpleDateFormat(pattern, Locale.getDefault())
            } catch (_: IllegalArgumentException) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }
            cachedPattern = pattern
            cachedFormatter = fresh
            return fresh
        }
    }

    /** Formats a timestamp using the user's chosen date format. */
    fun formatDate(timestamp: Long): String = formatter().format(Date(timestamp))

    /** Formats a timestamp using the chosen format, with an optional extra time suffix. */
    fun formatDateTime(timestamp: Long, timePattern: String = "hh:mm a"): String =
        formatDate(timestamp) + ", " + SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))

    /** Calendar.DAY_OF_WEEK constant for the user's chosen first day of week. */
    fun firstDayOfWeekConstant(): Int = when (firstDayOfWeek) {
        "Sunday" -> Calendar.SUNDAY
        "Saturday" -> Calendar.SATURDAY
        else -> Calendar.MONDAY
    }

    /** A Calendar for [monthAnchor] rotated so the user's first day of week is column 0. */
    fun weekStartCalendar(monthAnchor: Calendar): Calendar {
        val clone = monthAnchor.clone() as Calendar
        clone.firstDayOfWeek = firstDayOfWeekConstant()
        return clone
    }

    /** True when the persisted date format puts the day first (used by manual formatters). */
    fun dayFirst(): Boolean = dateFormat.startsWith("dd")
}
