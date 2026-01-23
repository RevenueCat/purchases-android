package com.revenuecat.purchasetester.ui.screens.proxysettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchasetester.proxysettings.ProxyMode
import com.revenuecat.purchasetester.proxysettings.ProxySettingsState
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme

private object ProxySettingsDefaults {
    val VERTICAL_SPACING: Dp = 16.dp
    val HORIZONTAL_SPACING: Dp = 16.dp
    val TOP_PADDING: Dp = 8.dp
    val BOTTOM_PADDING: Dp = 16.dp
    val RADIO_BUTTON_SPACING: Dp = 7.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySettingsSheet(
    onDismiss: () -> Unit,
    viewModel: ProxySettingsViewModel = viewModel(),
) {
    val sheetState = rememberModalBottomSheetState()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentState()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ProxySettingsContent(
            state = state,
            onModeSelected = { mode ->
                viewModel.changeMode(mode)
            }
        )
    }
}

@Composable
private fun ProxySettingsContent(
    state: ProxySettingsState,
    onModeSelected: (ProxyMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ProxySettingsDefaults.HORIZONTAL_SPACING)
            .padding(
                top = ProxySettingsDefaults.TOP_PADDING,
                bottom = ProxySettingsDefaults.BOTTOM_PADDING
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.proxy_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ProxySettingsDefaults.VERTICAL_SPACING))

        when (state) {
            is ProxySettingsState.Loading -> LoadingState()
            is ProxySettingsState.Error -> ErrorState(message = state.message)
            is ProxySettingsState.CurrentMode -> RadioButtonGroup(
                currentMode = state.mode,
                onModeSelected = onModeSelected
            )
        }

        Spacer(modifier = Modifier.height(ProxySettingsDefaults.BOTTOM_PADDING))
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProxySettingsDefaults.VERTICAL_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ProxySettingsDefaults.VERTICAL_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RadioButtonGroup(
    currentMode: ProxyMode,
    onModeSelected: (ProxyMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        ProxyMode.entries.forEach { mode ->
            ProxyModeRadioButton(
                mode = mode,
                selected = mode == currentMode,
                onClick = { onModeSelected(mode) }
            )
            Spacer(modifier = Modifier.height(ProxySettingsDefaults.VERTICAL_SPACING))
        }
    }
}

@Composable
private fun ProxyModeRadioButton(
    mode: ProxyMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(ProxySettingsDefaults.RADIO_BUTTON_SPACING))
        Text(
            text = stringResource(mode.stringResourceId()),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun ProxyMode.stringResourceId(): Int {
    return when (this) {
        ProxyMode.OFF -> R.string.proxy_disabled
        ProxyMode.ENTITLEMENT_OVERRIDE -> R.string.proxy_entitlement_override
        ProxyMode.SERVER_DOWN -> R.string.proxy_server_down
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Proxy Settings - Loading")
@Composable
private fun ProxySettingsPreview_Loading() {
    PurchaseTesterTheme {
        ProxySettingsContent(
            state = ProxySettingsState.Loading,
            onModeSelected = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Proxy Settings - Error")
@Composable
private fun ProxySettingsPreview_Error() {
    PurchaseTesterTheme {
        ProxySettingsContent(
            state = ProxySettingsState.Error("There is no Proxy URL configured"),
            onModeSelected = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Proxy Settings - Current Mode")
@Composable
private fun ProxySettingsPreview_CurrentMode() {
    PurchaseTesterTheme {
        ProxySettingsContent(
            state = ProxySettingsState.CurrentMode(ProxyMode.OFF),
            onModeSelected = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Proxy Settings - Dark Theme")
@Composable
private fun ProxySettingsPreview_Dark() {
    PurchaseTesterTheme(darkTheme = true) {
        ProxySettingsContent(
            state = ProxySettingsState.CurrentMode(ProxyMode.ENTITLEMENT_OVERRIDE),
            onModeSelected = {}
        )
    }
}
