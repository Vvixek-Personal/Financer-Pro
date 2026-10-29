package com.example.ui

import com.example.ui.theme.*
import com.example.ui.health.FinancialHealthCalculator
import com.example.ui.projections.CashFlowProjector
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.AiAdvisor
import com.example.api.AiResult
import com.example.api.ExchangeRateClient
import com.example.api.FirebaseAiAdvisor
import com.example.api.FinancialTotalsContext
import com.example.data.*
import com.example.data.realExpense
import com.example.data.realIncome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "FinanceViewModel"

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository,
    private val aiAdvisor: AiAdvisor = FirebaseAiAdvisor()
) : AndroidViewModel(application) {

    val billsList = mutableStateListOf<BillEntry>()
    val remindersList = mutableStateListOf<ReminderEntry>()

    // Database states
    val expenses = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected Date Range (Long: start timestamp, Long: end timestamp)
    private val _selectedDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val selectedDateRange: StateFlow<Pair<Long, Long>?> = _selectedDateRange.asStateFlow()

    fun setDateRange(start: Long?, end: Long?) {
        _selectedDateRange.value = if (start != null && end != null) Pair(start, end) else null
    }

    // Filtered Expenses based on date range
    val filteredExpenses = combine(expenses, _selectedDateRange) { list, range ->
        if (range == null) {
            list
        } else {
            list.filter { it.date >= range.first && it.date <= range.second }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val accounts = repository.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val budgets = repository.allBudgets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savingsGoals = repository.allSavingsGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reminders = repository.allReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recurringRules = repository.allRecurringRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertRecurringRule(rule: RecurringRule) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insertRecurringRule(rule)
            try {
                val db = FinanceDatabase.getDatabase(getApplication())
                RecurringProcessor.processRecurringRules(getApplication(), db)
                // Schedule the pre-due nudge for the rule as it now stands after processing
                // (processRecurringRules may have already advanced nextDueDate if it was overdue).
                val saved = db.financeDao().getRecurringRuleById(newId) ?: rule.copy(id = newId)
                RecurringBillReminderScheduler.scheduleForRule(getApplication(), saved)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to process recurring rules or schedule reminder after inserting rule", e)
            }
        }
    }

    fun updateRecurringRule(rule: RecurringRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecurringRule(rule)
            try {
                val db = FinanceDatabase.getDatabase(getApplication())
                RecurringProcessor.processRecurringRules(getApplication(), db)
                val saved = db.financeDao().getRecurringRuleById(rule.id) ?: rule
                RecurringBillReminderScheduler.scheduleForRule(getApplication(), saved)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to process recurring rules or schedule reminder after updating rule", e)
            }
        }
    }

    fun deleteRecurringRule(rule: RecurringRule) {
        viewModelScope.launch(Dispatchers.IO) {
            RecurringBillReminderScheduler.cancelForRule(getApplication(), rule.id)
            repository.deleteRecurringRule(rule)
        }
    }

    fun insertReminder(text: String, dueDate: Long, isEnabled: Boolean = true) {
        viewModelScope.launch {
            val id = repository.insertReminder(ReminderEntity(text = text, dueDate = dueDate, isEnabled = isEnabled))
            if (isEnabled) {
                ReminderScheduler.scheduleReminder(getApplication(), id, text, dueDate)
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            if (reminder.isEnabled && !reminder.isCompleted) {
                ReminderScheduler.scheduleReminder(getApplication(), reminder.id, reminder.text, reminder.dueDate)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), reminder.id)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        }
    }

    // Chat and AI states
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your AI Financial Advisor. Ask me anything about budgeting, savings strategies, or request a complete 'AI Financial Audit' of your current finances using the dashboard button!",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _aiAuditReport = MutableStateFlow<String?>(null)
    val aiAuditReport: StateFlow<String?> = _aiAuditReport.asStateFlow()

    private val _isAuditLoading = MutableStateFlow(false)
    val isAuditLoading: StateFlow<Boolean> = _isAuditLoading.asStateFlow()

    // Daily Spending Insight states (Powered by Gemini AI Advisor)
    private val _dailySpendingInsight = MutableStateFlow<String?>(null)
    val dailySpendingInsight: StateFlow<String?> = _dailySpendingInsight.asStateFlow()

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading.asStateFlow()

    private val _insightLastUpdated = MutableStateFlow<Long?>(null)
    val insightLastUpdated: StateFlow<Long?> = _insightLastUpdated.asStateFlow()

    // Selected Language Preference
    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // SharedPreferences for local configuration
    private val _themeIndex = MutableStateFlow(0)
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    private val _customThemeHue = MutableStateFlow(200f)
    val customThemeHue: StateFlow<Float> = _customThemeHue.asStateFlow()

    private val _themeMode = MutableStateFlow("light")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isFollowDeviceColors = MutableStateFlow(false)
    val isFollowDeviceColors: StateFlow<Boolean> = _isFollowDeviceColors.asStateFlow()

    val appSettingsManager = AppSettingsManager.getInstance(getApplication())
    private val sharedPrefs = getApplication<Application>().getSharedPreferences(AppSettingsManager.PREFS_NAME, android.content.Context.MODE_PRIVATE)

    // Live Storage and Network/Data Usage states
    private val _storageSize = MutableStateFlow("0.0 KB")
    val storageSize: StateFlow<String> = _storageSize.asStateFlow()

    private val _dataSize = MutableStateFlow("0.0 KB")
    val dataSize: StateFlow<String> = _dataSize.asStateFlow()

    // Map storing Category Name to Material Icon Name
    private val _categoryIcons = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryIcons: StateFlow<Map<String, String>> = _categoryIcons.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userProfileImageUri = MutableStateFlow<String?>(null)
    val userProfileImageUri: StateFlow<String?> = _userProfileImageUri.asStateFlow()

    // Google Sign-In / Sign-Up Integration State (Firebase Auth + CredentialManager)
    private val _isGoogleSignedIn = MutableStateFlow(false)
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    private val _isGoogleAuthLoading = MutableStateFlow(false)
    val isGoogleAuthLoading: StateFlow<Boolean> = _isGoogleAuthLoading.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _googleProfileName = MutableStateFlow<String?>(null)
    val googleProfileName: StateFlow<String?> = _googleProfileName.asStateFlow()

    private val _googleProfilePhotoUrl = MutableStateFlow<String?>(null)
    val googleProfilePhotoUrl: StateFlow<String?> = _googleProfilePhotoUrl.asStateFlow()

    fun updateUserProfileImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "user_profile_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val localUri = Uri.fromFile(file).toString()
                _userProfileImageUri.value = localUri
                sharedPrefs.edit().putString("user_profile_image_uri", localUri).apply()
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Error saving user profile avatar", e)
                _userProfileImageUri.value = uri.toString()
                sharedPrefs.edit().putString("user_profile_image_uri", uri.toString()).apply()
            }
        }
    }

    fun removeUserProfileImage(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "user_profile_avatar.jpg")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Error deleting user profile avatar", e)
            }
            _userProfileImageUri.value = null
            sharedPrefs.edit().remove("user_profile_image_uri").apply()
        }
    }

    fun updateUserProfileImageUri(uriString: String?) {
        _userProfileImageUri.value = uriString
        if (uriString.isNullOrBlank()) {
            sharedPrefs.edit().remove("user_profile_image_uri").apply()
        } else {
            sharedPrefs.edit().putString("user_profile_image_uri", uriString).apply()
        }
    }

    private val _userDob = MutableStateFlow("24 December 1999")
    val userDob: StateFlow<String> = _userDob.asStateFlow()

    private val _userJob = MutableStateFlow("Successor Designer")
    val userJob: StateFlow<String> = _userJob.asStateFlow()

    private val _userMonthlyIncome = MutableStateFlow("500 - 3000 / year")
    val userMonthlyIncome: StateFlow<String> = _userMonthlyIncome.asStateFlow()

    private val _userGender = MutableStateFlow("Male")
    val userGender: StateFlow<String> = _userGender.asStateFlow()

    // Passcode Security PIN State & Logic
    private val _appPin = MutableStateFlow<String?>(null)
    val appPin: StateFlow<String?> = _appPin.asStateFlow()

    private val _hasPromptedFirstRunPin = MutableStateFlow<Boolean>(false)
    val hasPromptedFirstRunPin: StateFlow<Boolean> = _hasPromptedFirstRunPin.asStateFlow()

    private val _isAppLocked = MutableStateFlow<Boolean>(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setAppPin(pin: String?) {
        val hashedPin = pin?.let {
            if (it.isBlank()) null
            else if (it.startsWith("v3:") || it.startsWith("v2:")) it
            else PinSecurityUtils.hashPin(it)
        }
        appSettingsManager.dispatch(AppSettingsIntent.SetPin(hashedPin))
    }

    fun markFirstRunPinPrompted() {
        _hasPromptedFirstRunPin.value = true
        sharedPrefs.edit().putBoolean("pin_prompted_first_run", true).apply()
    }

    private val _routeToLocalRecovery = MutableStateFlow(false)
    val routeToLocalRecovery: StateFlow<Boolean> = _routeToLocalRecovery.asStateFlow()

    fun triggerLocalRecovery() {
        _routeToLocalRecovery.value = true
    }

    fun resetRouteToLocalRecovery() {
        _routeToLocalRecovery.value = false
    }

    fun unlockAppWithPin(enteredPin: String): PinSecurityUtils.PinVerifyResult {
        val lockoutStatus = appSettingsManager.getLockoutStatus()
        if (lockoutStatus.isLocked) {
            return PinSecurityUtils.PinVerifyResult.Failed
        }

        val storedHash = appSettingsManager.state.value.appPin ?: ""
        val result = PinSecurityUtils.verifyPin(enteredPin, storedHash)
        when (result) {
            PinSecurityUtils.PinVerifyResult.Success -> {
                appSettingsManager.resetPinLockoutAndAttempts()
                _isAppLocked.value = false
                if (PinSecurityUtils.needsRehash(storedHash)) {
                    val upgradedHash = PinSecurityUtils.hashPin(enteredPin)
                    setAppPin(upgradedHash)
                }
            }
            PinSecurityUtils.PinVerifyResult.KeystoreKeyCorrupted -> {
                // Do not increment failed attempts (avoids permanent lockout), route to LocalRecovery flow
                _routeToLocalRecovery.value = true
            }
            PinSecurityUtils.PinVerifyResult.Failed -> {
                appSettingsManager.recordFailedPinAttempt()
            }
        }
        return result
    }

    fun resetAppLockAndWipeData(targetContext: Context? = null) {
        val ctx = targetContext ?: getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Wipe Room Database completely in one transaction
            try {
                repository.clearAllData()
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to clear database during wipe", e)
            }

            // 2. Sign out of Firebase to eliminate the cloud backdoor
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to sign out of Firebase during wipe", e)
            }

            // 3. Cancel any scheduled reminders or background work
            try {
                androidx.work.WorkManager.getInstance(ctx).cancelAllWork()
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to cancel background work during wipe", e)
            }

            // 4. Delete files inside filesDir (receipts, avatar, local_recovery, db_pre_migration)
            try {
                ctx.filesDir?.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to delete filesDir contents during wipe", e)
            }

            // 5. Delete files inside cacheDir
            try {
                ctx.cacheDir?.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to delete cacheDir contents during wipe", e)
            }

            // 6. Clear both SharedPreferences files
            try {
                val financePrefs = ctx.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
                financePrefs.edit().clear().commit()

                val appSettingsPrefs = ctx.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
                appSettingsPrefs.edit().clear().commit()
            } catch (e: Throwable) {
                android.util.Log.e("FinanceWipe", "Failed to clear SharedPreferences during wipe", e)
            }

            // 7. Reload AppSettingsManager state and reset lockout
            appSettingsManager.reloadStateAfterWipe()
            appSettingsManager.resetPinLockoutAndAttempts()

            withContext(Dispatchers.Main) {
                _isAppLocked.value = false
                _toastMessage.value = "App lock and all data securely wiped"
            }
        }
    }

    fun unlockWithBiometrics() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        val hasPin = !appSettingsManager.state.value.appPin.isNullOrBlank()
        val isBiometric = appSettingsManager.state.value.biometricEnabled
        if (hasPin || isBiometric) {
            _isAppLocked.value = true
        }
    }

    private var backgroundedAtElapsed: Long = 0L
    private var _suppressNextAutoLock = false
    val suppressNextAutoLock: Boolean get() = _suppressNextAutoLock

    fun suppressNextAutoLock() {
        _suppressNextAutoLock = true
    }

    fun onAppBackgrounded() {
        backgroundedAtElapsed = android.os.SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded() {
        if (_suppressNextAutoLock) {
            _suppressNextAutoLock = false
            backgroundedAtElapsed = android.os.SystemClock.elapsedRealtime()
            return
        }

        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val hasPin = !appSettingsManager.state.value.appPin.isNullOrBlank()
        val isBiometric = appSettingsManager.state.value.biometricEnabled
        val hasLock = hasPin || isBiometric
        val duration = com.example.data.AutoLockDuration.fromString(appSettingsManager.state.value.autoLockDuration)

        if (com.example.data.PinSecurityUtils.shouldLock(
                durationMillis = duration.millis,
                backgroundedAtElapsed = backgroundedAtElapsed,
                nowElapsed = nowElapsed,
                hasPin = hasLock,
                wasProcessRestarted = false
            )
        ) {
            lockApp()
        }
    }

    // Daily Streak State & Logic
    private val _currentStreak = MutableStateFlow(1)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _showStreakDialog = MutableStateFlow(false)
    val showStreakDialog: StateFlow<Boolean> = _showStreakDialog.asStateFlow()

    fun dismissStreakDialog() {
        _showStreakDialog.value = false
    }

    fun triggerShowStreakDialog() {
        _showStreakDialog.value = true
    }

    private fun checkAndCalculateDailyStreak() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastStreakDate = sharedPrefs.getString("last_streak_date", null)
        var streak = sharedPrefs.getInt("current_streak", 0)

        if (lastStreakDate == null) {
            streak = 1
            sharedPrefs.edit()
                .putString("last_streak_date", todayStr)
                .putInt("current_streak", streak)
                .apply()
            _currentStreak.value = streak
            _showStreakDialog.value = true
        } else if (lastStreakDate == todayStr) {
            _currentStreak.value = if (streak < 1) 1 else streak
            _showStreakDialog.value = false
        } else {
            try {
                val lastDate = sdf.parse(lastStreakDate)
                val todayDate = sdf.parse(todayStr)
                if (lastDate != null && todayDate != null) {
                    val diffInMillis = todayDate.time - lastDate.time
                    val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                    if (diffInDays == 1) {
                        streak += 1
                    } else if (diffInDays > 1) {
                        streak = 1
                    } else {
                        if (streak < 1) streak = 1
                    }
                } else {
                    streak = 1
                }
            } catch (e: Exception) {
                streak = 1
            }

            sharedPrefs.edit()
                .putString("last_streak_date", todayStr)
                .putInt("current_streak", streak)
                .apply()
            _currentStreak.value = streak
            _showStreakDialog.value = true
        }
    }

    fun updateStreakCount(newStreak: Int? = null) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val current = _currentStreak.value
        val target = newStreak ?: (current + 1)
        sharedPrefs.edit()
            .putString("last_streak_date", todayStr)
            .putInt("current_streak", target)
            .apply()
        _currentStreak.value = target
        _showStreakDialog.value = true
    }

    fun recordDailyTransactionActivity() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val lastStreakDate = sharedPrefs.getString("last_streak_date", null)
        var streak = sharedPrefs.getInt("current_streak", 0)

        if (lastStreakDate == null) {
            streak = 1
            sharedPrefs.edit()
                .putString("last_streak_date", todayStr)
                .putInt("current_streak", streak)
                .apply()
            _currentStreak.value = streak
        } else if (lastStreakDate != todayStr) {
            try {
                val lastDate = sdf.parse(lastStreakDate)
                val todayDate = sdf.parse(todayStr)
                if (lastDate != null && todayDate != null) {
                    val diffInMillis = todayDate.time - lastDate.time
                    val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                    if (diffInDays == 1) {
                        streak += 1
                    } else if (diffInDays > 1) {
                        streak = 1
                    } else {
                        if (streak < 1) streak = 1
                    }
                } else {
                    streak = 1
                }
            } catch (e: Exception) {
                streak = 1
            }
            sharedPrefs.edit()
                .putString("last_streak_date", todayStr)
                .putInt("current_streak", streak)
                .apply()
            _currentStreak.value = streak
        }
    }

    private val _monthlyBudget = MutableStateFlow(25000.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _customCategories = MutableStateFlow<List<String>>(emptyList())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _customExpenseCategories = MutableStateFlow<List<String>>(emptyList())
    val customExpenseCategories: StateFlow<List<String>> = _customExpenseCategories.asStateFlow()

    private val _customIncomeCategories = MutableStateFlow<List<String>>(emptyList())
    val customIncomeCategories: StateFlow<List<String>> = _customIncomeCategories.asStateFlow()

    // GST Auto-Tax Reserve State
    private val _isGstEnabled = MutableStateFlow(true)
    val isGstEnabled: StateFlow<Boolean> = _isGstEnabled.asStateFlow()

    private val _gstRatePercent = MutableStateFlow(18.0)
    val gstRatePercent: StateFlow<Double> = _gstRatePercent.asStateFlow()

    // Monthly ₹50 Safe Vault State
    private val _isMonthlySafeEnabled = MutableStateFlow(true)
    val isMonthlySafeEnabled: StateFlow<Boolean> = _isMonthlySafeEnabled.asStateFlow()

    private val _monthlySafeAmount = MutableStateFlow(50.0)
    val monthlySafeAmount: StateFlow<Double> = _monthlySafeAmount.asStateFlow()

    // Currency Settings
    private val _selectedCurrencyCode = MutableStateFlow("INR")
    val selectedCurrencyCode: StateFlow<String> = _selectedCurrencyCode.asStateFlow()

    private val _selectedCurrencySymbol = MutableStateFlow("₹")
    val selectedCurrencySymbol: StateFlow<String> = _selectedCurrencySymbol.asStateFlow()

    private val _selectedCurrencyName = MutableStateFlow("Indian Rupee")
    val selectedCurrencyName: StateFlow<String> = _selectedCurrencyName.asStateFlow()

    private val _statsCurrencyCode = MutableStateFlow("INR")
    val statsCurrencyCode: StateFlow<String> = _statsCurrencyCode.asStateFlow()

    private val _statsCurrencySymbol = MutableStateFlow("₹")
    val statsCurrencySymbol: StateFlow<String> = _statsCurrencySymbol.asStateFlow()

    private val _statsCurrencyName = MutableStateFlow("Indian Rupee")
    val statsCurrencyName: StateFlow<String> = _statsCurrencyName.asStateFlow()

    private val _lastExchangeRateUpdate = MutableStateFlow("10 Aug 2026, 08:30 AM")
    val lastExchangeRateUpdate: StateFlow<String> = _lastExchangeRateUpdate.asStateFlow()

    private val _isAutoExchangeRateUpdateEnabled = MutableStateFlow(true)
    val isAutoExchangeRateUpdateEnabled: StateFlow<Boolean> = _isAutoExchangeRateUpdateEnabled.asStateFlow()

    private val _isUpdatingExchangeRates = MutableStateFlow(false)
    val isUpdatingExchangeRates: StateFlow<Boolean> = _isUpdatingExchangeRates.asStateFlow()

    private val _appLoadingMessage = MutableStateFlow<String?>(null)
    val appLoadingMessage: StateFlow<String?> = _appLoadingMessage.asStateFlow()

    fun showAppLoading(message: String) {
        _appLoadingMessage.value = message
    }

    fun hideAppLoading() {
        _appLoadingMessage.value = null
    }

    // Appearance & Layout Preferences
    private val _textSizeOption = MutableStateFlow("Medium")
    val textSizeOption: StateFlow<String> = _textSizeOption.asStateFlow()

    private val _isCompactLayout = MutableStateFlow(false)
    val isCompactLayout: StateFlow<Boolean> = _isCompactLayout.asStateFlow()

    private val _isAnimationEnabled = MutableStateFlow(appSettingsManager.state.value.isAnimationEnabled)
    val isAnimationEnabled: StateFlow<Boolean> = _isAnimationEnabled.asStateFlow()

    // Date & Time Preferences
    private val _dateFormat = MutableStateFlow("dd/MM/yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow("Monday")
    val firstDayOfWeek: StateFlow<String> = _firstDayOfWeek.asStateFlow()

    // Bill Preferences
    private val _billReminderTiming = MutableStateFlow("1 Day Before")
    val billReminderTiming: StateFlow<String> = _billReminderTiming.asStateFlow()

    private val _billAutoMarkPaid = MutableStateFlow(false)
    val billAutoMarkPaid: StateFlow<Boolean> = _billAutoMarkPaid.asStateFlow()

    private val _billOverdueAlert = MutableStateFlow(true)
    val billOverdueAlert: StateFlow<Boolean> = _billOverdueAlert.asStateFlow()

    private val _billDefaultRecurrence = MutableStateFlow("Monthly")
    val billDefaultRecurrence: StateFlow<String> = _billDefaultRecurrence.asStateFlow()

    private val _billRecurringEnd = MutableStateFlow("Never")
    val billRecurringEnd: StateFlow<String> = _billRecurringEnd.asStateFlow()

    private val _billDefaultCategory = MutableStateFlow("Utilities")
    val billDefaultCategory: StateFlow<String> = _billDefaultCategory.asStateFlow()

    private val _billArchiveDays = MutableStateFlow("30 Days")
    val billArchiveDays: StateFlow<String> = _billArchiveDays.asStateFlow()

    private val _billShowUpcomingDashboard = MutableStateFlow(true)
    val billShowUpcomingDashboard: StateFlow<Boolean> = _billShowUpcomingDashboard.asStateFlow()

    private val _billUpcomingDays = MutableStateFlow("7 Days")
    val billUpcomingDays: StateFlow<String> = _billUpcomingDays.asStateFlow()

    private val _billSortOrder = MutableStateFlow("Due Date (Nearest)")
    val billSortOrder: StateFlow<String> = _billSortOrder.asStateFlow()

    private val _billDefaultFilter = MutableStateFlow("All Bills")
    val billDefaultFilter: StateFlow<String> = _billDefaultFilter.asStateFlow()

    private val _billShowNotes = MutableStateFlow(true)
    val billShowNotes: StateFlow<Boolean> = _billShowNotes.asStateFlow()

    // General Preferences for Categories & Tags
    private val _preventDeleteUsedCategories = MutableStateFlow(true)
    val preventDeleteUsedCategories: StateFlow<Boolean> = _preventDeleteUsedCategories.asStateFlow()

    private val _showCategoryInTransactionList = MutableStateFlow(true)
    val showCategoryInTransactionList: StateFlow<Boolean> = _showCategoryInTransactionList.asStateFlow()

    private val _deletedCategories = MutableStateFlow<Set<String>>(emptySet())
    val deletedCategories: StateFlow<Set<String>> = _deletedCategories.asStateFlow()

    val defaultBudgetCategories = listOf("Overall", "Food & Dining", "Bills & Utilities", "Shopping", "Entertainment", "Transport", "Healthcare", "Personal Care", "Education", "Travel", "Others")
    private val _customBudgetCategories = MutableStateFlow<List<String>>(emptyList())

    val budgetCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customBudgetCategories, _deletedCategories) { custom, deleted ->
        ((defaultBudgetCategories + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultBudgetCategories)

    val defaultTags = listOf("Personal", "Work", "Tax Deductible", "Urgent", "Vacation", "Health", "Home", "Shopping", "Family", "Business")
    private val _customTags = MutableStateFlow<List<String>>(emptyList())
    private val _deletedTags = MutableStateFlow<Set<String>>(emptySet())

    val allTags: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customTags, _deletedTags) { custom, deleted ->
        ((defaultTags + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultTags)

    private val _categoryColorMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryColorMap: StateFlow<Map<String, String>> = _categoryColorMap.asStateFlow()

    private val _tagColorMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagColorMap: StateFlow<Map<String, String>> = _tagColorMap.asStateFlow()

    // Budget Preferences
    private val _budgetWarning80 = MutableStateFlow(true)
    val budgetWarning80: StateFlow<Boolean> = _budgetWarning80.asStateFlow()

    private val _budgetWarning90 = MutableStateFlow(true)
    val budgetWarning90: StateFlow<Boolean> = _budgetWarning90.asStateFlow()

    private val _budgetWarning100 = MutableStateFlow(true)
    val budgetWarning100: StateFlow<Boolean> = _budgetWarning100.asStateFlow()

    private val _budgetIncludeRecurringBills = MutableStateFlow(true)
    val budgetIncludeRecurringBills: StateFlow<Boolean> = _budgetIncludeRecurringBills.asStateFlow()

    // Savings Goals Preferences
    private val _goalViewMode = MutableStateFlow("Grid")
    val goalViewMode: StateFlow<String> = _goalViewMode.asStateFlow()

    private val _goalProgressStyle = MutableStateFlow("Circle")
    val goalProgressStyle: StateFlow<String> = _goalProgressStyle.asStateFlow()

    // Transaction Preferences
    private val _defaultTxType = MutableStateFlow("EXPENSE")
    val defaultTxType: StateFlow<String> = _defaultTxType.asStateFlow()

    private val _rememberLastCategory = MutableStateFlow(true)
    val rememberLastCategory: StateFlow<Boolean> = _rememberLastCategory.asStateFlow()

    private val _confirmTxDelete = MutableStateFlow(true)
    val confirmTxDelete: StateFlow<Boolean> = _confirmTxDelete.asStateFlow()

    private val _groupByDate = MutableStateFlow(true)
    val groupByDate: StateFlow<Boolean> = _groupByDate.asStateFlow()

    // Security & Privacy Preferences
    private val _lockOnRestart = MutableStateFlow(true)
    val lockOnRestart: StateFlow<Boolean> = _lockOnRestart.asStateFlow()

    private val _autoLockDuration = MutableStateFlow("Immediately")
    val autoLockDuration: StateFlow<String> = _autoLockDuration.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _hideSensitiveAmounts = MutableStateFlow(false)
    val hideSensitiveAmounts: StateFlow<Boolean> = _hideSensitiveAmounts.asStateFlow()

    private val _screenshotProtection = MutableStateFlow(false)
    val screenshotProtection: StateFlow<Boolean> = _screenshotProtection.asStateFlow()

    private val _fontFamily = MutableStateFlow("Default")
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _highContrastMode = MutableStateFlow(false)
    val highContrastMode: StateFlow<Boolean> = _highContrastMode.asStateFlow()

    private val _cardGlowAnimation = MutableStateFlow(true)
    val cardGlowAnimation: StateFlow<Boolean> = _cardGlowAnimation.asStateFlow()

    private val _defaultPaymentMode = MutableStateFlow("UPI")
    val defaultPaymentMode: StateFlow<String> = _defaultPaymentMode.asStateFlow()

    private val _highAmountWarningEnabled = MutableStateFlow(true)
    val highAmountWarningEnabled: StateFlow<Boolean> = _highAmountWarningEnabled.asStateFlow()

    private val _highAmountThreshold = MutableStateFlow(5000.0)
    val highAmountThreshold: StateFlow<Double> = _highAmountThreshold.asStateFlow()

    private val _billReminderPreferredTime = MutableStateFlow("09:00 AM")
    val billReminderPreferredTime: StateFlow<String> = _billReminderPreferredTime.asStateFlow()

    private val _privacyModeEnabled = MutableStateFlow(false)
    val privacyModeEnabled: StateFlow<Boolean> = _privacyModeEnabled.asStateFlow()

    // Session-only reveal override for Privacy Blur Mode — resets to false
    // (masked) every time the ViewModel is recreated, i.e. every app launch.
    // Tapping a masked amount or the eye icon flips this; it's intentionally
    // NOT persisted, so the app is always masked-by-default on open.
    private val _privacyRevealOverride = MutableStateFlow(false)
    val privacyRevealOverride: StateFlow<Boolean> = _privacyRevealOverride.asStateFlow()

    fun togglePrivacyReveal() {
        _privacyRevealOverride.value = !_privacyRevealOverride.value
    }

    // Last-used category per transaction type, for "Remember Last Category".
    // Persisted so it survives app restarts, not just the current session.
    private val _lastUsedExpenseCategory = MutableStateFlow<String?>(null)
    val lastUsedExpenseCategory: StateFlow<String?> = _lastUsedExpenseCategory.asStateFlow()

    private val _lastUsedIncomeCategory = MutableStateFlow<String?>(null)
    val lastUsedIncomeCategory: StateFlow<String?> = _lastUsedIncomeCategory.asStateFlow()

    // Backup & Restore Preferences
    private val _autoBackupEnabled = MutableStateFlow(true)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow("Never")
    val lastBackupTimestamp: StateFlow<String> = _lastBackupTimestamp.asStateFlow()

    private val _syncMode = MutableStateFlow("LOCAL_ONLY")
    val syncMode: StateFlow<String> = _syncMode.asStateFlow()

    private val _lastCloudSyncTimestamp = MutableStateFlow("Never")
    val lastCloudSyncTimestamp: StateFlow<String> = _lastCloudSyncTimestamp.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Local Only")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastDeletedExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val lastDeletedExpenses: StateFlow<List<Expense>> = _lastDeletedExpenses.asStateFlow()

    private val _undoDeleteVisible = MutableStateFlow(false)
    val undoDeleteVisible: StateFlow<Boolean> = _undoDeleteVisible.asStateFlow()

    private val _undoDeleteMessage = MutableStateFlow("")
    val undoDeleteMessage: StateFlow<String> = _undoDeleteMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun toggleGstEnabled(enabled: Boolean) {
        _isGstEnabled.value = enabled
        sharedPrefs.edit().putBoolean("is_gst_enabled", enabled).apply()
    }

    fun updateGstRate(rate: Double) {
        if (rate in 0.0..100.0) {
            _gstRatePercent.value = rate
            sharedPrefs.edit().putFloat("gst_rate_percent", rate.toFloat()).apply()
        }
    }

    fun toggleMonthlySafeEnabled(enabled: Boolean) {
        _isMonthlySafeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("is_monthly_safe_enabled", enabled).apply()
    }

    fun updateMonthlySafeAmount(amount: Double) {
        if (amount > 0) {
            _monthlySafeAmount.value = amount
            val minor = Money.fromDouble(amount, _selectedCurrencyCode.value)
            sharedPrefs.edit()
                .putLong("monthly_safe_amount_minor", minor)
                .remove("monthly_safe_amount")
                .apply()
        }
    }

    val defaultExpenseCategories = listOf("Food", "Travel", "Rent", "Utilities", "Entertainment", "Shopping", "Home", "Others")
    val defaultIncomeCategories = listOf("Salary", "Freelance", "Investments", "Gifts", "Others")
    val defaultCategories = (defaultExpenseCategories + defaultIncomeCategories).distinct()

    val defaultGoalCategories = listOf(
        "Saving", "Investment", "Expenditure", "Travel", "Tech",
        "Shopping", "Vehicle", "Education", "Emergency"
    )

    private val _customGoalCategories = MutableStateFlow<List<String>>(emptyList())
    val goalCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customGoalCategories, _deletedCategories) { custom, deleted ->
        ((defaultGoalCategories + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultGoalCategories)

    val allCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customCategories, _deletedCategories) { custom, deleted ->
        ((defaultCategories + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultCategories)

    val expenseCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customExpenseCategories, _deletedCategories) { custom, deleted ->
        ((defaultExpenseCategories + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultExpenseCategories)

    val incomeCategories: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(_customIncomeCategories, _deletedCategories) { custom, deleted ->
        ((defaultIncomeCategories + custom).distinct() - deleted).toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultIncomeCategories)

    init {
        checkAndCalculateDailyStreak()

        try {
            RecurringTransactionWorker.schedule(getApplication())
            viewModelScope.launch(Dispatchers.IO) {
                val db = FinanceDatabase.getDatabase(getApplication())
                RecurringProcessor.processRecurringRules(getApplication(), db)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule recurring worker or process rules in init", e)
        }

        viewModelScope.launch {
            appSettingsManager.state.collect { settings ->
                _isAnimationEnabled.value = settings.isAnimationEnabled
                _selectedLanguage.value = settings.language
                _themeIndex.value = settings.themeIndex
                _themeMode.value = settings.themeMode
                _customThemeHue.value = settings.customThemeHue
                _isFollowDeviceColors.value = settings.isFollowDeviceColors
                _selectedCurrencyCode.value = settings.currencyCode
                _selectedCurrencySymbol.value = settings.currencySymbol
                _selectedCurrencyName.value = settings.currencyName
                _statsCurrencyCode.value = settings.statsCurrencyCode
                _statsCurrencySymbol.value = settings.statsCurrencySymbol
                _statsCurrencyName.value = settings.statsCurrencyName
                _monthlyBudget.value = settings.monthlyBudget
                _appPin.value = settings.appPin
                _biometricEnabled.value = settings.biometricEnabled
                _privacyModeEnabled.value = settings.privacyModeEnabled
                _hideSensitiveAmounts.value = settings.hideSensitiveAmounts
                _screenshotProtection.value = settings.screenshotProtection
                _fontFamily.value = settings.fontFamily
                com.example.ui.theme.activeFontFamilyChoiceState = settings.fontFamily
                _hapticsEnabled.value = settings.hapticsEnabled
                _highContrastMode.value = settings.highContrastMode
                com.example.ui.theme.isHighContrastActive = settings.highContrastMode
                _cardGlowAnimation.value = settings.cardGlowAnimation
                _autoLockDuration.value = settings.autoLockDuration
                _defaultPaymentMode.value = settings.defaultPaymentMode
                _highAmountWarningEnabled.value = settings.highAmountWarningEnabled
                _highAmountThreshold.value = settings.highAmountThreshold
                _billReminderTiming.value = settings.billReminderTiming
                _billReminderPreferredTime.value = settings.billReminderPreferredTime
                _billAutoMarkPaid.value = settings.billAutoMarkPaid
                _autoBackupEnabled.value = settings.autoBackupEnabled
                _lastBackupTimestamp.value = settings.lastBackupTimestamp
                _syncMode.value = settings.syncMode
                _lastCloudSyncTimestamp.value = settings.lastCloudSyncTimestamp
                _cloudSyncStatus.value = settings.cloudSyncStatus
            }
        }
        _userName.value = sharedPrefs.getString("user_name", null)
        _isGoogleSignedIn.value = sharedPrefs.getBoolean("is_google_signed_in", false)
        _googleAccountEmail.value = sharedPrefs.getString("google_account_email", null)
        _googleProfileName.value = sharedPrefs.getString("google_profile_name", null)
        _googleProfilePhotoUrl.value = sharedPrefs.getString("google_profile_photo_url", null)
        val savedImageUri = sharedPrefs.getString("user_profile_image_uri", null)
        if (!savedImageUri.isNullOrBlank()) {
            val localFile = File(getApplication<Application>().filesDir, "user_profile_avatar.jpg")
            if (savedImageUri.startsWith("content://")) {
                try {
                    val contentUri = Uri.parse(savedImageUri)
                    getApplication<Application>().contentResolver.openInputStream(contentUri)?.use { inputStream ->
                        FileOutputStream(localFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val localUri = Uri.fromFile(localFile).toString()
                    _userProfileImageUri.value = localUri
                    sharedPrefs.edit().putString("user_profile_image_uri", localUri).apply()
                } catch (e: Exception) {
                    if (localFile.exists() && localFile.length() > 0) {
                        val localUri = Uri.fromFile(localFile).toString()
                        _userProfileImageUri.value = localUri
                        sharedPrefs.edit().putString("user_profile_image_uri", localUri).apply()
                    } else {
                        _userProfileImageUri.value = savedImageUri
                    }
                }
            } else if (savedImageUri.startsWith("file://")) {
                val filePath = Uri.parse(savedImageUri).path
                val file = if (filePath != null) File(filePath) else localFile
                if (file.exists() && file.length() > 0) {
                    _userProfileImageUri.value = Uri.fromFile(file).toString()
                } else if (localFile.exists() && localFile.length() > 0) {
                    val localUri = Uri.fromFile(localFile).toString()
                    _userProfileImageUri.value = localUri
                    sharedPrefs.edit().putString("user_profile_image_uri", localUri).apply()
                } else {
                    _userProfileImageUri.value = null
                    sharedPrefs.edit().remove("user_profile_image_uri").apply()
                }
            } else {
                _userProfileImageUri.value = savedImageUri
            }
        }
        _userDob.value = sharedPrefs.getString("user_dob", "24 December 1999") ?: "24 December 1999"
        _userJob.value = sharedPrefs.getString("user_job", "Successor Designer") ?: "Successor Designer"
        _userMonthlyIncome.value = sharedPrefs.getString("user_monthly_income", "500 - 3000 / year") ?: "500 - 3000 / year"
        _userGender.value = sharedPrefs.getString("user_gender", "Male") ?: "Male"
        val budgetCur = sharedPrefs.getString("monthly_budget_currency", null)
            ?: sharedPrefs.getString("selected_currency_code", "INR") ?: "INR"
        val budgetMinor = if (sharedPrefs.contains("monthly_budget")) {
            val oldF = sharedPrefs.getFloat("monthly_budget", 25000.0f)
            val m = Money.fromDouble(oldF.toDouble(), budgetCur)
            sharedPrefs.edit()
                .putLong("monthly_budget_minor", m)
                .putString("monthly_budget_currency", budgetCur)
                .remove("monthly_budget")
                .apply()
            m
        } else {
            sharedPrefs.getLong("monthly_budget_minor", Money.fromDouble(25000.0, budgetCur))
        }
        _monthlyBudget.value = Money.toDouble(budgetMinor, budgetCur)
        val savedCats = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val savedExpenseCats = sharedPrefs.getStringSet("custom_expense_categories", null)
        val savedIncomeCats = sharedPrefs.getStringSet("custom_income_categories", null)

        val expSet = savedExpenseCats ?: (savedCats - defaultIncomeCategories.toSet())
        val incSet = savedIncomeCats ?: emptySet()

        _customExpenseCategories.value = expSet.toList().sorted()
        _customIncomeCategories.value = incSet.toList().sorted()
        _customCategories.value = (expSet + incSet + savedCats).distinct().sorted()

        val savedGoalCats = sharedPrefs.getStringSet("custom_goal_categories", emptySet()) ?: emptySet()
        _customGoalCategories.value = savedGoalCats.toList().sorted()
        
        _themeIndex.value = sharedPrefs.getInt("theme_index", 0)
        _customThemeHue.value = sharedPrefs.getFloat("custom_theme_hue", 200f)
        _selectedLanguage.value = sharedPrefs.getString("selected_language", "English") ?: "English"
        LanguageManager.applyAppLocale(getApplication(), _selectedLanguage.value)
        
        // Load Theme Mode & Follow Device Colors
        val defaultMode = if (sharedPrefs.getBoolean("dark_mode_active", false)) "dark" else "light"
        val savedMode = sharedPrefs.getString("theme_mode", defaultMode) ?: defaultMode
        _themeMode.value = savedMode
        com.example.ui.theme.themeModeState = savedMode

        val savedFollowColors = sharedPrefs.getBoolean("follow_device_colors", false)
        _isFollowDeviceColors.value = savedFollowColors
        com.example.ui.theme.isFollowDeviceColorsState = savedFollowColors

        // Load Passcode PIN Security Settings
        val savedPin = appSettingsManager.state.value.appPin ?: sharedPrefs.getString("app_pin", null)
        _appPin.value = savedPin
        _hasPromptedFirstRunPin.value = sharedPrefs.getBoolean("pin_prompted_first_run", false)
        val isBiometric = appSettingsManager.state.value.biometricEnabled || sharedPrefs.getBoolean("biometric_enabled", false)
        if (!savedPin.isNullOrBlank() || isBiometric) {
            _isAppLocked.value = true
        }
        com.example.ui.theme.updateThemeColors(_themeIndex.value, _customThemeHue.value)

        // Load GST and Monthly Safe settings
        _isGstEnabled.value = sharedPrefs.getBoolean("is_gst_enabled", true)
        _gstRatePercent.value = sharedPrefs.getFloat("gst_rate_percent", 18.0f).toDouble()
        _isMonthlySafeEnabled.value = sharedPrefs.getBoolean("is_monthly_safe_enabled", true)

        val cur = sharedPrefs.getString("selected_currency_code", "INR") ?: "INR"
        val defSafeMinor = Money.fromDouble(50.0, cur)
        val safeMinor = if (sharedPrefs.contains("monthly_safe_amount_minor")) {
            sharedPrefs.getLong("monthly_safe_amount_minor", defSafeMinor)
        } else if (sharedPrefs.contains("monthly_safe_amount")) {
            val oldFloat = sharedPrefs.getFloat("monthly_safe_amount", 50.0f)
            val converted = Money.fromDouble(oldFloat.toDouble(), cur)
            sharedPrefs.edit().putLong("monthly_safe_amount_minor", converted).remove("monthly_safe_amount").apply()
            converted
        } else {
            defSafeMinor
        }
        _monthlySafeAmount.value = Money.toDouble(safeMinor, cur)

        // Load last-used category memory (for "Remember Last Category")
        _lastUsedExpenseCategory.value = sharedPrefs.getString("last_used_expense_category", null)
        _lastUsedIncomeCategory.value = sharedPrefs.getString("last_used_income_category", null)

        // Load Currency Settings
        _selectedCurrencyCode.value = sharedPrefs.getString("selected_currency_code", "INR") ?: "INR"
        _selectedCurrencySymbol.value = sharedPrefs.getString("selected_currency_symbol", "₹") ?: "₹"
        _selectedCurrencyName.value = sharedPrefs.getString("selected_currency_name", "Indian Rupee") ?: "Indian Rupee"
        _statsCurrencyCode.value = sharedPrefs.getString("stats_currency_code", "INR") ?: "INR"
        _statsCurrencySymbol.value = sharedPrefs.getString("stats_currency_symbol", "₹") ?: "₹"
        _statsCurrencyName.value = sharedPrefs.getString("stats_currency_name", "Indian Rupee") ?: "Indian Rupee"
        _lastExchangeRateUpdate.value = sharedPrefs.getString("last_exchange_rate_update", "10 Aug 2026, 08:30 AM") ?: "10 Aug 2026, 08:30 AM"
        _isAutoExchangeRateUpdateEnabled.value = sharedPrefs.getBoolean("is_auto_exchange_rate_update", true)

        // Load Bills Preferences
        _billReminderTiming.value = sharedPrefs.getString("bill_reminder_timing", "1 Day Before") ?: "1 Day Before"
        _billAutoMarkPaid.value = sharedPrefs.getBoolean("bill_auto_mark_paid", false)
        _billOverdueAlert.value = sharedPrefs.getBoolean("bill_overdue_alert", true)
        _billDefaultRecurrence.value = sharedPrefs.getString("bill_default_recurrence", "Monthly") ?: "Monthly"
        _billRecurringEnd.value = sharedPrefs.getString("bill_recurring_end", "Never") ?: "Never"
        _billDefaultCategory.value = sharedPrefs.getString("bill_default_category", "Utilities") ?: "Utilities"
        _billArchiveDays.value = sharedPrefs.getString("bill_archive_days", "30 Days") ?: "30 Days"
        _billShowUpcomingDashboard.value = sharedPrefs.getBoolean("bill_show_upcoming_dashboard", true)
        _billUpcomingDays.value = sharedPrefs.getString("bill_upcoming_days", "7 Days") ?: "7 Days"
        _billSortOrder.value = sharedPrefs.getString("bill_sort_order", "Due Date (Nearest)") ?: "Due Date (Nearest)"
        _billDefaultFilter.value = sharedPrefs.getString("bill_default_filter", "All Bills") ?: "All Bills"
        _billShowNotes.value = sharedPrefs.getBoolean("bill_show_notes", true)

        _budgetIncludeRecurringBills.value = sharedPrefs.getBoolean("budget_include_recurring_bills", true)

        // Load Category & Tag Preferences
        _preventDeleteUsedCategories.value = sharedPrefs.getBoolean("prevent_delete_used_categories", true)
        _showCategoryInTransactionList.value = sharedPrefs.getBoolean("show_category_in_transaction_list", true)
        _deletedCategories.value = sharedPrefs.getStringSet("deleted_categories", emptySet()) ?: emptySet()
        _customBudgetCategories.value = (sharedPrefs.getStringSet("custom_budget_categories", emptySet()) ?: emptySet()).toList().sorted()
        _customTags.value = (sharedPrefs.getStringSet("custom_tags", emptySet()) ?: emptySet()).toList().sorted()
        _deletedTags.value = sharedPrefs.getStringSet("deleted_tags", emptySet()) ?: emptySet()

        // Load custom category icons
        val iconsMap = mutableMapOf<String, String>()
        savedCats.forEach { cat ->
            iconsMap[cat] = sharedPrefs.getString("cat_icon_$cat", "Star") ?: "Star"
        }
        _categoryIcons.value = iconsMap

        // Compute initial storage & network values
        refreshUsageData()
    }

    fun updateLanguage(language: String) {
        _selectedLanguage.value = language
        LanguageManager.applyAppLocale(getApplication(), language)
        // Persistence for this key now lives solely in AppSettingsManager —
        // dispatch below writes it once and its state flow reflects back
        // into _selectedLanguage via the init{} collector. Writing it here
        // too was a duplicate write to the same SharedPreferences file/key.
        appSettingsManager.dispatch(AppSettingsIntent.UpdateLanguage(language))
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        com.example.ui.theme.themeModeState = mode
        com.example.ui.theme.updateThemeColors(_themeIndex.value, _customThemeHue.value)
        // Persistence for theme_mode now lives solely in AppSettingsManager
        // (see updateLanguage's comment above for why the direct sharedPrefs
        // write was removed here).
        appSettingsManager.dispatch(AppSettingsIntent.UpdateThemeMode(mode))
    }

    fun toggleFollowDeviceColors(enabled: Boolean) {
        _isFollowDeviceColors.value = enabled
        sharedPrefs.edit().putBoolean("follow_device_colors", enabled).apply()
        com.example.ui.theme.isFollowDeviceColorsState = enabled
    }

    fun toggleDarkMode() {
        val nextMode = if (com.example.ui.theme.isDarkModeActive) "light" else "dark"
        updateThemeMode(nextMode)
    }

    fun refreshUsageData() {
        val context = getApplication<Application>()
        
        viewModelScope.launch(Dispatchers.IO) {
            val dbFile = context.getDatabasePath("finance_database")
            var bytes = if (dbFile.exists()) dbFile.length() else 0L
            val dbJournal = context.getDatabasePath("finance_database-journal")
            if (dbJournal.exists()) bytes += dbJournal.length()
            val dbWal = context.getDatabasePath("finance_database-wal")
            if (dbWal.exists()) bytes += dbWal.length()
            val dbShm = context.getDatabasePath("finance_database-shm")
            if (dbShm.exists()) bytes += dbShm.length()

            fun getFolderSize(dir: java.io.File?): Long {
                if (dir == null || !dir.exists()) return 0
                if (dir.isFile) return dir.length()
                var sum = 0L
                dir.listFiles()?.forEach { sum += getFolderSize(it) }
                return sum
            }
            bytes += getFolderSize(context.filesDir)
            bytes += getFolderSize(context.cacheDir)

            val kb = bytes / 1024.0
            val formatted = if (kb < 1024.0) {
                String.format(Locale.getDefault(), "%.2f KB", kb)
            } else {
                String.format(Locale.getDefault(), "%.2f MB", kb / 1024.0)
            }
            _storageSize.value = formatted
        }

        val uid = android.os.Process.myUid()
        val rx = android.net.TrafficStats.getUidRxBytes(uid)
        val tx = android.net.TrafficStats.getUidTxBytes(uid)
        val netBytes = (if (rx == android.net.TrafficStats.UNSUPPORTED.toLong()) 0L else rx) +
                       (if (tx == android.net.TrafficStats.UNSUPPORTED.toLong()) 0L else tx)
        
        val netKb = netBytes / 1024.0
        val formattedNet = if (netKb < 1024.0) {
            String.format(Locale.getDefault(), "%.2f KB", netKb)
        } else {
            String.format(Locale.getDefault(), "%.2f MB", netKb / 1024.0)
        }
        _dataSize.value = formattedNet
    }

    fun updateTheme(index: Int) {
        _themeIndex.value = index
        com.example.ui.theme.updateThemeColors(index, _customThemeHue.value)
        // Persistence for theme_index now lives solely in AppSettingsManager
        // (see updateLanguage's comment for why the direct sharedPrefs write
        // was removed here).
        appSettingsManager.dispatch(AppSettingsIntent.UpdateTheme(index, _customThemeHue.value))
    }

    fun updateCustomThemeHue(hue: Float) {
        _customThemeHue.value = hue
        com.example.ui.theme.updateThemeColors(_themeIndex.value, hue)
        // Persistence for custom_theme_hue now lives solely in
        // AppSettingsManager (see updateLanguage's comment).
        appSettingsManager.dispatch(AppSettingsIntent.UpdateTheme(_themeIndex.value, hue))
    }

    fun saveUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            sharedPrefs.edit().putString("user_name", trimmed).apply()
            _userName.value = trimmed
        }
    }

    fun saveUserProfile(name: String, dob: String, job: String, income: String, gender: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            sharedPrefs.edit()
                .putString("user_name", trimmedName)
                .putString("user_dob", dob)
                .putString("user_job", job)
                .putString("user_monthly_income", income)
                .putString("user_gender", gender)
                .apply()
            _userName.value = trimmedName
            _userDob.value = dob
            _userJob.value = job
            _userMonthlyIncome.value = income
            _userGender.value = gender
        }
    }

    fun updateMonthlyBudget(newLimit: Double) {
        if (newLimit > 0.0) {
            val cur = _selectedCurrencyCode.value
            val minor = Money.fromDouble(newLimit, cur)
            sharedPrefs.edit()
                .putLong("monthly_budget_minor", minor)
                .putString("monthly_budget_currency", cur)
                .apply()
            _monthlyBudget.value = newLimit
        }
    }

    fun updateTextSizeOption(option: String) {
        _textSizeOption.value = option
        sharedPrefs.edit().putString("text_size_option", option).apply()
    }

    fun toggleCompactLayout(enabled: Boolean) {
        _isCompactLayout.value = enabled
        sharedPrefs.edit().putBoolean("is_compact_layout", enabled).apply()
    }

    fun toggleAnimationEnabled(enabled: Boolean) {
        _isAnimationEnabled.value = enabled
        appSettingsManager.dispatch(AppSettingsIntent.UpdateAnimationEnabled(enabled))
    }

    fun toggleAutoExchangeRateUpdate(enabled: Boolean) {
        _isAutoExchangeRateUpdateEnabled.value = enabled
        sharedPrefs.edit().putBoolean("is_auto_exchange_rate_update", enabled).apply()
        _toastMessage.value = if (enabled) {
            "Automatic exchange rate updates turned ON"
        } else {
            "Automatic exchange rate updates turned OFF"
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _isUpdatingExchangeRates.value = true
            try {
                ExchangeRateClient.fetchExchangeRates()
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val nowFormatted = sdf.format(Date())
                _lastExchangeRateUpdate.value = nowFormatted
                sharedPrefs.edit().putString("last_exchange_rate_update", nowFormatted).apply()
                _toastMessage.value = "Exchange rates updated successfully"
            } catch (e: Exception) {
                // Do NOT touch _lastExchangeRateUpdate/sharedPrefs here — the fetch
                // genuinely failed, so the last known-good timestamp (from the most
                // recent real success) should stay as-is instead of being stamped
                // with "now", which would falsely tell the user rates are current.
                _toastMessage.value = "Couldn't update exchange rates — check your connection and try again"
            } finally {
                _isUpdatingExchangeRates.value = false
            }
        }
    }

    fun updateDefaultCurrency(code: String, symbol: String, name: String, convertExisting: Boolean = false) {
        val oldCode = _selectedCurrencyCode.value
        _selectedCurrencyCode.value = code
        _selectedCurrencySymbol.value = symbol
        _selectedCurrencyName.value = name
        appSettingsManager.dispatch(AppSettingsIntent.UpdateCurrency(code, symbol, name, convertExisting))
        if (convertExisting && oldCode != code) {
            viewModelScope.launch {
                val rates = CurrencyManager.getRatesMap()
                repository.convertAllEntitiesToCurrency(code, rates)
            }
            _toastMessage.value = "Currency changed to $code ($symbol) and all records converted safely using verified exchange rates"
        } else {
            _toastMessage.value = "Default currency updated to $code ($symbol)"
        }
    }

    fun updateStatsCurrency(code: String, symbol: String, name: String) {
        _statsCurrencyCode.value = code
        _statsCurrencySymbol.value = symbol
        _statsCurrencyName.value = name
        appSettingsManager.dispatch(AppSettingsIntent.UpdateStatsCurrency(code, symbol, name))
        _toastMessage.value = "Statistics currency updated to $code ($symbol)"
    }

    fun resetCurrencySettings() {
        updateDefaultCurrency("INR", "₹", "Indian Rupee", false)
        updateStatsCurrency("INR", "₹", "Indian Rupee")
        _lastExchangeRateUpdate.value = "10 Aug 2026, 08:30 AM"
        sharedPrefs.edit().putString("last_exchange_rate_update", "10 Aug 2026, 08:30 AM").apply()
        _toastMessage.value = "Currency settings restored to default"
    }

    fun updateDateFormat(fmt: String) {
        _dateFormat.value = fmt
        sharedPrefs.edit().putString("date_format", fmt).apply()
    }

    fun updateFirstDayOfWeek(day: String) {
        _firstDayOfWeek.value = day
        sharedPrefs.edit().putString("first_day_of_week", day).apply()
    }

    fun updateBillReminderTiming(timing: String) {
        _billReminderTiming.value = timing
        sharedPrefs.edit().putString("bill_reminder_timing", timing).apply()
    }

    fun toggleBillOverdueAlert(enabled: Boolean) {
        _billOverdueAlert.value = enabled
        sharedPrefs.edit().putBoolean("bill_overdue_alert", enabled).apply()
    }

    fun toggleBillAutoMarkPaid(enabled: Boolean) {
        _billAutoMarkPaid.value = enabled
        sharedPrefs.edit().putBoolean("bill_auto_mark_paid", enabled).apply()
    }

    fun updateBillDefaultRecurrence(recurrence: String) {
        _billDefaultRecurrence.value = recurrence
        sharedPrefs.edit().putString("bill_default_recurrence", recurrence).apply()
    }

    fun updateBillRecurringEnd(end: String) {
        _billRecurringEnd.value = end
        sharedPrefs.edit().putString("bill_recurring_end", end).apply()
    }

    fun updateBillDefaultCategory(category: String) {
        _billDefaultCategory.value = category
        sharedPrefs.edit().putString("bill_default_category", category).apply()
    }

    fun updateBillArchiveDays(days: String) {
        _billArchiveDays.value = days
        sharedPrefs.edit().putString("bill_archive_days", days).apply()
    }

    fun toggleBillShowUpcomingDashboard(enabled: Boolean) {
        _billShowUpcomingDashboard.value = enabled
        sharedPrefs.edit().putBoolean("bill_show_upcoming_dashboard", enabled).apply()
    }

    fun updateBillUpcomingDays(days: String) {
        _billUpcomingDays.value = days
        sharedPrefs.edit().putString("bill_upcoming_days", days).apply()
    }

    fun updateBillSortOrder(order: String) {
        _billSortOrder.value = order
        sharedPrefs.edit().putString("bill_sort_order", order).apply()
    }

    fun updateBillDefaultFilter(filter: String) {
        _billDefaultFilter.value = filter
        sharedPrefs.edit().putString("bill_default_filter", filter).apply()
    }

    fun toggleBillShowNotes(enabled: Boolean) {
        _billShowNotes.value = enabled
        sharedPrefs.edit().putBoolean("bill_show_notes", enabled).apply()
    }

    fun togglePreventDeleteUsedCategories(enabled: Boolean) {
        _preventDeleteUsedCategories.value = enabled
        sharedPrefs.edit().putBoolean("prevent_delete_used_categories", enabled).apply()
    }

    fun toggleShowCategoryInTransactionList(enabled: Boolean) {
        _showCategoryInTransactionList.value = enabled
        sharedPrefs.edit().putBoolean("show_category_in_transaction_list", enabled).apply()
    }

    fun addCategoryWithType(name: String, type: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        when (type.uppercase()) {
            "EXPENSE" -> addCustomCategory(trimmed, "EXPENSE")
            "INCOME" -> addCustomCategory(trimmed, "INCOME")
            "SAVINGS", "GOAL" -> addGoalCategory(trimmed)
            "BUDGET" -> {
                val current = sharedPrefs.getStringSet("custom_budget_categories", emptySet()) ?: emptySet()
                val updated = (current + trimmed).sorted()
                sharedPrefs.edit().putStringSet("custom_budget_categories", updated.toSet()).apply()
                _customBudgetCategories.value = updated
            }
            else -> addCustomCategory(trimmed, "EXPENSE")
        }
    }

    fun deleteAnyCategory(categoryName: String): Boolean {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank()) return false

        val updatedDeleted = _deletedCategories.value + trimmed
        _deletedCategories.value = updatedDeleted
        sharedPrefs.edit().putStringSet("deleted_categories", updatedDeleted).apply()

        deleteCustomCategory(trimmed)
        deleteGoalCategory(trimmed)

        val currentBudget = sharedPrefs.getStringSet("custom_budget_categories", emptySet()) ?: emptySet()
        if (currentBudget.contains(trimmed)) {
            val updated = currentBudget - trimmed
            sharedPrefs.edit().putStringSet("custom_budget_categories", updated).apply()
            _customBudgetCategories.value = updated.toList().sorted()
        }

        return true
    }

    fun renameAnyCategory(oldName: String, newName: String, type: String = "EXPENSE") {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || trimmedOld == trimmedNew) return

        renameCustomCategory(trimmedOld, trimmedNew)

        if (defaultCategories.contains(trimmedOld) || defaultGoalCategories.contains(trimmedOld) || defaultBudgetCategories.contains(trimmedOld)) {
            val updatedDeleted = _deletedCategories.value + trimmedOld
            _deletedCategories.value = updatedDeleted
            sharedPrefs.edit().putStringSet("deleted_categories", updatedDeleted).apply()
            addCategoryWithType(trimmedNew, type)
        }
    }

    fun addTag(tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isBlank()) return
        val current = sharedPrefs.getStringSet("custom_tags", emptySet()) ?: emptySet()
        val updated = (current + trimmed).sorted()
        sharedPrefs.edit().putStringSet("custom_tags", updated.toSet()).apply()
        _customTags.value = updated
    }

    fun renameTag(oldName: String, newName: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || trimmedOld == trimmedNew) return

        deleteTag(trimmedOld)
        addTag(trimmedNew)
    }

    fun deleteTag(tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isBlank()) return

        val updatedDeleted = _deletedTags.value + trimmed
        _deletedTags.value = updatedDeleted
        sharedPrefs.edit().putStringSet("deleted_tags", updatedDeleted).apply()

        val currentCustom = sharedPrefs.getStringSet("custom_tags", emptySet()) ?: emptySet()
        if (currentCustom.contains(trimmed)) {
            val updated = currentCustom - trimmed
            sharedPrefs.edit().putStringSet("custom_tags", updated).apply()
            _customTags.value = updated.toList().sorted()
        }
    }

    fun updateCategoryColor(category: String, hexColor: String) {
        val map = _categoryColorMap.value.toMutableMap()
        map[category] = hexColor
        _categoryColorMap.value = map
        sharedPrefs.edit().putString("cat_color_$category", hexColor).apply()
    }

    fun updateTagColor(tag: String, hexColor: String) {
        val map = _tagColorMap.value.toMutableMap()
        map[tag] = hexColor
        _tagColorMap.value = map
        sharedPrefs.edit().putString("tag_color_$tag", hexColor).apply()
    }

    fun toggleBudgetWarning(percentage: Int, enabled: Boolean) {
        when (percentage) {
            80 -> {
                _budgetWarning80.value = enabled
                sharedPrefs.edit().putBoolean("budget_warning_80", enabled).apply()
            }
            90 -> {
                _budgetWarning90.value = enabled
                sharedPrefs.edit().putBoolean("budget_warning_90", enabled).apply()
            }
            100 -> {
                _budgetWarning100.value = enabled
                sharedPrefs.edit().putBoolean("budget_warning_100", enabled).apply()
            }
        }
    }

    fun toggleBudgetIncludeRecurringBills(enabled: Boolean) {
        _budgetIncludeRecurringBills.value = enabled
        sharedPrefs.edit().putBoolean("budget_include_recurring_bills", enabled).apply()
    }

    fun updateGoalViewMode(mode: String) {
        _goalViewMode.value = mode
        sharedPrefs.edit().putString("goal_view_mode", mode).apply()
    }

    fun updateGoalProgressStyle(style: String) {
        _goalProgressStyle.value = style
        sharedPrefs.edit().putString("goal_progress_style", style).apply()
    }

    fun updateDefaultTxType(type: String) {
        _defaultTxType.value = type
        sharedPrefs.edit().putString("default_tx_type", type).apply()
    }

    fun toggleRememberLastCategory(enabled: Boolean) {
        _rememberLastCategory.value = enabled
        sharedPrefs.edit().putBoolean("remember_last_category", enabled).apply()
    }

    fun toggleConfirmTxDelete(enabled: Boolean) {
        _confirmTxDelete.value = enabled
        sharedPrefs.edit().putBoolean("confirm_tx_delete", enabled).apply()
    }

    fun toggleGroupByDate(enabled: Boolean) {
        _groupByDate.value = enabled
        sharedPrefs.edit().putBoolean("group_by_date", enabled).apply()
    }

    fun markBackupPerformed() {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val now = sdf.format(Date())
        _lastBackupTimestamp.value = now
        sharedPrefs.edit().putString("last_backup_timestamp", now).apply()
        _toastMessage.value = "Backup created successfully"
    }

    fun toggleAutoBackupEnabled(enabled: Boolean) {
        _autoBackupEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        appSettingsManager.dispatch(AppSettingsIntent.SetAutoBackupEnabled(enabled))
    }

    fun setSyncMode(mode: String) {
        _syncMode.value = mode
        appSettingsManager.dispatch(com.example.data.AppSettingsIntent.UpdateSyncMode(mode))
        if (mode == "ONLINE_SYNC") {
            performCloudSync()
        }
    }

    fun performCloudSync(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            val syncManager = com.example.data.CloudSyncManager.getInstance(getApplication())
            val currentCurrencyCode = _selectedCurrencyCode.value
            val currentCurrencySymbol = _selectedCurrencySymbol.value
            val currentCurrencyName = _selectedCurrencyName.value
            val currentBudget = _monthlyBudget.value

            val result = syncManager.performSync(
                database = com.example.data.FinanceDatabase.getDatabase(getApplication()),
                currencyCode = currentCurrencyCode,
                currencySymbol = currentCurrencySymbol,
                currencyName = currentCurrencyName,
                monthlyBudget = currentBudget
            )
            _isCloudSyncing.value = false
            when (result) {
                is com.example.data.CloudSyncResult.Success -> {
                    _lastCloudSyncTimestamp.value = syncManager.lastSyncTimestamp.value
                    _cloudSyncStatus.value = "Synced"
                    _toastMessage.value = result.message
                    onResult?.invoke(true, result.message)
                }
                is com.example.data.CloudSyncResult.Failure -> {
                    _cloudSyncStatus.value = if (result.isOffline) "Offline (Local)" else "Error"
                    _toastMessage.value = result.error
                    onResult?.invoke(false, result.error)
                }
                is com.example.data.CloudSyncResult.Conflict -> {
                    _cloudSyncStatus.value = "Conflict Detected"
                    _toastMessage.value = "⚠️ Sync Conflict: ${result.message}"
                    onResult?.invoke(false, result.message)
                }
            }
        }
    }

    fun restoreFromCloud(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            val syncManager = com.example.data.CloudSyncManager.getInstance(getApplication())
            val result = syncManager.restoreFromCloud(
                database = com.example.data.FinanceDatabase.getDatabase(getApplication()),
                currencyCallback = { code, symbol, name, budget ->
                    updateDefaultCurrency(code, symbol, name, convertExisting = false)
                    updateMonthlyBudget(budget)
                }
            )
            _isCloudSyncing.value = false
            when (result) {
                is com.example.data.CloudSyncResult.Success -> {
                    _lastCloudSyncTimestamp.value = syncManager.lastSyncTimestamp.value
                    _cloudSyncStatus.value = "Restored"
                    _toastMessage.value = result.message
                    onResult?.invoke(true, result.message)
                }
                is com.example.data.CloudSyncResult.Failure -> {
                    _cloudSyncStatus.value = "Restore Error"
                    _toastMessage.value = result.error
                    onResult?.invoke(false, result.error)
                }
                is com.example.data.CloudSyncResult.Conflict -> {
                    _cloudSyncStatus.value = "Conflict Detected"
                    _toastMessage.value = "⚠️ Sync Conflict: ${result.message}"
                    onResult?.invoke(false, result.message)
                }
            }
        }
    }

    fun clearAllData(targetContext: Context? = null) {
        val ctx = targetContext ?: getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            try {
                ctx.filesDir?.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to delete filesDir contents during clearAllData", e)
            }
            try {
                ctx.cacheDir?.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to delete cacheDir contents during clearAllData", e)
            }
            withContext(Dispatchers.Main) {
                _toastMessage.value = "All data cleared successfully"
            }
        }
    }

    fun toggleLockOnRestart(enabled: Boolean) {
        _lockOnRestart.value = enabled
        sharedPrefs.edit().putBoolean("lock_on_restart", enabled).apply()
        appSettingsManager.dispatch(AppSettingsIntent.SetLockOnRestart(enabled))
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        sharedPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        appSettingsManager.dispatch(AppSettingsIntent.SetBiometricEnabled(enabled))
    }

    fun toggleHideSensitiveAmounts(enabled: Boolean) {
        _hideSensitiveAmounts.value = enabled
        sharedPrefs.edit().putBoolean("hide_sensitive_amounts", enabled).apply()
        appSettingsManager.dispatch(AppSettingsIntent.SetHideSensitiveAmounts(enabled))
    }

    fun togglePrivacyModeEnabled(enabled: Boolean) {
        _privacyModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("privacy_mode_enabled", enabled).apply()
        appSettingsManager.dispatch(AppSettingsIntent.SetPrivacyModeEnabled(enabled))
        // Reset session reveal whenever the feature itself is toggled, so
        // turning it off-then-on again doesn't leave amounts pre-revealed.
        _privacyRevealOverride.value = false
    }

    fun toggleScreenshotProtection(enabled: Boolean) {
        _screenshotProtection.value = enabled
        appSettingsManager.dispatch(AppSettingsIntent.SetScreenshotProtection(enabled))
    }

    fun updateFontFamily(font: String) {
        _fontFamily.value = font
        com.example.ui.theme.activeFontFamilyChoiceState = font
        appSettingsManager.dispatch(AppSettingsIntent.UpdateFontFamily(font))
    }

    fun toggleHaptics(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        appSettingsManager.dispatch(AppSettingsIntent.UpdateHapticsEnabled(enabled))
    }

    fun toggleHighContrast(enabled: Boolean) {
        _highContrastMode.value = enabled
        com.example.ui.theme.isHighContrastActive = enabled
        appSettingsManager.dispatch(AppSettingsIntent.UpdateHighContrastMode(enabled))
    }

    fun toggleCardGlow(enabled: Boolean) {
        _cardGlowAnimation.value = enabled
        appSettingsManager.dispatch(AppSettingsIntent.UpdateCardGlowAnimation(enabled))
    }

    fun updateAutoLockDuration(duration: String) {
        _autoLockDuration.value = duration
        appSettingsManager.dispatch(AppSettingsIntent.SetAutoLockDuration(duration))
    }

    fun updateDefaultPaymentMode(mode: String) {
        _defaultPaymentMode.value = mode
        appSettingsManager.dispatch(AppSettingsIntent.UpdateDefaultPaymentMode(mode))
    }

    fun updateHighAmountWarning(enabled: Boolean, threshold: Double) {
        _highAmountWarningEnabled.value = enabled
        _highAmountThreshold.value = threshold
        appSettingsManager.dispatch(AppSettingsIntent.UpdateHighAmountWarning(enabled, threshold))
    }

    fun updateBillReminderPreferences(timing: String, preferredTime: String, autoMarkPaid: Boolean) {
        _billReminderTiming.value = timing
        _billReminderPreferredTime.value = preferredTime
        _billAutoMarkPaid.value = autoMarkPaid
        appSettingsManager.dispatch(AppSettingsIntent.UpdateBillReminderTiming(timing))
        appSettingsManager.dispatch(AppSettingsIntent.UpdateBillReminderPreferredTime(preferredTime))
        appSettingsManager.dispatch(AppSettingsIntent.UpdateBillAutoMarkPaid(autoMarkPaid))
    }

    fun addCustomCategory(category: String, type: String = "EXPENSE") {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return

        val prefKey = if (type == "INCOME") "custom_income_categories" else "custom_expense_categories"
        val currentTyped = sharedPrefs.getStringSet(prefKey, emptySet()) ?: emptySet()
        val updatedTyped = currentTyped + trimmed

        val currentLegacy = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val updatedLegacy = currentLegacy + trimmed

        sharedPrefs.edit()
            .putStringSet(prefKey, updatedTyped)
            .putStringSet("custom_categories", updatedLegacy)
            .apply()

        if (type == "INCOME") {
            _customIncomeCategories.value = updatedTyped.toList().sorted()
        } else {
            _customExpenseCategories.value = updatedTyped.toList().sorted()
        }
        _customCategories.value = updatedLegacy.toList().sorted()
    }

    fun addGoalCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = sharedPrefs.getStringSet("custom_goal_categories", emptySet()) ?: emptySet()
        val updated = (current + trimmed).sorted()
        sharedPrefs.edit().putStringSet("custom_goal_categories", updated.toSet()).apply()
        _customGoalCategories.value = updated
    }

    fun deleteGoalCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = sharedPrefs.getStringSet("custom_goal_categories", emptySet()) ?: emptySet()
        val updated = (current - trimmed).toSet()
        sharedPrefs.edit().putStringSet("custom_goal_categories", updated).apply()
        _customGoalCategories.value = updated.toList().sorted()

        viewModelScope.launch {
            val allGoals = repository.allSavingsGoals.first()
            allGoals.forEach { g ->
                if (g.category.equals(trimmed, ignoreCase = true)) {
                    repository.updateSavingsGoal(g.copy(category = "Saving"))
                }
            }
        }
    }

    fun deleteCustomCategory(category: String) {
        val trimmed = category.trim()
        val current = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val updated = current - trimmed
        sharedPrefs.edit()
            .putStringSet("custom_categories", updated)
            .remove("cat_icon_$trimmed")
            .apply()
        _customCategories.value = updated.toList().sorted()
        _categoryIcons.value = _categoryIcons.value - trimmed
        
        viewModelScope.launch {
            val allExpensesList = repository.allExpenses.first()
            allExpensesList.forEach { exp ->
                if (exp.category == trimmed) {
                    repository.updateExpense(exp.copy(category = "Others"))
                }
            }
        }
    }

    fun renameCustomCategory(oldName: String, newName: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedNew.isEmpty() || trimmedOld == trimmedNew) return
        
        val current = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        if (current.contains(trimmedOld)) {
            val updated = current - trimmedOld + trimmedNew
            val savedIcon = sharedPrefs.getString("cat_icon_$trimmedOld", "Star") ?: "Star"
            sharedPrefs.edit()
                .putStringSet("custom_categories", updated)
                .remove("cat_icon_$trimmedOld")
                .putString("cat_icon_$trimmedNew", savedIcon)
                .apply()
            _customCategories.value = updated.toList().sorted()
            
            val updatedIcons = _categoryIcons.value.toMutableMap()
            updatedIcons.remove(trimmedOld)
            updatedIcons[trimmedNew] = savedIcon
            _categoryIcons.value = updatedIcons
            
            viewModelScope.launch {
                val allExpensesList = repository.allExpenses.first()
                allExpensesList.forEach { exp ->
                    if (exp.category == trimmedOld) {
                        repository.updateExpense(exp.copy(category = trimmedNew))
                    }
                }
            }
        }
    }

    fun getAvailableNetBalance(): Double {
        val allExp = expenses.value
        val curCode = _selectedCurrencyCode.value
        // Cash on hand (all income minus all expenses, including savings transfers)
        val rawBalance = allExp.availableCash(curCode)

        // GST Auto-Tax Reserve: set aside a % of true all-time income for tax
        val trueIncome = allExp.realIncome(curCode)
        val gstReserve = if (_isGstEnabled.value) trueIncome * (_gstRatePercent.value / 100.0) else 0.0

        // Monthly Safe Amount: a fixed emergency buffer that's never counted
        // as available, regardless of income/expense flow.
        val safeBuffer = if (_isMonthlySafeEnabled.value) _monthlySafeAmount.value else 0.0

        return rawBalance - gstReserve - safeBuffer
    }

    // Raw balance before GST reserve / Safe Amount are subtracted — useful
    // for showing "Total Net Balance" on the dashboard as-is, separate from
    // what's actually safe/available to spend.
    fun getRawNetBalance(): Double {
        return expenses.value.netWorth(savingsGoals.value, _selectedCurrencyCode.value)
    }

    fun getGstReserveAmount(): Double {
        if (!_isGstEnabled.value) return 0.0
        val totalInc = expenses.value.realIncome(_selectedCurrencyCode.value)
        return totalInc * (_gstRatePercent.value / 100.0)
    }

    fun addExpense(
        amount: Double,
        category: String,
        date: Long,
        note: String?,
        imagePath: String? = null,
        type: String = "EXPENSE",
        currencyCode: String? = null
    ) {
        val resolvedCurrencyCode = currencyCode ?: _selectedCurrencyCode.value
        if (type == "EXPENSE") {
            val available = getAvailableNetBalance()
            if (amount > available) {
                val symbol = _selectedCurrencySymbol.value
                _toastMessage.value = "🔒 Total Balance Locked: Cannot expense %s%,.2f! Exceeds available net balance (%s%,.2f)".format(symbol, amount, symbol, available.coerceAtLeast(0.0))
                return
            }
        }
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    amountMinor = Money.fromDouble(amount, resolvedCurrencyCode),
                    category = category,
                    date = date,
                    note = note,
                    imagePath = imagePath,
                    type = type,
                    currencyCode = resolvedCurrencyCode,
                    accountId = 1L, // Default to first account for now
                    kind = "REGULAR"
                )
            )
        }

        // Keep streak updated on adding transaction
        recordDailyTransactionActivity()

        // Remember this category as the last-used one for its type, so the
        // next Add Transaction dialog can pre-select it (if the "Remember
        // Last Selected Category" setting is on).
        if (type == "INCOME") {
            _lastUsedIncomeCategory.value = category
            sharedPrefs.edit().putString("last_used_income_category", category).apply()
        } else {
            _lastUsedExpenseCategory.value = category
            sharedPrefs.edit().putString("last_used_expense_category", category).apply()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _lastDeletedExpenses.value = listOf(expense)
            _undoDeleteMessage.value = "Deleted ${expense.category} (${_selectedCurrencySymbol.value}${String.format(Locale.getDefault(), "%,.2f", expense.amount)})"
            _undoDeleteVisible.value = true
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch {
            val exp = repository.getExpenseById(id)
            repository.deleteExpenseById(id)
            if (exp != null) {
                _lastDeletedExpenses.value = listOf(exp)
                _undoDeleteMessage.value = "Deleted ${exp.category} (${_selectedCurrencySymbol.value}${String.format(Locale.getDefault(), "%,.2f", exp.amount)})"
                _undoDeleteVisible.value = true
            }
        }
    }

    fun deleteExpenses(expensesList: List<Expense>) {
        if (expensesList.isEmpty()) return
        viewModelScope.launch {
            expensesList.forEach { repository.deleteExpense(it) }
            _lastDeletedExpenses.value = expensesList
            _undoDeleteMessage.value = "Deleted ${expensesList.size} transactions"
            _undoDeleteVisible.value = true
        }
    }

    fun undoDeleteExpense() {
        val toRestore = _lastDeletedExpenses.value
        if (toRestore.isEmpty()) return
        viewModelScope.launch {
            toRestore.forEach { expense ->
                repository.insertExpense(expense)
            }
            _lastDeletedExpenses.value = emptyList()
            _undoDeleteVisible.value = false
            _toastMessage.value = if (toRestore.size == 1) "Transaction restored" else "${toRestore.size} transactions restored"
            recordDailyTransactionActivity()
        }
    }

    fun dismissUndoDelete() {
        _undoDeleteVisible.value = false
        _lastDeletedExpenses.value = emptyList()
    }

    fun transferFunds(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String?,
        date: Long = System.currentTimeMillis(),
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        if (fromAccountId == toAccountId) {
            onResult?.invoke(false, "Source and destination accounts must be different.")
            _toastMessage.value = "Please select two different accounts."
            return
        }
        if (amount <= 0.0) {
            onResult?.invoke(false, "Transfer amount must be greater than zero.")
            _toastMessage.value = "Please enter an amount greater than 0."
            return
        }

        viewModelScope.launch {
            try {
                val cur = _selectedCurrencyCode.value
                val (outId, inId) = repository.transferBetweenAccounts(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = amount,
                    date = date,
                    note = note,
                    currencyCode = cur
                )
                recordDailyTransactionActivity()
                val symbol = _selectedCurrencySymbol.value
                val msg = "Transferred %s%,.2f successfully".format(symbol, amount)
                _toastMessage.value = msg
                onResult?.invoke(true, msg)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to process transfer."
                _toastMessage.value = err
                onResult?.invoke(false, err)
            }
        }
    }

    fun importTransactions(
        transactions: List<Expense>,
        targetAccountId: Long? = null,
        skipDuplicates: Boolean = true,
        onComplete: ((importedCount: Int, skippedCount: Int) -> Unit)? = null
    ) {
        if (transactions.isEmpty()) {
            onComplete?.invoke(0, 0)
            return
        }

        viewModelScope.launch {
            try {
                val existing = repository.allExpenses.firstOrNull() ?: emptyList()
                val defaultAccId = targetAccountId ?: (repository.getAccountsSnapshot().firstOrNull()?.id ?: 1L)

                var imported = 0
                var skipped = 0

                transactions.forEach { tx ->
                    val isDuplicate = skipDuplicates && existing.any { ex ->
                        ex.type == tx.type &&
                        Math.abs(ex.amount - tx.amount) < 0.001 &&
                        ex.category.equals(tx.category, ignoreCase = true) &&
                        Math.abs(ex.date - tx.date) < 86400000L // same day
                    }

                    if (isDuplicate) {
                        skipped++
                    } else {
                        val toInsert = tx.copy(
                            id = 0,
                            accountId = targetAccountId ?: if (tx.accountId > 0) tx.accountId else defaultAccId
                        )
                        repository.insertExpense(toInsert)
                        imported++
                    }
                }

                if (imported > 0) {
                    recordDailyTransactionActivity()
                    repository.syncAllAccountBalances()
                }

                val msg = "Imported $imported transactions" + if (skipped > 0) " ($skipped duplicates skipped)" else ""
                _toastMessage.value = msg
                onComplete?.invoke(imported, skipped)
            } catch (e: Exception) {
                _toastMessage.value = "Import failed: ${e.localizedMessage}"
                onComplete?.invoke(0, 0)
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun addAccount(name: String, balance: Double, type: String) {
        viewModelScope.launch {
            val cur = _selectedCurrencyCode.value
            repository.insertAccount(
                Account(
                    name = name,
                    openingBalance = balance,
                    openingBalanceMinor = Money.fromDouble(balance, cur),
                    balance = balance,
                    balanceMinor = Money.fromDouble(balance, cur),
                    type = type,
                    currencyCode = cur
                )
            )
        }
    }

    fun deleteAccount(account: Account, targetAccountId: Long? = null, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.getExpensesCountForAccount(account.id)
            if (count > 0) {
                if (targetAccountId != null) {
                    repository.reassignExpensesAccount(account.id, targetAccountId)
                    repository.deleteAccount(account)
                    onResult?.invoke(true, "Moved $count entries and deleted account")
                } else {
                    val fallback = repository.getAccountsSnapshot().firstOrNull { it.id != account.id }
                    if (fallback != null) {
                        repository.reassignExpensesAccount(account.id, fallback.id)
                        repository.deleteAccount(account)
                        onResult?.invoke(true, "Moved $count entries to ${fallback.name} and deleted account")
                    } else {
                        onResult?.invoke(false, "Cannot delete account with existing entries. Move entries first.")
                        _toastMessage.value = "Cannot delete account with $count entries. Move entries first."
                    }
                }
            } else {
                repository.deleteAccount(account)
                onResult?.invoke(true, "Account deleted")
            }
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        accountId: Long,
        currencyCode: String? = null
    ) {
        viewModelScope.launch {
            val cur = currencyCode ?: _selectedCurrencyCode.value
            repository.insertExpense(
                Expense(
                    amount = amount,
                    amountMinor = Money.fromDouble(amount, cur),
                    category = category,
                    date = System.currentTimeMillis(),
                    note = title.ifBlank { null },
                    type = type,
                    currencyCode = cur,
                    accountId = accountId,
                    kind = if (type.equals("INCOME", true)) "INCOME" else "EXPENSE"
                )
            )
        }
    }

    fun updateTransaction(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteTransaction(expense: Expense) {
        deleteExpense(expense)
    }

    fun addBudget(category: String, amountLimit: Double) {
        setCategoryBudget(category, amountLimit)
    }

    fun setCategoryBudget(category: String, amountLimit: Double) {
        viewModelScope.launch {
            val mYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())
            val existing = budgets.value.find { it.category.equals(category, ignoreCase = true) }
            val cur = _selectedCurrencyCode.value
            if (existing != null) {
                repository.insertBudget(existing.copy(amountLimit = amountLimit, amountLimitMinor = Money.fromDouble(amountLimit, cur), monthYear = mYear))
            } else {
                repository.insertBudget(Budget(category = category, amountLimit = amountLimit, amountLimitMinor = Money.fromDouble(amountLimit, cur), monthYear = mYear, currencyCode = cur))
            }
        }
    }

    fun deleteCategoryBudget(category: String) {
        viewModelScope.launch {
            val existing = budgets.value.filter { it.category.equals(category, ignoreCase = true) }
            existing.forEach { repository.deleteBudget(it) }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun addSavingsGoal(
        name: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        targetDate: Long = 0L,
        frequency: String = "WEEKLY",
        contributionAmount: Double = 0.0,
        isAutoGap: Boolean = true,
        iconTag: String = "🎯",
        category: String = "Saving",
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val cur = _selectedCurrencyCode.value
            repository.insertSavingsGoal(
                SavingsGoal(
                    name = name,
                    targetAmount = targetAmount,
                    targetAmountMinor = Money.fromDouble(targetAmount, cur),
                    currentAmount = initialAmount,
                    currentAmountMinor = Money.fromDouble(initialAmount, cur),
                    targetDate = targetDate,
                    frequency = frequency,
                    contributionAmount = contributionAmount,
                    contributionAmountMinor = Money.fromDouble(contributionAmount, cur),
                    isAutoGap = isAutoGap,
                    iconTag = iconTag,
                    category = category,
                    imageUri = imageUri,
                    currencyCode = cur
                )
            )
            if (initialAmount > 0) {
                repository.insertExpense(
                    Expense(
                        amount = initialAmount,
                        amountMinor = Money.fromDouble(initialAmount, cur),
                        category = "Locked Savings",
                        date = System.currentTimeMillis(),
                        note = "🔒 Initial savings locked in goal: $name",
                        type = "EXPENSE",
                        currencyCode = cur
                    )
                )
            }
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun quickDepositToGoal(goal: SavingsGoal, amount: Double, fromAccountId: Long? = null) {
        if (amount <= 0) return
        val available = getAvailableNetBalance()
        if (amount > available) {
            val symbol = _selectedCurrencySymbol.value
            _toastMessage.value = "🔒 Goal Deposit Locked: Cannot deposit %s%,.2f! Exceeds available net balance (%s%,.2f)".format(symbol, amount, symbol, available.coerceAtLeast(0.0))
            return
        }
        val depositMinor = Money.fromDouble(amount, goal.currencyCode)
        viewModelScope.launch {
            val newCurrMinor = goal.currentAmountMinor + depositMinor
            val updated = goal.copy(
                currentAmountMinor = newCurrMinor,
                currentAmount = Money.toDouble(newCurrMinor, goal.currencyCode)
            )
            repository.updateSavingsGoal(updated)

            val cur = _selectedCurrencyCode.value
            val expMinor = Money.fromDouble(amount, cur)
            repository.insertExpense(
                Expense(
                    amount = amount,
                    amountMinor = expMinor,
                    category = "Locked Savings",
                    date = System.currentTimeMillis(),
                    note = "🔒 Saved & locked in ${goal.name}",
                    type = "EXPENSE",
                    currencyCode = cur
                )
            )

            val targetAccount = if (fromAccountId != null) accounts.value.find { it.id == fromAccountId } else accounts.value.firstOrNull()
            if (targetAccount != null) {
                val accDepositMinor = Money.convert(depositMinor, goal.currencyCode, targetAccount.currencyCode)
                val newBalMinor = (targetAccount.openingBalanceMinor - accDepositMinor).coerceAtLeast(0L)
                val updatedAcc = targetAccount.copy(
                    openingBalanceMinor = newBalMinor,
                    openingBalance = Money.toDouble(newBalMinor, targetAccount.currencyCode),
                    balanceMinor = newBalMinor,
                    balance = Money.toDouble(newBalMinor, targetAccount.currencyCode)
                )
                repository.updateAccount(updatedAcc)
            }
        }
    }

    fun quickDeductFromGoal(goal: SavingsGoal, amount: Double, toAccountId: Long? = null) {
        if (amount <= 0 || goal.currentAmountMinor <= 0L) return
        val reqMinor = Money.fromDouble(amount, goal.currencyCode)
        val deductMinor = reqMinor.coerceAtMost(goal.currentAmountMinor)
        viewModelScope.launch {
            val newCurrMinor = (goal.currentAmountMinor - deductMinor).coerceAtLeast(0L)
            val updated = goal.copy(
                currentAmountMinor = newCurrMinor,
                currentAmount = Money.toDouble(newCurrMinor, goal.currencyCode)
            )
            repository.updateSavingsGoal(updated)

            val cur = _selectedCurrencyCode.value
            val deductAmount = Money.toDouble(deductMinor, goal.currencyCode)
            val expMinor = Money.fromDouble(deductAmount, cur)
            repository.insertExpense(
                Expense(
                    amount = deductAmount,
                    amountMinor = expMinor,
                    category = "Goal Withdrawal",
                    date = System.currentTimeMillis(),
                    note = "🔓 Deducted/unlocked from ${goal.name}",
                    type = "INCOME",
                    currencyCode = cur
                )
            )

            val targetAccount = if (toAccountId != null) accounts.value.find { it.id == toAccountId } else accounts.value.firstOrNull()
            if (targetAccount != null) {
                val accDeductMinor = Money.convert(deductMinor, goal.currencyCode, targetAccount.currencyCode)
                val newBalMinor = targetAccount.openingBalanceMinor + accDeductMinor
                val updatedAcc = targetAccount.copy(
                    openingBalanceMinor = newBalMinor,
                    openingBalance = Money.toDouble(newBalMinor, targetAccount.currencyCode),
                    balanceMinor = newBalMinor,
                    balance = Money.toDouble(newBalMinor, targetAccount.currencyCode)
                )
                repository.updateAccount(updatedAcc)
            }
        }
    }

    fun allocateFundsToGoals(allocations: Map<Long, Double>, fromAccountId: Long? = null) {
        val totalToAllocate = allocations.values.sum()
        if (totalToAllocate <= 0) return
        val available = getAvailableNetBalance()
        if (totalToAllocate > available) {
            val symbol = _selectedCurrencySymbol.value
            _toastMessage.value = "🔒 Goal Allocation Locked: Cannot allocate %s%,.2f! Exceeds available net balance (%s%,.2f)".format(symbol, totalToAllocate, symbol, available.coerceAtLeast(0.0))
            return
        }
        viewModelScope.launch {
            val cur = _selectedCurrencyCode.value
            val currentGoals = savingsGoals.value
            allocations.forEach { (goalId, amount) ->
                if (amount > 0) {
                    val goal = currentGoals.find { it.id == goalId }
                    if (goal != null) {
                        val depositMinor = Money.fromDouble(amount, goal.currencyCode)
                        val newCurrMinor = goal.currentAmountMinor + depositMinor
                        val updated = goal.copy(
                            currentAmountMinor = newCurrMinor,
                            currentAmount = Money.toDouble(newCurrMinor, goal.currencyCode)
                        )
                        repository.updateSavingsGoal(updated)
                        repository.insertExpense(
                            Expense(
                                amount = amount,
                                amountMinor = Money.fromDouble(amount, cur),
                                category = "Locked Savings",
                                date = System.currentTimeMillis(),
                                note = "🔒 Allocated savings to ${goal.name}",
                                type = "EXPENSE",
                                currencyCode = cur
                            )
                        )
                    }
                }
            }

            val targetAccount = if (fromAccountId != null) accounts.value.find { it.id == fromAccountId } else accounts.value.firstOrNull()
            if (targetAccount != null) {
                val totalMinor = Money.fromDouble(totalToAllocate, cur)
                val accDepositMinor = Money.convert(totalMinor, cur, targetAccount.currencyCode)
                val newBalMinor = (targetAccount.openingBalanceMinor - accDepositMinor).coerceAtLeast(0L)
                val updatedAcc = targetAccount.copy(
                    openingBalanceMinor = newBalMinor,
                    openingBalance = Money.toDouble(newBalMinor, targetAccount.currencyCode),
                    balanceMinor = newBalMinor,
                    balance = Money.toDouble(newBalMinor, targetAccount.currencyCode)
                )
                repository.updateAccount(updatedAcc)
            }
            val symbol = _selectedCurrencySymbol.value
            _toastMessage.value = "🎯 Successfully allocated %s%,.2f across %d goal(s)".format(symbol, totalToAllocate, allocations.count { it.value > 0 })
        }
    }

    // AI Financial Advisor Layer
    private var lastAiRequestTimestamp: Long = 0L

    private val _aiCardAnswer = MutableStateFlow<String?>(null)
    val aiCardAnswer: StateFlow<String?> = _aiCardAnswer.asStateFlow()

    private val _isAiCardLoading = MutableStateFlow(false)
    val isAiCardLoading: StateFlow<Boolean> = _isAiCardLoading.asStateFlow()

    private val _showAiConsentDialog = MutableStateFlow(false)
    val showAiConsentDialog: StateFlow<Boolean> = _showAiConsentDialog.asStateFlow()

    val aiConsentEnabled: StateFlow<Boolean> = appSettingsManager.state
        .map { it.aiConsentEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAiConsent(enabled: Boolean) {
        appSettingsManager.dispatch(AppSettingsIntent.SetAiConsent(enabled))
        _showAiConsentDialog.value = false
    }

    fun dismissAiConsentDialog() {
        _showAiConsentDialog.value = false
    }

    fun promptAiConsentDialog() {
        _showAiConsentDialog.value = true
    }

    fun buildCurrentTotalsContext(): FinancialTotalsContext {
        val allExp = expenses.value
        val curCode = _selectedCurrencyCode.value
        val curSymbol = _selectedCurrencySymbol.value
        val totalInc = allExp.realIncome(curCode)
        val totalExp = allExp.realExpense(curCode)
        val net = totalInc - totalExp
        val topCat = allExp.filter { it.type != "INCOME" && it.category != "Locked Savings" && it.kind != "SAVINGS_LOCK" && it.kind != "SAVINGS_RELEASE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { Money.convert(it.amountMinor, it.currencyCode, curCode) } }
            .maxByOrNull { it.value }

        return FinancialTotalsContext(
            totalIncome = totalInc,
            totalExpenses = totalExp,
            netBalance = net,
            topExpenseCategory = topCat?.key ?: "General",
            categoryNames = allCategories.value,
            currencySymbol = curSymbol
        )
    }

    suspend fun askAiAdvisor(userPrompt: String): AiResult {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) {
            return AiResult.Failure.Unknown("Please enter a question.")
        }
        if (trimmed.length > FirebaseAiAdvisor.MAX_PROMPT_LENGTH) {
            return AiResult.Failure.Unknown("Prompt is too long (maximum ${FirebaseAiAdvisor.MAX_PROMPT_LENGTH} characters).")
        }

        if (!appSettingsManager.state.value.aiConsentEnabled) {
            _showAiConsentDialog.value = true
            return AiResult.Failure.ConsentRequired
        }

        val now = System.currentTimeMillis()
        if (now - lastAiRequestTimestamp < 3_000L) {
            return AiResult.Failure.RateLimited
        }

        // Daily limit check (20 requests/day soft cap per device)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDate = appSettingsManager.state.value.aiLastRequestDate
        val count = if (lastDate == todayStr) appSettingsManager.state.value.aiDailyRequestCount else 0
        if (count >= 20) {
            return AiResult.Failure.DailyLimitReached
        }

        lastAiRequestTimestamp = now
        appSettingsManager.dispatch(AppSettingsIntent.IncrementAiRequestCount)

        val context = buildCurrentTotalsContext()
        return aiAdvisor.ask(trimmed, context)
    }

    fun askAiCardQuestion(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiCardLoading.value = true
            val result = askAiAdvisor(question)
            when (result) {
                is AiResult.Success -> {
                    _aiCardAnswer.value = result.text
                }
                is AiResult.Failure -> {
                    if (result !is AiResult.Failure.ConsentRequired) {
                        _aiCardAnswer.value = result.userFriendlyMessage
                    }
                }
            }
            _isAiCardLoading.value = false
        }
    }

    fun sendChatMessage(userMessageText: String) {
        if (userMessageText.isBlank()) return
        val userMsg = ChatMessage(text = userMessageText, isUser = true)
        _chatMessages.update { it + userMsg }
        _isChatLoading.value = true

        viewModelScope.launch {
            val result = askAiAdvisor(userMessageText)
            val reply = when (result) {
                is AiResult.Success -> result.text
                is AiResult.Failure -> result.userFriendlyMessage
            }
            _chatMessages.update { it + ChatMessage(text = reply, isUser = false) }
            _isChatLoading.value = false
        }
    }

    fun requestFinancialAudit() {
        _isAuditLoading.value = true
        _aiAuditReport.value = null

        viewModelScope.launch {
            val result = askAiAdvisor("Please provide a comprehensive financial audit and 3 actionable budget optimization tips based on my monthly totals.")
            _aiAuditReport.value = when (result) {
                is AiResult.Success -> result.text
                is AiResult.Failure -> result.userFriendlyMessage
            }
            _isAuditLoading.value = false
        }
    }

    fun generateDailySpendingInsight(forceRefresh: Boolean = false) {
        if (_isInsightLoading.value && !forceRefresh) return
        val now = System.currentTimeMillis()
        val lastUpdated = _insightLastUpdated.value
        if (!forceRefresh && _dailySpendingInsight.value != null && lastUpdated != null && (now - lastUpdated < 4 * 3600 * 1000L)) {
            return
        }

        _isInsightLoading.value = true
        viewModelScope.launch {
            val result = askAiAdvisor("Provide a concise 2-sentence spending observation and budget health summary for today based on my totals.")
            _dailySpendingInsight.value = when (result) {
                is AiResult.Success -> result.text
                is AiResult.Failure -> result.userFriendlyMessage
            }
            _insightLastUpdated.value = System.currentTimeMillis()
            _isInsightLoading.value = false
        }
    }

    // ==========================================
    // 📦 FULL APP DATA BACKUP & RESTORE ENGINE (PORTABLE CODE)
    // ==========================================
    suspend fun generateFullBackupCode(): String = withContext(Dispatchers.IO) {
        val json = JSONObject()
        json.put("app", "AIStudioFinance")
        json.put("version", 2)
        json.put("snapshotFormatVersion", 2)
        json.put("exportedAt", System.currentTimeMillis())

        // Profile
        val profileObj = JSONObject()
        profileObj.put("userName", _userName.value ?: "")
        profileObj.put("userProfileImageUri", _userProfileImageUri.value ?: "")
        profileObj.put("userDob", _userDob.value)
        profileObj.put("userJob", _userJob.value)
        profileObj.put("userMonthlyIncome", _userMonthlyIncome.value)
        profileObj.put("userGender", _userGender.value)
        json.put("profile", profileObj)

        // Accounts
        val accountsArr = JSONArray()
        val accountList = repository.allAccounts.firstOrNull() ?: emptyList()
        accountList.forEach { acc ->
            val accObj = JSONObject()
            accObj.put("id", acc.id)
            accObj.put("name", acc.name)
            accObj.put("balanceMinor", acc.balanceMinor)
            accObj.put("balance", Money.toDouble(acc.balanceMinor, acc.currencyCode))
            accObj.put("type", acc.type)
            accObj.put("currencyCode", acc.currencyCode)
            accountsArr.put(accObj)
        }
        json.put("accounts", accountsArr)

        // Transactions
        json.put("transactions", JSONArray())

        // Expenses
        val expensesArr = JSONArray()
        val expenseList = repository.allExpenses.firstOrNull() ?: emptyList()
        expenseList.forEach { exp ->
            val expObj = JSONObject()
            expObj.put("id", exp.id)
            expObj.put("amountMinor", exp.amountMinor)
            expObj.put("amount", Money.toDouble(exp.amountMinor, exp.currencyCode))
            expObj.put("category", exp.category)
            expObj.put("date", exp.date)
            expObj.put("note", exp.note ?: "")
            expObj.put("imagePath", exp.imagePath ?: "")
            expObj.put("type", exp.type)
            expObj.put("currencyCode", exp.currencyCode)
            expensesArr.put(expObj)
        }
        json.put("expenses", expensesArr)

        // Budgets
        val budgetsArr = JSONArray()
        val budgetList = repository.allBudgets.firstOrNull() ?: emptyList()
        budgetList.forEach { b ->
            val bObj = JSONObject()
            bObj.put("id", b.id)
            bObj.put("category", b.category)
            bObj.put("amountLimitMinor", b.amountLimitMinor)
            bObj.put("amountLimit", Money.toDouble(b.amountLimitMinor, b.currencyCode))
            bObj.put("monthYear", b.monthYear)
            bObj.put("currencyCode", b.currencyCode)
            budgetsArr.put(bObj)
        }
        json.put("budgets", budgetsArr)

        // Savings Goals
        val goalsArr = JSONArray()
        val goalList = repository.allSavingsGoals.firstOrNull() ?: emptyList()
        goalList.forEach { g ->
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("name", g.name)
            gObj.put("targetAmountMinor", g.targetAmountMinor)
            gObj.put("targetAmount", Money.toDouble(g.targetAmountMinor, g.currencyCode))
            gObj.put("currentAmountMinor", g.currentAmountMinor)
            gObj.put("currentAmount", Money.toDouble(g.currentAmountMinor, g.currencyCode))
            gObj.put("targetDate", g.targetDate)
            gObj.put("frequency", g.frequency)
            gObj.put("contributionAmountMinor", g.contributionAmountMinor)
            gObj.put("contributionAmount", Money.toDouble(g.contributionAmountMinor, g.currencyCode))
            gObj.put("isAutoGap", g.isAutoGap)
            gObj.put("iconTag", g.iconTag)
            gObj.put("category", g.category)
            gObj.put("imageUri", g.imageUri ?: "")
            gObj.put("currencyCode", g.currencyCode)
            goalsArr.put(gObj)
        }
        json.put("savingsGoals", goalsArr)

        // Bills
        val billsArr = JSONArray()
        billsList.forEach { bill ->
            val bObj = JSONObject()
            bObj.put("id", bill.id)
            bObj.put("title", bill.title)
            bObj.put("amountMinor", bill.amountMinor)
            bObj.put("amount", Money.toDouble(bill.amountMinor, "INR"))
            bObj.put("dueDate", bill.dueDate)
            billsArr.put(bObj)
        }
        json.put("bills", billsArr)

        // Reminders
        val remindersArr = JSONArray()
        remindersList.forEach { rem ->
            val rObj = JSONObject()
            rObj.put("id", rem.id)
            rObj.put("text", rem.text)
            rObj.put("dueDate", rem.dueDate)
            rObj.put("isCompleted", rem.isCompleted)
            rObj.put("isEnabled", rem.isEnabled)
            remindersArr.put(rObj)
        }
        json.put("reminders", remindersArr)

        // Recurring Rules
        val rulesArr = JSONArray()
        val rulesList = repository.allRecurringRules.firstOrNull() ?: emptyList()
        rulesList.forEach { r ->
            val rObj = JSONObject()
            rObj.put("id", r.id)
            rObj.put("title", r.title)
            rObj.put("amountMinor", r.amountMinor)
            rObj.put("category", r.category)
            rObj.put("frequency", r.frequency)
            rObj.put("startDate", r.startDate)
            rObj.put("nextDueDate", r.nextDueDate)
            rObj.put("isActive", r.isActive)
            rObj.put("currencyCode", r.currencyCode)
            rObj.put("accountId", r.accountId)
            rObj.put("note", r.note ?: "")
            rulesArr.put(rObj)
        }
        json.put("recurringRules", rulesArr)

        // Settings
        val settingsObj = JSONObject()
        settingsObj.put("monthlyBudget", _monthlyBudget.value)
        settingsObj.put("selectedCurrencyCode", _selectedCurrencyCode.value)
        settingsObj.put("selectedCurrencySymbol", _selectedCurrencySymbol.value)
        settingsObj.put("selectedCurrencyName", _selectedCurrencyName.value)
        settingsObj.put("statsCurrencyCode", _statsCurrencyCode.value)
        settingsObj.put("statsCurrencySymbol", _statsCurrencySymbol.value)
        settingsObj.put("statsCurrencyName", _statsCurrencyName.value)
        settingsObj.put("selectedLanguage", _selectedLanguage.value)
        settingsObj.put("appPin", _appPin.value ?: "")
        settingsObj.put("themeIndex", _themeIndex.value)
        settingsObj.put("customThemeHue", _customThemeHue.value)
        settingsObj.put("themeMode", _themeMode.value)

        settingsObj.put("customCategories", JSONArray(_customCategories.value))
        settingsObj.put("customExpenseCategories", JSONArray(_customExpenseCategories.value))
        settingsObj.put("customIncomeCategories", JSONArray(_customIncomeCategories.value))
        settingsObj.put("customGoalCategories", JSONArray(_customGoalCategories.value))
        settingsObj.put("deletedCategories", JSONArray(_deletedCategories.value.toList()))
        settingsObj.put("customTags", JSONArray(_customTags.value))
        settingsObj.put("deletedTags", JSONArray(_deletedTags.value.toList()))
        json.put("settings", settingsObj)

        val jsonString = json.toString()
        val base64 = android.util.Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        "AISTUDIO_BACKUP_V1:$base64"
    }

    suspend fun restoreFromBackupCode(codeString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmed = codeString.trim()
            if (trimmed.isBlank()) return@withContext Result.failure(IllegalArgumentException("Backup code cannot be empty."))

            val jsonString = if (trimmed.startsWith("AISTUDIO_BACKUP_V1:")) {
                val rawB64 = trimmed.substring("AISTUDIO_BACKUP_V1:".length)
                String(android.util.Base64.decode(rawB64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed
            } else {
                String(android.util.Base64.decode(trimmed, android.util.Base64.DEFAULT), Charsets.UTF_8)
            }

            val json = JSONObject(jsonString)

            // Parse Accounts
            val newAccounts = mutableListOf<Account>()
            val accountsArr = json.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accountsArr.length()) {
                val o = accountsArr.getJSONObject(i)
                val cur = o.optString("currencyCode", _selectedCurrencyCode.value)
                val balMinor = if (o.has("openingBalanceMinor")) {
                    o.optLong("openingBalanceMinor", 0L)
                } else if (o.has("balanceMinor")) {
                    o.optLong("balanceMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("balance", 0.0), cur)
                }
                newAccounts.add(
                    Account(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", "Account"),
                        openingBalanceMinor = balMinor,
                        type = o.optString("type", "CASH"),
                        currencyCode = cur
                    )
                )
            }

            // Parse Expenses & Legacy Transactions
            val newExpenses = mutableListOf<Expense>()
            val txArr = json.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                val cur = o.optString("currencyCode", _selectedCurrencyCode.value)
                val amtMinor = if (o.has("amountMinor")) {
                    o.optLong("amountMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("amount", 0.0), cur)
                }
                newExpenses.add(
                    Expense(
                        id = o.optLong("id", 0L),
                        amountMinor = amtMinor,
                        category = o.optString("category", o.optString("title", "General")),
                        date = o.optLong("timestamp", o.optLong("date", System.currentTimeMillis())),
                        note = if (o.has("note") && !o.isNull("note")) o.getString("note").ifBlank { null } else null,
                        imagePath = if (o.has("imagePath") && !o.isNull("imagePath")) o.getString("imagePath").ifBlank { null } else null,
                        type = o.optString("type", "EXPENSE"),
                        currencyCode = cur,
                        accountId = o.optLong("accountId", 1L)
                    )
                )
            }

            val expArr = json.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expArr.length()) {
                val o = expArr.getJSONObject(i)
                val cur = o.optString("currencyCode", _selectedCurrencyCode.value)
                val amtMinor = if (o.has("amountMinor")) {
                    o.optLong("amountMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("amount", 0.0), cur)
                }
                newExpenses.add(
                    Expense(
                        id = o.optLong("id", 0L),
                        amountMinor = amtMinor,
                        category = o.optString("category", "General"),
                        date = o.optLong("date", System.currentTimeMillis()),
                        note = if (o.has("note") && !o.isNull("note")) o.getString("note").ifBlank { null } else null,
                        imagePath = if (o.has("imagePath") && !o.isNull("imagePath")) o.getString("imagePath").ifBlank { null } else null,
                        type = o.optString("type", "EXPENSE"),
                        currencyCode = cur,
                        accountId = o.optLong("accountId", 1L),
                        kind = o.optString("kind", "EXPENSE"),
                        goalId = if (o.has("goalId") && !o.isNull("goalId")) o.getLong("goalId") else null
                    )
                )
            }

            // Parse Budgets
            val newBudgets = mutableListOf<Budget>()
            val bArr = json.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until bArr.length()) {
                val o = bArr.getJSONObject(i)
                val cur = o.optString("currencyCode", _selectedCurrencyCode.value)
                val limitMinor = if (o.has("amountLimitMinor")) {
                    o.optLong("amountLimitMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("amountLimit", 0.0), cur)
                }
                newBudgets.add(
                    Budget(
                        id = o.optLong("id", 0L),
                        category = o.optString("category", "General"),
                        amountLimitMinor = limitMinor,
                        monthYear = o.optString("monthYear", ""),
                        currencyCode = cur
                    )
                )
            }

            // Parse Savings Goals
            val newGoals = mutableListOf<SavingsGoal>()
            val gArr = json.optJSONArray("savingsGoals") ?: JSONArray()
            for (i in 0 until gArr.length()) {
                val o = gArr.getJSONObject(i)
                val cur = o.optString("currencyCode", _selectedCurrencyCode.value)
                val targetMinor = if (o.has("targetAmountMinor")) {
                    o.optLong("targetAmountMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("targetAmount", 0.0), cur)
                }
                val currentMinor = if (o.has("currentAmountMinor")) {
                    o.optLong("currentAmountMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("currentAmount", 0.0), cur)
                }
                val contribMinor = if (o.has("contributionAmountMinor")) {
                    o.optLong("contributionAmountMinor", 0L)
                } else {
                    Money.fromDouble(o.optDouble("contributionAmount", 0.0), cur)
                }
                newGoals.add(
                    SavingsGoal(
                        id = o.optLong("id", 0L),
                        name = o.optString("name", "Goal"),
                        targetAmountMinor = targetMinor,
                        currentAmountMinor = currentMinor,
                        targetDate = o.optLong("targetDate", 0L),
                        frequency = o.optString("frequency", "WEEKLY"),
                        contributionAmountMinor = contribMinor,
                        isAutoGap = o.optBoolean("isAutoGap", true),
                        iconTag = o.optString("iconTag", "🎮"),
                        category = o.optString("category", "Saving"),
                        imageUri = if (o.has("imageUri") && !o.isNull("imageUri")) o.getString("imageUri").ifBlank { null } else null,
                        currencyCode = cur
                    )
                )
            }

            val newRules = mutableListOf<RecurringRule>()
            val rulesArr = json.optJSONArray("recurringRules")
            if (rulesArr != null) {
                for (i in 0 until rulesArr.length()) {
                    val o = rulesArr.getJSONObject(i)
                    newRules.add(
                        RecurringRule(
                            id = o.optLong("id", 0L),
                            title = o.optString("title", "Recurring"),
                            amountMinor = o.optLong("amountMinor", 0L),
                            category = o.optString("category", "General"),
                            frequency = o.optString("frequency", "MONTHLY"),
                            startDate = o.optLong("startDate", System.currentTimeMillis()),
                            nextDueDate = o.optLong("nextDueDate", System.currentTimeMillis()),
                            isActive = o.optBoolean("isActive", true),
                            currencyCode = o.optString("currencyCode", "INR"),
                            accountId = o.optLong("accountId", 0L),
                            note = o.optString("note", "")
                        )
                    )
                }
            }

            // Execute DB restore
            repository.restoreAllData(newExpenses, newAccounts, newBudgets, newGoals, emptyList(), newRules)

            // Restore Profile
            val prof = json.optJSONObject("profile")
            if (prof != null) {
                val name = prof.optString("userName", "")
                val img = prof.optString("userProfileImageUri", "")
                val dob = prof.optString("userDob", "24 December 1999")
                val job = prof.optString("userJob", "Successor Designer")
                val inc = prof.optString("userMonthlyIncome", "500 - 3000 / year")
                val gen = prof.optString("userGender", "Male")

                _userName.value = if (name.isNotBlank()) name else null
                _userProfileImageUri.value = if (img.isNotBlank()) img else null
                _userDob.value = dob
                _userJob.value = job
                _userMonthlyIncome.value = inc
                _userGender.value = gen

                sharedPrefs.edit().apply {
                    putString("user_name", _userName.value)
                    putString("user_profile_image_uri", _userProfileImageUri.value)
                    putString("user_dob", dob)
                    putString("user_job", job)
                    putString("user_monthly_income", inc)
                    putString("user_gender", gen)
                    apply()
                }
            }

            // Restore Bills & Reminders
            val billsArr = json.optJSONArray("bills")
            if (billsArr != null) {
                withContext(Dispatchers.Main) {
                    billsList.clear()
                    for (i in 0 until billsArr.length()) {
                        val o = billsArr.getJSONObject(i)
                        val amtMinor = if (o.has("amountMinor")) o.optLong("amountMinor", 0L) else Money.fromDouble(o.optDouble("amount", 0.0), "INR")
                        billsList.add(
                            BillEntry(
                                id = o.optString("id", System.currentTimeMillis().toString()),
                                title = o.optString("title", "Bill"),
                                amountMinor = amtMinor,
                                dueDate = o.optString("dueDate", ""),
                                amount = Money.toDouble(amtMinor, "INR")
                            )
                        )
                    }
                }
            }

            val remArr = json.optJSONArray("reminders")
            if (remArr != null) {
                withContext(Dispatchers.Main) {
                    remindersList.clear()
                    for (i in 0 until remArr.length()) {
                        val o = remArr.getJSONObject(i)
                        remindersList.add(
                            ReminderEntry(
                                id = o.optString("id", System.currentTimeMillis().toString()),
                                text = o.optString("text", "Reminder"),
                                dueDate = o.optString("dueDate", ""),
                                isCompleted = o.optBoolean("isCompleted", false),
                                isEnabled = o.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }
            }

            // Restore Settings
            val set = json.optJSONObject("settings")
            if (set != null) {
                val mb = set.optDouble("monthlyBudget", 25000.0)
                val cc = set.optString("selectedCurrencyCode", "INR")
                val cs = set.optString("selectedCurrencySymbol", "₹")
                val cn = set.optString("selectedCurrencyName", "Indian Rupee")
                val scc = set.optString("statsCurrencyCode", "INR")
                val scs = set.optString("statsCurrencySymbol", "₹")
                val scn = set.optString("statsCurrencyName", "Indian Rupee")
                val lang = set.optString("selectedLanguage", "English")

                _monthlyBudget.value = mb
                _selectedCurrencyCode.value = cc
                _selectedCurrencySymbol.value = cs
                _selectedCurrencyName.value = cn
                _statsCurrencyCode.value = scc
                _statsCurrencySymbol.value = scs
                _statsCurrencyName.value = scn
                _selectedLanguage.value = lang

                val minor = Money.fromDouble(mb, cc)
                appSettingsManager.dispatch(AppSettingsIntent.UpdateMonthlyBudget(mb))
                appSettingsManager.dispatch(AppSettingsIntent.UpdateCurrency(cc, cs, cn, false))
                appSettingsManager.dispatch(AppSettingsIntent.UpdateStatsCurrency(scc, scs, scn))
                appSettingsManager.dispatch(AppSettingsIntent.UpdateLanguage(lang))

                LanguageManager.applyAppLocale(getApplication(), lang)
            }

            markBackupPerformed()
            Result.success("Restored ${newExpenses.size} expenses, ${newGoals.size} goals, profile, and settings successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 🧠 FINANCIAL HEALTH SCORE STUDIO & CASH FLOW
    // ==========================================

    private val healthCalculator = FinancialHealthCalculator()
    private val cashFlowProjector = CashFlowProjector()

    fun getFinancialHealthScore(
        simulatedMonthlySavingsBoost: Double = 0.0,
        simulatedExpenseCut: Double = 0.0
    ): com.example.ui.health.FinancialHealthCalculator.HealthScoreBreakdown {
        val cur = _selectedCurrencyCode.value
        val allExpenses = expenses.value
        val allGoals = savingsGoals.value
        val allAccounts = accounts.value
        val budget = _monthlyBudget.value

        return healthCalculator.calculateHealthScore(
            allExpenses = allExpenses,
            allGoals = allGoals,
            allAccounts = allAccounts,
            budget = budget,
            currencyCode = cur,
            simulatedMonthlySavingsBoost = simulatedMonthlySavingsBoost,
            simulatedExpenseCut = simulatedExpenseCut
        )
    }

    fun getCashFlowProjections(daysToForecast: Int = 30): List<com.example.ui.projections.CashFlowProjector.CashFlowDayProjection> {
        val cur = _selectedCurrencyCode.value
        val allAccounts = accounts.value
        val allExpenses = expenses.value
        val allBills = billsList.toList()

        return cashFlowProjector.projectCashFlow(
            allAccounts = allAccounts,
            allExpenses = allExpenses,
            allBills = allBills,
            currencyCode = cur,
            daysToForecast = daysToForecast
        )
    }

    fun getNetWorthSummary(targetCurrencyCode: String? = null): NetWorthSummary {
        val cur = targetCurrencyCode ?: _selectedCurrencyCode.value
        val allAccounts = accounts.value

        var totalAssets = 0.0
        var totalLiabilities = 0.0
        val items = mutableListOf<AccountAllocationItem>()

        for (acc in allAccounts) {
            val converted = Money.convertWithFallback(acc.balanceMinor, acc.currencyCode, cur)
            val balanceDouble = Money.toDouble(converted, cur)
            val assetClass = AssetClass.fromAccountType(acc.type)

            if (assetClass == AssetClass.LIABILITIES || balanceDouble < 0) {
                totalLiabilities += kotlin.math.abs(balanceDouble)
            } else {
                totalAssets += balanceDouble
            }

            items.add(
                AccountAllocationItem(
                    account = acc,
                    convertedBalance = balanceDouble,
                    assetClass = assetClass,
                    percentageOfTotalAssets = 0.0
                )
            )
        }

        val netWorth = totalAssets - totalLiabilities
        val safeTotalAssets = if (totalAssets > 0) totalAssets else 1.0

        val populatedItems = items.map { item ->
            val pct = if (item.assetClass != AssetClass.LIABILITIES && item.convertedBalance > 0) {
                (item.convertedBalance / safeTotalAssets) * 100.0
            } else 0.0
            item.copy(percentageOfTotalAssets = pct)
        }

        val breakdowns = AssetClass.values().map { cls ->
            val clsItems = populatedItems.filter { it.assetClass == cls }
            val clsTotal = clsItems.sumOf { kotlin.math.abs(it.convertedBalance) }
            val clsPct = if (totalAssets > 0 && cls != AssetClass.LIABILITIES) {
                (clsTotal / totalAssets) * 100.0
            } else if (totalLiabilities > 0 && cls == AssetClass.LIABILITIES) {
                (clsTotal / totalLiabilities) * 100.0
            } else 0.0

            AssetClassBreakdown(
                assetClass = cls,
                totalAmount = clsTotal,
                percentageOfAssets = clsPct,
                accountsCount = clsItems.size,
                accounts = clsItems
            )
        }

        val liquidAmount = breakdowns.firstOrNull { it.assetClass == AssetClass.LIQUID }?.totalAmount ?: 0.0
        val growthAmount = breakdowns.firstOrNull { it.assetClass == AssetClass.GROWTH }?.totalAmount ?: 0.0
        val reserveAmount = breakdowns.firstOrNull { it.assetClass == AssetClass.RESERVES }?.totalAmount ?: 0.0
        val liabilitiesAmount = breakdowns.firstOrNull { it.assetClass == AssetClass.LIABILITIES }?.totalAmount ?: 0.0

        val liquidityRatio = if (totalAssets > 0) (liquidAmount / totalAssets) else 0.0
        val debtToAssetRatio = if (totalAssets > 0) (liabilitiesAmount / totalAssets) else 0.0

        val advice = mutableListOf<String>()
        if (debtToAssetRatio > 0.40) {
            advice.add("⚠️ Liabilities exceed 40% of gross assets. Consider prioritizing high-interest debt payoffs.")
        } else if (liabilitiesAmount == 0.0) {
            advice.add("🛡️ Zero liabilities! You have full equity ownership of your personal balance sheet.")
        }

        if (liquidityRatio > 0.60 && totalAssets > 5000) {
            advice.add("💡 Over 60% of your assets are in liquid cash. Consider allocating a portion to growth investments or inflation-hedging reserves.")
        } else if (liquidityRatio < 0.15 && totalAssets > 0) {
            advice.add("⚠️ Liquid cash is under 15% of total assets. Ensure adequate liquidity for day-to-day unexpected payments.")
        } else {
            advice.add("🎯 Healthy asset distribution across liquid, reserve, and growth categories.")
        }

        return NetWorthSummary(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = netWorth,
            liquidAssets = liquidAmount,
            growthAssets = growthAmount,
            reserveAssets = reserveAmount,
            targetCurrencyCode = cur,
            breakdowns = breakdowns,
            liquidityRatio = liquidityRatio,
            debtToAssetRatio = debtToAssetRatio,
            healthAdvice = advice
        )
    }

    class Factory(
        private val application: Application,
        private val repository: FinanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
                return FinanceViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class FinanceViewModelFactory(
    private val application: Application,
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
