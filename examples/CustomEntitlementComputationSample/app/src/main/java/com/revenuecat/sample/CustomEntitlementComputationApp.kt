package com.revenuecat.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.sample.main.MainScreen
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme

@Composable
fun CustomEntitlementComputationApp() {
    MainScreen()
}

@Preview
@Composable
fun CustomEntitlementComputationAppPreview() {
    CustomEntitlementComputationTheme {
        CustomEntitlementComputationApp()
    }
}
