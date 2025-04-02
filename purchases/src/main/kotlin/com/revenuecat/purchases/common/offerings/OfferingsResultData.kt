package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.Offerings

internal data class OfferingsResultData(
    val offerings: Offerings,
    val requestedProductIds: Set<String>,
    val notFoundProductIds: Set<String>,
)
