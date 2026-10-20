package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.ui.theme.MyApplicationTheme

class MainActivity : BaseActivity() {
    // Android application entry point for Financer
    val viewModel: FinanceViewModel by viewModels {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()
        val repository = FinanceRepository(dao, database)
        FinanceViewModelFactory(application, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Real Android FLAG_SECURE & recents preview protection
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.screenshotProtection,
                    viewModel.appPin,
                    viewModel.biometricEnabled
                ) { isProtected, pin, biometric ->
                    isProtected || !pin.isNullOrBlank() || biometric
                }.collect { shouldProtect ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setRecentsScreenshotEnabled(!shouldProtect)
                    }
                    if (shouldProtect) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            MyApplicationTheme {
                FinanceAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppBackgrounded()
        if (!viewModel.appPin.value.isNullOrBlank() || viewModel.biometricEnabled.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
