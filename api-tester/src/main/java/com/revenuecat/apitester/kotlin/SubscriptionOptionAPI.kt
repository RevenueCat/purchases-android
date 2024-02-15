package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.SubscriptionOption

@Suppress("unused", "UNUSED_VARIABLE", "deprecation")
private class SubscriptionOptionAPI {
    fun checkSubscriptionOption(subscriptionOption: SubscriptionOption) {
        val phases: List<PricingPhase> = subscriptionOption.pricingPhases
        val tags: List<String> = subscriptionOption.tags
        val isBasePlan: Boolean = subscriptionOption.isBasePlan
        val presentedOfferingId: String? = subscriptionOption.presentedOfferingIdentifier
        val presentedOfferingContext: PresentedOfferingContext? = subscriptionOption.presentedOfferingContext
        val isPrepaid: Boolean = subscriptionOption.isPrepaid
    }

    fun checkGoogleSubscriptionOption(googleSubscriptionOption: GoogleSubscriptionOption) {
        checkSubscriptionOption(googleSubscriptionOption)
        val productId = googleSubscriptionOption.productId
        val basePlanId = googleSubscriptionOption.basePlanId
        val offerId = googleSubscriptionOption.offerId
        val offerToken = googleSubscriptionOption.offerToken
        val productDetails = googleSubscriptionOption.productDetails

        val subscriptionOption = GoogleSubscriptionOption(
            productId,
            basePlanId,
            offerId,
            googleSubscriptionOption.pricingPhases,
            googleSubscriptionOption.tags,
            productDetails,
            offerToken,
        )

        val subscriptionOptionWithOfferingId = GoogleSubscriptionOption(
            productId,
            basePlanId,
            offerId,
            googleSubscriptionOption.pricingPhases,
            googleSubscriptionOption.tags,
            productDetails,
            offerToken,
            "offeringId",
        )

        val subscriptionOptionWithOfferingContext = GoogleSubscriptionOption(
            productId,
            basePlanId,
            offerId,
            googleSubscriptionOption.pricingPhases,
            googleSubscriptionOption.tags,
            productDetails,
            offerToken,
            googleSubscriptionOption.presentedOfferingContext,
        )

        val subscriptionOptionWithNullOfferingContext = GoogleSubscriptionOption(
            productId,
            basePlanId,
            offerId,
            googleSubscriptionOption.pricingPhases,
            googleSubscriptionOption.tags,
            productDetails,
            offerToken,
            null,
        )
    }
}
