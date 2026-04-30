package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.Offering
import dev.drewhamilton.poko.Poko

/**
 * Parameters for tracking a custom paywall impression event.
 *
 * @property paywallId An optional identifier for the custom paywall being shown.
 * @property offeringId An optional identifier for the offering associated with the custom paywall.
 * If neither [offeringId] nor [offering] is provided, the SDK will use the current offering
 * identifier from the cache.
 * @property offering An optional [Offering] associated with the custom paywall. When provided, the
 * SDK will derive both the offering identifier and the presented offering context (placement and
 * targeting information) from this offering.
 */
@Poko
public class CustomPaywallImpressionParams @JvmOverloads constructor(
    public val paywallId: String? = null,
    public val offeringId: String? = null,
    public val offering: Offering? = null,
) {
    /**
     * Creates parameters for a custom paywall impression from the offering it was obtained from.
     *
     * Use this constructor when presenting a paywall for an offering that is not the current
     * offering (for example, a placement-resolved offering). The SDK will derive both the offering
     * identifier and the presented offering context (placement and targeting information) from
     * the provided offering.
     *
     * @param paywallId An optional identifier for the custom paywall being shown.
     * @param offering The [Offering] associated with the custom paywall.
     */
    @JvmOverloads
    public constructor(paywallId: String? = null, offering: Offering) : this(
        paywallId = paywallId,
        offeringId = offering.identifier,
        offering = offering,
    )
}
