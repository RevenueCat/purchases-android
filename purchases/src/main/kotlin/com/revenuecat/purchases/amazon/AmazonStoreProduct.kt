package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.utils.formattedPricePerMonth
import org.json.JSONObject
import java.util.Locale

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
     * The offering ID this `AmazonStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    override val presentedOfferingIdentifier: String? = null,
) : StoreProduct {

    /**
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: AmazonPurchasingData
        get() = AmazonPurchasingData.Product(this)

    @Deprecated(
        "Replaced with id",
        ReplaceWith("id"),
    )
    override val sku: String
        get() = id

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `AmazonStoreProduct` with the specified `offeringId` set.
     */
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
            offeringId,
        )
    }

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a monthly recurrence.
     * This means that, for example, if the period is annual, the price will be divided by 12.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    override fun formattedPricePerMonth(locale: Locale): String? {
        return period?.let { price.formattedPricePerMonth(it, locale) }
    }

    override fun equals(other: Any?) = other is AmazonStoreProduct &&
        ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

/**
 * StoreProduct object containing Amazon-specific fields:
 * `originalProductJson`
 * `freeTrialPeriod`
 * `iconUrl`
 */
val StoreProduct.amazonProduct: AmazonStoreProduct?
    get() = this as? AmazonStoreProduct

/**
 * Contains fields to be used for equality, which ignores jsonObject.
 * jsonObject is excluded because we're already using the parsed fields for comparisons,
 * and to avoid complicating parcelization
 */
private data class ComparableData(
    val id: String,
    val type: ProductType,
    val title: String,
    val description: String,
    val period: Period?,
    val price: Price,
    val subscriptionOptions: SubscriptionOptions?,
    val defaultOption: SubscriptionOption?,
    val iconUrl: String,
    val freeTrialPeriod: Period?,
    val offeringId: String?,
) {
    constructor(
        amazonStoreProduct: AmazonStoreProduct,
    ) : this(
        id = amazonStoreProduct.id,
        type = amazonStoreProduct.type,
        title = amazonStoreProduct.title,
        description = amazonStoreProduct.description,
        period = amazonStoreProduct.period,
        price = amazonStoreProduct.price,
        subscriptionOptions = amazonStoreProduct.subscriptionOptions,
        defaultOption = amazonStoreProduct.defaultOption,
        iconUrl = amazonStoreProduct.iconUrl,
        freeTrialPeriod = amazonStoreProduct.freeTrialPeriod,
        offeringId = amazonStoreProduct.presentedOfferingIdentifier,
    )
}
