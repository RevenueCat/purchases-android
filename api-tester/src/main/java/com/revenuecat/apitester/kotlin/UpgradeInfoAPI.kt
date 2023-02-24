package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ProductChangeInfo
import com.revenuecat.purchases.models.GoogleProrationMode

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class UpgradeInfoAPI {
    fun check(productChangeInfo: ProductChangeInfo) {
        with(productChangeInfo) {
            val oldProductId: String = oldProductId
            val prorationMode: GoogleProrationMode = googleProrationMode

            val constructedProductChangeInfo =
                ProductChangeInfo(
                    oldProductId,
                    googleProrationMode
                )

            val constructedProductChangeInfoProductIdOnly = ProductChangeInfo(oldProductId)
        }
    }
}
