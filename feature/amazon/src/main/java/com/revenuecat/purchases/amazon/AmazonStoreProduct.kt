package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import org.json.JSONObject

data class AmazonStoreProduct(

    /**
     * The productId
     * For subscriptions: the term sku
     * For INAPP: the sku
     */
    override val id: String,

    /**
     * Type of product. One of [ProductType].
     */
    override val type: ProductType,

    /**
     * Title of the product.
     */
    override val title: String,

    /**
     * The description of the product.
     */
    override val description: String,

    /**
     * Subscription period.
     *
     * Note: Returned only for subscriptions. Null for INAPP products.
     */
    override val period: Period?,

    /**
     * Price information for a non-subscription product.
     * Term price for a subscription.
     */
    override val price: Price,

    /**
     * Always null for AmazonStoreProduct
     */
    override val subscriptionOptions: SubscriptionOptions?,

    /**
     * Always null for AmazonStoreProduct
     */
    override val defaultOption: SubscriptionOption?,

    /**
     * The icon URL of the product.
     */
    val iconUrl: String,

    /**
     * The [Period] of a subscription's free trial. Null for INAPP.
     */
    val freeTrialPeriod: Period?,

    /**
     * JSONObject representing the original [Product] class from Amazon.
     */
    val originalProductJSON: JSONObject,

    /**
     * The offering ID this `SubscriptionOption` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `getProducts`
     */
    override val presentedOfferingIdentifier: String?
) : StoreProduct {

    /**
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: AmazonPurchasingData
        get() = AmazonPurchasingData.Product(this)

    @Deprecated(
        "Replaced with id",
        ReplaceWith("id")
    )
    override val sku: String
        get() = id

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return AmazonStoreProduct(
            this.id,
            this.type,
            this.title,
            this.description,
            this.period,
            this.price,
            this.subscriptionOptions,
            this.defaultOption,
            this.iconUrl,
            this.freeTrialPeriod,
            this.originalProductJSON,
            offeringId
        )
    }
}

/**
 * StoreProduct object containing Amazon-specific fields:
 * `originalProductJson`
 * `freeTrialPeriod`
 * `iconUrl`
 */
val StoreProduct.amazonProduct: AmazonStoreProduct?
    get() = this as? AmazonStoreProduct
