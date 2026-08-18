@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

/**
 * The offerings a customer could be served next: a project using placements gets one current offering per
 * placement, so all of them plus the fallback count, not just [Offerings.current].
 *
 * All of it arrives in the same offerings response, so this costs no network. Resolved against
 * [Offerings.all] rather than [Offerings.getCurrentOfferingForPlacement], which allocates a
 * presented-context copy warming has no use for.
 *
 * Experiments need no handling: the backend sets the winning variant as the current offering before the SDK
 * sees the response.
 */
internal fun Offerings.prewarmTargetOfferingIds(): Set<String> {
    val identifiers = linkedSetOf<String>()
    current?.identifier?.let(identifiers::add)
    placements?.let { config ->
        // A placement mapped to null means "show nothing here", so it contributes no offering.
        config.offeringIdsByPlacement.values.filterNotNullTo(identifiers)
        config.fallbackOfferingId?.let(identifiers::add)
    }
    return identifiers
}

/** The offerings [prewarmTargetOfferingIds] names, resolved in the same order. */
internal fun Offerings.prewarmTargetOfferings(): List<Offering> =
    prewarmTargetOfferingIds().mapNotNull(::getOffering)
