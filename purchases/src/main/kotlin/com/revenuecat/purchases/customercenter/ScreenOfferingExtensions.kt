package com.revenuecat.purchases.customercenter

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith

@InternalRevenueCatAPI
public fun CustomerCenterConfigData.Screen.resolveOffering(
    purchases: Purchases,
    onError: (error: PurchasesError) -> Unit = {},
    onSuccess: (offering: Offering?) -> Unit,
) {
    val screenOffering = this.offering

    if (screenOffering == null) {
        onSuccess(null)
        return
    }

    purchases.getOfferingsWith(
        onError = onError,
        onSuccess = { offerings ->
            val resolvedOffering = when (screenOffering.type) {
                CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT -> {
                    offerings.current
                }
                CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC -> {
                    val offeringId = screenOffering.offeringId
                    if (offeringId != null) {
                        offerings.all[offeringId]
                    } else {
                        null
                    }
                }
            }
            onSuccess(resolvedOffering)
        },
    )
}
