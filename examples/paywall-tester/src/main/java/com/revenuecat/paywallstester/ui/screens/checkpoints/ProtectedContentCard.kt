package com.revenuecat.paywallstester.ui.screens.checkpoints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProtectedContentCard(completed: Boolean, onReset: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Protected content", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (completed) "Checkpoint completed" else "Locked · Not completed",
                style = MaterialTheme.typography.titleLarge,
                color = if (completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (completed) {
                HorizontalDivider()
                Text(text = "Your premium library", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "You can read this content because the checkpoint callback ran. " +
                        "A soft checkpoint can grant access without a purchase.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "Run a checkpoint to open this content. " +
                        "Closing an unmet hard gate keeps it locked.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onReset) {
                Text(text = "Reset test")
            }
        }
    }
}
