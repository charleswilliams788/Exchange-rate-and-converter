package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.ui.theme.VeltroElectricBlue
import com.example.ui.theme.VeltroEmeraldGreen
import com.example.ui.theme.VeltroMidnightNavy
import com.example.ui.theme.VeltroMidnightNavyBorder
import com.example.ui.theme.VeltroMidnightNavyCard

/**
 * Simplified Midnight Navy Account Card for Veltro Send & Receive tab.
 * Virtual bank name, virtual account numbers, and static 250k NGN / 200 USD balance cards
 * have been removed as required. The authenticated user's email/username is dynamically displayed
 * near the top-right corner.
 */
@Composable
fun AccountOverviewCard(
    account: UserAccount,
    userEmail: String = "",
    cryptoBalances: Map<String, Double> = emptyMap(),
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onConvertClick: () -> Unit,
    onAddFundsClick: () -> Unit = {},
    onCopyAccount: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayEmailOrUser = when {
        userEmail.isNotBlank() -> userEmail
        account.email.isNotBlank() -> account.email
        account.displayName.isNotBlank() -> account.displayName
        else -> "demo@veltro.org"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_overview_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = VeltroMidnightNavy
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, VeltroMidnightNavyBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VeltroMidnightNavyCard,
                            VeltroMidnightNavy
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Top Row: Brand Chip on Left, Dynamic User Email on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Veltro Brand Chip with Electric Blue accent
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VeltroElectricBlue.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeltroElectricBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(VeltroEmeraldGreen, CircleShape)
                        )
                        Text(
                            text = "VELTRO DEMO WALLET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VeltroElectricBlue,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Authenticated user's email/username dynamically displayed near top-right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.testTag("user_email_display")
                    ) {
                        Text(
                            text = displayEmailOrUser,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = onEditProfileClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_dashboard_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Details",
                            tint = VeltroElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Demonstration Balances Row (starts at zero for all assets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEMO ASSETS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Credited on Completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeltroElectricBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-Asset Balances Pills Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CryptoBalPill(
                    symbol = "USDT",
                    balance = cryptoBalances["USDT"] ?: 0.0,
                    formatDecimals = 2,
                    modifier = Modifier.weight(1f)
                )
                CryptoBalPill(
                    symbol = "ETH",
                    balance = cryptoBalances["ETH"] ?: 0.0,
                    formatDecimals = 4,
                    modifier = Modifier.weight(1f)
                )
                CryptoBalPill(
                    symbol = "BNB",
                    balance = cryptoBalances["BNB"] ?: 0.0,
                    formatDecimals = 4,
                    modifier = Modifier.weight(1f)
                )
                CryptoBalPill(
                    symbol = "SOL",
                    balance = cryptoBalances["SOL"] ?: 0.0,
                    formatDecimals = 4,
                    modifier = Modifier.weight(1f)
                )
                CryptoBalPill(
                    symbol = "USDC",
                    balance = cryptoBalances["USDC"] ?: 0.0,
                    formatDecimals = 2,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons with Electric Blue accents
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onReceiveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = VeltroElectricBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_deposit_crypto")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deposit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = onSendClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_send_money")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = onConvertClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_convert_balance")
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Swap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CryptoBalPill(
    symbol: String,
    balance: Double,
    formatDecimals: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F2937),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = symbol,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = String.format(java.util.Locale.US, "%.${formatDecimals}f", balance),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
