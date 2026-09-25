package com.revenuecat.purchases.ui.revenuecatui.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class CurrencyLocaleForStorefrontTests {

    @Test
    fun `excludes POSIX locale with different currency spacing`() {
        val availableLocales = arrayOf(
            Locale.US,
            Locale.forLanguageTag("en-Latn-US"),
            Locale.forLanguageTag("en-US-POSIX"),
        )

        val result = requireNotNull(
            getAvailableStorefrontCountryLocalesByLanguage(
                storefrontCountryCode = "US",
                availableLocales = availableLocales,
            )["en"],
        )

        assertThat(result).isEqualTo(Locale.forLanguageTag("en-Latn-US"))
        val formattedPrice = NumberFormat.getCurrencyInstance(result).apply {
            currency = Currency.getInstance("USD")
        }.format(1.99)
        assertThat(formattedPrice).isEqualTo("$1.99")
    }

    @Test
    fun `preserves existing selection behavior for non-POSIX variants`() {
        val valencian = Locale.forLanguageTag("ca-ES-VALENCIA")

        val result = getAvailableStorefrontCountryLocalesByLanguage(
            storefrontCountryCode = "ES",
            availableLocales = arrayOf(Locale.forLanguageTag("ca-ES"), valencian),
        )["ca"]

        assertThat(result).isEqualTo(valencian)
    }

    @Test
    fun `falls back to synthesized locale instead of an unrelated language for an es paywall on IL storefront`() {
        // Regression test for https://github.com/RevenueCat/purchases-android/issues/4237
        // The IL storefront has no "es" locale on-device, only ar, en and he. Previously, the code fell back to
        // `values.firstOrNull()`, which could arbitrarily resolve to `ar_IL` and render prices with Arabic-Indic
        // digits for a paywall localized in Spanish. It should instead synthesize "es_IL" so the paywall's own
        // language conventions (Latin digits) are preserved while still reflecting the storefront region.
        val availableLocales = arrayOf(
            Locale.forLanguageTag("ar-IL"),
            Locale.forLanguageTag("en-IL"),
            Locale.forLanguageTag("he-IL"),
        )
        val storefrontCountryCode = "IL"
        val paywallLanguageLocale = Locale.forLanguageTag("es-ES")

        val availableStorefrontCountryLocalesByLanguage = getAvailableStorefrontCountryLocalesByLanguage(
            storefrontCountryCode = storefrontCountryCode,
            availableLocales = availableLocales,
        )

        // Confirm the fixture matches the bug report: no "es" entry among the IL locales.
        assertThat(availableStorefrontCountryLocalesByLanguage).doesNotContainKey("es")

        // Mirrors the resolution in PaywallState.Loaded.Components.currencyLocale.
        val deviceLanguageCode = paywallLanguageLocale.language.lowercase()
        val resolvedLocale = availableStorefrontCountryLocalesByLanguage[deviceLanguageCode]
            ?: Locale.Builder()
                .setLocale(paywallLanguageLocale)
                .setRegion(storefrontCountryCode.uppercase())
                .build()

        assertThat(resolvedLocale.language).isEqualTo("es")

        val formattedPrice = NumberFormat.getCurrencyInstance(resolvedLocale).apply {
            currency = Currency.getInstance("ILS")
        }.format(1.99)
        assertThat(formattedPrice).matches { price -> price.none { it.code in 0x0660..0x0669 } }
    }
}
