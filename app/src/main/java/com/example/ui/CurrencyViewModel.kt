package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RatePreferences
import com.example.data.model.ConversionDirection
import com.example.data.model.ConversionRecord
import com.example.data.model.RateMode
import com.example.data.repository.CurrencyRepository
import com.example.notification.NotificationHelper
import com.example.notification.RateAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class CurrencyUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val officialRate: Double = 1530.0,
    val effectiveRate: Double = 1530.0,
    val rateMode: RateMode = RateMode.OFFICIAL,
    val parallelSpreadPercent: Double = 3.5,
    val customRate: Double = 1580.0,
    val lastUpdatedText: String = "",
    val isOffline: Boolean = false,
    val direction: ConversionDirection = ConversionDirection.USD_TO_NGN,
    val inputAmount: String = "100",
    val calculatedAmount: Double = 153000.0,
    val recentConversions: List<ConversionRecord> = emptyList(),
    val statusNotification: String? = null,
    // Rate Alert State
    val isAlertEnabled: Boolean = false,
    val targetAlertRate: Double = 1500.0,
    val alertCondition: String = "GREATER_OR_EQUAL" // GREATER_OR_EQUAL or LESS_OR_EQUAL
)

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CurrencyRepository(application)
    private val prefs = RatePreferences(application)

    private val _uiState = MutableStateFlow(
        CurrencyUiState(
            officialRate = repository.getCachedRate(),
            parallelSpreadPercent = repository.getParallelSpreadPercent(),
            customRate = repository.getCustomRate(),
            lastUpdatedText = repository.getLastUpdateText(),
            isAlertEnabled = prefs.isAlertEnabled,
            targetAlertRate = prefs.targetAlertRate,
            alertCondition = prefs.alertCondition,
            rateMode = try {
                RateMode.valueOf(repository.getSelectedRateMode())
            } catch (e: Exception) {
                RateMode.OFFICIAL
            }
        )
    )
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        // Collect conversion history
        viewModelScope.launch {
            repository.recentConversions.collect { history ->
                _uiState.update { it.copy(recentConversions = history) }
            }
        }

        // Recalculate initial effective rate and output
        recalculateWithCurrentMode()

        // Fetch live rates on launch
        fetchLiveRates(isManualRefresh = false)
    }

    fun fetchLiveRates(isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isManualRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true)
            }

            val result = repository.fetchLiveRate()
            if (result.isSuccess) {
                val newRate = result.getOrNull() ?: _uiState.value.officialRate
                val updateText = repository.getLastUpdateText()
                _uiState.update { current ->
                    current.copy(
                        officialRate = newRate,
                        lastUpdatedText = if (updateText.isNotBlank()) updateText else "Updated just now",
                        isLoading = false,
                        isRefreshing = false,
                        isOffline = false,
                        statusNotification = if (isManualRefresh) "Exchange rate updated: 1 USD = ₦${formatNumber(newRate)}" else null
                    )
                }

                // Check alert trigger
                checkAndTriggerAlert(newRate)
            } else {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isOffline = true,
                        statusNotification = "Using cached rate (offline mode)"
                    )
                }
            }
            recalculateWithCurrentMode()
        }
    }

    private fun checkAndTriggerAlert(currentRate: Double) {
        if (!prefs.isAlertEnabled) return
        val target = prefs.targetAlertRate
        val condition = prefs.alertCondition

        val isConditionMet = if (condition == "GREATER_OR_EQUAL") {
            currentRate >= target
        } else {
            currentRate <= target
        }

        if (isConditionMet) {
            val now = System.currentTimeMillis()
            // Throttle notification within app session to at least once per 15 minutes
            if (now - prefs.lastNotifiedTimestamp > 15 * 60 * 1000L) {
                NotificationHelper.sendRateAlertNotification(
                    context = getApplication(),
                    currentRate = currentRate,
                    targetRate = target,
                    condition = condition
                )
                prefs.lastNotifiedTimestamp = now
                prefs.lastNotifiedRate = currentRate
            }
        }
    }

    fun onInputAmountChanged(input: String) {
        val sanitized = input.filter { it.isDigit() || it == '.' }
        val parts = sanitized.split(".")
        val validated = if (parts.size > 2) {
            parts[0] + "." + parts.subList(1, parts.size).joinToString("")
        } else {
            sanitized
        }

        _uiState.update { current ->
            val parsed = validated.toDoubleOrNull() ?: 0.0
            val computed = calculateTarget(parsed, current.effectiveRate, current.direction)
            current.copy(
                inputAmount = validated,
                calculatedAmount = computed
            )
        }
    }

    fun onDirectionToggled() {
        _uiState.update { current ->
            val newDirection = if (current.direction == ConversionDirection.USD_TO_NGN) {
                ConversionDirection.NGN_TO_USD
            } else {
                ConversionDirection.USD_TO_NGN
            }

            val currentCalculated = current.calculatedAmount
            val newInput = if (currentCalculated > 0) {
                String.format(Locale.US, "%.0f", currentCalculated)
            } else {
                if (newDirection == ConversionDirection.USD_TO_NGN) "100" else "100000"
            }
            val parsed = newInput.toDoubleOrNull() ?: 0.0
            val newCalculated = calculateTarget(parsed, current.effectiveRate, newDirection)

            current.copy(
                direction = newDirection,
                inputAmount = newInput,
                calculatedAmount = newCalculated
            )
        }
    }

    fun onPresetSelected(amount: Double) {
        val amountStr = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
        _uiState.update { current ->
            val computed = calculateTarget(amount, current.effectiveRate, current.direction)
            current.copy(
                inputAmount = amountStr,
                calculatedAmount = computed
            )
        }
        saveConversionHistory()
    }

    fun onRateModeSelected(mode: RateMode) {
        repository.setSelectedRateMode(mode.name)
        _uiState.update { it.copy(rateMode = mode) }
        recalculateWithCurrentMode()
    }

    fun onUpdateParallelSpread(spread: Double) {
        repository.setParallelSpreadPercent(spread)
        _uiState.update { it.copy(parallelSpreadPercent = spread) }
        recalculateWithCurrentMode()
    }

    fun onUpdateCustomRate(rate: Double) {
        repository.setCustomRate(rate)
        _uiState.update { it.copy(customRate = rate) }
        recalculateWithCurrentMode()
    }

    fun setTargetAlertRate(rate: Double) {
        prefs.targetAlertRate = rate
        _uiState.update { it.copy(targetAlertRate = rate) }
    }

    fun setAlertCondition(condition: String) {
        prefs.alertCondition = condition
        _uiState.update { it.copy(alertCondition = condition) }
    }

    fun toggleAlert(enabled: Boolean, context: Context) {
        prefs.isAlertEnabled = enabled
        _uiState.update { it.copy(isAlertEnabled = enabled) }

        if (enabled) {
            RateAlarmScheduler.schedulePeriodicCheck(context)
            _uiState.update {
                it.copy(statusNotification = "Rate alert active: Notify when 1 USD ${if (prefs.alertCondition == "GREATER_OR_EQUAL") "≥" else "≤"} ₦${formatNumber(prefs.targetAlertRate)}")
            }
            // Check immediately
            checkAndTriggerAlert(_uiState.value.officialRate)
        } else {
            RateAlarmScheduler.cancelPeriodicCheck(context)
            _uiState.update { it.copy(statusNotification = "Rate alert deactivated") }
        }
    }

    fun sendTestAlertNotification(context: Context) {
        NotificationHelper.sendTestNotification(context, _uiState.value.effectiveRate)
        _uiState.update { it.copy(statusNotification = "Test notification sent") }
    }

    fun saveConversionHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            val inputVal = state.inputAmount.toDoubleOrNull() ?: 0.0
            if (inputVal <= 0) return@launch

            val (fromCurr, toCurr) = if (state.direction == ConversionDirection.USD_TO_NGN) {
                Pair("USD", "NGN")
            } else {
                Pair("NGN", "USD")
            }

            repository.recordConversion(
                fromCurrency = fromCurr,
                toCurrency = toCurr,
                fromAmount = inputVal,
                toAmount = state.calculatedAmount,
                rateUsed = state.effectiveRate,
                rateMode = state.rateMode.displayName
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(statusNotification = "History cleared") }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(statusNotification = null) }
    }

    private fun recalculateWithCurrentMode() {
        _uiState.update { current ->
            val effRate = when (current.rateMode) {
                RateMode.OFFICIAL -> current.officialRate
                RateMode.PARALLEL -> current.officialRate * (1.0 + (current.parallelSpreadPercent / 100.0))
                RateMode.CUSTOM -> current.customRate
            }

            val inputVal = current.inputAmount.toDoubleOrNull() ?: 0.0
            val computed = calculateTarget(inputVal, effRate, current.direction)

            current.copy(
                effectiveRate = effRate,
                calculatedAmount = computed
            )
        }
    }

    private fun calculateTarget(
        amount: Double,
        rate: Double,
        direction: ConversionDirection
    ): Double {
        if (amount <= 0.0 || rate <= 0.0) return 0.0
        return when (direction) {
            ConversionDirection.USD_TO_NGN -> amount * rate
            ConversionDirection.NGN_TO_USD -> amount / rate
        }
    }

    private fun formatNumber(value: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(value)
    }
}
