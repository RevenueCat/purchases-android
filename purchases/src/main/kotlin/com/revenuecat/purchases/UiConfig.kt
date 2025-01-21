package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizedVariableLocalizationKeyMapSerializer
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
class UiConfig(
    @get:JvmSynthetic
    val app: AppConfig = AppConfig(),
    @Serializable(with = LocalizedVariableLocalizationKeyMapSerializer::class)
    @get:JvmSynthetic
    val localizations: Map<LocaleId, Map<VariableLocalizationKey, String>> = emptyMap(),
    @SerialName("variable_config")
    @get:JvmSynthetic
    val variableConfig: VariableConfig = VariableConfig(),
) {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    class AppConfig(
        @get:JvmSynthetic
        val colors: Map<ColorAlias, ColorScheme> = emptyMap(),
        @get:JvmSynthetic
        val fonts: Map<FontAlias, FontsConfig> = emptyMap(),
    ) {
        @InternalRevenueCatAPI
        @Poko
        @Serializable
        class FontsConfig(
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
                class Name(@get:JvmSynthetic val value: String) : FontInfo

                @InternalRevenueCatAPI
                @Poko
                @Serializable
                @SerialName("google_fonts")
                class GoogleFonts(@get:JvmSynthetic val value: String) : FontInfo
            }
        }
    }

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    class VariableConfig(
        @SerialName("variable_compatibility_map")
        @get:JvmSynthetic
        val variableCompatibilityMap: Map<String, String> = emptyMap(),
        @SerialName("function_compatibility_map")
        @get:JvmSynthetic
        val functionCompatibilityMap: Map<String, String> = emptyMap(),
    )
}
