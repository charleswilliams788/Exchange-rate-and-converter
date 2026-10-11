package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedCurrency by remember { mutableStateOf("NGN") } // "NGN" or "USD"
    val banks = listOf(
        "NairaRate Account / P2P",
        "GTBank",
        "Access Bank",
        "Zenith Bank",
        "Kuda Bank",
        "OPay",
        "PalmPay",
        "First Bank of Nigeria",
        "United Bank for Africa (UBA)"
    )

    var selectedBank by remember { mutableStateOf(banks[0]) }
    var isBankDropdownExpanded by remember { mutableStateOf(false) }

    var recipientAccount by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableBalance = if (selectedCurrency == "NGN") account.ngnBalance else account.usdBalance
    val symbol = if (selectedCurrency == "NGN") "₦" else "$"

    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val convertedEquivalent = if (selectedCurrency == "USD") {
        enteredAmount * currentExchangeRate
    } else {
        if (currentExchangeRate > 0) enteredAmount / currentExchangeRate else 0.0
    }

    val formattedConverted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(convertedEquivalent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallMade,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Send Money",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Currency Choice
                Text(
                    text = "Select Currency to Send",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "NGN",
                        onClick = { selectedCurrency = "NGN"; errorMessage = null },
                        label = { Text("🇳🇬 Nigerian Naira (₦)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = { selectedCurrency = "USD"; errorMessage = null },
                        label = { Text("🇺🇸 US Dollars ($)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Bank Dropdown
                Text(
                    text = "Destination Bank / Channel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = isBankDropdownExpanded,
                    onExpandedChange = { isBankDropdownExpanded = !isBankDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBank,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBankDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isBankDropdownExpanded,
                        onDismissRequest = { isBankDropdownExpanded = false }
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    selectedBank = bank
                                    isBankDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Account / Tag
                Text(
                    text = if (selectedBank.contains("NairaRate")) "Recipient Account Number or Tag (@tag)" else "Recipient 10-Digit Account Number",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = recipientAccount,
                    onValueChange = { recipientAccount = it.trim() },
                    placeholder = { Text(if (selectedBank.contains("NairaRate")) "e.g. 9041285372 or @amelie" else "e.g. 0123456789") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_send_recipient_account")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Name
                Text(
                    text = "Recipient Name",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    placeholder = { Text("e.g. Chiemelie Achu") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Amount to Send",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Available: $symbol${NumberFormat.getNumberInstance().format(availableBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() || c == '.' }
                        errorMessage = null
                    },
                    prefix = { Text(symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_send_amount")
                )

                // Real-time Conversion preview
                if (enteredAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedCurrency == "USD") {
                                "≈ ₦$formattedConverted at real-time rate (1 USD = ₦${String.format(Locale.US, "%.2f", currentExchangeRate)})"
                            } else {
                                "≈ $$formattedConverted USD equivalent at live rate"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Note
                Text(
                    text = "Transfer Note (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = transferNote,
                    onValueChange = { transferNote = it },
                    placeholder = { Text("e.g. Payment for design work") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipientAccount.isBlank()) {
                        errorMessage = "Please enter recipient account number or tag"
                        return@Button
                    }
                    if (enteredAmount <= 0) {
                        errorMessage = "Please enter a valid amount"
                        return@Button
                    }
                    if (enteredAmount > availableBalance) {
                        errorMessage = "Insufficient balance ($symbol${NumberFormat.getNumberInstance().format(availableBalance)} available)"
                        return@Button
                    }

                    onSend(
                        recipientAccount,
                        recipientName.ifBlank { "Recipient ($recipientAccount)" },
                        selectedBank,
                        enteredAmount,
                        selectedCurrency,
                        transferNote
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_confirm_send")
            ) {
                Text("Send $symbol$amountText")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
