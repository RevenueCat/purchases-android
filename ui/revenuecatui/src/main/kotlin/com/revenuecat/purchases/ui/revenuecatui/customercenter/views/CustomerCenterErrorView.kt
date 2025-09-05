package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Composable
internal fun CustomerCenterErrorView(state: CustomerCenterState.Error) {
    // CustomerCenter WIP: Add proper error UI
    Text("Error: ${state.error}")
}

@Preview(
    name = "Customer Center Error View",
    showBackground = true,
)
@Composable
internal fun CustomerCenterErrorViewPreview() {
    CustomerCenterPreviewTheme {
        CustomerCenterErrorView(
            state = CustomerCenterState.Error(
                error = PurchasesError(
                    code = PurchasesErrorCode.UnknownError,
                    underlyingErrorMessage = "Mock error"
                )
            )
        )
    }
}