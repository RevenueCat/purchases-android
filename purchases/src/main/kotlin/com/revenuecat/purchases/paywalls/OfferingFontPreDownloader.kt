package com.revenuecat.purchases.paywalls

import android.annotation.SuppressLint
import android.content.Context
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.paywalls.fonts.toDownloadableFontInfo
import com.revenuecat.purchases.utils.Result
import java.net.MalformedURLException
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
internal class OfferingFontPreDownloader(
    private val context: Context,
    private val fontLoader: FontLoader,
) {

    private val assetsFontsDir = "fonts"

    // GenericFontFamily names as defined by Compose. Restated here because we don't include any Compose dependencies.
    private val genericFonts = setOf(
        "sans-serif",
        "serif",
        "monospace",
    )

    public fun preDownloadOfferingFontsIfNeeded(offerings: Offerings) {
        // Getting the first offering's paywall components to check for fonts.
        // All offerings are expected to have the same fonts.
        val fontsToCheck = offerings.all.values
            .firstNotNullOfOrNull { it.paywallComponents?.uiConfig?.app?.fonts?.values }
            ?: emptyList()
        val fontInfosToDownload = fontsToCheck
            .map { it.android }
            .filterIsInstance<FontInfo.Name>()
            .filter {
                it.toDownloadableFontInfo() is Result.Success &&
                    !isBundled(it)
            }
            .filter {
                try {
                    URL(it.url)
                    true
                } catch (e: MalformedURLException) {
                    errorLog(e) { "Malformed URL for font: ${it.value}. Skipping download." }
                    false
                }
            }

        for (fontToDownload in fontInfosToDownload) {
            fontLoader.getCachedFontFamilyOrStartDownload(fontToDownload)
        }
    }

    private fun isBundled(info: FontInfo.Name): Boolean {
        if (info.value.isEmpty()) return false
        return when (info.value) {
            in genericFonts -> true
            else -> context.getResourceIdentifier(info.value, "font") != 0 ||
                context.getAssetFontPath(info.value) != null
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
