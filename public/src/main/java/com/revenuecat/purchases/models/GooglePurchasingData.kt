package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

sealed class GooglePurchasingData : PurchasingData {
    data class InAppProduct(
        override val productId: String,
        val productData: GoogleProductData,
    ) : GooglePurchasingData()

    data class Subscription(
        override val productId: String,
        val optionId: String,
        val productData: GoogleProductData,
        val token: String,
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
