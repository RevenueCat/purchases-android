package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

sealed class GooglePurchaseInfo: PurchaseInfo {
    data class NotSubscription(
        override val productId: String,
        val productDetails: ProductDetails
    ) : GooglePurchaseInfo()

    data class Subscription(
        override val productId: String,
        val productDetails: ProductDetails,

        val optionId: String?,
        val token: String
    ) : GooglePurchaseInfo()

    override val productType: ProductType
        get() = when (this) {
            is NotSubscription -> {
                ProductType.INAPP
            }
            is Subscription -> {
                ProductType.SUBS
            }
        }
}