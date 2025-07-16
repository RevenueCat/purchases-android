package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import dev.drewhamilton.poko.Poko
import java.util.Locale

@Poko
public class GoogleStoreProduct
@JvmOverloads
@Deprecated(
    "Replaced with constructor that takes a presentedOfferingContext",
    ReplaceWith(
        "GoogleStoreProduct(productId, basePlanId, type, price, name, title, description, " +
            "period, subscriptionOptions, defaultOption, productDetails, " +
            "PresentedOfferingContext(presentedOfferingIdentifier))",
    ),
)
constructor(

    /**
     * The productId.
     * For subscriptions, this is the high-level productId set up for a subscription in the Play Console,
     * unique to an entitlement.
     */
    public val productId: String,

    /**
     * The basePlanId for subscription products. Null for INAPP.
     */
    public val basePlanId: String?,

    /**
     * Type of product. One of [ProductType].
     */
    public override val type: ProductType,

    /**
     * Price information for a non-subscription product.
     * Base plan price for a subscription.
     * Use SubscriptionOption's pricing phases for offer pricing.
     */
    public override val price: Price,

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
    public override val name: String,

    /**
     * Title of the product.
     *
     * If you are using Google subscriptions with multiple base plans, this title
     * will be the same for every subscription duration (monthly, yearly, etc) as
     * base plans don't have their own titles. Google suggests using the duration
     * as a way to title base plans.
     */
    public override val title: String,

    /**
     * The description of the product.
     */
    public override val description: String,

    /**
     * Subscription period.
     * Null for INAPP products.
     */
    public override val period: Period?,

    /**
     * Contains all [SubscriptionOption]s. Null for INAPP products.
     */
    public override val subscriptionOptions: SubscriptionOptions?,

    /**
     * The default [SubscriptionOption] that will be used when purchasing and not specifying a different option.
     * Null for INAPP products.
     */
    public override val defaultOption: SubscriptionOption?,

    /**
     * The [ProductDetails] object returned from BillingClient that was used to construct this product.
     */
    public val productDetails: ProductDetails,

    /**
     * The offering ID this `GoogleStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    @Deprecated(
        "Use presentedOfferingContext instead",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    public override val presentedOfferingIdentifier: String? = null,

    /**
     * The context from which this product was obtained.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    public override val presentedOfferingContext: PresentedOfferingContext? = null,
) : StoreProduct {

    internal constructor(
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
        presentedOfferingContext: PresentedOfferingContext? = null,
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
        presentedOfferingContext?.offeringIdentifier,
        presentedOfferingContext,
    )

    @Deprecated(
        "Replaced with constructor that takes a name",
        ReplaceWith(
            "GoogleStoreProduct(productId, basePlanId, type, price, name, title, description, " +
                "period, subscriptionOptions, defaultOption, productDetails, presentedOfferingIdentifier)",
        ),
    )
    public constructor(
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
        presentedOfferingIdentifier?.let { PresentedOfferingContext(it) },
    )

    private constructor(
        otherProduct: GoogleStoreProduct,
        defaultOption: SubscriptionOption?,
        subscriptionOptionsWithOfferingId: SubscriptionOptions?,
        presentedOfferingContext: PresentedOfferingContext?,
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
    public override val id: String
        get() = basePlanId?.let {
            "$productId:$basePlanId"
        } ?: productId

    /**
     * Contains only data that is required to make the purchase.
     */
    public override val purchasingData: PurchasingData
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
    public override val sku: String
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
    public override fun copyWithOfferingId(offeringId: String): StoreProduct {
        val newPresentedOfferingContext = presentedOfferingContext?.copy(offeringIdentifier = offeringId)
            ?: PresentedOfferingContext(offeringId)
        return copyWithPresentedOfferingContext(newPresentedOfferingContext)
    }

    /**
     * For internal RevenueCat use.
     *
     * Creates a copy of this `GoogleStoreProduct` with the specified `presentedOfferingContext` set on itself and its
     * `defaultOption`/`subscriptionOptions`.
     */
    public override fun copyWithPresentedOfferingContext(
        presentedOfferingContext: PresentedOfferingContext?,
    ): StoreProduct {
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
    public override fun formattedPricePerMonth(locale: Locale): String? {
        return subscriptionOptions?.basePlan?.pricingPhases?.last()?.formattedPriceInMonths(locale)
    }
}

/**
 * StoreProduct object containing Google-specific fields:
 * `productId`
 * `basePlanId`
 * `productDetails`
 */
public val StoreProduct.googleProduct: GoogleStoreProduct?
    get() = this as? GoogleStoreProduct
