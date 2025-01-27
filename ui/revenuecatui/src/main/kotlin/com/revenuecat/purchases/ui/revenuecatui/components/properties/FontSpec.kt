@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.annotation.SuppressLint
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result

@get:JvmSynthetic
private val GoogleFontsProvider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * A `FontSpec` is a more detailed version of [FontInfo]. A [FontInfo.Name] can be resolved to a generic font
 * (e.g. "sans-serif"), a font resource provided by the app, or a device font provided by the OEM.
 *
 * Determining this is relatively costly for font resources. So this abstraction allows us to perform this logic only
 * once for each one (see [determineFontSpecs]), before resolving the actual font where needed.
 */
internal sealed interface FontSpec {
    data class Resource(@get:JvmSynthetic val id: Int) : FontSpec
    data class Google(@get:JvmSynthetic val name: String) : FontSpec
    sealed interface Generic : FontSpec {
        object SansSerif : Generic
        object Serif : Generic
        object Monospace : Generic
        object Cursive : Generic
    }

    data class Device(@get:JvmSynthetic val name: String) : FontSpec
}

@JvmSynthetic
internal fun Map<FontAlias, FontsConfig>.determineFontSpecs(
    resourceProvider: ResourceProvider,
): Map<FontAlias, FontSpec> {
    // Get unique FontsConfigs, and determine their FontSpec.
    val configToSpec: Map<FontsConfig, FontSpec> = values.toSet().associateWith { fontsConfig ->
        resourceProvider.determineFontSpec(fontsConfig.android)
    }
    // Create a map of FontAliases to FontSpecs.
    return mapValues { (_, fontsConfig) -> configToSpec.getValue(fontsConfig) }
}

@JvmSynthetic
internal fun Map<FontAlias, FontSpec>.getFontSpec(
    alias: FontAlias,
): Result<FontSpec, PaywallValidationError> =
    this[alias]
        ?.let { spec ->
            Result.Success(spec)
        } ?: Result.Error(PaywallValidationError.MissingFontAlias(alias))

@JvmSynthetic
internal fun FontSpec.resolve(
    weight: FontWeight,
    style: FontStyle,
): FontFamily = when (this) {
    is FontSpec.Resource -> FontFamily(Font(resId = id, weight = weight, style = style))
    is FontSpec.Google -> FontFamily(
        Font(googleFont = GoogleFont(name), fontProvider = GoogleFontsProvider, weight = weight, style = style),
    )

    is FontSpec.Generic -> when (this) {
        FontSpec.Generic.SansSerif -> FontFamily.SansSerif
        FontSpec.Generic.Serif -> FontFamily.Serif
        FontSpec.Generic.Monospace -> FontFamily.Monospace
        FontSpec.Generic.Cursive -> FontFamily.Cursive
    }

    is FontSpec.Device -> FontFamily(
        Font(familyName = DeviceFontFamilyName(name), weight = weight, style = style),
    )
}

private fun ResourceProvider.determineFontSpec(info: FontInfo): FontSpec =
    when (info) {
        is FontInfo.GoogleFonts -> FontSpec.Google(name = info.value)
        is FontInfo.Name -> when (info.value) {
            FontFamily.SansSerif.name -> FontSpec.Generic.SansSerif
            FontFamily.Serif.name -> FontSpec.Generic.Serif
            FontFamily.Monospace.name -> FontSpec.Generic.Monospace
            FontFamily.Cursive.name -> FontSpec.Generic.Cursive
            else -> {
                @SuppressLint("DiscouragedApi")
                val fontId = getResourceIdentifier(name = info.value, type = "font")
                if (fontId != 0) {
                    FontSpec.Resource(id = fontId)
                } else {
                    Logger.d(
                        "Could not find a font resource named `${info.value}`. Assuming it's an OEM system font. " +
                            "If it isn't, make sure the font exists in the `res/font` folder. See for more info: " +
                            "https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml",
                    )
                    FontSpec.Device(name = info.value)
                }
            }
        }
    }
