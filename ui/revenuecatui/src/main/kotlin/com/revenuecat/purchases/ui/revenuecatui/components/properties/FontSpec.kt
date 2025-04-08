@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.content.res.AssetManager
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
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMapError

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
 * once for each one (see [determineFontSpecs]) in the validation step, before resolving the actual font where needed.
 *
 * It also allows us to defer resolving the actual font to the UI layer, as only at that time do we know the exact
 * override that's being used. We need to know this, because we need to know the [FontWeight] for which to resolve the
 * font.
 */
internal sealed interface FontSpec {
    data class Resource(@get:JvmSynthetic val id: Int) : FontSpec
    data class Asset(@get:JvmSynthetic val path: String) : FontSpec
    data class Google(@get:JvmSynthetic val name: String) : FontSpec
    sealed interface Generic : FontSpec {
        object SansSerif : Generic
        object Serif : Generic
        object Monospace : Generic
    }

    data class System(@get:JvmSynthetic val name: String) : FontSpec
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

/**
 * Retrieves a [FontSpec] from this map, and returns a [PaywallValidationError.MissingFontAlias] if it doesn't exist.
 * If you want to treat blank [FontAlias]es as a null [FontSpec], chain it with [recoverFromBlankFontAlias].
 */
@JvmSynthetic
internal fun Map<FontAlias, FontSpec>.getFontSpec(
    alias: FontAlias,
): Result<FontSpec, PaywallValidationError> =
    this[alias]
        ?.let { spec ->
            Result.Success(spec)
        } ?: Result.Error(PaywallValidationError.MissingFontAlias(alias))

/**
 * Returns a successful Result containing `null` if this is an error Result caused by a blank [FontAlias]. This
 * scenario should not happen, as our dashboard should not allow devs to publish a paywall containing a blank Font
 * Family field, but this is defensive in case it does happen.
 */
@Suppress("MaxLineLength")
@JvmSynthetic
internal fun Result<FontSpec, PaywallValidationError>.recoverFromBlankFontAlias(): Result<FontSpec?, PaywallValidationError> =
    flatMapError { error ->
        if (error is PaywallValidationError.MissingFontAlias && error.alias.value.isBlank()) {
            Result.Success(null)
        } else {
            Result.Error(error)
        }
    }

@JvmSynthetic
internal fun FontSpec.resolve(
    assets: AssetManager,
    weight: FontWeight,
    style: FontStyle,
): FontFamily = when (this) {
    is FontSpec.Resource -> FontFamily(Font(resId = id, weight = weight, style = style))
    is FontSpec.Asset -> FontFamily(Font(path = path, assetManager = assets, weight = weight, style = style))
    is FontSpec.Google -> FontFamily(
        Font(googleFont = GoogleFont(name), fontProvider = GoogleFontsProvider, weight = weight, style = style),
    )

    is FontSpec.Generic -> when (this) {
        FontSpec.Generic.SansSerif -> FontFamily.SansSerif
        FontSpec.Generic.Serif -> FontFamily.Serif
        FontSpec.Generic.Monospace -> FontFamily.Monospace
    }

    is FontSpec.System -> FontFamily(
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
            else -> getResourceIdentifier(name = info.value, type = "font")
                .takeUnless { it == 0 }
                ?.let { fontId -> FontSpec.Resource(id = fontId) }
                ?: getAssetFontPath(name = info.value)
                    ?.let { path -> FontSpec.Asset(path = path) }
                ?: FontSpec.System(name = info.value).also {
                    Logger.d(
                        "Could not find a font resource named `${info.value}`. Assuming it's an OEM system font. " +
                            "If it isn't, make sure the font exists in the `res/font` folder. See for more info: " +
                            "https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml",
                    )
                }
        }
    }
