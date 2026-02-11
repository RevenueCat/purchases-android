package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings

internal data class OfferingsResultData(
    public val offerings: Offerings,
    public val requestedProductIds: Set<String>,
    public val notFoundProductIds: Set<String>,
)
