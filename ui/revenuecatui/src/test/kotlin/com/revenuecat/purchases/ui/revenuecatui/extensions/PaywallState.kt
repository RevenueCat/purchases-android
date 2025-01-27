package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState as actualToComponentsPaywallState

/**
 * Same as the production-code namesake, but with some parameters made optional as a convenience for testing code.
 */
internal fun Offering.toComponentsPaywallState(
    validationResult: PaywallValidationResult.Components,
    activelySubscribedProductIds: Set<String> = emptySet(),
    purchasedNonSubscriptionProductIds: Set<String> = emptySet(),
    storefrontCountryCode: String? = null,
): PaywallState.Loaded.Components =
    actualToComponentsPaywallState(
        validationResult = validationResult,
        activelySubscribedProductIds = activelySubscribedProductIds,
        purchasedNonSubscriptionProductIds = purchasedNonSubscriptionProductIds,
        storefrontCountryCode = storefrontCountryCode
    )
