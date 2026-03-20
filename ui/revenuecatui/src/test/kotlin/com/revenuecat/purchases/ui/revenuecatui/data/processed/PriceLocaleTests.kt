package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.models.Price
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Locale

/**
 * Tests for [Price.getFormatted] to ensure it properly formats prices using
 * the provided locale, fixing the mixed currencies bug (PW-133).
 */
class PriceLocaleTests {

    @Test
    fun `getFormatted uses provided locale for currency symbol position`() {
        // Price with USD currency
        val price = Price(
            amountMicros = 10_000_000, // $10.00
            currencyCode = "USD",
            formatted = "US$10.00", // Pre-formatted with a different locale
        )

        // Format with US locale - currency symbol before number
        val usFormatted = price.getFormatted(Locale.US)
        assertThat(usFormatted).startsWith("$")
        assertThat(usFormatted).contains("10")

        // Format with German locale - currency symbol after number
        val deFormatted = price.getFormatted(Locale.GERMANY)
        assertThat(deFormatted).endsWith("$")
        assertThat(deFormatted).contains("10")
    }

    @Test
    fun `getFormatted uses provided locale for decimal separator`() {
        val price = Price(
            amountMicros = 10_990_000, // $10.99
            currencyCode = "USD",
            formatted = "$10.99",
        )

        // US uses period as decimal separator
        val usFormatted = price.getFormatted(Locale.US)
        assertThat(usFormatted).contains(".")
        assertThat(usFormatted).doesNotContain(",")

        // Germany uses comma as decimal separator
        val deFormatted = price.getFormatted(Locale.GERMANY)
        assertThat(deFormatted).contains(",")
    }

    @Test
    fun `getFormatted respects currency decimal places`() {
        // Japanese Yen has 0 decimal places
        val jpyPrice = Price(
            amountMicros = 1000_000_000, // ¥1000
            currencyCode = "JPY",
            formatted = "¥1,000",
        )

        val jpFormatted = jpyPrice.getFormatted(Locale.JAPAN)
        // Should not have decimal places for JPY
        assertThat(jpFormatted).doesNotContain(".")
        assertThat(jpFormatted).contains("1,000") // thousands separator
    }

    @Test
    fun `getFormatted handles EUR currency with correct symbol`() {
        val eurPrice = Price(
            amountMicros = 5_000_000, // €5.00
            currencyCode = "EUR",
            formatted = "€5.00",
        )

        // Format with German locale
        val deFormatted = eurPrice.getFormatted(Locale.GERMANY)
        assertThat(deFormatted).contains("€")
        assertThat(deFormatted).contains("5")

        // Format with French locale
        val frFormatted = eurPrice.getFormatted(Locale.FRANCE)
        assertThat(frFormatted).contains("€")
        assertThat(frFormatted).contains("5")
    }

    @Test
    fun `getFormatted ignores pre-formatted value and uses locale`() {
        // Price pre-formatted in a very wrong way
        val price = Price(
            amountMicros = 5_000_000,
            currencyCode = "USD",
            formatted = "WRONG FORMAT!!!",
        )

        // Should still format correctly based on locale
        val formatted = price.getFormatted(Locale.US)
        assertThat(formatted).isEqualTo("$5.00")
    }
}
