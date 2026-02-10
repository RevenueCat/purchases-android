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
    }
}

@Composable
private fun ConfigurationRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    modifier: Modifier = Modifier,
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

private fun redactAPIKey(apiKey: String): String {
    if (apiKey.isEmpty()) return "—"

    val prefix: String
    val underscoreIndex = apiKey.indexOf('_')
    prefix = if (underscoreIndex != -1) {
        apiKey.substring(0, underscoreIndex + 1)
    } else {
        apiKey.take(4)
    }

    val suffix = apiKey.takeLast(4)

    // Avoid showing duplicates if key is too short
    if (apiKey.length <= prefix.length + 4) {
        return apiKey
    }

    return "$prefix•••••$suffix"
}
