package com.revenuecat.purchases.paywalls

import android.annotation.SuppressLint
import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.errorLog
import java.net.MalformedURLException
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingFontPreDownloader(
    private val context: Context,
    private val remoteFontLoader: RemoteFontLoader,
) {

    private val assetsFontsDir = "fonts"

    // GenericFontFamily names as defined by Compose. Restated here because we don't include any Compose dependencies.
    private val genericFonts = setOf(
        "sans-serif",
        "serif",
        "monospace",
    )

    fun preDownloadOfferingFontsIfNeeded(offerings: Offerings) {
        // Getting the first offering's paywall components to check for fonts.
        // All offerings are expected to have the same fonts.
        val fontInfosToDownload = offerings.all.values.firstOrNull()?.paywallComponents?.uiConfig?.app?.fonts?.values
            ?.filterNot { isBundled(it.android) }
            ?.filter {
                if (it.web != null && it.web.hash == null) {
                    val androidValue = (it.android as? FontInfo.Name)?.value ?: "(Unknown android font)"
                    errorLog(
                        "Font $androidValue does not have a validation hash. Skipping download. " +
                            "Pleases try to re-upload the font in the RevenueCat dashboard.",
                    )
                    false
                } else {
                    true
                }
            }
            ?.mapNotNull { it.web }
            ?.filter {
                try {
                    URL(it.value)
                    true
                } catch (e: MalformedURLException) {
                    errorLog("Malformed URL for font: ${it.value}. Skipping download.", e)
                    false
                }
            } ?: emptyList()

        for (fontToDownload in fontInfosToDownload) {
            fontToDownload.hash?.takeIf { it.isNotEmpty() }?.let { hash ->
                remoteFontLoader.getCachedFontFileOrStartDownload(fontToDownload.value, hash)
            }
        }
    }

    private fun isBundled(info: FontInfo): Boolean {
        return when (info) {
            is FontInfo.GoogleFonts -> true
            is FontInfo.Name -> when (info.value.takeIf { it.isNotEmpty() }) {
                in genericFonts -> true
                null -> false
                else -> context.getResourceIdentifier(info.value, "font") != 0 ||
                    context.getAssetFontPath(info.value) != null
            }
        }
    }

    /**
     * Use sparingly. The underlying platform API is discouraged because
     * > resource reflection makes it harder to perform build optimizations and compile-time verification of code.
     */
    @SuppressLint("DiscouragedApi")
    private fun Context.getResourceIdentifier(name: String, type: String): Int =
        resources.getIdentifier(name, type, packageName)

    private fun Context.getAssetFontPath(name: String): String? {
        val nameWithExtension = if (name.endsWith(".ttf")) name else "$name.ttf"

        return resources.assets.list(assetsFontsDir)
            ?.find { it == nameWithExtension }
            ?.let { "$assetsFontsDir/$it" }
    }
}
