package com.revenuecat.purchases.paywalls.components.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Dimension {

    @Serializable
    @SerialName("vertical")
    data class Vertical(
        val alignment: HorizontalAlignment,
        val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("horizontal")
    data class Horizontal(
        val alignment: VerticalAlignment,
        val distribution: FlexDistribution,
    ) : Dimension

    @Serializable
    @SerialName("zlayer")
    data class ZLayer(
        val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
