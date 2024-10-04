package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.PurchasesError

internal sealed class CustomerCenterState {
    object Loading : CustomerCenterState()
    data class Error(val error: PurchasesError) : CustomerCenterState()

    // CustomerCenter WIP: Change to use the actual data the customer center will use.
    data class Success(val customerCenterConfigDataString: String) : CustomerCenterState()
}
