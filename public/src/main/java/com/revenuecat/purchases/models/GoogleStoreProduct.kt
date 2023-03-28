package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType

data class GoogleStoreProduct(
    val productId: String,
    val basePlanId: String?,
    override val type: ProductType,
    override val price: Price,
    override val title: String,
    override val description: String,
    override val period: Period?,
    override val subscriptionOptions: SubscriptionOptions?,
    override val defaultOption: SubscriptionOption?,
    val productDetails: ProductDetails
) : StoreProduct {

    override val id: String
        get() = basePlanId?.let {
            "$productId:$basePlanId"
        } ?: productId

    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                id,
                productDetails
            )
        }

    override val platformProductId: PlatformProductId
        get() = GooglePlatformProductId(productId)

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
