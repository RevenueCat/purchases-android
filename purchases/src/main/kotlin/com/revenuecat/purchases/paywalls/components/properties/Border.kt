package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
public class Border(
    @get:JvmSynthetic
    public val color: ColorScheme,
    @get:JvmSynthetic
    public val width: Double,
)
