package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.Locale

/**
 * Utility class for resolving the appropriate Locale for currency formatting based on storefront country code
 * and device locales.
 */
@InternalRevenueCatAPI
object CurrencyLocaleResolver {

    /**
     * Determines the Locale used for formatting currencies based on the `storefrontCountryCode` and the device's
     * available locale's. The `storefrontCountryCode` argument will be used instead of the cached
     * `storefrontCountryCode` if provided. The `locale` argument is used as fallback in case no
     * `storefrontCountryCode` is available or when there are no matching device locale's.
     *
     * @param storefrontCountryCode The storefront country code (e.g., "US", "NL"). If null, only the locale
     * parameter is returned.
     * @param locale The device locale to use as a fallback. Defaults to system default locale.
     * @return The best matching locale for currency formatting.
     */
    @JvmStatic
    fun resolve(
        storefrontCountryCode: String?,
        locale: Locale = Locale.getDefault(),
    ): Locale {
        if (storefrontCountryCode.isNullOrBlank()) {
            return locale
        }

        // We find all available device locales with the same country as the storefront country.
        val availableStorefrontCountryLocalesByLanguage: Map<String, Locale> = buildMap {
            Locale.getAvailableLocales().forEach { availableLocale ->
                if (availableLocale.country.equals(storefrontCountryCode, ignoreCase = true)) {
                    put(availableLocale.language.lowercase(), availableLocale)
                }
            }
        }

        val deviceLanguageCode = locale.language.lowercase()

        // We pick the one with the same language as the device if available. If not, we just pick the
        // first. If the list is empty, we use the device locale with the storefront country.
        return availableStorefrontCountryLocalesByLanguage[deviceLanguageCode]
            ?: availableStorefrontCountryLocalesByLanguage.values.firstOrNull()
            ?: Locale.Builder()
                .setLocale(locale)
                .setRegion(storefrontCountryCode.uppercase())
                .build()
    }
}
