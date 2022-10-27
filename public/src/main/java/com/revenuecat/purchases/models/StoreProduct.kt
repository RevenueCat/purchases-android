package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import org.json.JSONObject

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct : Parcelable {
    /**
     * sku for BC4 and Amazon, subId for BC5
     */
    val storeProductId: String

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType

    /**
     * Price information for a non-subscription product. Null for subscriptions.
     * For subscriptions, use PurchaseOption's pricing phases.
     */
    val oneTimeProductPrice: Price?

    /**
     * Title of the product.
     */
    val title: String

    /**
     * The description of the product.
     */
    val description: String

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Null for Amazon or for INAPP products.
     */
    val subscriptionPeriod: String?
}

data class ComparableData(
    val storeProductId: String,
    val type: ProductType,
    val oneTimeProductPrice: Price?,
    val title: String,
    val description: String,
    val subscriptionPeriod: String?
) {
    constructor(
        storeProduct: StoreProduct
    ) : this(
        storeProductId = storeProduct.storeProductId,
        type = storeProduct.type,
        oneTimeProductPrice = storeProduct.oneTimeProductPrice,
        title = storeProduct.title,
        description = storeProduct.description,
        subscriptionPeriod = storeProduct.subscriptionPeriod
    )
}
