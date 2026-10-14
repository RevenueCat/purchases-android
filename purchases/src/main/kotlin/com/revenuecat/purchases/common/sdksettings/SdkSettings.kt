package com.revenuecat.purchases.common.sdksettings

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * The SDK-specific settings served through the `sdk_settings` remote config topic's inline `default` item.
 * Each setting is `null` when the backend did not send it, so a consumer can fall back to its build-time value.
 */
internal data class SdkSettings(
    val diagnostics: DiagnosticsSettings? = null,
) {
    companion object {
        val DEFAULT = SdkSettings()

        /**
         * Every section is decoded on its own: one that does not parse is dropped (and logged) rather than
         * failing the whole object, so a shape change in one setting cannot make every other setting unreadable.
         * Unknown sections are ignored.
         */
        fun parse(item: JsonObject): SdkSettings = SdkSettings(
            diagnostics = item.section("diagnostics"),
        )

        private inline fun <reified T> JsonObject.section(key: String): T? {
            val element = this[key] ?: return null
            return try {
                JsonTools.json.decodeFromJsonElement<T>(element)
            } catch (e: IllegalArgumentException) {
                errorLog(e) { "Ignoring the '$key' SDK setting: it could not be parsed." }
                null
            }
        }
    }
}

@Serializable
internal data class DiagnosticsSettings(
    /**
     * The remotely configured diagnostics preference.
     *
     * This value is set only at the developer's request or with their explicit approval. When present, it takes
     * precedence over [PurchasesConfiguration.Builder.diagnosticsEnabled]. `null` leaves the local configuration
     * unchanged.
     */
    @Serializable(with = StrictBooleanSerializer::class)
    val enabled: Boolean? = null,
)

/**
 * kotlinx-serialization 1.7 decodes a quoted `"true"` as a boolean even when the [Json] is not lenient.
 */
private object StrictBooleanSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("StrictBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val primitive = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive
        return primitive.takeUnless { it.isString }?.booleanOrNull
            ?: throw SerializationException("Expected a boolean, got $primitive")
    }

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
}
