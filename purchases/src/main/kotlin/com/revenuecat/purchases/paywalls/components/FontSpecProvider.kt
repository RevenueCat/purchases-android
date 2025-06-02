package com.revenuecat.purchases.paywalls.components

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.RemoteFontLoader
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
import com.revenuecat.purchases.paywalls.components.properties.determineFontSpecs

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(InternalRevenueCatAPI::class)
internal class FontSpecProvider internal constructor(
    private val context: Context,
    private val fontLoader: RemoteFontLoader,
) {
    private val fontSpecs: MutableMap<FontAlias, FontSpec> = mutableMapOf()

    fun loadFonts(offering: Offering) {
        val fonts = offering.paywallComponents?.uiConfig?.app?.fonts?.takeIf { it.isNotEmpty() } ?: return
        val fontsToCalculate = fonts.filter { !fontSpecs.contains(it.key) }
        if (fontsToCalculate.isNotEmpty()) {
            val newFontSpecs = fontsToCalculate.determineFontSpecs(context)

            for ((_, fontConfig) in newFontSpecs) {
                if (fontConfig is FontSpec.Downloadable) {
                    fontLoader.getOrDownloadFont(fontConfig.url, fontConfig.hash)
                }
            }

            synchronized(this) {
                fontSpecs.putAll(newFontSpecs)
            }
        }

    }

    @Synchronized
    fun getFontSpecMap(): Map<FontAlias, FontSpec> {
        return fontSpecs.toMap()
    }
}
