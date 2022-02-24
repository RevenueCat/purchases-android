package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PriceExtractorTest {

    @Test
    fun `US marketplace 7 USD dot`() {
        val (currencyCode, priceAmountMicros) = "$7.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 70 USD dot`() {
        val (currencyCode, priceAmountMicros) = "$7,00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD comma`() {
        val (currencyCode, priceAmountMicros) = "$7,00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 1000 USD dot`() {
        val (currencyCode, priceAmountMicros) = "$1,000.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$`() {
        val (currencyCode, priceAmountMicros) = "US$7.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space`() {
        val (currencyCode, priceAmountMicros) = "US$ 7.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma`() {
        val (currencyCode, priceAmountMicros) = "US$7,00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space`() {
        val (currencyCode, priceAmountMicros) = "US$ 7,00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD symbol after`() {
        val (currencyCode, priceAmountMicros) = "7.00$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD comma symbol after`() {
        val (currencyCode, priceAmountMicros) = "7,00$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ symbol after`() {
        val (currencyCode, priceAmountMicros) = "7.00US$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space symbol after`() {
        val (currencyCode, priceAmountMicros) = "7.00 US$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma symbol after`() {
        val (currencyCode, priceAmountMicros) = "7,00 US$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space symbol after`() {
        val (currencyCode, priceAmountMicros) = "7,00 US$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `Inconsistent case with IN marketplace but price in dollars, sets symbol as currency code`() {
        val (currencyCode, priceAmountMicros) = "US$ 7.00".extractPrice("IN")
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD in Spain`() {
        val (currencyCode, priceAmountMicros) = "US$7.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in US`() {
        val (currencyCode, priceAmountMicros) = "CA$7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in CA`() {
        val (currencyCode, priceAmountMicros) = "$7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in Spain`() {
        val (currencyCode, priceAmountMicros) = "CA$7,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in US`() {
        val (currencyCode, priceAmountMicros) = "R$7.00".extractPrice("BR")
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Brazil`() {
        val (currencyCode, priceAmountMicros) = "R$7,00".extractPrice("BR")
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Spain`() {
        val (currencyCode, priceAmountMicros) = "BRL7,00".extractPrice("BR")
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in US`() {
        val (currencyCode, priceAmountMicros) = "MX$7.00".extractPrice("MX")
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Mexico`() {
        val (currencyCode, priceAmountMicros) = "MX$7,00".extractPrice("MX")
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Brazil`() {
        val (currencyCode, priceAmountMicros) = "MX$7,00".extractPrice("MX")
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Spain`() {
        val (currencyCode, priceAmountMicros) = "MXN7,00".extractPrice("MX")
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace 7 GB in US`() {
        val (currencyCode, priceAmountMicros) = "£7.00".extractPrice("GB")
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace 7 GB in Spain`() {
        val (currencyCode, priceAmountMicros) = "GBP7,00".extractPrice("GB")
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Germany`() {
        val (currencyCode, priceAmountMicros) = "€7,00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Spain`() {
        val (currencyCode, priceAmountMicros) = "€7,00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `ES marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `FR marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IT marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("ES")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in US`() {
        val (currencyCode, priceAmountMicros) = "₹7.00".extractPrice("IN")
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in India`() {
        val (currencyCode, priceAmountMicros) = "₹7.00".extractPrice("IN")
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in Spain`() {
        val (currencyCode, priceAmountMicros) = "INR7,00".extractPrice("IN")
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in US`() {
        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice("JP")
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Japan`() {
        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice("JP")
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Spain`() {
        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice("JP")
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in US`() {
        val (currencyCode, priceAmountMicros) = "A$7.00".extractPrice("AU")
        assertThat(currencyCode).isEqualTo("AUD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in Spain`() {
        val (currencyCode, priceAmountMicros) = "AUD7,00".extractPrice("AU")
        assertThat(currencyCode).isEqualTo("AUD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

}
