@file:JvmSynthetic

package com.revenuecat.purchases.paywalls.components.properties

import android.annotation.SuppressLint
import android.content.Context
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import java.net.MalformedURLException
import java.net.URL

/**
 * A `FontSpec` is a more detailed version of [FontInfo]. A [FontInfo.Name] can be resolved to a generic font
 * (e.g. "sans-serif"), a font resource provided by the app, or a device font provided by the OEM.
 *
 * Determining this is relatively costly for font resources. So this abstraction allows us to perform this logic only
 * once for each one (see [determineFontSpecs]), before resolving the actual font where needed.
 *
 * It also allows us to defer resolving the actual font to the UI layer, as only at that time do we know the exact
 * override that's being used. We need to know this, because we need to know the [FontWeight] for which to resolve the
 * font.
 */
@InternalRevenueCatAPI
sealed interface FontSpec {
    data class Resource(@get:JvmSynthetic val id: Int) : FontSpec
    data class Asset(@get:JvmSynthetic val path: String) : FontSpec
    data class Google(@get:JvmSynthetic val name: String) : FontSpec
    sealed interface Generic : FontSpec {
        object SansSerif : Generic
        object Serif : Generic
        object Monospace : Generic
    }
    data class Downloadable(
        @get:JvmSynthetic val url: String,
        @get:JvmSynthetic val family: String,
        @get:JvmSynthetic val weight: FontWeight,
        @get:JvmSynthetic val fontStyle: FontStyle,
        @get:JvmSynthetic val hash: String,
    ): FontSpec

    data class System(@get:JvmSynthetic val name: String) : FontSpec
}

private const val ASSETS_FONTS_DIR = "fonts"
private const val SANS_SERIF_FONT_NAME = "sans-serif"
private const val SERIF_FONT_NAME = "serif"
private const val MONOSPACE_FONT_NAME = "monospace"

@OptIn(InternalRevenueCatAPI::class)
@JvmSynthetic
internal fun Map<FontAlias, FontsConfig>.determineFontSpecs(context: Context): Map<FontAlias, FontSpec> {
    // Get unique FontsConfigs, and determine their FontSpec.
    val configToSpec: Map<FontsConfig, FontSpec> = values.toSet().associateWith { fontsConfig ->
        context.determineFontSpec(fontsConfig)
    }
    // Create a map of FontAliases to FontSpecs.
    return mapValues { (_, fontsConfig) -> configToSpec.getValue(fontsConfig) }
}

@OptIn(InternalRevenueCatAPI::class)
private fun Context.determineFontSpec(fontsConfig: FontsConfig): FontSpec {
    val androidInfo = fontsConfig.android
    val androidInfoValue = androidInfo.value
    val bundledFont = if (androidInfoValue.isNotEmpty()) {
        when (androidInfo) {
            is FontInfo.GoogleFonts -> FontSpec.Google(name = androidInfoValue)
            is FontInfo.Name -> when (androidInfoValue) {
                SANS_SERIF_FONT_NAME -> FontSpec.Generic.SansSerif
                SERIF_FONT_NAME -> FontSpec.Generic.Serif
                MONOSPACE_FONT_NAME -> FontSpec.Generic.Monospace
                else -> getResourceIdentifier(name = androidInfoValue, type = "font")
                    .takeUnless { it == 0 }
                    ?.let { fontId -> FontSpec.Resource(id = fontId) }
                    ?: getAssetFontPath(name = androidInfoValue)
                        ?.let { path -> FontSpec.Asset(path = path) }
            }
        }
    } else {
        null
    }
    if (bundledFont != null) {
        return bundledFont
    }
    val webUrl = fontsConfig.web?.value?.takeIf { it.isNotEmpty() }
    val webHash = fontsConfig.web?.hash?.takeIf { it.isNotEmpty() }
    val webFont = if (webUrl != null && webHash != null) {
        val url = try {
            URL(webUrl)
        } catch (e: MalformedURLException) {
            errorLog("Error parsing web font URL: $webUrl", e)
            null
        }
        url?.let {
            FontSpec.Downloadable(
                url = webUrl,
                family = fontsConfig.family ?: "default",
                weight = fontsConfig.weight ?: FontWeight.REGULAR,
                fontStyle = fontsConfig.fontStyle ?: FontStyle.NORMAL,
                hash = webHash,
            )
        }
    } else {
        null
    }

    if (webFont != null) {
        return webFont
    }

    return FontSpec.System(name = androidInfoValue).also {
        debugLog(
            "Could not find a font resource named `${androidInfoValue}`. Assuming it's an OEM system font. " +
                "If it isn't, make sure the font exists in the `res/font` folder. See for more info: " +
                "https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml",
        )
    }
}


/**
 * Use sparingly. The underlying platform API is discouraged because
 * > resource reflection makes it harder to perform build optimizations and compile-time verification of code.
 */
@SuppressLint("DiscouragedApi")
private fun Context.getResourceIdentifier(name: String, type: String) =
    resources.getIdentifier(name, type, packageName)

private fun Context.getAssetFontPath(name: String): String? {
    val nameWithExtension = if (name.endsWith(".ttf")) name else "$name.ttf"

    return resources.assets.list(ASSETS_FONTS_DIR)
        ?.find { it == nameWithExtension }
        ?.let { "${ASSETS_FONTS_DIR}/$it" }
}
