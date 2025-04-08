package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable(with = MaskShapeDeserializer::class)
sealed interface MaskShape {

    @Poko
    @Serializable
    @SerialName("rectangle")
    class Rectangle(
        @get:JvmSynthetic
        val corners: CornerRadiuses? = null,
    ) : MaskShape

    @Serializable
    @SerialName("concave")
    object Concave : MaskShape

    @Serializable
    @SerialName("convex")
    object Convex : MaskShape

    @Serializable
    @SerialName("circle")
    object Circle : MaskShape
}

@OptIn(InternalRevenueCatAPI::class)
private object MaskShapeDeserializer : SealedDeserializerWithDefault<MaskShape>(
    serialName = "MaskShape",
    serializerByType = mapOf(
        "rectangle" to { MaskShape.Rectangle.serializer() },
        "concave" to { MaskShape.Concave.serializer() },
        "convex" to { MaskShape.Convex.serializer() },
        "circle" to { MaskShape.Circle.serializer() },
    ),
    defaultValue = { MaskShape.Rectangle() },
)
