package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class Border(
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    val width: Double,
)
