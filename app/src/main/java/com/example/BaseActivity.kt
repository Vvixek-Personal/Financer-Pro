package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import com.example.data.AppSettingsManager
import com.example.data.UiPrefs

/**
 * Base activity that applies the user's chosen text-size setting to the Activity's
 * configuration. Text size is stored as a *label* ("Small" / "Normal" / "Large" /
 * "Extra Large") by the Appearance screen and multiplied against the device font scale,
 * so the preference composes with Android's own accessibility font scaling instead of
 * overriding it.
 *
 * Note: extending ComponentActivity (not FragmentActivity) keeps this in line with the
 * Compose-only UI, and [recreate] is used by AppearanceScreen after a change so the new
 * configuration is picked up immediately without restarting the whole process.
 */
open class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
        val textSizeOption = prefs.getString("text_size_option", "Normal") ?: "Normal"

        val config = Configuration(newBase.resources.configuration)
        config.fontScale = config.fontScale * UiPrefs.scaleForLabel(textSizeOption)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
