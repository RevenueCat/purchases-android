package com.revenuecat.rcttester.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.revenuecat.rcttester.config.PurchasesCompletedByType
import com.revenuecat.rcttester.config.SDKConfiguration

@Composable
fun ConfigurationSummarySection(
    configuration: SDKConfiguration,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ConfigurationRow(
            label = "API Key",
            value = redactAPIKey(configuration.apiKey),
            monospace = true,
        )
        ConfigurationRow(
            label = "Purchases Completed By",
            value = configuration.purchasesAreCompletedBy.displayName,
        )
        if (configuration.purchasesAreCompletedBy == PurchasesCompletedByType.MY_APP) {
            ConfigurationRow(
                label = "Purchase Logic",
                value = configuration.purchaseLogic.displayName,
            )
        }
    }
}

@Composable
private fun ConfigurationRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

private const val DEFAULT_PREFIX_LENGTH = 4
private const val DEFAULT_SUFFIX_LENGTH = 4

private fun redactAPIKey(apiKey: String): String {
    if (apiKey.isEmpty()) {
        return "—"
    }

    val underscoreIndex = apiKey.indexOf('_')
    val prefix = if (underscoreIndex != -1) {
        apiKey.substring(0, underscoreIndex + 1)
    } else {
        apiKey.take(DEFAULT_PREFIX_LENGTH)
    }

    val suffix = apiKey.takeLast(DEFAULT_SUFFIX_LENGTH)

    // Avoid showing duplicates if key is too short
    val minKeyLength = prefix.length + DEFAULT_SUFFIX_LENGTH
    return if (apiKey.length <= minKeyLength) {
        apiKey
    } else {
        "$prefix•••••$suffix"
    }
}
