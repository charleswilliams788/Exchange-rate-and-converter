package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object RateAlarmScheduler {
    private const val ALARM_REQUEST_CODE = 2001
    private const val INTERVAL_MILLIS = 30 * 60 * 1000L // 30 minutes

    fun schedulePeriodicCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RateCheckReceiver::class.java).apply {
            action = RateCheckReceiver.ACTION_CHECK_RATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MILLIS,
            INTERVAL_MILLIS,
            pendingIntent
        )
    }

    fun cancelPeriodicCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RateCheckReceiver::class.java).apply {
            action = RateCheckReceiver.ACTION_CHECK_RATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
