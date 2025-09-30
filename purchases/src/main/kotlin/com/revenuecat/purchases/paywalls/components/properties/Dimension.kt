package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Stable
@Serializable
sealed interface Dimension {

    @Immutable
    @Serializable
    @SerialName("vertical")
    data class Vertical(
        @get:JvmSynthetic val alignment: HorizontalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("horizontal")
    data class Horizontal(
        @get:JvmSynthetic val alignment: VerticalAlignment,
        @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("zlayer")
    data class ZLayer(
        @get:JvmSynthetic val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
