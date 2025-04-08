package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = FlexDistributionSerializer::class)
enum class FlexDistribution {
    // SerialNames are handled by the FlexDistributionSerializer.

    START,
    END,
    CENTER,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
}

@OptIn(InternalRevenueCatAPI::class)
private object FlexDistributionSerializer : EnumDeserializerWithDefault<FlexDistribution>(
    defaultValue = FlexDistribution.START,
)
