package com.revenuecat.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.sample.main.MainScreenNavigation
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme

@Composable
fun CustomEntitlementComputationApp() {
    MainScreenNavigation()
}

@Preview
@Composable
fun CustomEntitlementComputationAppPreview() {
    CustomEntitlementComputationTheme {
        CustomEntitlementComputationApp()
    }
}
