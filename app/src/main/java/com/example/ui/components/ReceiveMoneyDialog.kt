package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockchainType
import com.example.data.model.EvmWalletConfig
import com.example.data.model.UserAccount
import com.example.ui.theme.VeltroElectricBlue
import com.example.ui.theme.VeltroEmeraldGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMoneyDialog(
    account: UserAccount,
    onCopyText: (String) -> Unit,
    onSubmitDeposit: (crypto: String, network: String, amount: Double) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    var selectedAssetSymbol by remember { mutableStateOf("USDT") }
    val currentAsset = EvmWalletConfig.getAssetBySymbol(selectedAssetSymbol)

    var selectedNetworkId by remember { mutableStateOf(currentAsset.defaultNetworkId) }
    val currentNetwork = currentAsset.networks.find { it.id == selectedNetworkId }
        ?: currentAsset.networks.first()

    var isNetworkMenuExpanded by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var depositAmountText by remember { mutableStateOf("") }
    var depositError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = VeltroElectricBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = null,
                                tint = VeltroElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Crypto Deposit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "DEMO MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VeltroElectricBlue
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Demo Mode Notice
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DEMO MODE: Deposit amounts, fees, and processing times are simulated. No actual blockchain transactions are performed.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                // Network Warning
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Only send the selected cryptocurrency through the selected network. Sending assets through an unsupported network may result in loss of funds.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                // Asset Selector Chips
                Text(
                    text = "Select Cryptocurrency",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EvmWalletConfig.ALL_ASSETS.forEach { asset ->
                        FilterChip(
                            selected = selectedAssetSymbol == asset.symbol,
                            onClick = {
                                selectedAssetSymbol = asset.symbol
                                selectedNetworkId = asset.defaultNetworkId
                                isCopied = false
                                depositError = null
                            },
                            label = { Text(asset.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VeltroElectricBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_crypto_${asset.symbol.lowercase()}")
                        )
                    }
                }

                // Network Dropdown
                Text(
                    text = "Select Network",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                ExposedDropdownMenuBox(
                    expanded = isNetworkMenuExpanded,
                    onExpandedChange = { isNetworkMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentNetwork.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isNetworkMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .testTag("dropdown_deposit_network"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isNetworkMenuExpanded,
                        onDismissRequest = { isNetworkMenuExpanded = false }
                    ) {
                        currentAsset.networks.forEach { net ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = net.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (net.isEnabled) {
                                            Text(
                                                text = "Fee: ${net.estimatedFee} • Time: ${net.estimatedTime}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                },
                                enabled = net.isEnabled,
                                onClick = {
                                    selectedNetworkId = net.id
                                    isNetworkMenuExpanded = false
                                    isCopied = false
                                    depositError = null
                                }
                            )
                        }
                    }
                }

                // Receiving Address Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentNetwork.blockchainType == BlockchainType.SOLANA) "SOLANA RECEIVING ADDRESS" else "EVM RECEIVING ADDRESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )

                            IconButton(
                                onClick = {
                                    onCopyText(currentNetwork.receivingAddress)
                                    isCopied = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Address",
                                    tint = if (isCopied) VeltroEmeraldGreen else VeltroElectricBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = currentNetwork.receivingAddress,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = "Demonstration configuration address only.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Minimum & Fee Specs
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Minimum Deposit", fontSize = 10.sp, color = Color.Gray)
                            Text(currentNetwork.minDepositDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Est. Network Fee", fontSize = 10.sp, color = Color.Gray)
                            Text(currentNetwork.estimatedFee, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Demonstration Deposit Amount Input
                OutlinedTextField(
                    value = depositAmountText,
                    onValueChange = {
                        depositAmountText = it
                        depositError = null
                    },
                    label = { Text("Demonstration Deposit Amount") },
                    placeholder = { Text("e.g. 50") },
                    trailingIcon = { Text(selectedAssetSymbol, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                    isError = depositError != null,
                    supportingText = depositError?.let { { Text(it, color = Color.Red) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_demo_deposit_amount")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = depositAmountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        depositError = "Please enter a valid amount greater than zero"
                        return@Button
                    }
                    if (amt < currentNetwork.minAmountNumeric) {
                        depositError = "Amount must be at least ${currentNetwork.minDepositDisplay}"
                        return@Button
                    }
                    onSubmitDeposit(selectedAssetSymbol, currentNetwork.shortName, amt)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VeltroElectricBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_submit_demo_deposit")
            ) {
                Text("Submit Demo Deposit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
