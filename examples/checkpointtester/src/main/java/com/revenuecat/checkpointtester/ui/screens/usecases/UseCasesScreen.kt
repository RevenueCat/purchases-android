package com.revenuecat.checkpointtester.ui.screens.usecases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.checkpointtester.ui.Screen
import com.revenuecat.checkpointtester.ui.theme.CheckpointTesterTheme

private data class NavigatedUseCase(
    val screen: Screen,
    val description: String,
)

private data class InlineUseCase(
    val identifier: String,
    val title: String,
    val description: String,
)

private val NAVIGATED_USE_CASES = listOf(
    NavigatedUseCase(
        screen = Screen.HardPaywall,
        description = "Content stays locked unless the checkpoint grants access. Dismissing keeps you out.",
    ),
    NavigatedUseCase(
        screen = Screen.SoftPaywall,
        description = "Content is always available. The outcome only changes a banner.",
    ),
    NavigatedUseCase(
        screen = Screen.Onboarding,
        description = "A multi-step flow that hits a checkpoint mid-way and always completes.",
    ),
    NavigatedUseCase(
        screen = Screen.EntitlementGate,
        description = "Checks CustomerInfo first and only hits the checkpoint when nothing is active.",
    ),
    NavigatedUseCase(
        screen = Screen.CustomCheckpoint,
        description = "Runs any identifier you type and shows the raw result, without gating anything on it.",
    ),
)

private val INLINE_USE_CASES = listOf(
    InlineUseCase(
        identifier = "offering_checkpoint",
        title = "App-owned offering",
        description = "A terminal offering workflow returns offering data without presenting RevenueCat UI.",
    ),
    InlineUseCase(
        identifier = "unknown_checkpoint",
        title = "No action",
        description = "An identifier the dashboard doesn't know about. " +
            "Resolves without presenting anything. Expect NoAction with reason UNKNOWN_CHECKPOINT.",
    ),
    InlineUseCase(
        identifier = "error_checkpoint",
        title = "Simulated error",
        description = "The checkpoint call throws a ConfigurationError.",
    ),
)

@Composable
fun UseCasesScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UseCasesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SectionHeader(text = "App-driven use cases")
        }
        items(NAVIGATED_USE_CASES) { useCase ->
            ListItem(
                headlineContent = { Text(text = useCase.screen.title) },
                supportingContent = { Text(text = useCase.description) },
                modifier = Modifier.clickable { onNavigate(useCase.screen) },
            )
            HorizontalDivider()
        }
        item {
            SectionHeader(text = "Checkpoint outcomes")
        }
        items(INLINE_USE_CASES) { useCase ->
            ListItem(
                headlineContent = { Text(text = useCase.title) },
                supportingContent = { Text(text = useCase.description) },
                modifier = Modifier.clickable(enabled = !state.running) {
                    viewModel.hit(useCase.identifier)
                },
            )
            HorizontalDivider()
        }
        item {
            Text(
                text = when {
                    state.running -> "Running the checkpoint…"
                    else -> state.message ?: "Tap one of the outcomes above to run it here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Suppress("EmptyFunctionBlock")
@Preview(showBackground = true)
@Composable
private fun UseCasesScreenPreview() {
    CheckpointTesterTheme {
        UseCasesScreen(onNavigate = {})
    }
}
