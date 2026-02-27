package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Deserializer for sealed classes with a default value.
 *
 * Falls back to [defaultValue] when the type is unknown or deserialization of a known type fails
 * (e.g. missing or invalid fields).
 *
 * @param serialName Name used in the serial descriptor.
 * @param serializerByType Map from type discriminator values to serializer factories.
 * @param defaultValue Fallback value when the type is unknown or deserialization fails.
 * @param typeDiscriminator JSON field name used as the type discriminator.
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

    @Suppress("ReturnCount")
    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Can only deserialize $serialName from JSON, got: ${decoder::class}")
        val jsonObject = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: return defaultValue("null")
        val type = (jsonObject[typeDiscriminator] as? JsonPrimitive)?.content
        val serializer = type?.let { serializerByType[it] }
            ?: return defaultValue(type ?: "null")
        return try {
            jsonDecoder.json.decodeFromJsonElement(serializer(), jsonObject)
        } catch (_: Exception) {
            defaultValue(type)
        }
    }

    override fun serialize(encoder: Encoder, value: T) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
