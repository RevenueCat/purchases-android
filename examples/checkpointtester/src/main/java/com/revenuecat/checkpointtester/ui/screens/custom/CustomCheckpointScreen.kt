package com.revenuecat.checkpointtester.ui.screens.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.checkpointtester.ui.screens.custom.CustomCheckpointViewModel.UiState
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

@Composable
fun CustomCheckpointScreen(
    modifier: Modifier = Modifier,
    viewModel: CustomCheckpointViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Runs any checkpoint identifier against the dashboard configuration. Nothing here is gated " +
                "on the outcome, so this is the place to try a checkpoint you just configured.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HitCheckpointSection(enabled = state.runningFor == null, onHit = viewModel::hit)
        ResultCard(state)
    }
}

@Composable
private fun HitCheckpointSection(enabled: Boolean, onHit: (String) -> Unit) {
    var identifier by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text(text = "Checkpoint identifier") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onHit(identifier) },
            enabled = enabled && identifier.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Run checkpoint")
        }
    }
}

@Composable
private fun ResultCard(state: UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when {
                state.runningFor != null -> WaitingRow(state.runningFor)
                state.title == null -> Text(
                    text = "Run a checkpoint to see its result here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    state.detail?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                    state.raw?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingRow(identifier: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text = "Waiting for '$identifier'…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomCheckpointScreenPreview() {
    CheckpointTesterTheme {
        CustomCheckpointScreen()
    }
}
