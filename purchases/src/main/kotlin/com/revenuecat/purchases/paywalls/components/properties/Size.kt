package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Immutable
@Serializable
class Size(
    @get:JvmSynthetic val width: SizeConstraint,
    @get:JvmSynthetic val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Stable
@Serializable(with = SizeConstraintDeserializer::class)
sealed interface SizeConstraint {
    // SerialNames are handled by the SizeConstraintDeserializer.

    @Serializable
    object Fit : SizeConstraint

    @Serializable
    object Fill : SizeConstraint

    @Poko
    @Immutable
    @Serializable
    class Fixed(
        @get:JvmSynthetic val value: UInt,
    ) : SizeConstraint
}

@OptIn(InternalRevenueCatAPI::class)
internal object SizeConstraintDeserializer : SealedDeserializerWithDefault<SizeConstraint>(
    serialName = "SizeConstraint",
    serializerByType = mapOf(
        "fit" to { SizeConstraint.Fit.serializer() },
        "fill" to { SizeConstraint.Fill.serializer() },
        "fixed" to { SizeConstraint.Fixed.serializer() },
    ),
    defaultValue = { SizeConstraint.Fit },
)
