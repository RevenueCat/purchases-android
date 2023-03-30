package com.revenuecat.purchases.models

import com.android.billingclient.api.ProductDetails

/**
 * Defines an option for purchasing a Google subscription
 */
data class GoogleSubscriptionOption(
    val productId: String,
    val basePlanId: String,
    val offerId: String?,
    override val pricingPhases: List<PricingPhase>,
    override val tags: List<String>,
    val productDetails: ProductDetails,
    val offerToken: String,
    override val presentedOfferingIdentifier: String? = null
) : SubscriptionOption {

    constructor(subscriptionOption: GoogleSubscriptionOption, presentedOfferingIdentifier: String?) :
        this(
            subscriptionOption.productId,
            subscriptionOption.basePlanId,
            subscriptionOption.offerId,
            subscriptionOption.pricingPhases,
            subscriptionOption.tags,
            subscriptionOption.productDetails,
            subscriptionOption.offerToken,
            presentedOfferingIdentifier
        )

    override val id: String
        get() = basePlanId + if (offerId.isNullOrBlank()) "" else ":$offerId"

    override val purchasingData: PurchasingData
        get() = GooglePurchasingData.Subscription(
            productId,
            id,
            productDetails,
            offerToken
        )
}
