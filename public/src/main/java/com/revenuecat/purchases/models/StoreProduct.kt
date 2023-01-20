package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct : Parcelable {
    /**
     * The product ID
     */
    val productId: String

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

    /**
     * List of PurchaseOptions. Empty list for INAPP products.
     */
    val purchaseOptions: List<PurchaseOption>

    /**
     * The default [PurchaseOption] that will be used when purchasing and not specifying a different option.
     * Null for INAPP products.
     */
    val defaultOption: PurchaseOption?

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
