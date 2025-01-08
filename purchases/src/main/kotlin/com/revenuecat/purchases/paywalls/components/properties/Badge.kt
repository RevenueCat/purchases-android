package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class Badge(
    @get:JvmSynthetic val stack: StackComponent,
    @get:JvmSynthetic val style: Style,
    @get:JvmSynthetic val alignment: TwoDimensionalAlignment,
) {

    @InternalRevenueCatAPI
    @Serializable
    enum class Style {
        @SerialName("overlay")
        Overlay,

        @SerialName("edge_to_edge")
        EdgeToEdge,

        @SerialName("nested")
        Nested,
    }
}
