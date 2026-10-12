package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthHelper
import com.example.data.local.RatePreferences
import com.example.ui.WalletViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `verify wallet viewmodel creation and default unauthenticated state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AuthHelper.clearLocalSession(app)
        val vm = WalletViewModel(app)
        assertNotNull(vm)
        val state = vm.uiState.value
        // Initial state should not have access to verified account
        assertFalse(state.isAuthenticated)
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

    @Test
    fun `verify otp code validation lifecycle and single use prevention`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testEmail = "testuser_${System.currentTimeMillis()}@example.com"

        // Register user locally to generate OTP
        val signupResult = AuthHelper.signUpWithEmail(context, testEmail, "Password123!", "Test User")
        assertTrue(signupResult.isSuccess)
        val user = signupResult.getOrThrow()
        assertFalse("Newly registered user must not be marked verified", user.isEmailVerified)

        // Invalid code test
        val failResult = AuthHelper.verifyOtpCode(context, testEmail, "000000")
        assertTrue("Incorrect OTP must fail", failResult.isFailure)

        // Invalid format test
        val shortResult = AuthHelper.verifyOtpCode(context, testEmail, "123")
        assertTrue("Short code must fail", shortResult.isFailure)

        // Fetch generated code directly from prefs to test successful verification
        val prefs = context.getSharedPreferences("naira_auth_prefs", Context.MODE_PRIVATE)
        val storedOtp = prefs.getString("otp_$testEmail", null)
        assertNotNull("Generated OTP should be stored", storedOtp)

        // Verify with correct code
        val successResult = AuthHelper.verifyOtpCode(context, testEmail, storedOtp!!)
        assertTrue("Correct OTP must succeed", successResult.isSuccess)

        // Crucial test: Code must NOT be reusable
        val reuseResult = AuthHelper.verifyOtpCode(context, testEmail, storedOtp)
        assertTrue("Reused OTP must fail", reuseResult.isFailure)
    }

    @Test
    fun `verify resend verification cooldown enforcement`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testEmail = "cooldown_${System.currentTimeMillis()}@example.com"

        // First check passes
        val firstCheck = AuthHelper.checkResendCooldown(context, testEmail)
        assertTrue("First cooldown check should pass", firstCheck.isSuccess)

        // Simulate sending a resend by setting last_resend timestamp to now
        val prefs = context.getSharedPreferences("naira_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_resend_$testEmail", System.currentTimeMillis()).apply()

        // Immediate subsequent resend check must fail with cooldown error
        val secondCheck = AuthHelper.checkResendCooldown(context, testEmail)
        assertTrue("Subsequent immediate request must be blocked by cooldown", secondCheck.isFailure)
        assertTrue(secondCheck.exceptionOrNull()?.message?.contains("wait") == true)
    }
}
