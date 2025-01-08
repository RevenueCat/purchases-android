package com.revenuecat.purchases.paywalls.components.properties

import androidx.annotation.ColorInt
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.parseRGBAColor
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Serializable
sealed interface ColorInfo {

    @Poko
    @Serializable
    @SerialName("hex")
    class Hex(
        @get:JvmSynthetic
        @Serializable(with = RgbaStringArgbColorIntDeserializer::class)
        @ColorInt
        val value: Int,
    ) : ColorInfo

    @Poko
    @Serializable
    @SerialName("alias")
    class Alias(@get:JvmSynthetic val value: String) : ColorInfo

    sealed interface Gradient : ColorInfo {

        @Poko
        @Serializable
        @SerialName("linear")
        class Linear(
            @get:JvmSynthetic val degrees: Float,
            @get:JvmSynthetic val points: List<Point>,
        ) : Gradient

        @Poko
        @Serializable
        @SerialName("radial")
        class Radial(
            @get:JvmSynthetic val points: List<Point>,
        ) : Gradient

        /**
         * A point in a gradient. Used by [Linear] and [Radial].
         */
        @Poko
        @Serializable
        class Point(
            @Serializable(with = RgbaStringArgbColorIntDeserializer::class)
            @ColorInt
            @get:JvmSynthetic val color: Int,
            /**
             * A percentage value in the range 0..100.
             */
            @get:JvmSynthetic val percent: Float,
        )
    }
}

@InternalRevenueCatAPI
@Poko
@Serializable
class ColorScheme(
    @get:JvmSynthetic val light: ColorInfo,
    @get:JvmSynthetic val dark: ColorInfo? = null,
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
