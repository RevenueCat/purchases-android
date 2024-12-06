package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class Border internal constructor(
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    val width: Double,
)
