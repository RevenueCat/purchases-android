@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.PADDING_MEDIUM

@Composable
@JvmSynthetic
internal fun SettingsButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(PADDING_MEDIUM),
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SettingsButton_Preview() {
    SettingsButton(
        title = "Cancel Subscription",
    ) {}
}
