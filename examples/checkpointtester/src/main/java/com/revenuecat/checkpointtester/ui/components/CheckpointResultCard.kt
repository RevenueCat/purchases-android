package com.revenuecat.checkpointtester.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.checkpointtester.checkpoints.CheckpointResultUi
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

/**
 * Renders the outcome of a checkpoint call: the spinner while it's in flight, then the formatted result plus the
 * raw [com.revenuecat.purchases.checkpoints.CheckpointResult] for reference.
 */
@Composable
fun CheckpointResultCard(
    result: CheckpointResultUi?,
    waitingFor: String?,
    modifier: Modifier = Modifier,
    emptyText: String = "No checkpoint has run on this screen yet.",
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when {
                waitingFor != null -> WaitingRow(waitingFor)
                result == null -> Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> ResultContent(result)
            }
        }
    }
}

@Composable
private fun ResultContent(result: CheckpointResultUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = result.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (result.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        if (result.detail.isNotEmpty()) {
            Text(text = result.detail, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = result.raw,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WaitingRow(waitingFor: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text = "Waiting for '$waitingFor'…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckpointResultCardPreview() {
    CheckpointTesterTheme {
        CheckpointResultCard(
            result = CheckpointResultUi(
                title = "Paywall presented",
                detail = "Dismissed",
                raw = "PaywallPresented(checkpoint=CheckpointInfo(identifier=hard_paywall), " +
                    "paywallOutcome=Dismissed)",
            ),
            waitingFor = null,
        )
    }
}
