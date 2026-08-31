package com.revenuecat.checkpointtester.ui.screens.hardpaywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

@Composable
fun HardPaywallScreen(
    modifier: Modifier = Modifier,
    viewModel: HardPaywallViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.hitIfNeeded() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Hits the 'hard_paywall' checkpoint on entry. Access is granted only on Purchased or " +
                "Restored, so dismissing the paywall keeps the content locked.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.unlocked) {
            PremiumContent(message = state.message)
        } else {
            LockedContent(
                attempts = state.attempts,
                message = if (state.running) "Presenting the paywall…" else state.message,
                canRetry = !state.running,
                onRetry = viewModel::hit,
            )
        }
    }
}

@Composable
private fun PremiumContent(message: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Unlocked",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "The checkpoint granted access, so the gated content renders.",
                style = MaterialTheme.typography.bodyMedium,
            )
            message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun LockedContent(attempts: Int, message: String?, canRetry: Boolean, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Locked",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "This content needs an active subscription. Attempts so far: $attempts.",
                style = MaterialTheme.typography.bodyMedium,
            )
            message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = onRetry, enabled = canRetry) {
                Text(text = "Try again")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HardPaywallScreenPreview() {
    CheckpointTesterTheme {
        HardPaywallScreen()
    }
}
