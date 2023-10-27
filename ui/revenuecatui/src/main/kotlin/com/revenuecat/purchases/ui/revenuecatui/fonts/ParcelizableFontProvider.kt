package com.revenuecat.purchases.ui.revenuecatui.fonts

import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher

/**
 * Implement this interface to provide custom fonts to the [PaywallActivityLauncher].
 * If you don't, the default material3 theme fonts will be used.
 * If you only want to use a single [PaywallFontFamily] for all text styles use [CustomParcelizableFontProvider].
 * Use [FontProvider] instead if you are using Compose with [PaywallView] or [PaywallDialog].
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
interface ParcelizableFontProvider {
    /**
     * Returns the [PaywallFontFamily] to be used for the given [TypographyType]. If null is returned,
     * the default font will be used.
     * @param type the [TypographyType] for which the font is being requested.
     * @return the [PaywallFontFamily] to be used for the given [TypographyType].
     */
    fun getFont(type: TypographyType): PaywallFontFamily?
}
