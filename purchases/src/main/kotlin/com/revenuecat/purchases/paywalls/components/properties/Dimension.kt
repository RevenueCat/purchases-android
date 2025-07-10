package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
public sealed interface Dimension {

    @Serializable
    @SerialName("vertical")
    public data class Vertical(
        @get:JvmSynthetic public val alignment: HorizontalAlignment,
        @get:JvmSynthetic public val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("horizontal")
    public data class Horizontal(
        @get:JvmSynthetic public val alignment: VerticalAlignment,
        @get:JvmSynthetic public val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("zlayer")
    public data class ZLayer(
        @get:JvmSynthetic public val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
