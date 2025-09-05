package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Composable
internal fun CustomerCenterLoadingView() {
    // CustomerCenter WIP: Add proper loading UI
    Text("Loading...")
}

@Preview(
    name = "Customer Center Loading View",
    showBackground = true,
)
@Composable
internal fun CustomerCenterLoadingViewPreview() {
    CustomerCenterPreviewTheme {
        CustomerCenterLoadingView()
    }
}
