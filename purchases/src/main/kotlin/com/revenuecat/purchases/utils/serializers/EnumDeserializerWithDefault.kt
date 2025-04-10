package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Deserializer for enums with a default value.
 */
internal abstract class EnumDeserializerWithDefault<T : Enum<T>>(
    private val valuesByType: Map<String, T>,
    private val defaultValue: T,
) : KSerializer<T> {

    constructor(
        defaultValue: T,
        typeForValue: (T) -> String = { value -> value.name.lowercase() },
    ) : this(
        valuesByType = defaultValue::class.java.enumConstants.associateBy(typeForValue),
        defaultValue = defaultValue,
    )

    private val enumName = defaultValue.javaClass.simpleName

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(enumName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val key = decoder.decodeString()
        return valuesByType[key] ?: defaultValue
    }

    override fun serialize(encoder: Encoder, value: T) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}
