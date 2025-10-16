package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
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
        
        // These two properties are marked with @get:JvmSynthetic because their synthesized
        // getters were not getting the @ExperimentalPreviewRevenueCatPurchasesAPI annotation
        // applied to them, and there doesn't appear to be a way to do so.
        // We can remove the @get:JvmSynthetic annotation when we remove the experimental annotations from these
        // properties.
        @ExperimentalPreviewRevenueCatPurchasesAPI
        @get:JvmSynthetic
        val billingPeriod: Period? = null,
        @ExperimentalPreviewRevenueCatPurchasesAPI
        @get:JvmSynthetic
        val addOnProducts: List<GooglePurchasingData>? = null,
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
