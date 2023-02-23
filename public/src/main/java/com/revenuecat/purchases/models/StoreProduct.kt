package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct : Parcelable {
    /**
     * The product ID.
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
     *
     * If you are using Google subscriptions with multiple base plans, this title
     * will be the same for every subscription duration (monthly, yearly, etc) as
     * base plans don't have their own titles. Google suggests using the duration
     * as a way to title base plans.
     */
    val title: String

    /**
     * The description of the product.
     */
    val description: String

    /**
     * Subscription period.
     *
     * Note: Returned only for Google subscriptions. Null for Amazon or for INAPP products.
     */
    val period: Period?

    /**
     * Contains all [SubscriptionOption]s. Null for INAPP products.
     */
    val subscriptionOptions: SubscriptionOptions?

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
