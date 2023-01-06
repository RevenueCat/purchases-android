package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

sealed class GooglePurchaseInfo: PurchaseInfo {
    data class NotSubscription(
        val productId: String,
        val type: ProductType,
        val productDetails: ProductDetails
    ) : GooglePurchaseInfo()

    data class Subscription(
        val productId: String,
        val type: ProductType,
        val productDetails: ProductDetails,

        val optionId: String?,
        val token: String?
    ) : GooglePurchaseInfo()
}