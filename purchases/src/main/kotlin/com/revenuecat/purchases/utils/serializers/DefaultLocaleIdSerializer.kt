package com.revenuecat.purchases.utils.serializers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodes a paywall's `default_locale`, falling back to [FALLBACK] when the backend sends `null`.
 *
 * Pair this with a property default so a *missing* key is tolerated too; this serializer is only
 * reached when the key is present.
 */
@OptIn(InternalRevenueCatAPI::class)
internal object DefaultLocaleIdSerializer : KSerializer<LocaleId> {

    val FALLBACK = LocaleId("en")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DefaultLocaleId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocaleId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): LocaleId {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer can be used only with JSON format")
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return FALLBACK
        return LocaleId(element.jsonPrimitive.content)
    }
}
