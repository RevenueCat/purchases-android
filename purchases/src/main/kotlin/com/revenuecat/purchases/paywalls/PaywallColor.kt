package com.revenuecat.purchases.paywalls

import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale

private const val COLOR_HEX_FORMAT = "#%06X"
private const val COLOR_WHITE_HEX = 0xFFFFFF

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
    /**
     * The color converted to a @ColorInt representation
     */
    @ColorInt
    val colorInt: Int = parseRGBAColor(stringRepresentation)

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
            Color.valueOf(parseRGBAColor(stringRepresentation))
        } else {
            null
        },
    )

    constructor(@ColorInt colorInt: Int) : this(
        stringRepresentation = String.format(Locale.US, COLOR_HEX_FORMAT, COLOR_WHITE_HEX and colorInt),
        underlyingColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.valueOf(colorInt)
        } else {
            null
        },
    )

    companion object {
        private val rgbaColorRegex = Regex("^#([A-Fa-f0-9]{8})$")

        @Suppress("MagicNumber")
        @ColorInt
        private fun parseRGBAColor(stringRepresentation: String): Int {
            return if (stringRepresentation.matches(rgbaColorRegex)) {
                val radix = 16
                val r = stringRepresentation.substring(1, 3).toInt(radix)
                val g = stringRepresentation.substring(3, 5).toInt(radix)
                val b = stringRepresentation.substring(5, 7).toInt(radix)
                val a = stringRepresentation.substring(7, 9).toInt(radix)
                Color.argb(a, r, g, b)
            } else {
                Color.parseColor(stringRepresentation)
            }
        }
    }
}
