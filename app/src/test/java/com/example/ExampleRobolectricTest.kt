package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.RatePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NairaRate", appName)
    }

    @Test
    fun `verify rate preferences default values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = RatePreferences(context)
        assertTrue(prefs.cachedRate > 0)
        assertEquals(1500.0, prefs.targetAlertRate, 0.001)
    }

    @Test
    fun `verify wallet viewmodel creation`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.WalletViewModel(app)
        org.junit.Assert.assertNotNull(vm)
    }

    @Test
    fun `verify target rate alert update`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = RatePreferences(context)
        prefs.targetAlertRate = 1600.0
        prefs.isAlertEnabled = true
        prefs.alertCondition = "GREATER_OR_EQUAL"

        assertEquals(1600.0, prefs.targetAlertRate, 0.001)
        assertTrue(prefs.isAlertEnabled)
        assertEquals("GREATER_OR_EQUAL", prefs.alertCondition)
    }
}
