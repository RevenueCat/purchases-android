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
public class Badge(
    @get:JvmSynthetic public val stack: StackComponent,
    @get:JvmSynthetic public val style: Style,
    @get:JvmSynthetic public val alignment: TwoDimensionalAlignment,
) {

    @InternalRevenueCatAPI
    @Serializable(with = BadgeStyleSerializer::class)
    public enum class Style {
        @SerialName("overlay")
        Overlay,

        @SerialName("edge_to_edge")
        EdgeToEdge,

        @SerialName("nested")
        Nested,
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object BadgeStyleSerializer : EnumDeserializerWithDefault<Badge.Style>(
    defaultValue = Badge.Style.Overlay,
    typeForValue = { style ->
        when (style) {
            Badge.Style.Overlay -> "overlay"
            Badge.Style.EdgeToEdge -> "edge_to_edge"
            Badge.Style.Nested -> "nested"
        }
    },
)
