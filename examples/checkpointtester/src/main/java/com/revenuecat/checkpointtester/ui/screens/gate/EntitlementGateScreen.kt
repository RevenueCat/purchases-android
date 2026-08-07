package com.revenuecat.checkpointtester.ui.screens.gate

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
fun EntitlementGateScreen(
    modifier: Modifier = Modifier,
    viewModel: EntitlementGateViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshIfNeeded() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Reads CustomerInfo first and only hits 'entitlement_gate' when no entitlement is active. " +
                "A purchase or restore carries its own CustomerInfo on the outcome, so the state below " +
                "flips without a second fetch.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        EntitlementsCard(
            loading = state.loading,
            activeEntitlements = state.activeEntitlements,
            error = state.customerInfoError,
            checkpointSkipped = state.checkpointSkipped,
        )

        Button(
            onClick = viewModel::refresh,
            enabled = !state.loading && !state.running,
        ) {
            Text(text = "Refresh")
        }

        val status = if (state.running) "Running the checkpoint…" else state.message
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntitlementsCard(
    loading: Boolean,
    activeEntitlements: List<String>,
    error: String?,
    checkpointSkipped: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Active entitlements", style = MaterialTheme.typography.titleMedium)
            when {
                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                loading -> Text(text = "Loading CustomerInfo…", style = MaterialTheme.typography.bodyMedium)
                activeEntitlements.isEmpty() -> Text(
                    text = "None",
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> Text(
                    text = activeEntitlements.joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (checkpointSkipped) {
                Text(
                    text = "Already subscribed, so the checkpoint was skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntitlementGateScreenPreview() {
    CheckpointTesterTheme {
        EntitlementGateScreen()
    }
}
