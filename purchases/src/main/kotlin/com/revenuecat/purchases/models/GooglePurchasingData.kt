package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import dev.drewhamilton.poko.Poko

sealed class GooglePurchasingData : PurchasingData {
    @Poko
    class InAppProduct(
        override val productId: String,
        val productDetails: ProductDetails,
    ) : GooglePurchasingData()

    @Poko
    class Subscription(
        override val productId: String,
        val optionId: String,
        val productDetails: ProductDetails,
        val token: String,
        val billingPeriod: Period?,
        val addOnProducts: List<GooglePurchasingData>?,
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
