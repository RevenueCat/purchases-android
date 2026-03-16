package com.revenuecat.purchases.paywalls.events

import dev.drewhamilton.poko.Poko

/**
 * Parameters for tracking a custom paywall impression event.
 *
 * @property paywallId An optional identifier for the custom paywall being shown.
 * @property offeringId An optional identifier for the offering associated with the custom paywall.
 * If not provided, the SDK will use the current offering identifier from the cache.
 */
@Poko
public class CustomPaywallImpressionParams @JvmOverloads constructor(
    public val paywallId: String? = null,
    public val offeringId: String? = null,
)
