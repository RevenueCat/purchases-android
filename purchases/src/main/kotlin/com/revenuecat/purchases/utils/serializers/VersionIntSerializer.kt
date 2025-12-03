package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Serializer that converts version strings like "3.2.0" to integers like 320 by keeping only digits.
 * Also handles integer inputs directly for backward compatibility.
 */
internal object VersionIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("VersionInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int = (decoder as? JsonDecoder)?.let { jsonDecoder ->
        val element = jsonDecoder.decodeJsonElement().jsonPrimitive

        // If it's already an integer, return it directly
        // Otherwise, treat it as a string and filter to digits only
        element.intOrNull?.let { it }
            ?: element.content.filter { it.isDigit() }.toIntOrNull()
            ?: 0
    } ?: decoder.decodeInt()

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}
