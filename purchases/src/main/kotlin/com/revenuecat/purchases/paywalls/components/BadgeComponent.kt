package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("badge")
class BadgeComponent(
    @get:JvmSynthetic
    @SerialName("stack")
    val stack: StackComponent,
    @get:JvmSynthetic
    @SerialName("style")
    val style: Style,
    @get:JvmSynthetic
    @SerialName("alignment")
    val alignment: TwoDimensionalAlignment,
) : PaywallComponent {

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
