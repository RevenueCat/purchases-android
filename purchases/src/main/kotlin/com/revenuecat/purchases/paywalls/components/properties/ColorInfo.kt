package com.revenuecat.purchases.paywalls.components.properties

import androidx.annotation.ColorInt
import com.revenuecat.purchases.paywalls.parseRGBAColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
internal sealed interface ColorInfo {

    @Serializable
    @SerialName("hex")
    data class Hex(
        @Serializable(with = RgbaStringArgbColorIntDeserializer::class)
        @ColorInt
        val value: Int,
    ) : ColorInfo

    @Serializable
    @SerialName("alias")
    data class Alias(val value: String) : ColorInfo

    sealed interface Gradient : ColorInfo {

        @Serializable
        @SerialName("linear")
        data class Linear(
            val degrees: Float,
            val points: List<Point>,
        ) : Gradient

        @Serializable
        @SerialName("radial")
        data class Radial(
            val points: List<Point>,
        ) : Gradient

        /**
         * A point in a gradient. Used by [Linear] and [Radial].
         */
        @Serializable
        data class Point(
            @Serializable(with = RgbaStringArgbColorIntDeserializer::class)
            @ColorInt
            val color: Int,
            val percent: Float,
        )
    }
}

@Serializable
internal data class ColorScheme(
    val light: ColorInfo,
    val dark: ColorInfo? = null,
)

/**
 * A serializer that deserializes an RGBA string into an ARGB ColorInt.
 */
private object RgbaStringArgbColorIntDeserializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, @ColorInt value: Int) {
        error("Serialization is not implemented as it is not (yet) needed.")
    }

    @ColorInt
    override fun deserialize(decoder: Decoder): Int {
        val string = decoder.decodeString()
        return parseRGBAColor(string)
    }
}
