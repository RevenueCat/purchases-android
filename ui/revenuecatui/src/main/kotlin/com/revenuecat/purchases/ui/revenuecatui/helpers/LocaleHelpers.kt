package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * Creates a [Locale] from a string representation, supporting both "es-ES" and "es_ES" formats.
 *
 * @param localeString The locale string to parse (e.g., "en", "es-ES", "es_ES")
 * @return A [Locale] instance parsed from the string
 */
internal fun createLocaleFromString(localeString: String): Locale {
    return if (localeString.contains('-') || localeString.contains('_')) {
        val parts = if (localeString.contains('-')) {
            localeString.split('-', limit = 2)
        } else {
            localeString.split('_', limit = 2)
        }
        if (parts.size >= 2) {
            Locale(parts[0], parts[1])
        } else {
            Locale(parts[0])
        }
    } else {
        Locale(localeString)
    }
}

/**
 * Returns the Compose [LayoutDirection] matching this locale's character direction.
 */
internal fun Locale.toLayoutDirection(): LayoutDirection {
    val configuration = Configuration().apply { setLocale(this@toLayoutDirection) }

    return if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
}
