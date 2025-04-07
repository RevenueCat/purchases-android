package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.utils.serializers.EnumDeserializerWithDefault
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
    @Serializable(with = BadgeStyleSerializer::class)
    enum class Style {
        @SerialName("overlay")
        Overlay,

        @SerialName("edge_to_edge")
        EdgeToEdge,

        @SerialName("nested")
        Nested,
    }
}

@InternalRevenueCatAPI
private object BadgeStyleSerializer : EnumDeserializerWithDefault<Badge.Style>(
    valuesByType = mapOf(
        "overlay" to Badge.Style.Overlay,
        "edge_to_edge" to Badge.Style.EdgeToEdge,
        "nested" to Badge.Style.Nested,
    ),
    defaultValue = Badge.Style.Overlay,
)
