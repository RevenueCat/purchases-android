package com.revenuecat.purchases.ui.debugview.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.debugview.models.SettingState

@Composable
internal fun SettingText(
    settingState: SettingState.Text,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = settingState.title,
            style = MaterialTheme.typography.body1,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Box {
            Text(
                text = settingState.content,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }
}

@Preview
@Composable
private fun SettingPreview() {
    SettingText(SettingState.Text("Settings title", "Settings content"))
}
