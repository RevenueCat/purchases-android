package com.revenuecat.purchases.utils.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.MalformedURLException
import java.net.URL

internal object URLSerializer : KSerializer<URL> {
    override val descriptor = PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URL {
        return URL(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }
}

/**
 * Equivalent to URLSerializer but allows empty or invalid URLs.
 */
internal object OptionalURLSerializer : KSerializer<URL?> {
    private val delegate = URLSerializer.nullable
    override val descriptor = PrimitiveSerialDescriptor("URL?", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URL? {
        return try {
            delegate.deserialize(decoder)
        } catch (e: MalformedURLException) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: URL?) {
        when (value) {
            null -> encoder.encodeString("")
            else -> delegate.serialize(encoder, value)
        }
    }
}
