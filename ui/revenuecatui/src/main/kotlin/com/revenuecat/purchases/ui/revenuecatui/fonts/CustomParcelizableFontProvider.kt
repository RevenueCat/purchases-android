package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.compose.runtime.Immutable

/**
 * Class that allows to provide a font family for all text styles.
 * @param fontFamily the [PaywallFontFamily] to be used for all text styles.
 */
@Immutable
public class CustomParcelizableFontProvider(
    private val fontFamily: PaywallFontFamily,
) : ParcelizableFontProvider {
    override fun getFont(type: TypographyType): PaywallFontFamily = fontFamily
}
