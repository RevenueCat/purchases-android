package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object GoogleListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GoogleList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<String>) {
        throw UnsupportedOperationException("Serialization is not supported")
    }

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer can be used only with JSON format")
        val jsonElement = jsonDecoder.decodeJsonElement().jsonObject
        val googleList = jsonElement["google"]?.jsonArray
        return googleList?.map { it.jsonPrimitive.content } ?: emptyList()
    }
}
