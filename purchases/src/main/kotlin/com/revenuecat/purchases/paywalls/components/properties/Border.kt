package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class Border(
    val color: ColorScheme,
    val width: Double,
)
