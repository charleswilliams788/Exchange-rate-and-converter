package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvmWalletConfig
import com.example.data.model.UserAccount
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyDialog(
    account: UserAccount,
    currentExchangeRate: Double,
    onSend: (
        recipientAccount: String,
        recipientName: String,
        recipientBank: String,
        amount: Double,
        currency: String,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAssetSymbol by remember { mutableStateOf("USDT") }
    val currentAsset = EvmWalletConfig.getAssetBySymbol(selectedAssetSymbol)

    var selectedNetworkName by remember { mutableStateOf(currentAsset.defaultNetwork) }
    val currentNetwork = EvmWalletConfig.getNetworkByName(selectedNetworkName)
    var isNetworkMenuExpanded by remember { mutableStateOf(false) }

    var recipientWalletAddress by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Account balances
    val availableUsd = account.usdBalance
    val availableNgn = account.ngnBalance

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val equivalentNgn = enteredAmount * currentExchangeRate
    val formattedNgn = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(equivalentNgn)

    fun validateAndSubmit() {
        val trimmedAddress = recipientWalletAddress.trim()
        if (trimmedAddress.isBlank()) {
            errorMessage = "Please enter the recipient EVM wallet address"
            return
        }
        if (!trimmedAddress.startsWith("0x") || trimmedAddress.length != 42) {
            errorMessage = "Invalid EVM wallet address. Must begin with 0x and be 42 characters."
            return
        }
        if (enteredAmount <= 0.0) {
            errorMessage = "Please enter a valid transfer amount"
            return
        }
        if (enteredAmount < currentAsset.minTransferAmount) {
            errorMessage = "Minimum transfer for ${currentAsset.symbol} is ${currentAsset.minTransferAmount} ${currentAsset.symbol}"
            return
        }
        if (enteredAmount > availableUsd) {
            errorMessage = "Insufficient USD wallet balance (Available: $${String.format(Locale.US, "%.2f", availableUsd)})"
            return
        }

        errorMessage = null
        onSend(
            trimmedAddress,
            recipientName.trim().ifEmpty { "John Doe" },
            "${currentAsset.symbol} via ${currentNetwork.name}",
            enteredAmount,
            "USD",
            transferNote.trim().ifEmpty { "Transfer ${currentAsset.symbol} on ${currentNetwork.shortName}" }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallMade,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Send Crypto (EVM)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Transfer to any EVM-compatible wallet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Select Crypto Asset to Transfer
                Text(
                    text = "Select Asset to Send",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EvmWalletConfig.ASSETS.take(3).forEach { asset ->
                        FilterChip(
                            selected = selectedAssetSymbol == asset.symbol,
                            onClick = {
                                selectedAssetSymbol = asset.symbol
                                if (!asset.supportedNetworks.contains(selectedNetworkName)) {
                                    selectedNetworkName = asset.defaultNetwork
                                }
                                errorMessage = null
                            },
                            label = { Text("${asset.iconEmoji} ${asset.symbol}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EvmWalletConfig.ASSETS.drop(3).forEach { asset ->
                        FilterChip(
                            selected = selectedAssetSymbol == asset.symbol,
                            onClick = {
                                selectedAssetSymbol = asset.symbol
                                if (!asset.supportedNetworks.contains(selectedNetworkName)) {
                                    selectedNetworkName = asset.defaultNetwork
                                }
                                errorMessage = null
                            },
                            label = { Text("${asset.iconEmoji} ${asset.symbol}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Select EVM Destination Network
                Text(
                    text = "Transfer Network",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = isNetworkMenuExpanded,
                    onExpandedChange = { isNetworkMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedNetworkName,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNetworkMenuExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isNetworkMenuExpanded,
                        onDismissRequest = { isNetworkMenuExpanded = false }
                    ) {
                        currentAsset.supportedNetworks.forEach { networkName ->
                            val net = EvmWalletConfig.getNetworkByName(networkName)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = net.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Gas: ${net.gasFeeEstimate} • ${net.speed}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedNetworkName = networkName
                                    isNetworkMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Recipient EVM Wallet Address
                Text(
                    text = "Recipient EVM Wallet Address",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = recipientWalletAddress,
                    onValueChange = {
                        recipientWalletAddress = it
                        errorMessage = null
                    },
                    placeholder = { Text("0x...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null)
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                recipientWalletAddress = EvmWalletConfig.DEFAULT_EVM_WALLET_ADDRESS
                            }
                        ) {
                            Text("Use Default", fontSize = 11.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_recipient_evm_address")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Recipient Name / Label (Updated placeholder to John Doe)
                Text(
                    text = "Recipient Name / Label",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    placeholder = { Text("e.g. John Doe") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_recipient_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Amount Input & Balance Prompts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount (${currentAsset.symbol})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Available: $${String.format(Locale.US, "%.2f", availableUsd)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    placeholder = { Text("0.00") },
                    trailingIcon = {
                        TextButton(
                            onClick = { amountText = String.format(Locale.US, "%.2f", availableUsd) }
                        ) {
                            Text("MAX", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_send_amount")
                )

                if (enteredAmount > 0.0) {
                    Text(
                        text = "≈ ₦$formattedNgn NGN (at live market rate)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prompts Card: Gas Fee & Minimum Transfer
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GasMeter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Estimated Gas Fee",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = currentNetwork.gasFeeEstimate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Minimum Transfer",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${currentAsset.minTransferAmount} ${currentAsset.symbol}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transfer Note
                OutlinedTextField(
                    value = transferNote,
                    onValueChange = { transferNote = it },
                    label = { Text("Transfer Note / Memo (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Banner
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSubmit() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_confirm_send_money")
            ) {
                Text("Confirm Transfer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
