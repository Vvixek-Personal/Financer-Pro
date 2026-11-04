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
