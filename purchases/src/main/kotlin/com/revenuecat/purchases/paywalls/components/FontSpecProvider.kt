package com.revenuecat.purchases.paywalls.components

import android.content.Context
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
import com.revenuecat.purchases.paywalls.components.properties.determineFontSpecs

@OptIn(InternalRevenueCatAPI::class)
internal class FontSpecProvider internal constructor(
    private val context: Context,
) {
    private val fontSpecs: MutableMap<FontAlias, FontSpec> = mutableMapOf()

    // WIP: We probably want to do this asynchronously.
    fun loadFonts(offering: Offering) {
        val fonts = offering.paywallComponents?.uiConfig?.app?.fonts?.takeIf { it.isNotEmpty() } ?: return
        val fontsToCalculate = fonts.filter { !fontSpecs.contains(it.key) }
        if (fontsToCalculate.isNotEmpty()) {
            fontSpecs.putAll(fontsToCalculate.determineFontSpecs(context))
            // WIP: Trigger download of Fonts that need to be downloaded.
        }
    }

    fun getFontSpecMap() = fontSpecs
}
