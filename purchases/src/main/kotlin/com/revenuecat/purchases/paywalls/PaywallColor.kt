package com.revenuecat.purchases.paywalls

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a color to be used by `RevenueCatUI`.
 */
data class PaywallColor(
    /**
     * The original Hex representation for this color.
     */
    val stringRepresentation: String,
    /**
     * The underlying `Color`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    val underlyingColor: Color?,
) {
    object Serializer : KSerializer<PaywallColor> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PaywallColor", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PaywallColor {
            return PaywallColor(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: PaywallColor) {
            encoder.encodeString(value.toString())
        }
    }

    /**
     * Creates a color from a Hex string: `#RRGGBB` or `#RRGGBBAA`.
     */
    constructor(stringRepresentation: String) : this(
        stringRepresentation,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.valueOf(Color.parseColor(stringRepresentation))
        } else {
            null
        },
    )
}

/**
 * Represents a color scheme.
 */
enum class ColorScheme {
    LIGHT, DARK
}
