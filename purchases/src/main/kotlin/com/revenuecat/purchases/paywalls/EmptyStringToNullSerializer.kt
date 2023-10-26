package com.revenuecat.purchases.paywalls

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Decodes empty or blank strings as null
 */
internal object EmptyStringToNullSerializer : KSerializer<String?> {
    private val delegate = String.serializer().nullable

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "EmptyStringToNullSerializer",
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): String? {
        return delegate.deserialize(decoder)?.takeIf(String::isNotBlank)
    }

    override fun serialize(encoder: Encoder, value: String?) {
        when (value) {
            null -> encoder.encodeString("")
            else -> encoder.encodeString(value)
        }
    }
}
