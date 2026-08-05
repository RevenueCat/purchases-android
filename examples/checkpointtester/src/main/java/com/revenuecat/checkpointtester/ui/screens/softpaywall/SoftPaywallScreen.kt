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
import com.revenuecat.checkpointtester.checkpoints.CheckpointResultUi
import com.revenuecat.checkpointtester.ui.components.CheckpointResultCard
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

        OutcomeBanner(result = state.result)
        AlwaysAvailableContent()

        OutlinedButton(onClick = viewModel::hit, enabled = state.waitingFor == null) {
            Text(text = "Hit the checkpoint again")
        }

        CheckpointResultCard(
            result = state.result,
            waitingFor = state.waitingFor,
            emptyText = "Presenting the soft paywall…",
        )
    }
}

@Composable
private fun OutcomeBanner(result: CheckpointResultUi?) {
    val upgraded = result?.grantedAccess == true
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
        Text(
            text = if (upgraded) "Upgraded. Thanks for subscribing." else "Continuing on the free tier.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
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
