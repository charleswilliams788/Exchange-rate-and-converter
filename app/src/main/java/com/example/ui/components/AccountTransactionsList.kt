package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WalletTransaction
import com.example.ui.theme.RateNegativeRed
import com.example.ui.theme.RatePositiveGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountTransactionsList(
    transactions: List<WalletTransaction>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_transactions_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Account Activity & Transfers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            transactions.take(8).forEachIndexed { index, tx ->
                val isOutgoing = tx.type == "SEND" && tx.senderId == currentUserId
                val isDeposit = tx.type == "DEPOSIT"
                val isConvert = tx.type == "CONVERT"

                val icon = when {
                    isConvert -> Icons.Default.CurrencyExchange
                    isDeposit -> Icons.Default.Add
                    isOutgoing -> Icons.Default.CallMade
                    else -> Icons.Default.CallReceived
                }

                val iconTint = when {
                    isConvert -> MaterialTheme.colorScheme.primary
                    isDeposit -> RatePositiveGreen
                    isOutgoing -> RateNegativeRed
                    else -> RatePositiveGreen
                }

                val containerColor = iconTint.copy(alpha = 0.12f)
                val sign = if (isOutgoing) "-" else "+"
                val symbol = if (tx.currency == "USD") "$" else "₦"
                val formattedAmt = symbol + NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }.format(tx.amount)

                val dateString = if (tx.createdAt != null) {
                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(tx.createdAt.seconds * 1000))
                } else {
                    "Just now"
                }

                val title = when (tx.type) {
                    "SEND" -> if (isOutgoing) "Sent to ${tx.recipientName}" else "Received from ${tx.senderName}"
                    "DEPOSIT" -> "Wallet Deposit"
                    "CONVERT" -> "Currency Conversion"
                    else -> tx.type
                }

                val subtitle = when (tx.type) {
                    "SEND" -> "${tx.recipientBank} • $dateString"
                    "CONVERT" -> "Converted at ₦${String.format(Locale.US, "%.2f", tx.exchangeRate)}/USD • $dateString"
                    else -> dateString
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = containerColor,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isConvert) formattedAmt else "$sign$formattedAmt",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOutgoing) MaterialTheme.colorScheme.onSurface else RatePositiveGreen
                        )
                        if (tx.convertedAmount > 0 && tx.type == "SEND") {
                            val convSymbol = if (tx.convertedCurrency == "USD") "$" else "₦"
                            Text(
                                text = "≈ $convSymbol${NumberFormat.getNumberInstance().format(tx.convertedAmount.toLong())}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (index < transactions.take(8).lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
