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

        // If user already logged in on cold start
        auth.currentUser?.let { onUserLoggedIn(it) }
    }

    private fun onUserLoggedIn(user: FirebaseUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true) }

            // Ensure user account exists in Firestore
            val result = repository.initializeAccountIfMissing(
                userId = user.uid,
                displayName = user.displayName ?: "NairaRate User",
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
        _uiState.update {
            it.copy(
                userAccount = null,
                transactions = emptyList()
            )
        }
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

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
