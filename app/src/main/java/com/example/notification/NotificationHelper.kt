package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "rate_alerts_channel"
    private const val CHANNEL_NAME = "Currency Rate Alerts"
    private const val CHANNEL_DESC = "Notifications when USD to NGN exchange rate reaches target"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendRateAlertNotification(
        context: Context,
        currentRate: Double,
        targetRate: Double,
        condition: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannel(context)

        val formattedCurrent = formatCurrency(currentRate)
        val formattedTarget = formatCurrency(targetRate)

        val title = "🎯 Rate Alert: 1 USD = ₦$formattedCurrent"
        val conditionText = if (condition == "GREATER_OR_EQUAL") {
            "surpassed your target of ₦$formattedTarget"
        } else {
            "dropped below your target of ₦$formattedTarget"
        }
        val content = "The real-time USD/NGN rate has $conditionText. Tap to view and convert."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_naira_dollar)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handled when permission is revoked
        }
    }

    fun sendTestNotification(context: Context, currentRate: Double) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannel(context)
        val formatted = formatCurrency(currentRate)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_naira_dollar)
            .setContentTitle("🔔 Rate Alert Test")
            .setContentText("Target rate alert is configured! Live rate: 1 USD = ₦$formatted.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Target rate alert is working! Live exchange rate is 1 USD = ₦$formatted. You will receive notifications when your target is met.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
        } catch (e: SecurityException) {
            // Handled when permission is revoked
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
    }
}
