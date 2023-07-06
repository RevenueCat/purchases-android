package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails

/**
 * Defines an option for purchasing a Google subscription
 */
data class GoogleSubscriptionOption(
    /**
     * If this SubscriptionOption represents a base plan, this will be the basePlanId.
     * If it represents an offer, it will be basePlanId:offerId
     */
    val productId: String,

    /**
     * The id of the base plan that this `GoogleSubscriptionOption` belongs to.
     */
    val basePlanId: String,

    /**
     * If this represents an offer, the offerId set in the Play Console.
     * Null otherwise.
     */
    val offerId: String?,

    /**
     * Pricing phases defining a user's payment plan for the product over time.
     */
    override val pricingPhases: List<PricingPhase>,

    /**
     * Tags defined on the base plan or offer. Keep in mind that offers automatically
     * inherit their base plan's tag.
     */
    override val tags: List<String>,

    /**
     * The `ProductDetails` object this `GoogleSubscriptionOption` was created from.
     * Use to get underlying BillingClient information.
     */
    val productDetails: ProductDetails,

    /**
     * The token used to purchase this `GoogleSubscriptionOption`, whether it represents
     * a base plan or an offer.
     */
    val offerToken: String,

    /**
     * The offering ID this `GoogleSubscriptionOption` was returned from.
     *
     * Null if not using RevenueCat offerings system, or if fetched directly via `Purchases.getProducts`
     */
    override val presentedOfferingIdentifier: String? = null,
) : SubscriptionOption {

    internal constructor(subscriptionOption: GoogleSubscriptionOption, presentedOfferingIdentifier: String?) :
        this(
            subscriptionOption.productId,
            subscriptionOption.basePlanId,
            subscriptionOption.offerId,
            subscriptionOption.pricingPhases,
            subscriptionOption.tags,
            subscriptionOption.productDetails,
            subscriptionOption.offerToken,
            presentedOfferingIdentifier,
        )

    override val id: String
        get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"

    override val purchasingData: PurchasingData
        get() = GooglePurchasingData.Subscription(
            productId,
            id,
            productDetails,
            offerToken,
        )
}
