package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
internal data class Border(
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    val width: Double,
)
