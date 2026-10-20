package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.fragment.app.FragmentActivity
import com.example.data.AppSettingsManager

open class BaseActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
        val textSizeOption = prefs.getString("text_size_option", "Normal") ?: "Normal"
        
        val fontScale = when (textSizeOption) {
            "Small" -> 0.85f
            "Normal" -> 1.0f
            "Large" -> 1.35f
            "Extra Large" -> 1.5f
            else -> 1.0f
        }

        val config = Configuration(newBase.resources.configuration)
        config.fontScale = fontScale
        val contextWithNewConfig = newBase.createConfigurationContext(config)
        
        super.attachBaseContext(contextWithNewConfig)
    }
}
