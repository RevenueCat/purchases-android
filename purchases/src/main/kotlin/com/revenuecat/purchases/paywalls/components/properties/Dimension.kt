package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
sealed interface Dimension {

    @Serializable
    @SerialName("vertical")
    data class Vertical(
        @get:JvmSynthetic val alignment: HorizontalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("horizontal")
    data class Horizontal(
        @get:JvmSynthetic val alignment: VerticalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("zlayer")
    data class ZLayer(
        @get:JvmSynthetic val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
