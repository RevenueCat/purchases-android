package com.revenuecat.purchasetester.ui.components.configuration.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ConfigurationHeaderPreview() {
    ConfigurationHeader(
        title = "SDK Configuration",
        subtitle = "Configure your app's SDK settings" ,
    )
}