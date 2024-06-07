package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.compose.ui.text.font.FontFamily

/**
 * Implement this interface to provide custom fonts to the [PaywallView]. If you don't, the current material3 theme
 * typography will be used.
 * If you only want to use a single FontFamily for all text styles use [CustomFontProvider].
 * This can't be used when launching the paywall as an activity since the fonts are not parcelable/serializable.
 * Use [FontResourceProvider] instead.
 */
interface FontProvider {
    /**
     * Returns the font to be used for the given [TypographyType]. If null is returned, the default font will be used.
     * @param type the [TypographyType] for which the font is being requested.
     * @return the `FontFamily` to be used for the given [TypographyType].
     */
    fun getFont(type: TypographyType): FontFamily?
}
