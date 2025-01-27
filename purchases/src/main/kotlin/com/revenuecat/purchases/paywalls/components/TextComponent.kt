package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.FontWeight.REGULAR
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.CENTER
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Padding.Companion.zero
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("text")
class TextComponent
@Suppress("LongParameterList")
constructor(
    @get:JvmSynthetic
    @SerialName("text_lid")
    val text: LocalizationKey,
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("font_name")
    val fontName: String? = null,
    @get:JvmSynthetic
    @SerialName("font_weight")
    val fontWeight: FontWeight = REGULAR,
    @get:JvmSynthetic
    @SerialName("font_size")
    @Serializable(with = FontSizeSerializer::class)
    val fontSize: Int = 15,
    @get:JvmSynthetic
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment = CENTER,
    @get:JvmSynthetic
    val size: Size = Size(width = Fill, height = Fit),
    @get:JvmSynthetic
    val padding: Padding = zero,
    @get:JvmSynthetic
    val margin: Padding = zero,
    @get:JvmSynthetic
    val overrides: ComponentOverrides<PartialTextComponent>? = null,
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
class PartialTextComponent
@Suppress("LongParameterList")
constructor(
    @get:JvmSynthetic
    val visible: Boolean? = true,
    @get:JvmSynthetic
    @SerialName("text_lid")
    val text: LocalizationKey? = null,
    @get:JvmSynthetic
    val color: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("background_color")
    val backgroundColor: ColorScheme? = null,
    @get:JvmSynthetic
    @SerialName("font_name")
    val fontName: String? = null,
    @get:JvmSynthetic
    @SerialName("font_weight")
    val fontWeight: FontWeight? = null,
    @get:JvmSynthetic
    @SerialName("font_size")
    @Serializable(with = FontSizeSerializer::class)
    val fontSize: Int? = null,
    @get:JvmSynthetic
    @SerialName("horizontal_alignment")
    val horizontalAlignment: HorizontalAlignment? = null,
    @get:JvmSynthetic
    val size: Size? = null,
    @get:JvmSynthetic
    val padding: Padding? = null,
    @get:JvmSynthetic
    val margin: Padding? = null,
) : PartialComponent

/**
 * A serializer that can deserialize a font size, whether it is serialized as an integer or as the FontSize enum.
 *
 * Remove after 2025-03-01 when we are sure no more paywalls are using this.
 */
private object FontSizeSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName = "FontSize", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        error("Serialization is not implemented as it is not (yet) needed.")
    }

    @Suppress("MagicNumber", "CyclomaticComplexMethod")
    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Expected font_size to be part of a JSON object")
        val element = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException("Expected font_size to be a JsonPrimitive")
        return if (element.isString) {
            when (val fontSizeString = element.content) {
                "heading_xxl" -> 40
                "heading_xl" -> 34
                "heading_l" -> 28
                "heading_m" -> 24
                "heading_s" -> 20
                "heading_xs" -> 16
                "body_xl" -> 18
                "body_l" -> 17
                "body_m" -> 15
                "body_s" -> 13
                else -> throw SerializationException("Unknown font size name: $fontSizeString")
            }
        } else {
            element.int
        }
    }
}
