package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.annotation.FontRes
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher

/**
 * Implement this interface to provide custom fonts to the [PaywallActivityLauncher].
 * If you don't, the default material3 theme fonts will be used.
 * Use [FontProvider] instead if you are using Compose with [PaywallView] or [PaywallDialog].
 */
interface FontResourceProvider {
    /**
     * Returns the font resource id to be used for the given [TypographyType]. If null is returned,
     * the default font will be used.
     * @param type the [TypographyType] for which the font is being requested.
     * @return the font resource id to be used for the given [TypographyType].
     */
    @FontRes
    fun getFontResourceId(type: TypographyType): Int?
}
