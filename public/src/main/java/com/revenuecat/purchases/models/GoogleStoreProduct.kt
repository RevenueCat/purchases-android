package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

data class GoogleStoreProduct(
    override val type: ProductType,
    override val price: Price,
    override val title: String,
    override val description: String,
    override val period: Period?,
    override val subscriptionOptions: SubscriptionOptions?,
    override val defaultOption: SubscriptionOption?,
    val productDetails: ProductDetails,
    override val platformProductId: GooglePlatformProductId,
) : StoreProduct {

    override val id: String
        get() = platformProductId.toId()

    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                id,
                productDetails
            )
        }

    /**
     * The sku of the StoreProduct
     */
    @Deprecated(
        "Replaced with productId",
        ReplaceWith("productId")
    )
    override val sku: String
        get() = sku
}

val StoreProduct.googleProduct: GoogleStoreProduct?
    get() = this as? GoogleStoreProduct
