package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversionDirection

@Composable
fun QuickPresetsRow(
    direction: ConversionDirection,
    onPresetClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUsdToNgn = direction == ConversionDirection.USD_TO_NGN
    val presets = if (isUsdToNgn) {
        listOf(
            10.0 to "$10",
            20.0 to "$20",
            50.0 to "$50",
            100.0 to "$100",
            250.0 to "$250",
            500.0 to "$500",
            1000.0 to "$1,000",
            2500.0 to "$2,500",
            5000.0 to "$5,000"
        )
    } else {
        listOf(
            10000.0 to "₦10K",
            25000.0 to "₦25K",
            50000.0 to "₦50K",
            100000.0 to "₦100K",
            250000.0 to "₦250K",
            500000.0 to "₦500K",
            1000000.0 to "₦1M",
            2500000.0 to "₦2.5M",
            5000000.0 to "₦5M"
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (amount, label) ->
                SuggestionChip(
                    onClick = { onPresetClick(amount) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("preset_chip_${label.replace("$", "").replace("₦", "").replace(",", "")}")
                )
            }
        }
    }
}
