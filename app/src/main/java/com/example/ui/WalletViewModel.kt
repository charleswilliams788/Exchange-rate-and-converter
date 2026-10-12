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
    val userAccount: UserAccount? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val isAuthLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authSuccessMessage: String? = null,
    val statusMessage: String? = null
) {
    val isAuthenticated: Boolean
        get() = userAccount != null
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = try {
        WalletRepository(application)
    } catch (e: Exception) {
        null
    }
    private val auth: FirebaseAuth? = try {
        if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(application)
        }
        Firebase.auth
    } catch (e: Exception) {
        null
    }

    private val _uiState = MutableStateFlow(WalletUiState(currentUser = auth?.currentUser))
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var accountObservationJob: Job? = null
    private var transactionsObservationJob: Job? = null

    init {
        // Listen to Firebase Auth state
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    onFirebaseUserLoggedIn(user)
                }
            }
        } catch (e: Exception) {
            // Ignore if Firebase Auth is not active
        }

        // Check local saved session or Firebase auth on launch
        if (auth?.currentUser != null) {
            onFirebaseUserLoggedIn(auth.currentUser!!)
        } else {
            val localUser = AuthHelper.getLocalUser(application)
            if (localUser != null) {
                loadAccountForUser(localUser.uid, localUser.displayName, localUser.email)
            }
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authSuccessMessage = null) }
            val result = AuthHelper.signUpWithEmail(getApplication(), email, pass, name)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        localUser = user,
                        authSuccessMessage = "Account created successfully! Welcome to NairaRate."
                    )
                }
                loadAccountForUser(user.uid, user.displayName, user.email)
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
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        localUser = user,
                        authSuccessMessage = "Welcome back, ${user.displayName}!"
                    )
                }
                loadAccountForUser(user.uid, user.displayName, user.email)
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
            uid = "user_amelie",
            email = "achuchiemelie2507@gmail.com",
            displayName = "Chiemelie Achu"
        )
        _uiState.update {
            it.copy(
                localUser = demoUser,
                authErrorMessage = null,
                authSuccessMessage = null
            )
        }
        loadAccountForUser(demoUser.uid, demoUser.displayName, demoUser.email)
    }

    private fun loadAccountForUser(uid: String, displayName: String, email: String) {
        val defaultTag = "@" + email.substringBefore("@").filter { it.isLetterOrDigit() }.ifEmpty { "amelie" }
        val loadedAccount = UserAccount(
            userId = uid,
            displayName = displayName.ifEmpty { "Chiemelie Achu" },
            email = email.ifEmpty { "achuchiemelie2507@gmail.com" },
            accountNumber = "8144314093",
            accountTag = defaultTag,
            bankName = "NairaRate MFB",
            ngnBalance = 250000.0,
            usdBalance = 200.0
        )

        val starterTransactions = listOf(
            WalletTransaction(
                id = "tx_welcome_ngn",
                senderId = "system",
                senderName = "Central Bank / Remittance",
                senderAccount = "0000000000",
                recipientId = uid,
                recipientName = loadedAccount.displayName,
                recipientAccount = "8144314093",
                recipientBank = "NairaRate MFB",
                amount = 250000.0,
                currency = "NGN",
                exchangeRate = 1.0,
                convertedAmount = 250000.0,
                convertedCurrency = "NGN",
                type = "DEPOSIT",
                status = "COMPLETED",
                note = "Starter Naira working balance credited",
                participants = listOf(uid)
            ),
            WalletTransaction(
                id = "tx_welcome_usd",
                senderId = "system",
                senderName = "USD Wire Transfer",
                senderAccount = "WIRE_REMIT",
                recipientId = uid,
                recipientName = loadedAccount.displayName,
                recipientAccount = "8144314093",
                recipientBank = "NairaRate MFB",
                amount = 200.0,
                currency = "USD",
                exchangeRate = 1.0,
                convertedAmount = 200.0,
                convertedCurrency = "USD",
                type = "DEPOSIT",
                status = "COMPLETED",
                note = "Starter USD working balance credited",
                participants = listOf(uid)
            )
        )

        _uiState.update {
            it.copy(
                userAccount = loadedAccount,
                transactions = starterTransactions,
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

    private fun onFirebaseUserLoggedIn(user: FirebaseUser) {
        val userData = AuthUserData(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "Chiemelie Achu"
        )
        _uiState.update { it.copy(localUser = userData) }
        loadAccountForUser(userData.uid, userData.displayName, userData.email)
    }

    fun updateProfile(displayName: String, accountTag: String, bankName: String) {
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
        AuthHelper.signOut(
            context = getApplication(),
            credentialManager = credentialManager,
            onSignOutComplete = {
                _uiState.update {
                    it.copy(
                        currentUser = null,
                        localUser = null,
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
        val currentAcc = _uiState.value.userAccount
        if (currentAcc == null) {
            _uiState.update { it.copy(statusMessage = "Please sign in to send money") }
            return
        }

        if (currency == "NGN" && currentAcc.ngnBalance < amount) {
            _uiState.update { it.copy(statusMessage = "Insufficient NGN balance (Available: ₦${currentAcc.ngnBalance})") }
            return
        }
        if (currency == "USD" && currentAcc.usdBalance < amount) {
            _uiState.update { it.copy(statusMessage = "Insufficient USD balance (Available: $${currentAcc.usdBalance})") }
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
                statusMessage = "Transfer of ${if (currency == "USD") "$" else "₦"}$amount sent successfully!"
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
                statusMessage = "Converted $amount $fromCurrency to $toCurrency at live rate!"
            )
        }

        if (auth?.currentUser != null) {
            viewModelScope.launch {
                repository?.convertOwnBalance(fromCurrency, toCurrency, amount, exchangeRate)
            }
        }
    }

    fun depositTestFunds(amount: Double, currency: String) {
        val currentAcc = _uiState.value.userAccount
        if (currentAcc == null) {
            _uiState.update { it.copy(statusMessage = "Please sign in to add funds") }
            return
        }

        val newNgn = if (currency == "NGN") currentAcc.ngnBalance + amount else currentAcc.ngnBalance
        val newUsd = if (currency == "USD") currentAcc.usdBalance + amount else currentAcc.usdBalance
        val updated = currentAcc.copy(ngnBalance = newNgn, usdBalance = newUsd)
        val newTx = WalletTransaction(
            id = "tx_${System.currentTimeMillis()}",
            senderId = updated.userId,
            senderName = "Card / Remittance",
            senderAccount = "VIRTUAL_TOPUP",
            recipientId = updated.userId,
            recipientName = updated.displayName,
            recipientAccount = updated.accountNumber,
            recipientBank = updated.bankName,
            amount = amount,
            currency = currency,
            exchangeRate = 1.0,
            convertedAmount = amount,
            convertedCurrency = currency,
            type = "DEPOSIT",
            status = "COMPLETED",
            note = "Instant deposit via NairaRate Top-up",
            participants = listOf(updated.userId)
        )
        _uiState.update {
            it.copy(
                userAccount = updated,
                transactions = listOf(newTx) + it.transactions,
                statusMessage = "Added ${if (currency == "USD") "$" else "₦"}$amount to wallet"
            )
        }

        if (auth?.currentUser != null) {
            viewModelScope.launch {
                repository?.depositFunds(amount, currency)
            }
        }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, authSuccessMessage = null, authErrorMessage = null) }
    }
}
