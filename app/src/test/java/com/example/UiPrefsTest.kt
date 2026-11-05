package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppSettingsIntent
import com.example.data.AppSettingsManager
import com.example.data.UiPrefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Covers the preference plumbing that previously stored values without applying them: text size,
 * compact layout, date format, first day of week, and the "show cents" money formatting switch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UiPrefsTest {

    private lateinit var context: Context
    private lateinit var manager: AppSettingsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        AppSettingsManager.resetInstanceForTesting()
        manager = AppSettingsManager.getInstance(context)
        UiPrefs.syncFrom(manager.state.value)
    }

    @After
    fun tearDown() {
        AppSettingsManager.resetInstanceForTesting()
    }

    @Test
    fun textSize_labelsMapToExpectedScales() {
        assertEquals(1f, UiPrefs.scaleForLabel("Normal"), 0.0001f)
        assertTrue(UiPrefs.scaleForLabel("Large") > 1f)
        assertTrue(UiPrefs.scaleForLabel("Extra Large") > UiPrefs.scaleForLabel("Large"))
        assertTrue(UiPrefs.scaleForLabel("Small") < 1f)
    }

    @Test
    fun textSize_settingPersistsAndSurvivesReload() {
        UiPrefs.setTextSize(manager, "Large")
        assertEquals("Large", manager.state.value.textSize)

        AppSettingsManager.resetInstanceForTesting()
        val reloaded = AppSettingsManager.getInstance(context)
        assertEquals("Large", reloaded.state.value.textSize)
    }

    @Test
    fun dateFormat_appliesToFormatter() {
        UiPrefs.setDateFormat(manager, "yyyy-MM-dd")
        val formatted = UiPrefs.formatDate(0L)
        assertTrue("Expected ISO-style date, got $formatted", formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        UiPrefs.setDateFormat(manager, "dd/MM/yyyy")
        val dayFirst = UiPrefs.formatDate(0L)
        assertTrue("Expected day-first date, got $dayFirst", dayFirst.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun firstDayOfWeek_controlsCalendarColumnOrder() {
        UiPrefs.setFirstDayOfWeek(manager, "Sunday")
        assertEquals(Calendar.SUNDAY, UiPrefs.firstDayOfWeekConstant())

        UiPrefs.setFirstDayOfWeek(manager, "Monday")
        assertEquals(Calendar.MONDAY, UiPrefs.firstDayOfWeekConstant())

        UiPrefs.setFirstDayOfWeek(manager, "Saturday")
        assertEquals(Calendar.SATURDAY, UiPrefs.firstDayOfWeekConstant())
    }

    @Test
    fun compactLayout_persistsThroughSettingsManager() {
        assertFalse(manager.state.value.compactLayout)
        UiPrefs.setCompactLayout(manager, true)
        assertTrue(manager.state.value.compactLayout)
        assertTrue(UiPrefs.compactLayout)
    }

    @Test
    fun showCents_isReadableByMoneyFormatter() {
        UiPrefs.setShowCents(manager, false)
        assertFalse(UiPrefs.showCents)
        assertFalse(manager.state.value.showCents)

        UiPrefs.setShowCents(manager, true)
        assertTrue(UiPrefs.showCents)
    }

    @Test
    fun newAppearanceToggles_roundTripThroughPersistence() {
        manager.dispatch(AppSettingsIntent.UpdateShowTransactionTags(false))
        manager.dispatch(AppSettingsIntent.UpdateLongPressToEdit(false))
        manager.dispatch(AppSettingsIntent.UpdateQuickAdd(false))
        manager.dispatch(AppSettingsIntent.UpdateConfirmBeforeExit(true))

        AppSettingsManager.resetInstanceForTesting()
        val reloaded = AppSettingsManager.getInstance(context)
        assertFalse(reloaded.state.value.showTransactionTags)
        assertFalse(reloaded.state.value.longPressToEdit)
        assertFalse(reloaded.state.value.quickAddEnabled)
        assertTrue(reloaded.state.value.confirmBeforeExit)
    }

    @Test
    fun backupTimestamp_isPersistedAcrossRestart() {
        manager.dispatch(AppSettingsIntent.SetLastBackupTimestamp("01 Jan 2027, 09:00 AM"))
        AppSettingsManager.resetInstanceForTesting()
        val reloaded = AppSettingsManager.getInstance(context)
        assertEquals("01 Jan 2027, 09:00 AM", reloaded.state.value.lastBackupTimestamp)
    }
}
