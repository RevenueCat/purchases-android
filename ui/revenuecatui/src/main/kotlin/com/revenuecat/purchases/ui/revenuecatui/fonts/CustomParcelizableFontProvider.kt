package com.revenuecat.purchases.ui.revenuecatui.fonts

import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI

/**
 * Class that allows to provide a font family for all text styles.
 * @param fontFamily the [PaywallFontFamily] to be used for all text styles.
 */
@ExperimentalPreviewRevenueCatUIPurchasesAPI
class CustomParcelizableFontProvider(
    private val fontFamily: PaywallFontFamily,
) : ParcelizableFontProvider {
    override fun getFont(type: TypographyType) = fontFamily
}
