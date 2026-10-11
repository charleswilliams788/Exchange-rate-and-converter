package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val userAccount: UserAccount? = null,
    val transactions: List<WalletTransaction> = emptyList(),
    val isAuthLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val statusMessage: String? = null
)

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WalletRepository(application)
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(WalletUiState(currentUser = auth.currentUser))
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var accountObservationJob: Job? = null
    private var transactionsObservationJob: Job? = null

    init {
        // Listen to Auth State changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _uiState.update { it.copy(currentUser = user) }
            if (user != null) {
                onUserLoggedIn(user)
            } else {
                onUserLoggedOut()
            }
        }

        // Initialize on cold start
        if (auth.currentUser != null) {
            onUserLoggedIn(auth.currentUser!!)
        } else {
            useDemoAccount()
        }
    }

    fun useDemoAccount() {
        val demoAccount = UserAccount(
            userId = "user_amelie",
            displayName = "Chiemelie Achu",
            email = "achuchiemelie2507@gmail.com",
            accountNumber = "8144314093",
            accountTag = "@amelie",
            bankName = "NairaRate MFB",
            ngnBalance = 250000.0,
            usdBalance = 200.0
        )
        val initialTransactions = listOf(
            WalletTransaction(
                id = "tx_welcome_ngn",
                senderId = "system",
                senderName = "Central Bank / Remittance",
                senderAccount = "0000000000",
                recipientId = "user_amelie",
                recipientName = "Chiemelie Achu",
                recipientAccount = "8144314093",
                recipientBank = "NairaRate MFB",
                amount = 250000.0,
                currency = "NGN",
                exchangeRate = 1.0,
                convertedAmount = 250000.0,
                convertedCurrency = "NGN",
                type = "DEPOSIT",
                status = "COMPLETED",
                note = "Welcome starter working balance credited",
                participants = listOf("user_amelie")
            ),
            WalletTransaction(
                id = "tx_welcome_usd",
                senderId = "system",
                senderName = "Wire Transfer Remittance",
                senderAccount = "WIRE_REMIT",
                recipientId = "user_amelie",
                recipientName = "Chiemelie Achu",
                recipientAccount = "8144314093",
                recipientBank = "NairaRate MFB",
                amount = 200.0,
                currency = "USD",
                exchangeRate = 1.0,
                convertedAmount = 200.0,
                convertedCurrency = "USD",
                type = "DEPOSIT",
                status = "COMPLETED",
                note = "USD working balance credited",
                participants = listOf("user_amelie")
            )
        )
        _uiState.update {
            it.copy(
                userAccount = demoAccount,
                transactions = initialTransactions,
                isAuthLoading = false
            )
        }
    }

    private fun onUserLoggedIn(user: FirebaseUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true) }

            // Ensure user account exists in Firestore
            val result = repository.initializeAccountIfMissing(
                userId = user.uid,
                displayName = user.displayName ?: "Chiemelie Achu",
                email = user.email ?: ""
            )

            _uiState.update { it.copy(isAuthLoading = false) }

            // Observe account changes in real time
            accountObservationJob?.cancel()
            accountObservationJob = launch {
                repository.observeUserAccount(user.uid)
                    .catch { emit(null) }
                    .collect { account ->
                        _uiState.update { it.copy(userAccount = account) }
                    }
            }

            // Observe transactions in real time
            transactionsObservationJob?.cancel()
            transactionsObservationJob = launch {
                repository.observeTransactions(user.uid)
                    .catch { emit(emptyList()) }
                    .collect { txList ->
                        _uiState.update { it.copy(transactions = txList) }
                    }
            }
        }
    }

    private fun onUserLoggedOut() {
        accountObservationJob?.cancel()
        transactionsObservationJob?.cancel()
        useDemoAccount()
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
        if (auth.currentUser == null && currentAcc != null) {
            // Local state operation
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
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.sendMoney(
                recipientAccount = recipientAccount,
                recipientName = recipientName,
                recipientBank = recipientBank,
                amount = amount,
                currency = currency,
                currentExchangeRate = exchangeRate,
                note = note
            )
            _uiState.update {
                it.copy(
                    isActionLoading = false,
                    statusMessage = if (result.isSuccess) {
                        "Transfer of ${if (currency == "USD") "$" else "₦"}$amount sent successfully!"
                    } else {
                        result.exceptionOrNull()?.message ?: "Transfer failed"
                    }
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
        if (auth.currentUser == null && currentAcc != null) {
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
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.convertOwnBalance(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                amount = amount,
                exchangeRate = exchangeRate
            )
            _uiState.update {
                it.copy(
                    isActionLoading = false,
                    statusMessage = if (result.isSuccess) {
                        "Converted $amount $fromCurrency to $toCurrency at live rate!"
                    } else {
                        result.exceptionOrNull()?.message ?: "Conversion failed"
                    }
                )
            }
        }
    }

    fun depositTestFunds(amount: Double, currency: String) {
        val currentAcc = _uiState.value.userAccount
        if (auth.currentUser == null && currentAcc != null) {
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
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.depositFunds(amount, currency)
            _uiState.update {
                it.copy(
                    isActionLoading = false,
                    statusMessage = if (result.isSuccess) {
                        "Added ${if (currency == "USD") "$" else "₦"}$amount to wallet"
                    } else {
                        "Failed to add funds"
                    }
                )
            }
        }
    }

    fun clearAccount() {
        _uiState.update {
            it.copy(
                userAccount = null,
                transactions = emptyList()
            )
        }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
