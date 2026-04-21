package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = FlexDistributionDeserializer::class)
public enum class FlexDistribution {
    // SerialNames are handled by the FlexDistributionDeserializer.

    START,
    END,
    CENTER,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
}

@OptIn(InternalRevenueCatAPI::class)
internal object FlexDistributionDeserializer : EnumDeserializerWithDefault<FlexDistribution>(
    defaultValue = FlexDistribution.START,
)
