package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.reflect.KClass

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
open class EnumAsObjectSerializer<T : Enum<T>>(
    private val enumClass: KClass<T>,
    private val defaultValue: T,
    private val keyName: String,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EnumAsObjectSerializer") {
        element<String>(keyName)
    }

    override fun serialize(encoder: Encoder, value: T) {
        val obj = buildJsonObject {
            put(keyName, value.name.lowercase())
        }
        encoder.encodeSerializableValue(JsonObject.serializer(), obj)
    }

    override fun deserialize(decoder: Decoder): T {
        val obj = decoder.decodeSerializableValue(JsonObject.serializer())
        val optionName = obj[keyName]?.jsonPrimitive?.content ?: defaultValue.name.lowercase()
        return enumClass.java.enumConstants.firstOrNull {
            it.name.equals(optionName, ignoreCase = true)
        } ?: defaultValue
    }
}
