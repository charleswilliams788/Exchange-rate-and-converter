package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.RatePreferences
import com.example.data.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RateCheckReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CHECK_RATE = "com.example.notification.ACTION_CHECK_RATE"
        private const val MIN_NOTIFICATION_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes cooldown
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = RatePreferences(appContext)
                if (!prefs.isAlertEnabled) return@launch

                val repository = CurrencyRepository(appContext)
                val result = repository.fetchLiveRate()
                val liveRate = result.getOrNull() ?: prefs.cachedRate
                val targetRate = prefs.targetAlertRate
                val condition = prefs.alertCondition

                val isMet = if (condition == "GREATER_OR_EQUAL") {
                    liveRate >= targetRate
                } else {
                    liveRate <= targetRate
                }

                val now = System.currentTimeMillis()
                val timeSinceLastAlert = now - prefs.lastNotifiedTimestamp

                if (isMet && timeSinceLastAlert > MIN_NOTIFICATION_INTERVAL_MS) {
                    NotificationHelper.sendRateAlertNotification(
                        context = appContext,
                        currentRate = liveRate,
                        targetRate = targetRate,
                        condition = condition
                    )
                    prefs.lastNotifiedTimestamp = now
                    prefs.lastNotifiedRate = liveRate
                }
            } catch (e: Exception) {
                // Log and ignore background exceptions
            } finally {
                pendingResult.finish()
            }
        }
    }
}
