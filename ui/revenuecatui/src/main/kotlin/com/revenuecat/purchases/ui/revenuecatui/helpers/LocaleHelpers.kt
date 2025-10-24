package com.revenuecat.purchases.ui.revenuecatui.helpers

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
