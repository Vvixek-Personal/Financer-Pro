package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import com.example.ui.findFragmentActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricAuthenticationTest {

    @Test
    fun baseActivity_extendsFragmentActivity() {
        val activityClass = BaseActivity::class.java
        assertTrue(
            "BaseActivity must extend FragmentActivity for androidx.biometric support",
            FragmentActivity::class.java.isAssignableFrom(activityClass)
        )
    }

    @Test
    fun findFragmentActivity_resolvesActivityThroughContextWrappers() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.get()
        assertNotNull("Activity must be created", activity)

        // Single wrapper
        val wrapper1 = ContextWrapper(activity)
        val resolved1 = wrapper1.findFragmentActivity()
        assertNotNull("Should resolve FragmentActivity from single ContextWrapper", resolved1)
        assertTrue("Resolved activity should be FragmentActivity", resolved1 is FragmentActivity)

        // Nested wrapper
        val wrapper2 = ContextWrapper(wrapper1)
        val resolved2 = wrapper2.findFragmentActivity()
        assertNotNull("Should resolve FragmentActivity from nested ContextWrapper", resolved2)
        assertTrue("Resolved activity should be FragmentActivity", resolved2 is FragmentActivity)
    }
}
