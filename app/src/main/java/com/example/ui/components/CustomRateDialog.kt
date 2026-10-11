package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CustomRateDialog(
    officialRate: Double,
    currentSpread: Double,
    currentCustomRate: Double,
    onSaveSpread: (Double) -> Unit,
    onSaveCustomRate: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var spreadValue by remember { mutableFloatStateOf(currentSpread.toFloat()) }
    var customRateInput by remember {
        mutableStateOf(
            if (currentCustomRate % 1.0 == 0.0) {
                currentCustomRate.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", currentCustomRate)
            }
        )
    }

    val calculatedParallelRate = officialRate * (1.0 + (spreadValue / 100.0))
    val formattedParallel = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(calculatedParallelRate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Market Rate Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 1: Parallel / BDC Spread
                Text(
                    text = "Parallel / BDC Market Spread",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust the percentage markup over the official rate (typical: 2% – 6%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "+${String.format(Locale.US, "%.1f", spreadValue)}% Spread",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "≈ ₦$formattedParallel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Slider(
                    value = spreadValue,
                    onValueChange = { spreadValue = it },
                    valueRange = 0f..15f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("slider_spread")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Custom Fixed Rate
                Text(
                    text = "Custom Fixed Exchange Rate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Specify an exact rate agreed upon with an exchange or P2P buyer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customRateInput,
                    onValueChange = { customRateInput = it.filter { char -> char.isDigit() || char == '.' } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = {
                        Text(
                            text = "₦",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    },
                    suffix = {
                        Text(
                            text = "/ USD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_rate")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSpread(spreadValue.toDouble())
                    val parsedRate = customRateInput.toDoubleOrNull()
                    if (parsedRate != null && parsedRate > 0) {
                        onSaveCustomRate(parsedRate)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_save_settings")
            ) {
                Text("Apply Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
