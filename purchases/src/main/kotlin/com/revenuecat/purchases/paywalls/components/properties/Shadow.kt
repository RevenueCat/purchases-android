package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.Serializable

@Serializable
internal data class Shadow(
    val color: ColorScheme,
    val radius: Double,
    val x: Double,
    val y: Double,
)
