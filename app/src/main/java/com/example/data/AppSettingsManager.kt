package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettingsState(
    val appPin: String? = null,
    val isAnimationEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val themeIndex: Int = 0,
    val language: String = "English",
    val userName: String = "",
    val userDob: String = "",
    val dailyStreak: Int = 0,
    val lastStreakDate: Long = 0L,
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val currencyName: String = "Indian Rupee",
    val statsCurrencyCode: String = "INR",
    val statsCurrencySymbol: String = "₹",
    val statsCurrencyName: String = "Indian Rupee",
    val monthlyBudget: Double = 0.0,
    val isAppLocked: Boolean = false,
    val privacyModeEnabled: Boolean = false,
    val hideSensitiveAmounts: Boolean = false,
    val screenshotProtection: Boolean = false,
    val autoLockDuration: String = "NEVER",
    val biometricEnabled: Boolean = false,
    val pinLockoutUntil: Long = 0L,
    val customThemeHue: Float = 210f,
    val isFollowDeviceColors: Boolean = true,
    val fontFamily: String = "Default",
    val hapticsEnabled: Boolean = true,
    val highContrastMode: Boolean = false,
    val cardGlowAnimation: Boolean = true,
    val defaultPaymentMode: String = "CASH",
    val highAmountWarningEnabled: Boolean = false,
    val highAmountThreshold: Double = 10000.0,
    val billReminderTiming: String = "0",
    val billReminderPreferredTime: String = "09:00",
    val billAutoMarkPaid: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: String = "Never",
)

sealed class AppSettingsIntent {
    data class SetPin(val pin: String?) : AppSettingsIntent()
    data class ResetPinLockout(val attempts: Int = 0) : AppSettingsIntent()
    data class SetAnimationsEnabled(val enabled: Boolean) : AppSettingsIntent()
    data class UpdateThemeMode(val mode: String) : AppSettingsIntent()
    data class UpdateTheme(val index: Int, val hue: Float) : AppSettingsIntent()
    data class UpdateLanguage(val lang: String) : AppSettingsIntent()
    data class SetUserName(val name: String) : AppSettingsIntent()
    data class SetUserDob(val dob: String) : AppSettingsIntent()
    data class UpdateCurrency(val code: String, val symbol: String, val name: String, val convertExisting: Boolean = false) : AppSettingsIntent()
    data class UpdateStatsCurrency(val code: String, val symbol: String, val name: String) : AppSettingsIntent()
    data class UpdateMonthlyBudget(val budget: Double) : AppSettingsIntent()
    data class SetScreenshotProtection(val enabled: Boolean) : AppSettingsIntent()
    data class UpdateFontFamily(val font: String) : AppSettingsIntent()
    data class UpdateHapticsEnabled(val enabled: Boolean) : AppSettingsIntent()
    data class UpdateHighContrastMode(val enabled: Boolean) : AppSettingsIntent()
    data class UpdateCardGlowAnimation(val enabled: Boolean) : AppSettingsIntent()
    data class SetAutoLockDuration(val duration: String) : AppSettingsIntent()
    data class UpdateDefaultPaymentMode(val mode: String) : AppSettingsIntent()
    data class UpdateHighAmountWarning(val enabled: Boolean, val threshold: Double) : AppSettingsIntent()
    data class UpdateBillReminderTiming(val days: String) : AppSettingsIntent()
    data class UpdateBillReminderPreferredTime(val time: String) : AppSettingsIntent()
    data class UpdateBillAutoMarkPaid(val enabled: Boolean) : AppSettingsIntent()
    data class SetBiometricEnabled(val enabled: Boolean) : AppSettingsIntent()
    data class SetPrivacyModeEnabled(val enabled: Boolean) : AppSettingsIntent()
    data class SetHideSensitiveAmounts(val enabled: Boolean) : AppSettingsIntent()
    data class SetLockOnRestart(val enabled: Boolean) : AppSettingsIntent()
    data class SetAutoBackupEnabled(val enabled: Boolean) : AppSettingsIntent()
    object ReloadStateAfterWipe : AppSettingsIntent()
    data class UpdateAnimationEnabled(val enabled: Boolean) : AppSettingsIntent()
}

class AppSettingsManager private constructor(private val context: Context) {
    companion object {
        const val PREFS_NAME = "app_settings_prefs"
        @Volatile
        private var INSTANCE: AppSettingsManager? = null
        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsManager(context).also { INSTANCE = it }
            }
        }

        fun resetInstanceForTesting() {
            INSTANCE = null
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Remove obsolete preferences left by older Firebase, cloud-sync, and Google sign-in builds.
        prefs.edit()
            .remove("ai_consent_enabled")
            .remove("ai_daily_request_count")
            .remove("ai_last_request_date")
            .remove("sync_mode")
            .remove("last_cloud_sync_timestamp")
            .remove("cloud_sync_status")
            .remove("is_google_signed_in")
            .remove("google_account_email")
            .remove("google_profile_name")
            .remove("google_profile_photo_url")
            .apply()
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private fun loadInitialState(): AppSettingsState {
        var rawPin = prefs.getString("app_pin", null)
        if (!rawPin.isNullOrBlank() && !rawPin.startsWith("v2:") && !rawPin.startsWith("v3:")) {
            val upgraded = PinSecurityUtils.hashPin(rawPin)
            prefs.edit().putString("app_pin", upgraded).commit()
            rawPin = upgraded
        }

        val initialLockoutWall = prefs.getLong("pin_lockout_until_wall", 0L)

        return AppSettingsState(
            appPin = rawPin,
            isAppLocked = !rawPin.isNullOrBlank(),
            pinLockoutUntil = initialLockoutWall,
            language = prefs.getString("language", prefs.getString("selected_language", "English")) ?: "English",
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM",
            themeIndex = prefs.getInt("theme_index", 0),
            customThemeHue = prefs.getFloat("custom_theme_hue", 210f),
            isFollowDeviceColors = prefs.getBoolean("follow_device_colors", false),
            currencyCode = prefs.getString("currency_code", prefs.getString("selected_currency_code", "INR")) ?: "INR",
            currencySymbol = prefs.getString("currency_symbol", prefs.getString("selected_currency_symbol", "₹")) ?: "₹",
            currencyName = prefs.getString("currency_name", prefs.getString("selected_currency_name", "Indian Rupee")) ?: "Indian Rupee",
            statsCurrencyCode = prefs.getString("stats_currency_code", "INR") ?: "INR",
            statsCurrencySymbol = prefs.getString("stats_currency_symbol", "₹") ?: "₹",
            statsCurrencyName = prefs.getString("stats_currency_name", "Indian Rupee") ?: "Indian Rupee",
            monthlyBudget = prefs.getFloat("monthly_budget", 0f).toDouble(),
            privacyModeEnabled = prefs.getBoolean("privacy_mode_enabled", false),
            hideSensitiveAmounts = prefs.getBoolean("hide_sensitive_amounts", false),
            screenshotProtection = prefs.getBoolean("screenshot_protection", false),
            autoLockDuration = prefs.getString("auto_lock_duration", "NEVER") ?: "NEVER",
            biometricEnabled = prefs.getBoolean("biometric_enabled", false),
            fontFamily = prefs.getString("font_family", "Default") ?: "Default",
            hapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            highContrastMode = prefs.getBoolean("high_contrast_mode", false),
            cardGlowAnimation = prefs.getBoolean("card_glow_animation", true),
            defaultPaymentMode = prefs.getString("default_payment_mode", "CASH") ?: "CASH",
            highAmountWarningEnabled = prefs.getBoolean("high_amount_warning_enabled", false),
            highAmountThreshold = prefs.getFloat("high_amount_threshold", 10000f).toDouble(),
            billReminderTiming = prefs.getString("bill_reminder_timing", "0") ?: "0",
            billReminderPreferredTime = prefs.getString("bill_reminder_preferred_time", "09:00") ?: "09:00",
            billAutoMarkPaid = prefs.getBoolean("bill_auto_mark_paid", false),
            autoBackupEnabled = prefs.getBoolean("auto_backup_enabled", false),
            lastBackupTimestamp = prefs.getString("last_backup_timestamp", "Never") ?: "Never",
            isAnimationEnabled = prefs.getBoolean("animations_enabled", true),
        )
    }

    fun getLockoutStatus(): PinSecurityUtils.LockoutStatus {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val lockoutUntilWall = prefs.getLong("pin_lockout_until_wall", 0L)
        val lockoutUntilElapsed = prefs.getLong("pin_lockout_until_elapsed", 0L)
        val lastAttemptWall = prefs.getLong("pin_last_attempt_wall", 0L)
        val lastLockoutDuration = prefs.getLong("pin_last_lockout_duration", 0L)
        val savedBootCount = prefs.getInt("pin_saved_boot_count", 0)
        val currentBootCount = try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.BOOT_COUNT,
                0
            )
        } catch (_: Exception) { 0 }

        return PinSecurityUtils.calculateLockoutStatus(
            nowWall = nowWall,
            nowElapsed = nowElapsed,
            currentBootCount = currentBootCount,
            lockoutUntilWall = lockoutUntilWall,
            lockoutUntilElapsed = lockoutUntilElapsed,
            lastAttemptWall = lastAttemptWall,
            savedBootCount = savedBootCount,
            lastLockoutDuration = lastLockoutDuration
        )
    }

    fun recordFailedPinAttempt(): PinSecurityUtils.LockoutStatus {
        val currentAttempts = prefs.getInt("pin_failed_attempts", 0) + 1
        val delayMs = PinSecurityUtils.lockoutDelayMs(currentAttempts)
        val nowWall = System.currentTimeMillis()
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val currentBootCount = try {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.BOOT_COUNT,
                0
            )
        } catch (_: Exception) { 0 }

        val lockoutUntilWall = if (delayMs > 0) nowWall + delayMs else 0L
        val lockoutUntilElapsed = if (delayMs > 0) nowElapsed + delayMs else 0L

        prefs.edit()
            .putInt("pin_failed_attempts", currentAttempts)
            .putLong("pin_lockout_until_wall", lockoutUntilWall)
            .putLong("pin_lockout_until_elapsed", lockoutUntilElapsed)
            .putLong("pin_last_attempt_wall", nowWall)
            .putLong("pin_last_lockout_duration", delayMs)
            .putInt("pin_saved_boot_count", currentBootCount)
            .commit()

        _state.value = _state.value.copy(pinLockoutUntil = lockoutUntilWall)
        return getLockoutStatus()
    }

    fun resetPinLockoutAndAttempts() {
        prefs.edit()
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until_wall", 0L)
            .putLong("pin_lockout_until_elapsed", 0L)
            .putLong("pin_last_lockout_duration", 0L)
            .commit()
        _state.value = _state.value.copy(pinLockoutUntil = 0L)
    }

    fun reloadStateAfterWipe() {
        _state.value = loadInitialState()
    }

    fun dispatch(intent: AppSettingsIntent) {
        _state.value = when (intent) {
            is AppSettingsIntent.SetPin -> {
                if (intent.pin.isNullOrBlank()) {
                    prefs.edit().remove("app_pin").commit()
                    _state.value.copy(appPin = null, isAppLocked = false)
                } else {
                    val hashed = if (intent.pin.startsWith("v2:") || intent.pin.startsWith("v3:")) {
                        intent.pin
                    } else {
                        PinSecurityUtils.hashPin(intent.pin)
                    }
                    prefs.edit().putString("app_pin", hashed).commit()
                    _state.value.copy(appPin = hashed, isAppLocked = true)
                }
            }
            is AppSettingsIntent.UpdateLanguage -> {
                prefs.edit().putString("language", intent.lang).apply()
                _state.value.copy(language = intent.lang)
            }
            is AppSettingsIntent.UpdateThemeMode -> {
                prefs.edit().putString("theme_mode", intent.mode).apply()
                _state.value.copy(themeMode = intent.mode)
            }
            is AppSettingsIntent.UpdateTheme -> {
                prefs.edit().putInt("theme_index", intent.index).putFloat("custom_theme_hue", intent.hue).apply()
                _state.value.copy(themeIndex = intent.index, customThemeHue = intent.hue)
            }
            is AppSettingsIntent.UpdateCurrency -> {
                prefs.edit().putString("currency_code", intent.code)
                    .putString("currency_symbol", intent.symbol)
                    .putString("currency_name", intent.name).apply()
                _state.value.copy(currencyCode = intent.code, currencySymbol = intent.symbol, currencyName = intent.name)
            }
            is AppSettingsIntent.UpdateMonthlyBudget -> {
                prefs.edit().putFloat("monthly_budget", intent.budget.toFloat()).apply()
                _state.value.copy(monthlyBudget = intent.budget)
            }
            is AppSettingsIntent.SetAutoLockDuration -> {
                prefs.edit().putString("auto_lock_duration", intent.duration).apply()
                _state.value.copy(autoLockDuration = intent.duration)
            }
            is AppSettingsIntent.UpdateBillReminderTiming -> {
                prefs.edit().putString("bill_reminder_timing", intent.days).apply()
                _state.value.copy(billReminderTiming = intent.days)
            }
            is AppSettingsIntent.UpdateAnimationEnabled -> {
                prefs.edit().putBoolean("animations_enabled", intent.enabled).apply()
                _state.value.copy(isAnimationEnabled = intent.enabled)
            }
            is AppSettingsIntent.SetAnimationsEnabled -> {
                prefs.edit().putBoolean("animations_enabled", intent.enabled).apply()
                _state.value.copy(isAnimationEnabled = intent.enabled)
            }
            is AppSettingsIntent.SetUserName -> {
                prefs.edit().putString("user_name", intent.name).apply()
                _state.value.copy(userName = intent.name)
            }
            is AppSettingsIntent.SetUserDob -> {
                prefs.edit().putString("user_dob", intent.dob).apply()
                _state.value.copy(userDob = intent.dob)
            }
            is AppSettingsIntent.UpdateStatsCurrency -> {
                prefs.edit().putString("stats_currency_code", intent.code)
                    .putString("stats_currency_symbol", intent.symbol)
                    .putString("stats_currency_name", intent.name).apply()
                _state.value.copy(statsCurrencyCode = intent.code, statsCurrencySymbol = intent.symbol, statsCurrencyName = intent.name)
            }
            is AppSettingsIntent.SetScreenshotProtection -> {
                prefs.edit().putBoolean("screenshot_protection", intent.enabled).apply()
                _state.value.copy(screenshotProtection = intent.enabled)
            }
            is AppSettingsIntent.UpdateFontFamily -> {
                prefs.edit().putString("font_family", intent.font).apply()
                _state.value.copy(fontFamily = intent.font)
            }
            is AppSettingsIntent.UpdateHapticsEnabled -> {
                prefs.edit().putBoolean("haptics_enabled", intent.enabled).apply()
                _state.value.copy(hapticsEnabled = intent.enabled)
            }
            is AppSettingsIntent.UpdateHighContrastMode -> {
                prefs.edit().putBoolean("high_contrast_mode", intent.enabled).apply()
                _state.value.copy(highContrastMode = intent.enabled)
            }
            is AppSettingsIntent.UpdateCardGlowAnimation -> {
                prefs.edit().putBoolean("card_glow_animation", intent.enabled).apply()
                _state.value.copy(cardGlowAnimation = intent.enabled)
            }
            is AppSettingsIntent.UpdateDefaultPaymentMode -> {
                prefs.edit().putString("default_payment_mode", intent.mode).apply()
                _state.value.copy(defaultPaymentMode = intent.mode)
            }
            is AppSettingsIntent.UpdateHighAmountWarning -> {
                prefs.edit().putBoolean("high_amount_warning_enabled", intent.enabled)
                    .putFloat("high_amount_threshold", intent.threshold.toFloat()).apply()
                _state.value.copy(highAmountWarningEnabled = intent.enabled, highAmountThreshold = intent.threshold)
            }
            is AppSettingsIntent.UpdateBillReminderPreferredTime -> {
                prefs.edit().putString("bill_reminder_preferred_time", intent.time).apply()
                _state.value.copy(billReminderPreferredTime = intent.time)
            }
            is AppSettingsIntent.UpdateBillAutoMarkPaid -> {
                prefs.edit().putBoolean("bill_auto_mark_paid", intent.enabled).apply()
                _state.value.copy(billAutoMarkPaid = intent.enabled)
            }
            is AppSettingsIntent.SetBiometricEnabled -> {
                prefs.edit().putBoolean("biometric_enabled", intent.enabled).apply()
                _state.value.copy(biometricEnabled = intent.enabled)
            }
            is AppSettingsIntent.SetPrivacyModeEnabled -> {
                prefs.edit().putBoolean("privacy_mode_enabled", intent.enabled).apply()
                _state.value.copy(privacyModeEnabled = intent.enabled)
            }
            is AppSettingsIntent.SetHideSensitiveAmounts -> {
                prefs.edit().putBoolean("hide_sensitive_amounts", intent.enabled).apply()
                _state.value.copy(hideSensitiveAmounts = intent.enabled)
            }
            is AppSettingsIntent.SetLockOnRestart -> {
                prefs.edit().putBoolean("lock_on_restart", intent.enabled).apply()
                _state.value
            }
            is AppSettingsIntent.SetAutoBackupEnabled -> {
                prefs.edit().putBoolean("auto_backup_enabled", intent.enabled).apply()
                _state.value.copy(autoBackupEnabled = intent.enabled)
            }
            is AppSettingsIntent.ResetPinLockout -> {
                resetPinLockoutAndAttempts()
                _state.value.copy(pinLockoutUntil = 0L)
            }
            is AppSettingsIntent.ReloadStateAfterWipe -> {
                loadInitialState()
            }
            else -> _state.value
        }
    }
}
