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
}
