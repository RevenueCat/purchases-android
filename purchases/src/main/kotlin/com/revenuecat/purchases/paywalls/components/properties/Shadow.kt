package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class Shadow(
    val color: ColorScheme,
    val radius: Double,
    val x: Double,
    val y: Double,
)
