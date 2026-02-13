package com.revenuecat.purchases.paywalls

import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
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
@InternalRevenueCatAPI
@Poko
public class PaywallColor(
    /**
     * The original Hex representation for this color.
     */
    public val stringRepresentation: String,
    /**
     * The underlying `Color`.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public val underlyingColor: Color?,
) {
    /**
     * The color converted to a @ColorInt representation
     */
    @ColorInt
    public val colorInt: Int = parseRGBAColor(stringRepresentation)

    public object Serializer : KSerializer<PaywallColor> {
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
    public constructor(stringRepresentation: String) : this(
        stringRepresentation,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.valueOf(parseRGBAColor(stringRepresentation))
        } else {
            null
        },
    )

    public constructor(@ColorInt colorInt: Int) : this(
        stringRepresentation = String.format(Locale.US, COLOR_HEX_FORMAT, COLOR_WHITE_HEX and colorInt),
        underlyingColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Color.valueOf(colorInt)
        } else {
            null
        },
    )
}
