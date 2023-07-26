package com.revenuecat.purchases.debugview.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.debugview.models.SettingGroupState
import com.revenuecat.purchases.debugview.models.SettingState

@Composable
internal fun SettingGroup(
    settingGroupState: SettingGroupState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = settingGroupState.title,
            style = MaterialTheme.typography.h6,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(elevation = 4.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (settingGroupState) {
                    is SettingGroupState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    is SettingGroupState.Loaded -> settingGroupState.settings.forEach { settingState ->
                        SettingText(settingState)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingGroupPreview() {
    SettingGroup(
        SettingGroupState.Loaded(
            "Settings group",
            listOf(
                SettingState.Loading("Settings text 1"),
                SettingState.TextLoaded("Settings text 2", "Settings content 2"),
                SettingState.Loading("Settings text 3"),
            ),
        ),
    )
}
