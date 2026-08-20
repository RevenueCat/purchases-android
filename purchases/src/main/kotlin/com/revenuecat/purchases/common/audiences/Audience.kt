package com.revenuecat.purchases.common.audiences

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class Audience(
    val id: String,
    @Serializable(with = JsonObjectStringSerializer::class)
    val rules: String,
)

internal object JsonObjectStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Audience rules can only be deserialized from JSON.")
        val element = jsonDecoder.decodeJsonElement()
        val rules = element as? JsonObject
            ?: throw SerializationException("Audience rules must be a JSON object.")
        return rules.toString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Audience rules can only be serialized to JSON.")
        val element = jsonEncoder.json.parseToJsonElement(value)
        val rules = element as? JsonObject
            ?: throw SerializationException("Audience rules must be a JSON object.")
        jsonEncoder.encodeJsonElement(rules)
    }
}
