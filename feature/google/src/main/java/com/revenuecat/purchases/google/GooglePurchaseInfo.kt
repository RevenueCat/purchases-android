package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseInfo

internal sealed class GooglePurchaseInfo: PurchaseInfo {
    data class InAppProduct(
        override val productId: String,
        val productDetails: ProductDetails
    ) : GooglePurchaseInfo()

    data class Subscription(
        override val productId: String,
        val productDetails: ProductDetails,

        val optionId: String,
        val token: String
    ) : GooglePurchaseInfo()

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