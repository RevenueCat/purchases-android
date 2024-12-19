package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData

internal sealed class CustomerCenterState {
    object Loading : CustomerCenterState()
    data class Error(val error: PurchasesError) : CustomerCenterState()

    // CustomerCenter WIP: Change to use the actual data the customer center will use.
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    data class Success(
        val customerCenterConfigData: CustomerCenterConfigData,
        val purchaseInformation: PurchaseInformation?,
    ) : CustomerCenterState()
}
