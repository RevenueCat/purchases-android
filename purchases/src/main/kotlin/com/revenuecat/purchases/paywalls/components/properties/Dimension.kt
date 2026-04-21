package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Stable
@Serializable
public sealed interface Dimension {

    @Immutable
    @Serializable
    @SerialName("vertical")
    public data class Vertical(
        @get:JvmSynthetic public val alignment: HorizontalAlignment,
        @get:JvmSynthetic public val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("horizontal")
    public data class Horizontal(
        @get:JvmSynthetic public val alignment: VerticalAlignment,
        @get:JvmSynthetic public val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("zlayer")
    public data class ZLayer(
        @get:JvmSynthetic public val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
