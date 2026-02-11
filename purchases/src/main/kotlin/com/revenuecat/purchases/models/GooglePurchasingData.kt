package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ProductType
import dev.drewhamilton.poko.Poko

sealed class GooglePurchasingData : PurchasingData {
    @Poko
    class InAppProduct(
        override val productId: String,
        public val productDetails: ProductDetails,
    ) : GooglePurchasingData()

    @Poko
    class Subscription @ExperimentalPreviewRevenueCatPurchasesAPI constructor(
        override val productId: String,
        public val optionId: String,
        public val productDetails: ProductDetails,
        public val token: String,

        // These two properties are marked with @get:JvmSynthetic because their synthesized
        // getters were not getting the @ExperimentalPreviewRevenueCatPurchasesAPI annotation
        // applied to them, and there doesn't appear to be a way to do so.
        // We can remove the @get:JvmSynthetic annotation when we remove the experimental annotations from these
        // properties.
        @ExperimentalPreviewRevenueCatPurchasesAPI
        @get:JvmSynthetic
        public val billingPeriod: Period? = null,
        @ExperimentalPreviewRevenueCatPurchasesAPI
        @get:JvmSynthetic
        public val addOnProducts: List<GooglePurchasingData>? = null,
    ) : GooglePurchasingData() {

        // This recreates the constructor without billingPeriod and addOnProducts so that we have a copy
        // that isn't marked with @ExperimentalPreviewRevenueCatPurchasesAPI. It can be removed when
        // @ExperimentalPreviewRevenueCatPurchasesAPI is removed from the primary constructor.
        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        constructor(
            productId: String,
            optionId: String,
            productDetails: ProductDetails,
            token: String,
        ) : this(
            productId = productId,
            optionId = optionId,
            productDetails = productDetails,
            token = token,
            billingPeriod = null,
            addOnProducts = null,
        )
    }

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
