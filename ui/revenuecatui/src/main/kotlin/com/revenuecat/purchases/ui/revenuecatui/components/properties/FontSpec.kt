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
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMapError
import java.io.File

@get:JvmSynthetic
private val GoogleFontsProvider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

internal data class ResourceFontSpec(
    val id: Int,
    val weight: Int?,
    val style: FontStyle?,
)

/**
 * A `FontSpec` is a more detailed version of [FontInfo]. A [FontInfo.Name] can be resolved to a generic font
 * (e.g. "sans-serif"), a font resource provided by the app, or a device font provided by the OEM.
 *
 * Determining this is relatively costly for font resources. So this abstraction allows us to perform this logic only
 * once for each one (see [determineFontSpecs]) in the validation step.
 */
internal sealed interface FontSpec {
    data class Resource(@get:JvmSynthetic val fontFamily: FontFamily) : FontSpec
    data class Asset(@get:JvmSynthetic val path: String) : FontSpec
    data class Google(@get:JvmSynthetic val name: String) : FontSpec
    sealed interface Generic : FontSpec {
        object SansSerif : Generic
        object Serif : Generic
        object Monospace : Generic
    }
    data class Downloaded(@get:JvmSynthetic val downloadedFontFamily: DownloadedFontFamily) : FontSpec

    data class System(@get:JvmSynthetic val name: String) : FontSpec
}

@JvmSynthetic
internal fun Map<FontAlias, FontsConfig>.determineFontSpecs(
    resourceProvider: ResourceProvider,
): Map<FontAlias, FontSpec> {
    val resourceFontFamilies = values
        .toSet()
        .map { it.android }
        .filterIsInstance<FontInfo.Name>()
        .filter { it.family != null }
        .groupBy { it.family!! }
        .mapValues { (_, fontInfos) ->
            val resourceIdsSeen = mutableSetOf<Int>()
            fontInfos.mapNotNull { fontInfo ->
                resourceProvider.getResourceIdentifier(name = fontInfo.value, type = "font")
                    .takeIf { it != 0 && it !in resourceIdsSeen }
                    ?.also { resourceIdsSeen.add(it) }
                    ?.let {
                        ResourceFontSpec(
                            id = it,
                            weight = fontInfo.weight,
                            style = fontInfo.style?.toComposeFontStyle(),
                        )
                    }
            }
        }
        .filterValues { it.isNotEmpty() }
        .mapValues { (_, resourceFonts) ->
            if (resourceFonts.size == 1) {
                resourceProvider.getXmlFontFamily(resourceFonts.first().id)?.let {
                    return@mapValues FontSpec.Resource(it)
                }
            }
            FontSpec.Resource(
                FontFamily(
                    resourceFonts.map { resourceFont ->
                        Font(
                            resId = resourceFont.id,
                            weight = resourceFont.weight?.let { FontWeight(it) } ?: FontWeight.Normal,
                            style = resourceFont.style ?: FontStyle.Normal,
                        )
                    },
                ),
            )
        }

    // Get unique FontsConfigs, and determine their FontSpec.
    val configToSpec: Map<FontsConfig, FontSpec> = values.toSet().associateWith { fontsConfig ->
        resourceProvider.determineFontSpec(fontsConfig, resourceFontFamilies)
    }
    // Create a map of FontAliases to FontSpecs.
    return mapValues { (_, fontsConfig) -> configToSpec.getValue(fontsConfig) }
}

/**
 * Retrieves a [FontSpec] from this map, and returns a [PaywallValidationError.MissingFontAlias] if it doesn't exist.
 * If you want to treat blank [FontAlias]es as a null [FontSpec], chain it with [recoverFromFontAliasError].
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
 * Returns a successful Result containing `null` if this is an error Result caused by a
 * [PaywallValidationError.MissingFontAlias] error. We still want to show the Paywall in this scenario, but we'll let
 * the developer know through a log message that something's wrong.
 *
 * There's a special case for a blank [FontAlias], in which case we don't log anything. This scenario should not happen,
 * as our dashboard should not allow devs to publish a paywall containing a blank Font Family field, but this is
 * defensive in case it does happen.
 */
@Suppress("MaxLineLength")
@JvmSynthetic
internal fun Result<FontSpec, PaywallValidationError>.recoverFromFontAliasError(): Result<FontSpec?, PaywallValidationError> =
    flatMapError { error ->
        if (error is PaywallValidationError.MissingFontAlias && error.alias.value.isBlank()) {
            // Treating this as a dashboard error and just ignoring it. No need to log.
            Result.Success(null)
        } else if (error is PaywallValidationError.MissingFontAlias) {
            Logger.w(
                "Font named '${error.alias}' was not found in the font config. Try re-adding it in the Paywall editor.",
            )
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
    is FontSpec.Resource -> fontFamily
    is FontSpec.Asset -> FontFamily(Font(path = path, assetManager = assets, weight = weight, style = style))
    is FontSpec.Google -> FontFamily(
        Font(googleFont = GoogleFont(name), fontProvider = GoogleFontsProvider, weight = weight, style = style),
    )

    is FontSpec.Generic -> when (this) {
        FontSpec.Generic.SansSerif -> FontFamily.SansSerif
        FontSpec.Generic.Serif -> FontFamily.Serif
        FontSpec.Generic.Monospace -> FontFamily.Monospace
    }

    is FontSpec.Downloaded -> FontFamily(
        fonts = downloadedFontFamily.fonts.map { font ->
            Font(
                file = File(font.file.path),
                weight = FontWeight(font.weight),
                style = font.style.toComposeFontStyle(),
            )
        },
    )

    is FontSpec.System -> FontFamily(
        Font(familyName = DeviceFontFamilyName(name), weight = weight, style = style),
    )
}

private fun ResourceProvider.determineFontSpec(
    fontsConfig: FontsConfig,
    resourceFontFamilies: Map<String, FontSpec.Resource>,
): FontSpec {
    return when (val fontInfo = fontsConfig.android) {
        is FontInfo.GoogleFonts -> FontSpec.Google(name = fontInfo.value)
        is FontInfo.Name -> getGenericFontSpec(fontInfo)
            ?: fontInfo.family?.let { resourceFontFamilies[fontInfo.family] }
            ?: getAssetFontPath(name = fontInfo.value)
                ?.let { path -> FontSpec.Asset(path = path) }
            ?: getDownloadedFontSpec(fontInfo)
            ?: FontSpec.System(name = fontInfo.value).also {
                Logger.d(
                    "Could not find a font resource named `${fontInfo.value}`. " +
                        "Assuming it's an OEM system font. If it isn't, make sure the font exists in the " +
                        "`res/font` folder. See for more info: " +
                        "https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml",
                )
            }
    }
}

private fun getGenericFontSpec(
    fontInfo: FontInfo.Name,
): FontSpec.Generic? {
    return when (fontInfo.value) {
        FontFamily.SansSerif.name -> FontSpec.Generic.SansSerif
        FontFamily.Serif.name -> FontSpec.Generic.Serif
        FontFamily.Monospace.name -> FontSpec.Generic.Monospace
        else -> null // Not a generic font.
    }
}

private fun com.revenuecat.purchases.paywalls.components.properties.FontStyle.toComposeFontStyle(): FontStyle {
    return when (this) {
        com.revenuecat.purchases.paywalls.components.properties.FontStyle.NORMAL -> FontStyle.Normal
        com.revenuecat.purchases.paywalls.components.properties.FontStyle.ITALIC -> FontStyle.Italic
    }
}

private fun ResourceProvider.getDownloadedFontSpec(fontInfo: FontInfo.Name): FontSpec.Downloaded? {
    return getCachedFontFamilyOrStartDownload(fontInfo)?.let {
        FontSpec.Downloaded(it)
    }
}
