package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal object JsonObjectToMapSerializer : KSerializer<Map<String, Any>> {

    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Only JSON decoding is supported")
        val element = jsonDecoder.decodeJsonElement() as? JsonObject ?: return emptyMap()
        return element.toAnyMap()
    }

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        error("Serialization of Map<String, Any> is not supported")
    }
}

private fun JsonObject.toAnyMap(): Map<String, Any> =
    entries.mapNotNull { (key, value) ->
        val mapped = value.toAny() ?: return@mapNotNull null
        key to mapped
    }.toMap()

private fun JsonElement.toAny(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull!!
        intOrNull != null -> intOrNull!!
        longOrNull != null -> longOrNull!!
        doubleOrNull != null -> doubleOrNull!!
        else -> content
    }
    is JsonObject -> toAnyMap()
    is JsonArray -> mapNotNull { it.toAny() }
}
