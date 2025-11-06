package com.revenuecat.purchasetester.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchasetester.proxysettings.ProxyMode
import com.revenuecat.purchasetester.proxysettings.ProxySettingsBottomSheetViewModel
import com.revenuecat.purchasetester.proxysettings.ProxySettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ProxySettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProxySettingsBottomSheetViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Proxy Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
        ) {
            when (state) {
                is ProxySettingsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                is ProxySettingsState.Error -> {
                    Text(
                        text = (state as ProxySettingsState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is ProxySettingsState.CurrentMode -> {
                    val currentMode = (state as ProxySettingsState.CurrentMode).mode
                    Text(
                        "Select Proxy Mode",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        ProxyModeOption(
                            label = "Disabled",
                            selected = currentMode == ProxyMode.OFF,
                            onClick = { viewModel.changeMode(ProxyMode.OFF) },
                        )
                        ProxyModeOption(
                            label = "Override Entitlements",
                            selected = currentMode == ProxyMode.ENTITLEMENT_OVERRIDE,
                            onClick = { viewModel.changeMode(ProxyMode.ENTITLEMENT_OVERRIDE) },
                        )
                        ProxyModeOption(
                            label = "Server Down",
                            selected = currentMode == ProxyMode.SERVER_DOWN,
                            onClick = { viewModel.changeMode(ProxyMode.SERVER_DOWN) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
