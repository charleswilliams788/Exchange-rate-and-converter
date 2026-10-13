package com.example.ui

import android.app.Application
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthHelper
import com.example.auth.AuthUserData
import com.example.data.model.UserAccount
import com.example.data.model.WalletTransaction
import com.example.data.repository.WalletRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class WalletUiState(
    val currentUser: FirebaseUser? = null,
    val localUser: AuthUserData? = null,
    val pendingVerificationUser: AuthUserData? = null,
    val isEmailVerified: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val userAccount: UserAccount? = null,
    val cryptoBalances: Map<String, Double> = mapOf(
        "USDT" to 0.0,
        "ETH" to 0.0,
        "BNB" to 0.0,
        "SOL" to 0.0,
        "USDC" to 0.0
    ),
    val creditedDepositIds: Set<String> = emptySet(),
    val transactions: List<WalletTransaction> = emptyList(),
    val isAuthLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authSuccessMessage: String? = null,
    val statusMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = userAccount != null && isEmailVerified
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = try {
        WalletRepository(application)
    } catch (e: Exception) {
        null
    }

    private val auth: FirebaseAuth? = try {
        AuthHelper.getFirebaseAuth(application)
    } catch (e: Exception) {
        null
    }

    private val _uiState = MutableStateFlow(WalletUiState(currentUser = auth?.currentUser))
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var accountObservationJob: Job? = null
    private var transactionsObservationJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        // Listen to Firebase Auth state
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    if (user.isEmailVerified) {
                        _uiState.update {
                            it.copy(
                                isEmailVerified = true,
                                pendingVerificationUser = null
                            )
                        }
                        onFirebaseUserLoggedIn(user)
                    } else {
                        // Firebase user is unverified - gate protected features
                        val pendingData = AuthUserData(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                            isEmailVerified = false
                        )
                        _uiState.update {
                            it.copy(
                                pendingVerificationUser = pendingData,
                                isEmailVerified = false,
                                userAccount = null,
                                transactions = emptyList()
                            )
                        }
                        updateCooldownTimer(pendingData.email)
                    }
                }
            }
        } catch (e: Exception) {
            // Firebase Auth not initialized
        }

        // Check local persisted session if Firebase not active
        if (auth?.currentUser == null) {
            val localSession = AuthHelper.getLocalUser(application)
            if (localSession != null) {
                if (localSession.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            localUser = localSession,
                            isEmailVerified = true,
                            pendingVerificationUser = null
                        )
                    }
                    loadAccountForUser(localSession.uid, localSession.displayName, localSession.email)
                } else {
                    _uiState.update {
                        it.copy(
                            pendingVerificationUser = localSession,
                            isEmailVerified = false,
                            userAccount = null,
                            transactions = emptyList()
                        )
                    }
                    updateCooldownTimer(localSession.email)
                }
            }
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.signUpWithEmail(getApplication(), email, pass, name)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                if (user.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            localUser = user,
                            pendingVerificationUser = null,
                            isEmailVerified = true,
                            authSuccessMessage = "Account created and verified! Welcome to Veltro."
                        )
                    }
                    loadAccountForUser(user.uid, user.displayName, user.email)
                } else {
                    // Prevent unverified user from accessing protected features
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            pendingVerificationUser = user,
                            isEmailVerified = false,
                            userAccount = null,
                            authSuccessMessage = "A 6-digit verification code has been sent to ${user.email}. Enter the code below to complete registration."
                        )
                    }
                    startCooldownCountdown(user.email)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                    )
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.signInWithEmail(getApplication(), email, pass)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                if (user.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            localUser = user,
                            pendingVerificationUser = null,
                            isEmailVerified = true,
                            authSuccessMessage = "Welcome back to Veltro!"
                        )
                    }
                    loadAccountForUser(user.uid, user.displayName, user.email)
                } else {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            pendingVerificationUser = user,
                            isEmailVerified = false,
                            userAccount = null,
                            authSuccessMessage = "Please verify your email address to access your wallet."
                        )
                    }
                    updateCooldownTimer(user.email)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                    )
                }
            }
        }
    }

    fun checkVerificationStatus() {
        val pendingUser = _uiState.value.pendingVerificationUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val checkResult = AuthHelper.checkVerificationStatus(getApplication(), pendingUser.email)
            if (checkResult.isSuccess && checkResult.getOrThrow()) {
                val verifiedUser = pendingUser.copy(isEmailVerified = true)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        pendingVerificationUser = null,
                        localUser = verifiedUser,
                        isEmailVerified = true,
                        authSuccessMessage = "Email verified successfully! Welcome to your Veltro wallet."
                    )
                }
                loadAccountForUser(verifiedUser.uid, verifiedUser.displayName, verifiedUser.email)
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = "Email not verified yet. Please enter the 6-digit code or check your inbox."
                    )
                }
            }
        }
    }

    fun resendVerification() {
        val pendingUser = _uiState.value.pendingVerificationUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.resendVerificationEmail(getApplication(), pendingUser.email)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authSuccessMessage = result.getOrThrow()
                    )
                }
                startCooldownCountdown(pendingUser.email)
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.exceptionOrNull()?.message ?: "Failed to resend verification email"
                    )
                }
                updateCooldownTimer(pendingUser.email)
            }
        }
    }

    fun verifyOtp(code: String) {
        val pendingUser = _uiState.value.pendingVerificationUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.verifyOtpCode(getApplication(), pendingUser.email, code)
            if (result.isSuccess) {
                val verifiedUser = pendingUser.copy(isEmailVerified = true)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        pendingVerificationUser = null,
                        localUser = verifiedUser,
                        isEmailVerified = true,
                        authSuccessMessage = "Verification successful! Welcome to your Veltro wallet."
                    )
                }
                loadAccountForUser(verifiedUser.uid, verifiedUser.displayName, verifiedUser.email)
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = result.exceptionOrNull()?.message ?: "Invalid verification code"
                    )
                }
            }
        }
    }

    fun cancelPendingVerification() {
        _uiState.update {
            it.copy(
                pendingVerificationUser = null,
                isEmailVerified = false,
                userAccount = null,
                transactions = emptyList(),
                authErrorMessage = null,
                authSuccessMessage = null
            )
        }
        AuthHelper.clearLocalSession(getApplication())
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun startCooldownCountdown(email: String) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val remaining = AuthHelper.getCooldownSecondsRemaining(getApplication(), email)
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
                if (remaining <= 0) break
                delay(1000L)
            }
        }
    }

    private fun updateCooldownTimer(email: String) {
        val remaining = AuthHelper.getCooldownSecondsRemaining(getApplication(), email)
        _uiState.update { it.copy(resendCooldownSeconds = remaining) }
        if (remaining > 0) {
            startCooldownCountdown(email)
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.sendPasswordReset(getApplication(), email)
            _uiState.update {
                it.copy(
                    isAuthLoading = false,
                    authSuccessMessage = if (result.isSuccess) result.getOrThrow() else null,
                    authErrorMessage = if (result.isFailure) result.exceptionOrNull()?.message else null
                )
            }
        }
    }

    fun useDemoAccount() {
        val demoUser = AuthUserData(
            uid = "demo_veltro_user",
            email = "demo@veltro.org",
            displayName = "Demo User",
            isEmailVerified = true
        )
        _uiState.update {
            it.copy(
                localUser = demoUser,
                pendingVerificationUser = null,
                isEmailVerified = true,
                authErrorMessage = null,
                authSuccessMessage = null
            )
        }
        loadAccountForUser(demoUser.uid, demoUser.displayName, demoUser.email)
    }

    // Loads account with ZERO STARTING BALANCES as mandated by user
    private fun loadAccountForUser(uid: String, displayName: String, email: String) {
        val defaultTag = "@" + email.substringBefore("@").filter { it.isLetterOrDigit() }.ifEmpty { "veltro" }
        val loadedAccount = UserAccount(
            userId = uid,
            displayName = displayName.ifEmpty { "Demo User" },
            email = email.ifEmpty { "demo@veltro.org" },
            accountNumber = "0xe8c91f657af4706e570c8fbb396238db60d136da",
            accountTag = defaultTag,
            bankName = "Veltro Vault",
            ngnBalance = 0.0,
            usdBalance = 0.0
        )

        _uiState.update {
            it.copy(
                userAccount = loadedAccount,
                cryptoBalances = mapOf(
                    "USDT" to 0.0,
                    "ETH" to 0.0,
                    "BNB" to 0.0,
                    "SOL" to 0.0,
                    "USDC" to 0.0
                ),
                transactions = emptyList(),
                isAuthLoading = false
            )
        }

        // Try syncing with Firestore if available
        viewModelScope.launch {
            try {
                repository?.initializeAccountIfMissing(uid, displayName, email)
                accountObservationJob?.cancel()
                accountObservationJob = launch {
                    repository?.observeUserAccount(uid)
                        ?.catch { emit(null) }
                        ?.collect { account ->
                            if (account != null) {
                                _uiState.update { it.copy(userAccount = account) }
                            }
                        }
                }
                transactionsObservationJob?.cancel()
                transactionsObservationJob = launch {
                    repository?.observeTransactions(uid)
                        ?.catch { emit(emptyList()) }
                        ?.collect { txList ->
                            if (txList.isNotEmpty()) {
                                _uiState.update { it.copy(transactions = txList) }
                            }
                        }
                }
            } catch (e: Exception) {
                // Keep local state
            }
        }
    }

    // SIMULATED CRYPTOCURRENCY DEPOSIT WORKFLOW WITH DUPLICATE-CREDITING GUARD
    fun submitSimulatedDeposit(crypto: String, network: String, amount: Double) {
        val userAcc = _uiState.value.userAccount ?: return
        val txId = "demo_dep_${System.currentTimeMillis()}"

        val newTx = WalletTransaction(
            id = txId,
            senderId = "simulated_external_wallet",
            senderName = "Simulated $crypto Depositor",
            senderAccount = "EXTERNAL",
            recipientId = userAcc.userId,
            recipientName = userAcc.displayName,
            recipientAccount = userAcc.accountNumber,
            recipientBank = network,
            amount = amount,
            currency = crypto,
            exchangeRate = 1.0,
            convertedAmount = amount,
            convertedCurrency = crypto,
            type = "DEPOSIT",
            status = "PENDING",
            note = "Simulated $crypto deposit on $network (Demo Mode)",
            participants = listOf(userAcc.userId)
        )

        _uiState.update {
            it.copy(
                transactions = listOf(newTx) + it.transactions,
                statusMessage = "Simulated deposit of $amount $crypto initiated. Awaiting confirmations..."
            )
        }

        // Deterministic progression:
        viewModelScope.launch {
            delay(2500L)
            advanceSimulatedDeposit(txId, "CONFIRMING")

            delay(3500L)
            advanceSimulatedDeposit(txId, "COMPLETED")
        }
    }

    // ADVANCE STATUS AND CREDIT BALANCE ONLY ONCE AT COMPLETED STAGE
    fun advanceSimulatedDeposit(txId: String, newStatus: String) {
        val currentState = _uiState.value
        val txList = currentState.transactions.toMutableList()
        val index = txList.indexOfFirst { it.id == txId }
        if (index < 0) return

        val existingTx = txList[index]
        val updatedTx = existingTx.copy(status = newStatus)
        txList[index] = updatedTx

        val currentBalances = currentState.cryptoBalances.toMutableMap()
        val creditedIds = currentState.creditedDepositIds.toMutableSet()

        // STRICT CREDITING ONLY AT COMPLETED STAGE, WITH DUPLICATE-CREDITING GUARD
        if (newStatus.equals("COMPLETED", ignoreCase = true) && !creditedIds.contains(txId)) {
            val prevBal = currentBalances[updatedTx.currency] ?: 0.0
            currentBalances[updatedTx.currency] = prevBal + updatedTx.amount
            creditedIds.add(txId) // Guarded permanently
            _uiState.update {
                it.copy(
                    transactions = txList,
                    cryptoBalances = currentBalances,
                    creditedDepositIds = creditedIds,
                    statusMessage = "Deposit confirmed! +${updatedTx.amount} ${updatedTx.currency} credited to demo balance."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    transactions = txList
                )
            }
        }
    }

    private fun onFirebaseUserLoggedIn(user: FirebaseUser) {
        val userData = AuthUserData(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
            isEmailVerified = user.isEmailVerified
        )
        _uiState.update {
            it.copy(
                localUser = userData,
                isEmailVerified = user.isEmailVerified
            )
        }
        if (user.isEmailVerified) {
            loadAccountForUser(userData.uid, userData.displayName, userData.email)
        }
    }

    fun updateProfile(displayName: String, accountTag: String, bankName: String) {
        if (!_uiState.value.isEmailVerified) {
            _uiState.update { it.copy(statusMessage = "Email verification required to edit profile.") }
            return
        }
        val current = _uiState.value.userAccount ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifEmpty { current.displayName },
            accountTag = if (accountTag.startsWith("@")) accountTag.trim() else "@${accountTag.trim()}",
            bankName = bankName.trim().ifEmpty { current.bankName }
        )
        _uiState.update {
            it.copy(
                userAccount = updated,
                statusMessage = "Profile updated successfully!"
            )
        }
        viewModelScope.launch {
            try {
                repository?.updateProfile(updated.userId, updated.displayName, updated.accountTag, updated.bankName)
            } catch (e: Exception) {
                // Local state is already updated
            }
        }
    }

    fun signOut(credentialManager: CredentialManager) {
        accountObservationJob?.cancel()
        transactionsObservationJob?.cancel()
        cooldownJob?.cancel()
        AuthHelper.signOut(
            context = getApplication(),
            credentialManager = credentialManager,
            onSignOutComplete = {
                _uiState.update {
                    it.copy(
                        currentUser = null,
                        localUser = null,
                        pendingVerificationUser = null,
                        isEmailVerified = false,
                        userAccount = null,
                        transactions = emptyList(),
                        authErrorMessage = null,
                        authSuccessMessage = "You have been signed out.",
                        statusMessage = "Signed out successfully"
                    )
                }
            },
            scope = viewModelScope
        )
    }

    fun setAuthLoading(loading: Boolean) {
        _uiState.update { it.copy(isAuthLoading = loading) }
    }

    fun sendMoney(
        recipientAccount: String,
        recipientName: String,
        recipientBank: String,
        amount: Double,
        currency: String,
        exchangeRate: Double,
        note: String
    ) {
        if (!_uiState.value.isEmailVerified) {
            _uiState.update { it.copy(statusMessage = "Email verification required to transfer funds.") }
            return
        }
        val currentAcc = _uiState.value.userAccount
        if (currentAcc == null) {
            _uiState.update { it.copy(statusMessage = "Please sign in to send money") }
            return
        }

        if (currency == "NGN" && currentAcc.ngnBalance < amount) {
            _uiState.update { it.copy(statusMessage = "Insufficient NGN balance") }
            return
        }
        if (currency == "USD" && currentAcc.usdBalance < amount) {
            _uiState.update { it.copy(statusMessage = "Insufficient USD balance") }
            return
        }

        val newNgn = if (currency == "NGN") currentAcc.ngnBalance - amount else currentAcc.ngnBalance
        val newUsd = if (currency == "USD") currentAcc.usdBalance - amount else currentAcc.usdBalance
        val updated = currentAcc.copy(ngnBalance = newNgn, usdBalance = newUsd)
        val (convertedAmt, convertedCurr) = if (currency == "USD") {
            Pair(amount * exchangeRate, "NGN")
        } else {
            Pair(if (exchangeRate > 0) amount / exchangeRate else 0.0, "USD")
        }
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            senderId = updated.userId,
            senderName = updated.displayName,
            senderAccount = updated.accountNumber,
            recipientId = recipientAccount,
            recipientName = recipientName.ifEmpty { "Recipient" },
            recipientAccount = recipientAccount,
            recipientBank = recipientBank,
            amount = amount,
            currency = currency,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmt,
            convertedCurrency = convertedCurr,
            type = "SEND",
            status = "COMPLETED",
            note = note.ifEmpty { "Transfer to $recipientName" },
            participants = listOf(updated.userId, recipientAccount)
        )
        _uiState.update {
            it.copy(
                userAccount = updated,
                transactions = listOf(newTx) + it.transactions,
                statusMessage = "Transfer sent successfully!"
            )
        }

        if (auth?.currentUser != null) {
            viewModelScope.launch {
                repository?.sendMoney(
                    recipientAccount = recipientAccount,
                    recipientName = recipientName,
                    recipientBank = recipientBank,
                    amount = amount,
                    currency = currency,
                    currentExchangeRate = exchangeRate,
                    note = note
                )
            }
        }
    }

    fun convertOwnBalance(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        exchangeRate: Double
    ) {
        if (!_uiState.value.isEmailVerified) {
            _uiState.update { it.copy(statusMessage = "Email verification required to convert balances.") }
            return
        }
        val currentAcc = _uiState.value.userAccount
        if (currentAcc == null) {
            _uiState.update { it.copy(statusMessage = "Please sign in to convert balances") }
            return
        }

        val (newUsd, newNgn, convertedAmt) = if (fromCurrency == "USD" && toCurrency == "NGN") {
            if (currentAcc.usdBalance < amount) {
                _uiState.update { it.copy(statusMessage = "Insufficient USD balance") }
                return
            }
            val receivedNgn = amount * exchangeRate
            Triple(currentAcc.usdBalance - amount, currentAcc.ngnBalance + receivedNgn, receivedNgn)
        } else if (fromCurrency == "NGN" && toCurrency == "USD") {
            if (currentAcc.ngnBalance < amount) {
                _uiState.update { it.copy(statusMessage = "Insufficient NGN balance") }
                return
            }
            val receivedUsd = if (exchangeRate > 0) amount / exchangeRate else 0.0
            Triple(currentAcc.usdBalance + receivedUsd, currentAcc.ngnBalance - amount, receivedUsd)
        } else {
            _uiState.update { it.copy(statusMessage = "Invalid currency pair") }
            return
        }

        val updated = currentAcc.copy(usdBalance = newUsd, ngnBalance = newNgn)
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            senderId = updated.userId,
            senderName = updated.displayName,
            senderAccount = updated.accountNumber,
            recipientId = updated.userId,
            recipientName = updated.displayName,
            recipientAccount = updated.accountNumber,
            recipientBank = updated.bankName,
            amount = amount,
            currency = fromCurrency,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmt,
            convertedCurrency = toCurrency,
            type = "CONVERT",
            status = "COMPLETED",
            note = "Converted $amount $fromCurrency to ${String.format(Locale.US, "%.2f", convertedAmt)} $toCurrency",
            participants = listOf(updated.userId)
        )
        _uiState.update {
            it.copy(
                userAccount = updated,
                transactions = listOf(newTx) + it.transactions,
                statusMessage = "Converted $amount $fromCurrency to $toCurrency!"
            )
        }

        if (auth?.currentUser != null) {
            viewModelScope.launch {
                repository?.convertOwnBalance(fromCurrency, toCurrency, amount, exchangeRate)
            }
        }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, authSuccessMessage = null, authErrorMessage = null) }
    }
}
