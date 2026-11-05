package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import com.example.ui.FinanceAppScreen
import com.example.ui.FinanceViewModel
import com.example.ui.FinanceViewModelFactory
import com.example.data.AppSettingsManager
import com.example.ui.theme.MyApplicationTheme

class MainActivity : BaseActivity() {
    // Android application entry point for Financer
    val viewModel: FinanceViewModel by viewModels {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()
        val repository = FinanceRepository(dao, database)
        FinanceViewModelFactory(application, repository)
    }

    /**
     * Bill and reminder notifications are dead on Android 13+ unless POST_NOTIFICATIONS is granted
     * at runtime. The manifest declared the permission but nothing ever asked, so every scheduled
     * reminder was silently dropped. We ask once, on first launch, and remember the answer.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            getSharedPreferences(AppSettingsManager.PREFS_NAME, MODE_PRIVATE)
                .edit().putBoolean("notification_permission_prompted", true).apply()
            if (!granted) {
                // Not fatal: the rest of the app works. Reminders simply cannot be delivered.
                android.util.Log.i("MainActivity", "Notification permission declined by user")
            }
        }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences(AppSettingsManager.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean("notification_permission_prompted", false)) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge with bar styles that match the resolved app theme, so status/navigation bar
        // icons stay readable in both light and dark mode (previously they were always dark).
        val darkBars = viewModel.isAppDarkTheme()
        enableEdgeToEdge(
            statusBarStyle = if (darkBars) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (darkBars) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        maybeRequestNotificationPermission()

        // Purely controlled by the screenshot toggle setting:
        // ON -> blocks screenshots & screen recordings (FLAG_SECURE)
        // OFF -> allows screenshots and recents previews
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenshotProtection.collect { isProtected ->
                    applyScreenshotProtection(isProtected)
                }
            }
        }

        setContent {
            MyApplicationTheme {
                // Re-apply the system bar icon colours whenever the in-app theme resolves, since
                // the user can switch Light/Dark/Device from the sidebar at any time.
                val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val mode = com.example.ui.theme.themeModeState
                val resolvedDark = when (mode) {
                    "dark" -> true
                    "light" -> false
                    else -> systemDark
                }
                androidx.compose.runtime.LaunchedEffect(resolvedDark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (resolvedDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = if (resolvedDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    )
                }
                FinanceAppScreen(viewModel = viewModel)
            }
        }
    }

    private fun applyScreenshotProtection(isProtected: Boolean) {
        if (isProtected) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!isProtected)
        }
    }

    /**
     * Recreates the activity when the user is following the device theme ("Device" mode) and the
     * system switches between light and dark while the app is still open. Without this the app kept
     * the palette it was launched with until it was killed — a "device" mode that only worked on
     * a cold start.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val mode = getSharedPreferences(AppSettingsManager.PREFS_NAME, MODE_PRIVATE)
            .getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        if (mode.equals("device", true) || mode.equals("system", true)) {
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
        applyScreenshotProtection(viewModel.screenshotProtection.value)
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotProtection(viewModel.screenshotProtection.value)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
        if (viewModel.screenshotProtection.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
