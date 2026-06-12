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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@InternalRevenueCatAPI
@Serializable(with = PaywallComponentSerializer::class)
public sealed interface PaywallComponent

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
        // Decode the JsonElement directly; re-stringifying (decodeFromString(json.toString())) is ~quadratic in
        // tree depth, as every nested PaywallComponent would re-stringify its whole subtree.
        return when (val type = json["type"]?.jsonPrimitive?.content) {
            "button" -> jsonDecoder.json.decodeFromJsonElement<ButtonComponent>(json)
            "image" -> jsonDecoder.json.decodeFromJsonElement<ImageComponent>(json)
            "package" -> jsonDecoder.json.decodeFromJsonElement<PackageComponent>(json)
            "purchase_button" -> jsonDecoder.json.decodeFromJsonElement<PurchaseButtonComponent>(json)
            "stack" -> jsonDecoder.json.decodeFromJsonElement<StackComponent>(json)
            "header" -> jsonDecoder.json.decodeFromJsonElement<HeaderComponent>(json)
            "sticky_footer" -> jsonDecoder.json.decodeFromJsonElement<StickyFooterComponent>(json)
            "text" -> jsonDecoder.json.decodeFromJsonElement<TextComponent>(json)
            "icon" -> jsonDecoder.json.decodeFromJsonElement<IconComponent>(json)
            "timeline" -> jsonDecoder.json.decodeFromJsonElement<TimelineComponent>(json)
            "carousel" -> jsonDecoder.json.decodeFromJsonElement<CarouselComponent>(json)
            "tab_control_button" -> jsonDecoder.json.decodeFromJsonElement<TabControlButtonComponent>(json)
            "tab_control_toggle" -> jsonDecoder.json.decodeFromJsonElement<TabControlToggleComponent>(json)
            "tab_control" -> jsonDecoder.json.decodeFromJsonElement<TabControlComponent>(json)
            "tabs" -> jsonDecoder.json.decodeFromJsonElement<TabsComponent>(json)
            "video" -> jsonDecoder.json.decodeFromJsonElement<VideoComponent>(json)
            "countdown" -> jsonDecoder.json.decodeFromJsonElement<CountdownComponent>(json)
            "fallback_header" -> FallbackHeaderComponent
            else -> json["fallback"]
                ?.let { it as? JsonObject }
                ?.let { jsonDecoder.json.decodeFromJsonElement<PaywallComponent>(it) }
                ?: throw SerializationException("No fallback provided for unknown type: $type")
        }
    }
}
