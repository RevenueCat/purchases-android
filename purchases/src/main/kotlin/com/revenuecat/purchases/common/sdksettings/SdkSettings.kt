package com.revenuecat.purchases.common.sdksettings

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.common.errorLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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
            } catch (e: SerializationException) {
                errorLog(e) { "Ignoring the '$key' SDK setting: it could not be parsed." }
                null
            } catch (e: IllegalArgumentException) {
                errorLog(e) { "Ignoring the '$key' SDK setting: it could not be parsed." }
                null
            }
        }
    }
}

@Serializable
internal data class DiagnosticsSettings(
    val enabled: Boolean? = null,
)
