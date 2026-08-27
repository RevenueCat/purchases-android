package com.revenuecat.purchases.utils

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings

/** An [Offerings] whose `all` map is derived from [current] + [others], the way a real response's is. */
internal fun offeringsWith(
    current: Offering?,
    others: List<Offering> = emptyList(),
    placements: Offerings.Placements? = null,
): Offerings = Offerings(
    current = current,
    all = (listOfNotNull(current) + others).associateBy { it.identifier },
    placements = placements,
    targeting = null,
)
