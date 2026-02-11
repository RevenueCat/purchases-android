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
public class Size(
    public @get:JvmSynthetic val width: SizeConstraint,
    public @get:JvmSynthetic val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Stable
@Serializable(with = SizeConstraintDeserializer::class)
public sealed interface SizeConstraint {
    // SerialNames are handled by the SizeConstraintDeserializer.

    @Serializable
    public object Fit : SizeConstraint

    @Serializable
    public object Fill : SizeConstraint

    @Poko
    @Immutable
    @Serializable
    public class Fixed(
        public @get:JvmSynthetic val value: UInt,
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
