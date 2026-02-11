package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Immutable
@Serializable
public class Shadow(
    public @get:JvmSynthetic val color: ColorScheme,
    public @get:JvmSynthetic val radius: Double,
    public @get:JvmSynthetic val x: Double,
    public @get:JvmSynthetic val y: Double,
)
