package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Stable
@Serializable(with = MaskShapeDeserializer::class)
sealed interface MaskShape {
    // SerialNames are handled by the MaskShapeDeserializer

    @Poko
    @Immutable
    @Serializable
    public class Rectangle(
        @get:JvmSynthetic
        val corners: CornerRadiuses? = null,
    ) : MaskShape

    @Serializable
    public object Concave : MaskShape

    @Serializable
    public object Convex : MaskShape

    @Serializable
    public object Circle : MaskShape
}

@OptIn(InternalRevenueCatAPI::class)
internal object MaskShapeDeserializer : SealedDeserializerWithDefault<MaskShape>(
    serialName = "MaskShape",
    serializerByType = mapOf(
        "rectangle" to { MaskShape.Rectangle.serializer() },
        "concave" to { MaskShape.Concave.serializer() },
        "convex" to { MaskShape.Convex.serializer() },
        "circle" to { MaskShape.Circle.serializer() },
    ),
    defaultValue = { MaskShape.Rectangle() },
)
