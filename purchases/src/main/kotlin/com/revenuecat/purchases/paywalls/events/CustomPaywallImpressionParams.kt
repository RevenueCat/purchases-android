package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PresentedOfferingContext
import dev.drewhamilton.poko.Poko

/**
 * Parameters for tracking a custom paywall impression event.
 *
 * @property paywallId An optional identifier for the custom paywall being shown.
 * @property offeringId An optional identifier for the offering associated with the custom paywall.
 * If neither [offeringId] nor an [Offering] is provided, the SDK will use the current offering
 * identifier from the cache.
 * @property presentedOfferingContext The presented offering context (placement and targeting
 * information) derived from the offering, if available.
 */
@Poko
public class CustomPaywallImpressionParams @InternalRevenueCatAPI constructor(
    public val paywallId: String?,
    public val offeringId: String?,
    public val presentedOfferingContext: PresentedOfferingContext?,
) {
    /**
     * Creates parameters with no offering information. The SDK will use the current offering
     * identifier and context from the cache.
     *
     * @param paywallId An optional identifier for the custom paywall being shown.
     */
    @JvmOverloads
    @OptIn(InternalRevenueCatAPI::class)
    public constructor(paywallId: String? = null) : this(
        paywallId = paywallId,
        offeringId = null,
        presentedOfferingContext = null,
    )

    /**
     * Creates parameters with an offering identifier string.
     *
     * @param paywallId An optional identifier for the custom paywall being shown.
     * @param offeringId The identifier for the offering associated with the custom paywall.
     * @deprecated Pass an [Offering] object instead. Using an offering identifier string prevents
     * the SDK from deriving placement and targeting context automatically.
     */
    @Deprecated(
        message = "Pass an Offering object instead. Using an offering identifier string prevents " +
            "the SDK from deriving placement and targeting context automatically.",
        replaceWith = ReplaceWith("CustomPaywallImpressionParams(paywallId, offering)"),
    )
    @OptIn(InternalRevenueCatAPI::class)
    public constructor(paywallId: String? = null, offeringId: String?) : this(
        paywallId = paywallId,
        offeringId = offeringId,
        presentedOfferingContext = null,
    )

    /**
     * Creates parameters from the offering the paywall was obtained from.
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
    @OptIn(InternalRevenueCatAPI::class)
    public constructor(paywallId: String? = null, offering: Offering) : this(
        paywallId = paywallId,
        offeringId = offering.identifier,
        presentedOfferingContext = offering.presentedOfferingContext,
    )
}
