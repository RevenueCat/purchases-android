package com.revenuecat.paywallstester.ui.screens.checkpoints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.CheckpointResultUi
import com.revenuecat.paywallstester.ui.screens.checkpoints.CheckpointsViewModel.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckpointsScreen(
    dismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckpointsViewModel = viewModel<CheckpointsViewModelImpl>(
        factory = CheckpointsViewModelImpl.Factory,
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Checkpoints") },
                navigationIcon = {
                    IconButton(onClick = dismissRequest) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HitCheckpointSection(
                enabled = state.waitingFor == null,
                onHit = viewModel::hit,
            )
            ResultCard(
                waitingFor = state.waitingFor,
                result = state.lastResult,
            )
            RecentCheckpointsSection(
                recents = state.recents,
                enabled = state.waitingFor == null,
                onRecentTap = viewModel::hit,
                modifier = Modifier.weight(1f),
            )
        }
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
            Text(text = "Hit checkpoint")
        }
    }
}

@Composable
private fun ResultCard(waitingFor: String?, result: CheckpointResultUi?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when {
                waitingFor != null -> WaitingRow(waitingFor)
                result == null -> Text(
                    text = "Hit a checkpoint to see its result here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (result.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
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
        }
    }
}

@Composable
private fun WaitingRow(waitingFor: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(text = "Waiting for '$waitingFor'…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RecentCheckpointsSection(
    recents: List<String>,
    enabled: Boolean,
    onRecentTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Recent checkpoints",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn {
            items(recents) { recentIdentifier ->
                ListItem(
                    headlineContent = { Text(text = recentIdentifier) },
                    modifier = Modifier.clickable(enabled = enabled) { onRecentTap(recentIdentifier) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
private fun CheckpointsScreenPreview() {
    CheckpointsScreen(
        dismissRequest = {},
        viewModel = object : CheckpointsViewModel {
            override val state: StateFlow<UiState>
                get() = MutableStateFlow(
                    UiState(
                        recents = listOf("test_checkpoint", "unknown_checkpoint", "error_checkpoint"),
                        lastResult = CheckpointResultUi(
                            title = "Paywall presented",
                            detail = "Dismissed",
                            isError = false,
                            raw = "PaywallPresented(checkpoint=..., paywallOutcome=Dismissed)",
                        ),
                    ),
                )

            override fun hit(identifier: String) {}
        },
    )
}
