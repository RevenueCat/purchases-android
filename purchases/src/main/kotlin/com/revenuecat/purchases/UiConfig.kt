package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizedVariableLocalizationKeyMapSerializer
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@InternalRevenueCatAPI
@Serializable
@JvmInline
value class ColorAlias(@get:JvmSynthetic val value: String)

@InternalRevenueCatAPI
@Serializable
@JvmInline
value class FontAlias(@get:JvmSynthetic val value: String)

@InternalRevenueCatAPI
@Poko
@Serializable
public class UiConfig(
    @get:JvmSynthetic
    public val app: AppConfig = AppConfig(),
    @Serializable(with = LocalizedVariableLocalizationKeyMapSerializer::class)
    @get:JvmSynthetic
    public val localizations: Map<LocaleId, Map<VariableLocalizationKey, String>> = emptyMap(),
    @SerialName("variable_config")
    @get:JvmSynthetic
    public val variableConfig: VariableConfig = VariableConfig(),
    @SerialName("custom_variables")
    @get:JvmSynthetic
    public val customVariables: Map<String, CustomVariableDefinition> = emptyMap(),
) {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    public class AppConfig(
        @get:JvmSynthetic
        val colors: Map<ColorAlias, ColorScheme> = emptyMap(),
        @get:JvmSynthetic
        val fonts: Map<FontAlias, FontsConfig> = emptyMap(),
    ) {
        @InternalRevenueCatAPI
        @Poko
        @Serializable
        public class FontsConfig(
            @get:JvmSynthetic
            val android: FontInfo,
        ) {

            @InternalRevenueCatAPI
            @Serializable
            sealed interface FontInfo {
                @InternalRevenueCatAPI
                @Poko
                @Serializable
                @SerialName("name")
                public class Name(
                    public @get:JvmSynthetic val value: String,
                    public @get:JvmSynthetic val url: String? = null,
                    public @get:JvmSynthetic val hash: String? = null,
                    public @get:JvmSynthetic val family: String? = null,
                    public @get:JvmSynthetic val weight: Int? = null,
                    public @get:JvmSynthetic val style: FontStyle? = null,
                ) : FontInfo

                @InternalRevenueCatAPI
                @Poko
                @Serializable
                @SerialName("google_fonts")
                public class GoogleFonts(@get:JvmSynthetic val value: String) : FontInfo
            }
        }
    }

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    public class VariableConfig(
        @SerialName("variable_compatibility_map")
        @get:JvmSynthetic
        val variableCompatibilityMap: Map<String, String> = emptyMap(),
        @SerialName("function_compatibility_map")
        @get:JvmSynthetic
        val functionCompatibilityMap: Map<String, String> = emptyMap(),
    )

    @InternalRevenueCatAPI
    @Poko
    @Serializable(with = CustomVariableDefinitionSerializer::class)
    public class CustomVariableDefinition(
        @get:JvmSynthetic
        val type: String,
        @get:JvmSynthetic
        val defaultValue: Any,
    )
}

/**
 * Custom serializer for [UiConfig.CustomVariableDefinition] that deserializes
 * the `default_value` field based on the `type` field.
 *
 * Supported types (matching the backend):
 * - "string" -> String
 * - "number" -> Double
 * - "boolean" -> Boolean
 *
 * Falls back to String representation for unknown types.
 */
@InternalRevenueCatAPI
internal object CustomVariableDefinitionSerializer : KSerializer<UiConfig.CustomVariableDefinition> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CustomVariableDefinition") {
        element<String>("type")
        element<String>("default_value")
    }

    override fun deserialize(decoder: Decoder): UiConfig.CustomVariableDefinition {
        require(decoder is JsonDecoder) { "CustomVariableDefinition can only be deserialized from JSON" }

        val jsonObject = decoder.decodeJsonElement().jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: "string"
        // Use safe cast to handle null, arrays, or objects gracefully - default to empty string
        val defaultValueElement = jsonObject["default_value"] as? JsonPrimitive
            ?: return UiConfig.CustomVariableDefinition(type = type, defaultValue = "")

        val defaultValue: Any = when (type) {
            "string" -> defaultValueElement.content
            "number" -> {
                defaultValueElement.doubleOrNull
                    ?: defaultValueElement.content.toDoubleOrNull()
                    ?: defaultValueElement.content
            }
            "boolean" -> {
                defaultValueElement.booleanOrNull
                    ?: defaultValueElement.content.toBooleanStrictOrNull()
                    ?: defaultValueElement.content
            }
            else -> defaultValueElement.content
        }

        return UiConfig.CustomVariableDefinition(type = type, defaultValue = defaultValue)
    }

    override fun serialize(encoder: Encoder, value: UiConfig.CustomVariableDefinition) {
        error("Serialization of CustomVariableDefinition is not implemented as it is not needed.")
    }
}
