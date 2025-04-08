package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = DimensionDeserializer::class)
sealed interface Dimension {
    // SerialNames are handled by the DimensionDeserializer

    @Serializable
    data class Vertical(
        @get:JvmSynthetic val alignment: HorizontalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    data class Horizontal(
        @get:JvmSynthetic val alignment: VerticalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    data class ZLayer(
        @get:JvmSynthetic val alignment: TwoDimensionalAlignment,
    ) : Dimension
}

@OptIn(InternalRevenueCatAPI::class)
internal object DimensionDeserializer : SealedDeserializerWithDefault<Dimension>(
    serialName = "Dimension",
    serializerByType = mapOf(
        "vertical" to Dimension.Vertical::serializer,
        "horizontal" to Dimension.Horizontal::serializer,
        "zlayer" to Dimension.ZLayer::serializer,
    ),
    defaultValue = {
        Dimension.Vertical(
            alignment = HorizontalAlignment.LEADING,
            distribution = FlexDistribution.START,
        )
    },
)
