package com.revenuecat.purchases.paywalls.components.properties

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class Size(
    @get:JvmSynthetic val width: SizeConstraint,
    @get:JvmSynthetic val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Serializable(with = SizeConstraintDeserializer::class)
sealed interface SizeConstraint {
    // SerialNames are handled by the SizeConstraintDeserializer.

    @Serializable
    object Fit : SizeConstraint

    @Serializable
    object Fill : SizeConstraint

    @Poko
    @Serializable
    class Fixed(
        @get:JvmSynthetic val value: UInt,
    ) : SizeConstraint
}

@OptIn(InternalRevenueCatAPI::class)
private object SizeConstraintDeserializer : SealedDeserializerWithDefault<SizeConstraint>(
    serialName = "SizeConstraint",
    serializerByType = mapOf(
        "fit" to { SizeConstraint.Fit.serializer() },
        "fill" to { SizeConstraint.Fill.serializer() },
        "fixed" to { SizeConstraint.Fixed.serializer() },
    ),
    defaultValue = { SizeConstraint.Fit },
)
