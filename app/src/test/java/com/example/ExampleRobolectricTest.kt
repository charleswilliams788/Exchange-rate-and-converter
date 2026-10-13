package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthHelper
import com.example.data.local.RatePreferences
import com.example.data.model.EvmWalletConfig
import com.example.ui.WalletViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context confirms Veltro branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Veltro", appName)
    }

    @Test
    fun `verify rate preferences default values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = RatePreferences(context)
        assertTrue(prefs.cachedRate > 0)
        assertEquals(1500.0, prefs.targetAlertRate, 0.001)
    }

    @Test
    fun `verify wallet viewmodel initial state has zero demonstration balance and is unauthenticated`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AuthHelper.clearLocalSession(app)
        val vm = WalletViewModel(app)
        assertNotNull(vm)
        val state = vm.uiState.value
        assertFalse(state.isAuthenticated)
        // Check zero demonstration starting balances
        assertEquals(0.0, state.cryptoBalances["USDT"] ?: 0.0, 0.001)
        assertEquals(0.0, state.cryptoBalances["ETH"] ?: 0.0, 0.001)
        assertEquals(0.0, state.cryptoBalances["SOL"] ?: 0.0, 0.001)
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

        val signupResult = AuthHelper.signUpWithEmail(context, testEmail, "Password123!", "John Doe")
        assertTrue(signupResult.isSuccess)
        val user = signupResult.getOrThrow()
        assertFalse("Newly registered user must not be marked verified", user.isEmailVerified)

        val failResult = AuthHelper.verifyOtpCode(context, testEmail, "000000")
        assertTrue("Incorrect OTP must fail", failResult.isFailure)

        val shortResult = AuthHelper.verifyOtpCode(context, testEmail, "123")
        assertTrue("Short code must fail", shortResult.isFailure)

        val prefs = context.getSharedPreferences("naira_auth_prefs", Context.MODE_PRIVATE)
        val storedOtp = prefs.getString("otp_$testEmail", null)
        assertNotNull("Generated OTP should be stored", storedOtp)

        val successResult = AuthHelper.verifyOtpCode(context, testEmail, storedOtp!!)
        assertTrue("Correct OTP must succeed", successResult.isSuccess)

        val reuseResult = AuthHelper.verifyOtpCode(context, testEmail, storedOtp)
        assertTrue("Reused OTP must fail", reuseResult.isFailure)
    }

    @Test
    fun `verify resend verification cooldown enforcement`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testEmail = "cooldown_${System.currentTimeMillis()}@example.com"

        val firstCheck = AuthHelper.checkResendCooldown(context, testEmail)
        assertTrue("First cooldown check should pass", firstCheck.isSuccess)

        val prefs = context.getSharedPreferences("naira_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_resend_$testEmail", System.currentTimeMillis()).apply()

        val secondCheck = AuthHelper.checkResendCooldown(context, testEmail)
        assertTrue("Subsequent immediate request must be blocked by cooldown", secondCheck.isFailure)
        assertTrue(secondCheck.exceptionOrNull()?.message?.contains("wait") == true)
    }

    @Test
    fun `verify demo deposit workflow with single crediting and duplicate prevention`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AuthHelper.clearLocalSession(app)
        val vm = WalletViewModel(app)
        vm.useDemoAccount()

        // Balances must start strictly at 0
        assertEquals(0.0, vm.uiState.value.cryptoBalances["USDT"] ?: 0.0, 0.0001)

        // Submit simulated deposit
        vm.submitSimulatedDeposit(crypto = "USDT", network = "BNB Smart Chain", amount = 100.0)
        val pendingTx = vm.uiState.value.transactions.firstOrNull()
        assertNotNull(pendingTx)
        assertEquals("PENDING", pendingTx?.status)

        // Balance should NOT be credited yet while PENDING
        assertEquals(0.0, vm.uiState.value.cryptoBalances["USDT"] ?: 0.0, 0.0001)

        // Advance to CONFIRMING - still not credited
        vm.advanceSimulatedDeposit(pendingTx!!.id, "CONFIRMING")
        assertEquals(0.0, vm.uiState.value.cryptoBalances["USDT"] ?: 0.0, 0.0001)

        // Advance to COMPLETED - MUST be credited exactly once
        vm.advanceSimulatedDeposit(pendingTx.id, "COMPLETED")
        assertEquals(100.0, vm.uiState.value.cryptoBalances["USDT"] ?: 0.0, 0.0001)

        // Crucial test: Duplicate crediting protection. Calling advance again on same tx MUST NOT add funds again!
        vm.advanceSimulatedDeposit(pendingTx.id, "COMPLETED")
        assertEquals("Balance must not be double credited", 100.0, vm.uiState.value.cryptoBalances["USDT"] ?: 0.0, 0.0001)
    }

    @Test
    fun `verify EVM and Solana demo wallet addresses are distinct and properly configured`() {
        assertEquals("0xe8c91f657af4706e570c8fbb396238db60d136da", EvmWalletConfig.DEMO_EVM_WALLET_ADDRESS)
        assertEquals("24RXeCxvZmfoo9gGMvhVM9dpf5m4yyXf9R8T3WdQmmvi", EvmWalletConfig.DEMO_SOLANA_WALLET_ADDRESS)

        val solAsset = EvmWalletConfig.getAssetBySymbol("SOL")
        val solNetwork = solAsset.networks.first()
        assertEquals(EvmWalletConfig.DEMO_SOLANA_WALLET_ADDRESS, solNetwork.receivingAddress)

        val ethAsset = EvmWalletConfig.getAssetBySymbol("ETH")
        val ethNetwork = ethAsset.networks.first()
        assertEquals(EvmWalletConfig.DEMO_EVM_WALLET_ADDRESS, ethNetwork.receivingAddress)
    }
}
