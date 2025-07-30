package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Deserializer for sealed classes with a default value.
 */
internal abstract class SealedDeserializerWithDefault<T : Any>(
    private val serialName: String,
    private val serializerByType: Map<String, () -> KSerializer<out T>>,
    private val defaultValue: (type: String) -> T,
    private val typeDiscriminator: String = "type",
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName) {
        element(typeDiscriminator, String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Can only deserialize $serialName from JSON, got: ${decoder::class}")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val type = jsonObject[typeDiscriminator]?.jsonPrimitive?.content
        return serializerByType[type]?.let { serializer ->
            jsonDecoder.json.decodeFromJsonElement(serializer(), jsonObject)
        } ?: defaultValue(type ?: "null")
    }

    override fun serialize(encoder: Encoder, value: T) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
