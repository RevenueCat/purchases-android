package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import java.util.Locale

data class GoogleStoreProduct @JvmOverloads constructor(

    /**
     * The productId.
     * For subscriptions, this is the high-level productId set up for a subscription in the Play Console,
     * unique to an entitlement.
     */
    val productId: String,

    /**
     * The basePlanId for subscription products. Null for INAPP.
     */
    val basePlanId: String?,

    /**
     * Type of product. One of [ProductType].
     */
    override val type: ProductType,

    /**
     * Price information for a non-subscription product.
     * Base plan price for a subscription.
     * Use SubscriptionOption's pricing phases for offer pricing.
     */
    override val price: Price,

    /**
     * Name of the product.
     *
     * For google subscriptions, this corresponds with the name of the subscription.
     * For amazon subscriptions, this will match the title.
     *
     * If you are using Google subscriptions with multiple base plans, this title
     * will be the same for every subscription duration (monthly, yearly, etc) as
     * base plans don't have their own titles. Google suggests using the duration
     * as a way to title base plans.
     */
    override val name: String,

    /**
     * Title of the product.
     *
     * If you are using Google subscriptions with multiple base plans, this title
     * will be the same for every subscription duration (monthly, yearly, etc) as
     * base plans don't have their own titles. Google suggests using the duration
     * as a way to title base plans.
     */
    override val title: String,

    /**
     * The description of the product.
     */
    override val description: String,

    /**
     * Subscription period.
     * Null for INAPP products.
     */
    override val period: Period?,

    /**
     * Contains all [SubscriptionOption]s. Null for INAPP products.
     */
    override val subscriptionOptions: SubscriptionOptions?,

    /**
     * The default [SubscriptionOption] that will be used when purchasing and not specifying a different option.
     * Null for INAPP products.
     */
    override val defaultOption: SubscriptionOption?,

    /**
     * The [ProductDetails] object returned from BillingClient that was used to construct this product.
     */
    val productDetails: ProductDetails,

    /**
     * The context from which this product was obtained.
     */
    override val presentedOfferingContext: PresentedOfferingContext = PresentedOfferingContext(),
) : StoreProduct {

    @Deprecated(
        "Replaced with constructor that takes a name",
        ReplaceWith(
            "GoogleStoreProduct(productId, basePlanId, type, price, name, title, description, " +
                "period, subscriptionOptions, defaultOption, productDetails, presentedOfferingIdentifier)",
        ),
    )
    constructor(
        productId: String,
        basePlanId: String?,
        type: ProductType,
        price: Price,
        title: String,
        description: String,
        period: Period?,
        subscriptionOptions: SubscriptionOptions?,
        defaultOption: SubscriptionOption?,
        productDetails: ProductDetails,
        presentedOfferingIdentifier: String? = null,
    ) : this(
        productId,
        basePlanId,
        type,
        price,
        title,
        title,
        description,
        period,
        subscriptionOptions,
        defaultOption,
        productDetails,
        PresentedOfferingContext(presentedOfferingIdentifier),
    )

    @Deprecated(
        "Replaced with constructor that takes a presentedOfferingContext",
        ReplaceWith(
            "GoogleStoreProduct(productId, basePlanId, type, price, name, title, description, " +
                "period, subscriptionOptions, defaultOption, productDetails, " +
                "PresentedOfferingContext(presentedOfferingIdentifier))",
        ),
    )
    constructor(
        productId: String,
        basePlanId: String?,
        type: ProductType,
        price: Price,
        name: String,
        title: String,
        description: String,
        period: Period?,
        subscriptionOptions: SubscriptionOptions?,
        defaultOption: SubscriptionOption?,
        productDetails: ProductDetails,
        presentedOfferingIdentifier: String?,
    ) : this(
        productId,
        basePlanId,
        type,
        price,
        name,
        title,
        description,
        period,
        subscriptionOptions,
        defaultOption,
        productDetails,
        PresentedOfferingContext(presentedOfferingIdentifier),
    )

    private constructor(
        otherProduct: GoogleStoreProduct,
        defaultOption: SubscriptionOption?,
        subscriptionOptionsWithOfferingId: SubscriptionOptions?,
        presentedOfferingContext: PresentedOfferingContext,
    ) :
        this(
            otherProduct.productId,
            otherProduct.basePlanId,
            otherProduct.type,
            otherProduct.price,
            otherProduct.name,
            otherProduct.title,
            otherProduct.description,
            otherProduct.period,
            subscriptionOptionsWithOfferingId,
            defaultOption,
            otherProduct.productDetails,
            presentedOfferingContext,
        )

    /**
     * The product ID.
     * INAPP: "<productId>"
     * Sub: "<productId:basePlanID>"
     */
    override val id: String
        get() = basePlanId?.let {
            "$productId:$basePlanId"
        } ?: productId

    /**
     * The offering ID this `GoogleStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    @Deprecated(
        "Use presentedOfferingContext instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    override val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext.offeringIdentifier

    /**
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                id,
                productDetails,
            )
        }

    /**
     * The sku of the StoreProduct
     */
    @Deprecated(
        "Replaced with productId",
        ReplaceWith("productId"),
    )
    override val sku: String
        get() = productId

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `GoogleStoreProduct` with the specified `offeringId` set on itself and its
     * `defaultOption`/`subscriptionOptions`.
     */
    @Deprecated(
        "Use copyWithPresentedOfferingContext instead",
        ReplaceWith("copyWithPresentedOfferingContext(presentedOfferingContext)"),
    )
    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return copyWithPresentedOfferingContext(PresentedOfferingContext(offeringId))
    }

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `GoogleStoreProduct` with the specified `presentedOfferingContext` set on itself and its
     * `defaultOption`/`subscriptionOptions`.
     */
    override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext): StoreProduct {
        val subscriptionOptionsWithContext = subscriptionOptions?.mapNotNull {
            (it as? GoogleSubscriptionOption)?.let { googleOption ->
                GoogleSubscriptionOption(googleOption, presentedOfferingContext)
            }
        }

        val defaultOptionWithOfferingId = (defaultOption as? GoogleSubscriptionOption)?.let {
            GoogleSubscriptionOption(
                it,
                presentedOfferingContext,
            )
        }

        return GoogleStoreProduct(
            this,
            defaultOptionWithOfferingId,
            subscriptionOptionsWithContext?.let { SubscriptionOptions(it) },
            presentedOfferingContext,
        )
    }

    /**
     * Null for INAPP products. The price of the [StoreProduct] in the given locale in a monthly recurrence.
     * This means that, for example, if the period is annual, the price will be divided by 12.
     * It uses a currency formatter to format the price in the given locale.
     * Note that this value may be an approximation.
     * This value will use the basePlan to calculate the value.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    override fun formattedPricePerMonth(locale: Locale): String? {
        return subscriptionOptions?.basePlan?.pricingPhases?.last()?.formattedPriceInMonths(locale)
    }
}

/**
 * StoreProduct object containing Google-specific fields:
 * `productId`
 * `basePlanId`
 * `productDetails`
 */
val StoreProduct.googleProduct: GoogleStoreProduct?
    get() = this as? GoogleStoreProduct
