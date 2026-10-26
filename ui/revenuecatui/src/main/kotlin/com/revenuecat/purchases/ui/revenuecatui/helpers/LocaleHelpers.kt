package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
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
internal fun Locale.toLayoutDirection(): LayoutDirection =
    Configuration().apply { setLocale(this@toLayoutDirection) }.toLayoutDirection()

/** Returns the Compose [LayoutDirection] this configuration lays out in. */
internal fun Configuration.toLayoutDirection(): LayoutDirection =
    if (layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr

/**
 * This context with its resources localized to [localeOverride] (the SDK's preferred UI locale override), or this
 * context itself when there is no override or it cannot be parsed.
 */
internal fun Context.localizedTo(localeOverride: String?): Context {
    val locale = localeOverride?.let {
        try {
            createLocaleFromString(it)
        } catch (e: IllegalArgumentException) {
            Logger.e("Invalid preferred locale format: $it. Using system default.", e)
            null
        }
    } ?: return this
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    // Context.createConfigurationContext returns `null` with Paparazzi
    return createConfigurationContext(configuration) ?: this
}
