package com.revenuecat.purchases.debugview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.purchases.debugview.models.InternalDebugRevenueCatScreenViewModel
import com.revenuecat.purchases.debugview.models.SettingGroupState
import com.revenuecat.purchases.debugview.models.SettingScreenState
import com.revenuecat.purchases.debugview.models.SettingState
import com.revenuecat.purchases.debugview.settings.SettingGroup
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun InternalDebugRevenueCatScreen(
    screenViewModel: DebugRevenueCatViewModel = viewModel<InternalDebugRevenueCatScreenViewModel>(),
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = "RevenueCat Debug Menu",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        screenViewModel.state.collectAsState().value.toGroupStates().forEach { SettingGroup(it) }
    }
}

@Preview(showBackground = true)
@Composable
private fun InternalDebugRevenueCatScreenPreview() {
    InternalDebugRevenueCatScreen(
        screenViewModel = object : DebugRevenueCatViewModel {
            override val state = MutableStateFlow<SettingScreenState>(
                SettingScreenState.Configured(
                    SettingGroupState(
                        "Configuration",
                        listOf(
                            SettingState.Text("SDK version", "3.0.0"),
                            SettingState.Text("Observer mode", "true"),
                        ),
                    ),
                    SettingGroupState(
                        "Customer info",
                        listOf(
                            SettingState.Text("Current User ID", "current-user-id"),
                            SettingState.Text("Active entitlements", "pro, premium"),
                        ),
                    ),
                    SettingGroupState(
                        "Offerings",
                        listOf(
                            SettingState.Text("current", "TODO"),
                            SettingState.Text("default", "TODO"),
                        ),
                    ),
                ),
            )
        },
    )
}
