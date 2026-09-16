package com.revenuecat.purchases.paywalls.components.properties

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Poko
@Immutable
@Serializable
public class Size(
    @get:JvmSynthetic public val width: SizeConstraint,
    @get:JvmSynthetic public val height: SizeConstraint,
)

@InternalRevenueCatAPI
@Stable
@Serializable(with = SizeConstraintDeserializer::class)
public sealed interface SizeConstraint {
    // SerialNames are handled by the SizeConstraintDeserializer.

    /**
     * Fits the content size.
     *
     * @param default Optional default size (in dp) to show during loading and initial content size calculations.
     * @param min Optional minimum size in dp.
     * @param max Optional maximum size in dp.
     */
    @Poko
    @Immutable
    @Serializable
    public class Fit(
        @get:JvmSynthetic @SerialName("default") public val default: UInt? = null,
        @get:JvmSynthetic public val min: UInt? = null,
        @get:JvmSynthetic public val max: UInt? = null,
    ) : SizeConstraint

    @Poko
    @Immutable
    @Serializable
    public class Fill(
        @get:JvmSynthetic public val min: UInt? = null,
        @get:JvmSynthetic public val max: UInt? = null,
    ) : SizeConstraint

    @Poko
    @Immutable
    @Serializable
    public class Fixed(
        @get:JvmSynthetic public val value: UInt,
    ) : SizeConstraint
}

@OptIn(InternalRevenueCatAPI::class)
internal object SizeConstraintDeserializer : KSerializer<SizeConstraint> {
    private val delegate = object : SealedDeserializerWithDefault<SizeConstraint>(
        serialName = "SizeConstraint",
        serializerByType = mapOf(
            "fit" to { SizeConstraint.Fit.serializer() },
            "fill" to { SizeConstraint.Fill.serializer() },
            "fixed" to { SizeConstraint.Fixed.serializer() },
        ),
        defaultValue = { SizeConstraint.Fit() },
    ) {}
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): SizeConstraint {
        val constraint = delegate.deserialize(decoder)
        if (BuildConfig.ENABLE_PAYWALL_MIN_MAX_SIZING) return constraint
        return when (constraint) {
            is SizeConstraint.Fit -> SizeConstraint.Fit(default = constraint.default)
            is SizeConstraint.Fill -> SizeConstraint.Fill()
            is SizeConstraint.Fixed -> constraint
        }
    }
    override fun serialize(encoder: Encoder, value: SizeConstraint) = delegate.serialize(encoder, value)
}
