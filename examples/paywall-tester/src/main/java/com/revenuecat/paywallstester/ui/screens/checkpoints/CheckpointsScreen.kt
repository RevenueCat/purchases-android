@file:OptIn(InviteOnlyCheckpointsAPI::class)

package com.revenuecat.paywallstester.ui.screens.checkpoints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowPresentationMode
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
    val paywallRequest by viewModel.paywallRequest.collectAsStateWithLifecycle()
    val errorRequest by viewModel.errorRequest.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        CheckpointsScaffold(
            state = state,
            onHit = viewModel::hit,
            onTogglePresentWithAppPaywall = viewModel::setPresentWithAppPaywall,
            onTogglePresentErrorsWithApp = viewModel::setPresentErrorsWithApp,
            onSelectPresentationMode = viewModel::setPresentationMode,
            dismissRequest = dismissRequest,
        )
        paywallRequest?.let { AppPaywall(request = it) }
        errorRequest?.let { AppErrorDialog(request = it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
private fun CheckpointsScaffold(
    state: UiState,
    onHit: (String) -> Unit,
    onTogglePresentWithAppPaywall: (Boolean) -> Unit,
    onTogglePresentErrorsWithApp: (Boolean) -> Unit,
    onSelectPresentationMode: (FlowPresentationMode) -> Unit,
    dismissRequest: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Checkpoints") },
                navigationIcon = {
                    IconButton(onClick = dismissRequest) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OptionsMenu(
                        state = state,
                        onTogglePresentWithAppPaywall = onTogglePresentWithAppPaywall,
                        onTogglePresentErrorsWithApp = onTogglePresentErrorsWithApp,
                        onSelectPresentationMode = onSelectPresentationMode,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HitCheckpointSection(state = state, onHit = onHit)
            ResultCard(
                waitingFor = state.waitingFor,
                result = state.lastResult,
            )
            RecentCheckpointsSection(
                recents = state.recents,
                onRecentTap = onHit,
            )
        }
    }
}

@Composable
private fun OptionsMenu(
    state: UiState,
    onTogglePresentWithAppPaywall: (Boolean) -> Unit,
    onTogglePresentErrorsWithApp: (Boolean) -> Unit,
    onSelectPresentationMode: (FlowPresentationMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CheckableMenuItem(
                text = "Present offerings with the app's own paywall",
                checked = state.presentWithAppPaywall,
                onToggle = onTogglePresentWithAppPaywall,
            )
            CheckableMenuItem(
                text = "Present errors with the app's own dialog",
                checked = state.presentErrorsWithApp,
                onToggle = onTogglePresentErrorsWithApp,
            )
            HorizontalDivider()
            PRESENTATION_MODE_LABELS.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    leadingIcon = { RadioButton(selected = mode == state.presentationMode, onClick = null) },
                    onClick = { onSelectPresentationMode(mode) },
                )
            }
        }
    }
}

@Composable
private fun CheckableMenuItem(text: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text) },
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = { onToggle(!checked) },
    )
}

@Composable
private fun HitCheckpointSection(state: UiState, onHit: (String) -> Unit) {
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
            enabled = identifier.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Hit checkpoint")
        }
        Text(
            text = state.optionsSummary(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val PRESENTATION_MODE_LABELS = mapOf(
    FlowPresentationMode.DEFAULT to "Default presentation (SDK picks, currently a sheet)",
    FlowPresentationMode.MODAL_FULL_SCREEN to "Full screen presentation",
    FlowPresentationMode.MODAL_SHEET to "Sheet presentation",
)

private val PRESENTATION_MODE_SHORT_LABELS = mapOf(
    FlowPresentationMode.DEFAULT to "Default",
    FlowPresentationMode.MODAL_FULL_SCREEN to "Full screen",
    FlowPresentationMode.MODAL_SHEET to "Sheet",
)

private fun UiState.optionsSummary(): String = listOf(
    if (presentWithAppPaywall) "App paywall" else "SDK paywall",
    if (presentErrorsWithApp) "App errors" else "SDK errors",
    PRESENTATION_MODE_SHORT_LABELS.getValue(presentationMode),
).joinToString(separator = " · ")

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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(text = "Waiting for '$waitingFor'…", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "Backing out of a paywall never reports a result. Just hit the checkpoint again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentCheckpointsSection(
    recents: List<String>,
    onRecentTap: (String) -> Unit,
) {
    Column {
        Text(
            text = "Recent checkpoints",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (recents.isEmpty()) {
            Text(
                text = "No recent checkpoints yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        recents.forEach { recentIdentifier ->
            ListItem(
                headlineContent = { Text(text = recentIdentifier) },
                modifier = Modifier.clickable { onRecentTap(recentIdentifier) },
            )
            HorizontalDivider()
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

            override val paywallRequest: StateFlow<AppPaywallPresenter.Request?>
                get() = MutableStateFlow(null)

            override val errorRequest: StateFlow<AppErrorPresenter.Request?>
                get() = MutableStateFlow(null)

            override fun hit(identifier: String) {}
            override fun setPresentWithAppPaywall(enabled: Boolean) {}
            override fun setPresentErrorsWithApp(enabled: Boolean) {}
            override fun setPresentationMode(mode: FlowPresentationMode) {}
        },
    )
}
