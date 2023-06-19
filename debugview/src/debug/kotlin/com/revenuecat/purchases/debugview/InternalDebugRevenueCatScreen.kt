package com.revenuecat.purchases.debugview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.debugview.models.InternalDebugRevenueCatScreenViewModel
import com.revenuecat.purchases.debugview.settings.SettingGroup

@Composable
internal fun InternalDebugRevenueCatScreen(
    viewModel: InternalDebugRevenueCatScreenViewModel = InternalDebugRevenueCatScreenViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        viewModel.settingGroups.forEach { SettingGroup(it) }
    }
}

@Preview
@Composable
private fun InternalDebugRevenueCatScreenPreview() {
    InternalDebugRevenueCatScreen()
}
