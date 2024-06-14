package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.compose.ui.text.font.FontFamily

/**
 * Class that allows to provide a font family for all text styles.
 * @param fontFamily the [FontFamily] to be used for all text styles.
 */
class CustomFontProvider(private val fontFamily: FontFamily) : FontProvider {
    override fun getFont(type: TypographyType) = fontFamily
}
