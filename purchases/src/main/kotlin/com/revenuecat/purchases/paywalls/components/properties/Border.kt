package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.Serializable

@Serializable
internal data class Border(
    val color: ColorScheme,
    val width: Double,
)
