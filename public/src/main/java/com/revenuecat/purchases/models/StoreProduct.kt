package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct : Parcelable {
    /**
     * The unique ID of the product.
     * Google INAPP: "<productId>"
     * Google Sub: "<productId:basePlanID>"
     * Amazon INAPP: "<sku>"
     * Amazon Sub: "<termSku>"
     */
    val id: String

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType

    /**
     * Price information for a non-subscription product.
     * Base plan price for a Google subscription.
     * Term price for an Amazon subscription.
     * For subscriptions, use SubscriptionOption's pricing phases.
     */
    val price: Price

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

    /**
     * List of SubscriptionOptions. Empty list for INAPP products.
     */
    val subscriptionOptions: List<SubscriptionOption>

    /**
     * The default [SubscriptionOption] that will be used when purchasing and not specifying a different option.
     * Null for INAPP products.
     */
    val defaultOption: SubscriptionOption?

    // TODO javadocs
    val purchasingData: PurchasingData

    /**
     * The sku of the StoreProduct
     */
    @IgnoredOnParcel
    @Deprecated(
        "Replaced with productId",
        ReplaceWith("productId")
    )
    val sku: String
}
