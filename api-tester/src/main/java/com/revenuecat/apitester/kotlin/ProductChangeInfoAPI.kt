package com.revenuecat.apitester.kotlin

import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.revenuecat.purchases.ProductChangeInfo

@Suppress("unused", "UNUSED_VARIABLE", "RemoveExplicitTypeArguments")
private class ProductChangeInfoAPI {
    fun check(productChangeInfo: ProductChangeInfo) {
        with(productChangeInfo) {
            val oldProductId: String = oldSku
            @ProrationMode val prorationMode: Int? = prorationMode

            val constructedProductChangeInfo =
                ProductChangeInfo(
                    oldSku,
                    prorationMode
                )

            val constructedProductChangeInfoSkuOnly = ProductChangeInfo(oldSku)
        }
    }
}
