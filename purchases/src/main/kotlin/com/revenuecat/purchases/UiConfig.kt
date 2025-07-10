package com.revenuecat.purchases

import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizedVariableLocalizationKeyMapSerializer
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontStyle
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
@JvmInline
public value class ColorAlias(@get:JvmSynthetic public val value: String)

@InternalRevenueCatAPI
@Serializable
@JvmInline
public value class FontAlias(@get:JvmSynthetic public val value: String)

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
) {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    public class AppConfig(
        @get:JvmSynthetic
        public val colors: Map<ColorAlias, ColorScheme> = emptyMap(),
        @get:JvmSynthetic
        public val fonts: Map<FontAlias, FontsConfig> = emptyMap(),
    ) {
        @InternalRevenueCatAPI
        @Poko
        @Serializable
        public class FontsConfig(
            @get:JvmSynthetic
            public val android: FontInfo,
        ) {

            @InternalRevenueCatAPI
            @Serializable
            public sealed interface FontInfo {
                @InternalRevenueCatAPI
                @Poko
                @Serializable
                @SerialName("name")
                public class Name(
                    @get:JvmSynthetic public val value: String,
                    @get:JvmSynthetic public val url: String? = null,
                    @get:JvmSynthetic public val hash: String? = null,
                    @get:JvmSynthetic public val family: String? = null,
                    @get:JvmSynthetic public val weight: Int? = null,
                    @get:JvmSynthetic public val style: FontStyle? = null,
                ) : FontInfo

                @InternalRevenueCatAPI
                @Poko
                @Serializable
                @SerialName("google_fonts")
                public class GoogleFonts(@get:JvmSynthetic public val value: String) : FontInfo
            }
        }
    }

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    public class VariableConfig(
        @SerialName("variable_compatibility_map")
        @get:JvmSynthetic
        public val variableCompatibilityMap: Map<String, String> = emptyMap(),
        @SerialName("function_compatibility_map")
        @get:JvmSynthetic
        public val functionCompatibilityMap: Map<String, String> = emptyMap(),
    )
}
