package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchasingData

internal sealed class GooglePurchasingData : PurchasingData {
    data class InAppProduct(
        override val productId: String,
        val productDetails: ProductDetails
    ) : GooglePurchasingData()

    data class Subscription(
        override val productId: String,
        val productDetails: ProductDetails,

        val optionId: String,
        val token: String
    ) : GooglePurchasingData()

    override val productType: ProductType
        get() = when (this) {
            is InAppProduct -> {
                ProductType.INAPP
            }
            is Subscription -> {
                ProductType.SUBS
            }
        }
}
