package com.revenuecat.checkpointssample.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Checkpoints Arcade", style = MaterialTheme.typography.headlineSmall)
        GameStatusCard(state)
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            Button(onClick = { viewModel.play(onPlay) }) {
                Text(if (state.gameUnlocked) "Play game" else "Unlock and play")
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = viewModel::refresh) {
            Text("Refresh status")
        }
    }
}

@Composable
private fun GameStatusCard(state: HomeViewModel.UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Tapper game", style = MaterialTheme.typography.titleMedium)
            val status = when {
                state.loading -> "Checking access..."
                state.gameUnlocked -> "Unlocked via ${state.activeEntitlements.joinToString()}"
                else -> "Locked"
            }
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (state.gamesPlayed > 0) {
                Text("Games played: ${state.gamesPlayed}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
