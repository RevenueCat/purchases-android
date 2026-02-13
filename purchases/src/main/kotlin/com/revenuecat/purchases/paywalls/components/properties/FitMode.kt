package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = FitModeDeserializer::class)
public enum class FitMode {
    // SerialNames are handled by the FitModeDeserializer.

    FIT,
    FILL,
}

@OptIn(InternalRevenueCatAPI::class)
internal object FitModeDeserializer : EnumDeserializerWithDefault<FitMode>(
    defaultValue = FitMode.FIT,
)
