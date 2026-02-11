package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import dev.drewhamilton.poko.Poko
import org.json.JSONObject

@Poko
public class AmazonStoreProduct @JvmOverloads constructor(

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
     * Name of the product. This will match the title for Amazon products.
     */
    override val name: String,

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
    public val iconUrl: String,

    /**
     * The [Period] of a subscription's free trial. Null for INAPP.
     */
    public val freeTrialPeriod: Period?,

    /**
     * JSONObject representing the original [Product] class from Amazon.
     */
    public val originalProductJSON: JSONObject,

    /**
     * The context from which this product was obtained.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    override val presentedOfferingContext: PresentedOfferingContext? = null,
) : StoreProduct {

    @Deprecated(
        "Replaced with constructor that takes a name",
        ReplaceWith(
            "AmazonStoreProduct(productId, type, name, title, description, period, price, " +
                "subscriptionOptions, defaultOption, iconUrl, freeTrialPeriod, originalProductJSON, " +
                "presentedOfferingIdentifier)",
        ),
    )
    public constructor(
        id: String,
        type: ProductType,
        title: String,
        description: String,
        period: Period?,
        price: Price,
        subscriptionOptions: SubscriptionOptions?,
        defaultOption: SubscriptionOption?,
        iconUrl: String,
        freeTrialPeriod: Period?,
        originalProductJSON: JSONObject,
        presentedOfferingIdentifier: String? = null,
    ) : this(
        id = id,
        type = type,
        name = title,
        title = title,
        description = description,
        period = period,
        price = price,
        subscriptionOptions = subscriptionOptions,
        defaultOption = defaultOption,
        iconUrl = iconUrl,
        freeTrialPeriod = freeTrialPeriod,
        originalProductJSON = originalProductJSON,
        presentedOfferingContext = presentedOfferingIdentifier?.let { PresentedOfferingContext(it) },
    )

    @Deprecated(
        "Replaced with constructor that takes a presentedOfferingContext",
        ReplaceWith(
            "AmazonStoreProduct(productId, type, name, title, description, period, price, " +
                "subscriptionOptions, defaultOption, iconUrl, freeTrialPeriod, originalProductJSON, " +
                "PresentedOfferingContext(presentedOfferingIdentifier))",
        ),
    )
    public constructor(
        id: String,
        type: ProductType,
        name: String,
        title: String,
        description: String,
        period: Period?,
        price: Price,
        subscriptionOptions: SubscriptionOptions?,
        defaultOption: SubscriptionOption?,
        iconUrl: String,
        freeTrialPeriod: Period?,
        originalProductJSON: JSONObject,
        presentedOfferingIdentifier: String,
    ) : this(
        id = id,
        type = type,
        name = name,
        title = title,
        description = description,
        period = period,
        price = price,
        subscriptionOptions = subscriptionOptions,
        defaultOption = defaultOption,
        iconUrl = iconUrl,
        freeTrialPeriod = freeTrialPeriod,
        originalProductJSON = originalProductJSON,
        presentedOfferingContext = PresentedOfferingContext(presentedOfferingIdentifier),
    )

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
     * The offering ID this `AmazonStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    @Deprecated(
        "Use presentedOfferingContext instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `AmazonStoreProduct` with the specified `offeringId` set.
     */
    @Deprecated(
        "Replaced with copyWithPresentedOfferingContext",
        ReplaceWith("copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))"),
    )
    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        val newPresentedOfferingContext = presentedOfferingContext?.copy(offeringIdentifier = offeringId)
            ?: PresentedOfferingContext(offeringId)
        return copyWithPresentedOfferingContext(newPresentedOfferingContext)
    }

    public override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
        return AmazonStoreProduct(
            this.id,
            this.type,
            this.name,
            this.title,
            this.description,
            this.period,
            this.price,
            this.subscriptionOptions,
            this.defaultOption,
            this.iconUrl,
            this.freeTrialPeriod,
            this.originalProductJSON,
            presentedOfferingContext,
        )
    }

    public override fun equals(other: Any?): Boolean = other is AmazonStoreProduct &&
        ComparableData(this) == ComparableData(other)
    public override fun hashCode(): Int = ComparableData(this).hashCode()
}

/**
 * StoreProduct object containing Amazon-specific fields:
 * `originalProductJson`
 * `freeTrialPeriod`
 * `iconUrl`
 */
public val StoreProduct.amazonProduct: AmazonStoreProduct?
    get() = this as? AmazonStoreProduct

/**
 * Contains fields to be used for equality, which ignores jsonObject.
 * jsonObject is excluded because we're already using the parsed fields for comparisons,
 * and to avoid complicating parcelization
 */
private data class ComparableData(
    public val id: String,
    public val type: ProductType,
    public val title: String,
    public val description: String,
    public val period: Period?,
    public val price: Price,
    public val subscriptionOptions: SubscriptionOptions?,
    public val defaultOption: SubscriptionOption?,
    public val iconUrl: String,
    public val freeTrialPeriod: Period?,
    public val presentedOfferingContext: PresentedOfferingContext?,
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
        presentedOfferingContext = amazonStoreProduct.presentedOfferingContext,
    )
}
