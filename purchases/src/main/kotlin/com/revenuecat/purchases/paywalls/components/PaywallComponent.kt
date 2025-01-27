package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@InternalRevenueCatAPI
@Serializable(with = PaywallComponentSerializer::class)
sealed interface PaywallComponent

@InternalRevenueCatAPI
internal class PaywallComponentSerializer : KSerializer<PaywallComponent> {
    // We're only describing the type field, while it will have many more. This works for now, but for a proper
    // implementation we can look at SealedClassSerializer. Unfortunately that's currently annotated with
    // @InternalSerializationApi.
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PaywallComponent") {
        element("type", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: PaywallComponent) {
        // Serialization is not implemented as it is not needed.
    }

    @Suppress("CyclomaticComplexMethod")
    override fun deserialize(decoder: Decoder): PaywallComponent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Can only deserialize PaywallComponent from JSON, got: ${decoder::class}")
        val json = jsonDecoder.decodeJsonElement().jsonObject
        return when (val type = json["type"]?.jsonPrimitive?.content) {
            "button" -> jsonDecoder.json.decodeFromString<ButtonComponent>(json.toString())
            "image" -> jsonDecoder.json.decodeFromString<ImageComponent>(json.toString())
            "package" -> jsonDecoder.json.decodeFromString<PackageComponent>(json.toString())
            "purchase_button" -> jsonDecoder.json.decodeFromString<PurchaseButtonComponent>(json.toString())
            "stack" -> jsonDecoder.json.decodeFromString<StackComponent>(json.toString())
            "sticky_footer" -> jsonDecoder.json.decodeFromString<StickyFooterComponent>(json.toString())
            "text" -> jsonDecoder.json.decodeFromString<TextComponent>(json.toString())
            "icon" -> jsonDecoder.json.decodeFromString<IconComponent>(json.toString())
            "timeline" -> jsonDecoder.json.decodeFromString<TimelineComponent>(json.toString())
            else -> json["fallback"]
                ?.let { it as? JsonObject }
                ?.toString()
                ?.let { jsonDecoder.json.decodeFromString<PaywallComponent>(it) }
                ?: throw SerializationException("No fallback provided for unknown type: $type")
        }
    }
}
