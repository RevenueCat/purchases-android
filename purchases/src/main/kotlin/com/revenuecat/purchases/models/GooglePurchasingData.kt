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
    ) : GooglePurchasingData()

    override val productType: ProductType
        get() = when (this) {
            is InAppProduct -> {
                ProductType.INAPP
            }
            is Subscription -> {
                ProductType.SUBS
            }
            // TODO: Write tests for this
            is ProductWithAddOns -> {
                // Can be either INAPP or SUBS. Since all of the types in the
                // multi-line purchase must match, we use the type from the
                // base product.
                baseProduct.productType
            }
        }

    @Poko
    class ProductWithAddOns(
        override val productId: String,
        val baseProduct: GooglePurchasingData,
        val addOnProducts: List<GooglePurchasingData>,
    ) : GooglePurchasingData()
}
