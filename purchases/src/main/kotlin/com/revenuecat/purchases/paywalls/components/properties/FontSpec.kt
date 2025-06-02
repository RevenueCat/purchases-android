@file:JvmSynthetic

package com.revenuecat.purchases.paywalls.components.properties

import android.annotation.SuppressLint
import android.content.Context
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.debugLog

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

    data class System(@get:JvmSynthetic val name: String) : FontSpec
}

private const val ASSETS_FONTS_DIR = "fonts"
private const val SANS_SERIF_FONT_NAME = "sans-serif"
private const val SERIF_FONT_NAME = "serif"
private const val MONOSPACE_FONT_NAME = "monospace"

@OptIn(InternalRevenueCatAPI::class)
@JvmSynthetic
internal fun Map<FontAlias, FontsConfig>.determineFontSpecs(
    context: Context,
): Map<FontAlias, FontSpec> {
    // Get unique FontsConfigs, and determine their FontSpec.
    val configToSpec: Map<FontsConfig, FontSpec> = values.toSet().associateWith { fontsConfig ->
        context.determineFontSpec(fontsConfig.android)
    }
    // Create a map of FontAliases to FontSpecs.
    return mapValues { (_, fontsConfig) -> configToSpec.getValue(fontsConfig) }
}

@OptIn(InternalRevenueCatAPI::class)
private fun Context.determineFontSpec(info: FontInfo): FontSpec =
    when (info) {
        is FontInfo.GoogleFonts -> FontSpec.Google(name = info.value)
        is FontInfo.Name -> when (info.value) {
            SANS_SERIF_FONT_NAME -> FontSpec.Generic.SansSerif
            SERIF_FONT_NAME -> FontSpec.Generic.Serif
            MONOSPACE_FONT_NAME -> FontSpec.Generic.Monospace
            else -> getResourceIdentifier(name = info.value, type = "font")
                .takeUnless { it == 0 }
                ?.let { fontId -> FontSpec.Resource(id = fontId) }
                ?: getAssetFontPath(name = info.value)
                    ?.let { path -> FontSpec.Asset(path = path) }
                ?: FontSpec.System(name = info.value).also {
                    debugLog(
                        "Could not find a font resource named `${info.value}`. Assuming it's an OEM system font. " +
                            "If it isn't, make sure the font exists in the `res/font` folder. See for more info: " +
                            "https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml",
                    )
                }
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
