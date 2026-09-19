@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

/**
 * The offerings a customer could be served next: with placements that is one per placement plus the fallback,
 * not just [Offerings.current]. All of it arrives in the same response, so this costs no network.
 *
 * Resolved against [Offerings.all], not [Offerings.getCurrentOfferingForPlacement], which allocates a
 * presented-context copy. Experiments need no handling: the backend makes the winning variant current.
 */
internal fun Offerings.prewarmTargetOfferingIds(): Set<String> {
    val identifiers = linkedSetOf<String>()
    current?.identifier?.let(identifiers::add)
    placements?.let { config ->
        // A placement mapped to null means "show nothing here", so it contributes no offering.
        config.offeringIdsByPlacement.toSortedMap().values.filterNotNullTo(identifiers)
        config.fallbackOfferingId?.let(identifiers::add)
    }
    return identifiers
}

internal fun Offerings.prewarmTargetOfferings(): List<Offering> =
    prewarmTargetOfferingIds().mapNotNull(::getOffering)
