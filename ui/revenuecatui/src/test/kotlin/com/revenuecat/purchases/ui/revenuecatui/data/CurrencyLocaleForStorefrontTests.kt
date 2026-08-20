package com.revenuecat.purchases.ui.revenuecatui.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class CurrencyLocaleForStorefrontTests {

    @Test
    fun `prefers canonical locale over variants with different currency spacing`() {
        val availableLocales = arrayOf(
            Locale.US,
            Locale.forLanguageTag("en-Latn-US"),
            Locale.forLanguageTag("en-US-POSIX"),
        )

        val result = currencyLocaleForStorefront(
            storefrontCountryCode = "US",
            locale = Locale.US,
            availableLocales = availableLocales,
        )

        assertThat(result).isEqualTo(Locale.US)
        val formattedPrice = NumberFormat.getCurrencyInstance(result).apply {
            currency = Currency.getInstance("USD")
        }.format(1.99)
        assertThat(formattedPrice).isEqualTo("$1.99")
    }
}
