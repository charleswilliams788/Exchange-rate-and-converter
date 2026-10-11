package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.NetworkClient
import com.example.data.local.AppDatabase
import com.example.data.local.RatePreferences
import com.example.data.model.ConversionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrencyRepository(context: Context) {
    private val prefs = RatePreferences(context)
    private val db = AppDatabase.getDatabase(context)
    private val conversionDao = db.conversionDao()

    val recentConversions: Flow<List<ConversionRecord>> = conversionDao.getRecentConversions()

    fun getCachedRate(): Double = prefs.cachedRate
    fun getLastUpdateTimestamp(): Long = prefs.lastUpdateTimestamp
    fun getLastUpdateText(): String = prefs.lastUpdateText
    fun getParallelSpreadPercent(): Double = prefs.parallelSpreadPercent
    fun setParallelSpreadPercent(spread: Double) { prefs.parallelSpreadPercent = spread }
    fun getCustomRate(): Double = prefs.customRate
    fun setCustomRate(rate: Double) { prefs.customRate = rate }
    fun getSelectedRateMode(): String = prefs.selectedRateMode
    fun setSelectedRateMode(mode: String) { prefs.selectedRateMode = mode }

    suspend fun fetchLiveRate(): Result<Double> = withContext(Dispatchers.IO) {
        // Try Primary API
        try {
            val response = NetworkClient.primaryApi.getLatestUsdRates()
            val ngnRate = response.rates?.get("NGN")
            if (ngnRate != null && ngnRate > 0) {
                prefs.cachedRate = ngnRate
                prefs.lastUpdateTimestamp = System.currentTimeMillis()
                val updateText = response.timeLastUpdateUtc ?: formatCurrentTime()
                prefs.lastUpdateText = updateText
                return@withContext Result.success(ngnRate)
            }
        } catch (e: Exception) {
            Log.w("CurrencyRepository", "Primary API failed, attempting fallback API", e)
        }

        // Try Fallback API
        try {
            val fallbackResponse = NetworkClient.fallbackApi.getFallbackUsdRates()
            val ngnRate = fallbackResponse.rates?.get("NGN")
            if (ngnRate != null && ngnRate > 0) {
                prefs.cachedRate = ngnRate
                prefs.lastUpdateTimestamp = System.currentTimeMillis()
                val updateText = fallbackResponse.date ?: formatCurrentTime()
                prefs.lastUpdateText = updateText
                return@withContext Result.success(ngnRate)
            }
        } catch (e: Exception) {
            Log.e("CurrencyRepository", "Fallback API also failed", e)
        }

        // If offline or both failed, return cached rate if available
        val cached = prefs.cachedRate
        if (cached > 0) {
            return@withContext Result.success(cached)
        }

        return@withContext Result.failure(Exception("Could not fetch live exchange rate"))
    }

    suspend fun recordConversion(
        fromCurrency: String,
        toCurrency: String,
        fromAmount: Double,
        toAmount: Double,
        rateUsed: Double,
        rateMode: String
    ) = withContext(Dispatchers.IO) {
        if (fromAmount <= 0.0) return@withContext
        val record = ConversionRecord(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            fromAmount = fromAmount,
            toAmount = toAmount,
            rateUsed = rateUsed,
            rateMode = rateMode
        )
        conversionDao.insertConversion(record)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversionDao.clearHistory()
    }

    private fun formatCurrentTime(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
