package com.revenuecat.purchases.ui.revenuecatui.fonts

/**
 * Class that allows to provide a font family for all text styles.
 * @param fontFamily the [PaywallFontFamily] to be used for all text styles.
 */
public class CustomParcelizableFontProvider(
    private val fontFamily: PaywallFontFamily,
) : ParcelizableFontProvider {
    public override fun getFont(type: TypographyType): PaywallFontFamily = fontFamily
}
