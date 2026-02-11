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
    public data class Vertical(
        public @get:JvmSynthetic val alignment: HorizontalAlignment,
        public @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("horizontal")
    public data class Horizontal(
        public @get:JvmSynthetic val alignment: VerticalAlignment,
        public @get:JvmSynthetic val distribution: FlexDistribution,
    ) : Dimension

    @Immutable
    @Serializable
    @SerialName("zlayer")
    public data class ZLayer(
        public @get:JvmSynthetic val alignment: TwoDimensionalAlignment,
    ) : Dimension
}
