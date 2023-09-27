package com.revenuecat.purchases.ui.revenuecatui.templates

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.TestData

@Composable
internal fun Template3(
    state: PaywallViewState.Loaded,
    viewModel: PaywallViewModel,
) {
}

@Preview
@Composable
private fun Template3Preview() {
    InternalPaywallView(offering = TestData.template3Offering)
}
