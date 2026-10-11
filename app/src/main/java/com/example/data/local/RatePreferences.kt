package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class RatePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("naira_rate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CACHED_RATE = "cached_usd_to_ngn_rate"
        private const val KEY_LAST_UPDATE_TIME = "last_update_timestamp"
        private const val KEY_LAST_UPDATE_TEXT = "last_update_text"
        private const val KEY_PARALLEL_SPREAD_PERCENT = "parallel_spread_percent"
        private const val KEY_CUSTOM_RATE = "custom_rate_value"
        private const val KEY_SELECTED_RATE_MODE = "selected_rate_mode"

        // Rate Alert Keys
        private const val KEY_ALERT_ENABLED = "key_alert_enabled"
        private const val KEY_TARGET_ALERT_RATE = "key_target_alert_rate"
        private const val KEY_ALERT_CONDITION = "key_alert_condition"
        private const val KEY_LAST_NOTIFIED_RATE = "key_last_notified_rate"
        private const val KEY_LAST_NOTIFIED_TIME = "key_last_notified_time"

        // Typical reasonable base rate for USD to NGN fallback
        const val DEFAULT_BASE_RATE = 1530.0
        const val DEFAULT_PARALLEL_SPREAD = 3.5 // 3.5% typical spread
        const val DEFAULT_TARGET_RATE = 1500.0
    }

    var cachedRate: Double
        get() = prefs.getFloat(KEY_CACHED_RATE, DEFAULT_BASE_RATE.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CACHED_RATE, value.toFloat()).apply()

    var lastUpdateTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_TIME, value).apply()

    var lastUpdateText: String
        get() = prefs.getString(KEY_LAST_UPDATE_TEXT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_UPDATE_TEXT, value).apply()

    var parallelSpreadPercent: Double
        get() = prefs.getFloat(KEY_PARALLEL_SPREAD_PERCENT, DEFAULT_PARALLEL_SPREAD.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_PARALLEL_SPREAD_PERCENT, value.toFloat()).apply()

    var customRate: Double
        get() = prefs.getFloat(KEY_CUSTOM_RATE, (DEFAULT_BASE_RATE * 1.035).toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CUSTOM_RATE, value.toFloat()).apply()

    var selectedRateMode: String
        get() = prefs.getString(KEY_SELECTED_RATE_MODE, "OFFICIAL") ?: "OFFICIAL"
        set(value) = prefs.edit().putString(KEY_SELECTED_RATE_MODE, value).apply()

    // Alert settings
    var isAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALERT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_ENABLED, value).apply()

    var targetAlertRate: Double
        get() = prefs.getFloat(KEY_TARGET_ALERT_RATE, DEFAULT_TARGET_RATE.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_TARGET_ALERT_RATE, value.toFloat()).apply()

    var alertCondition: String
        get() = prefs.getString(KEY_ALERT_CONDITION, "GREATER_OR_EQUAL") ?: "GREATER_OR_EQUAL"
        set(value) = prefs.edit().putString(KEY_ALERT_CONDITION, value).apply()

    var lastNotifiedRate: Double
        get() = prefs.getFloat(KEY_LAST_NOTIFIED_RATE, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_NOTIFIED_RATE, value.toFloat()).apply()

    var lastNotifiedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFIED_TIME, value).apply()
}
