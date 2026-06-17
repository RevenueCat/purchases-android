package com.revenuecat.purchases.ui.revenuecatui.testing

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering

/**
 * Thrown when an [Offering]'s components paywall fails validation while rendering through
 * [ComponentsPaywallForTesting].
 */
@InternalRevenueCatAPI
public class PaywallComponentsValidationException internal constructor(
    public val errors: List<String>,
) : IllegalStateException(
    "Paywall components failed validation:\n" + errors.joinToString("\n") { " - $it" },
)
