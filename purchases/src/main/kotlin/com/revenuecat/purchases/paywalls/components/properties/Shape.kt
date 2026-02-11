package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Stable
@Serializable(with = ShapeDeserializer::class)
sealed interface Shape {

    public companion object {
        private val pillCornerRadiuses = CornerRadiuses.Percentage(all = 50)
    }

    // SerialNames are handled by the ShapeDeserializer

    @Serializable
    @Poko
    @Immutable
    class Rectangle(
        @get:JvmSynthetic
        public val corners: CornerRadiuses? = null,
    ) : Shape

    @Serializable
    object Pill : Shape

    public val cornerRadiuses: CornerRadiuses
        get() = when (this) {
            is Rectangle -> corners ?: CornerRadiuses.Dp.zero
            else -> pillCornerRadiuses
        }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ShapeDeserializer : SealedDeserializerWithDefault<Shape>(
    serialName = "Shape",
    serializerByType = mapOf(
        "rectangle" to { Shape.Rectangle.serializer() },
        "pill" to { Shape.Pill.serializer() },
    ),
    defaultValue = { Shape.Rectangle() },
)
