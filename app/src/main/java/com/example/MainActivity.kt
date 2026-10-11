package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.NotificationHelper
import com.example.ui.CurrencyViewModel
import com.example.ui.MainScreen
import com.example.ui.WalletViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel on launch
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                val currencyViewModel: CurrencyViewModel = viewModel()
                val walletViewModel: WalletViewModel = viewModel()
                MainScreen(
                    currencyViewModel = currencyViewModel,
                    walletViewModel = walletViewModel
                )
            }
        }
    }
}
