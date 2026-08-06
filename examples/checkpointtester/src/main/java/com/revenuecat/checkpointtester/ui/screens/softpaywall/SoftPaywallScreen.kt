package com.revenuecat.checkpointtester.ui.screens.softpaywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun SoftPaywallScreen(
    modifier: Modifier = Modifier,
    viewModel: SoftPaywallViewModel = viewModel(),
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
            text = "Hits the 'soft_paywall' checkpoint on entry. The content below is rendered no matter how " +
                "the paywall is closed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutcomeBanner(
            upgraded = state.upgraded,
            message = if (state.running) "Presenting the paywall…" else state.message,
        )
        AlwaysAvailableContent()

        OutlinedButton(onClick = viewModel::hit, enabled = !state.running) {
            Text(text = "Hit the checkpoint again")
        }
    }
}

@Composable
private fun OutcomeBanner(upgraded: Boolean, message: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (upgraded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (upgraded) "Upgraded. Thanks for subscribing." else "Continuing on the free tier.",
                style = MaterialTheme.typography.titleMedium,
            )
            message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun AlwaysAvailableContent() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Feature screen", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "A soft paywall never gates this. It's visible before and after the checkpoint runs.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SoftPaywallScreenPreview() {
    CheckpointTesterTheme {
        SoftPaywallScreen()
    }
}
