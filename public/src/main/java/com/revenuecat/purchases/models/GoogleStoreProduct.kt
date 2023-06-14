package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType

data class GoogleStoreProduct(

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
    val googleProductData: GoogleProductData,

    /**
     * The offering ID this `GoogleStoreProduct` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    override val presentedOfferingIdentifier: String? = null,
) : StoreProduct {

    private constructor(
        otherProduct: GoogleStoreProduct,
        defaultOption: SubscriptionOption?,
        subscriptionOptionsWithOfferingId: SubscriptionOptions?,
        presentedOfferingIdentifier: String?,
    ) :
        this(
            otherProduct.productId,
            otherProduct.basePlanId,
            otherProduct.type,
            otherProduct.price,
            otherProduct.title,
            otherProduct.description,
            otherProduct.period,
            subscriptionOptionsWithOfferingId,
            defaultOption,
            otherProduct.googleProductData,
            presentedOfferingIdentifier,
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
     * Contains only data that is required to make the purchase.
     */
    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                id,
                googleProductData,
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
    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        val subscriptionOptionsWithOfferingIds = subscriptionOptions?.mapNotNull {
            (it as? GoogleSubscriptionOption)?.let { googleOption ->
                GoogleSubscriptionOption(googleOption, offeringId)
            }
        }

        val defaultOptionWithOfferingId = (defaultOption as? GoogleSubscriptionOption)?.let {
            GoogleSubscriptionOption(
                it,
                offeringId,
            )
        }

        return GoogleStoreProduct(
            this,
            defaultOptionWithOfferingId,
            subscriptionOptionsWithOfferingIds?.let { SubscriptionOptions(it) },
            offeringId,
        )
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

sealed class GoogleProductData {
    data class Product(
        val data: ProductDetails,
    ) : GoogleProductData()

    data class Sku(
        val data: SkuDetails,
    ) : GoogleProductData()

    val productId: String
        get() = when (this) {
            is GoogleProductData.Product -> {
                this.data.productId
            }
            is GoogleProductData.Sku -> {
                this.data.sku
            }
        }
}
