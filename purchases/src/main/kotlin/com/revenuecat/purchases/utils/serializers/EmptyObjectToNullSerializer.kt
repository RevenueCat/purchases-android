package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

/**
 * Deserializes a JSON object to [T], returning null when the object is empty.
 * When [resilient] is true (the default), deserialization errors also return null
 * instead of throwing.
 */
internal abstract class EmptyObjectToNullSerializer<T : Any>(
    private val delegate: KSerializer<T>,
    private val resilient: Boolean = true,
) : KSerializer<T?> {

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): T? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return delegate.deserialize(decoder)
        val element = jsonDecoder.decodeJsonElement()
        return when {
            element !is JsonObject || element.isEmpty() -> null
            resilient -> try {
                jsonDecoder.json.decodeFromJsonElement(delegate, element)
            } catch (_: SerializationException) {
                null
            }
            else -> jsonDecoder.json.decodeFromJsonElement(delegate, element)
        }
    }

    override fun serialize(encoder: Encoder, value: T?) {
        if (value != null) {
            delegate.serialize(encoder, value)
        }
    }
}
