package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class Shadow(
    @get:JvmSynthetic val color: ColorScheme,
    @get:JvmSynthetic val radius: Double,
    @get:JvmSynthetic val x: Double,
    @get:JvmSynthetic val y: Double,
)
