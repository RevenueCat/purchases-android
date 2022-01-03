package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PriceExtractorTest {

    private lateinit var mockCurrency: Currency

    @Before
    fun setup() {
        mockCurrency = mockk()
    }

    @Test
    fun `US marketplace 7 USD`() {
        mockCurrency("$", "USD")

        val (currencyCode, priceAmountMicros) = "$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD comma`() {
        mockCurrency("$", "USD")
        mockkStatic(NumberFormat::class)

        every {
            NumberFormat.getInstance()
        } returns numberFormatWithCommas()

        val (currencyCode, priceAmountMicros) = "$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
        unmockkStatic(NumberFormat::class)
    }

    @Test
    fun `US marketplace 1000 USD dot`() {
        mockCurrency("$", "USD")

        val (currencyCode, priceAmountMicros) = "$1,000.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "US$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "US$ 7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "US$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "US$ 7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD symbol after`() {
        mockCurrency("$", "USD")

        val (currencyCode, priceAmountMicros) = "7.00$".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD comma symbol after`() {
        mockCurrency("$", "USD")

        val (currencyCode, priceAmountMicros) = "7,00$".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ symbol after`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "7.00US$".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space symbol after`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "7.00 US$".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma symbol after`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "7,00 US$".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space symbol after`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "7,00 US$".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `Inconsistent case with IN marketplace but price in dollars, sets symbol as currency code`() {
        mockkStatic(Currency::class)

        val availableCurrencies = mutableSetOf<Currency>()
        availableCurrencies.add(Currency.getInstance(Locale.US))
        every {
            Currency.getAvailableCurrencies()
        } returns availableCurrencies

        mockCurrency("₹", "INR")

        val (currencyCode, priceAmountMicros) = "US$ 7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("US$")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
        unmockkStatic(Currency::class)
    }

    @Test
    fun `US marketplace 7 USD in Spain`() {
        mockCurrency("US$", "USD")

        val (currencyCode, priceAmountMicros) = "US$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in US`() {
        mockCurrency("CA$", "CAD")

        val (currencyCode, priceAmountMicros) = "CA$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in CA`() {
        mockCurrency("$", "CAD")

        val (currencyCode, priceAmountMicros) = "$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CAD in Spain`() {
        mockCurrency("CA$", "CAD")

        val (currencyCode, priceAmountMicros) = "CA$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in US`() {
        mockCurrency("R$", "BRL")

        val (currencyCode, priceAmountMicros) = "R$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Brazil`() {
        mockCurrency("R$", "BRL")

        val (currencyCode, priceAmountMicros) = "R$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Spain`() {
        mockCurrency("BRL", "BRL")

        val (currencyCode, priceAmountMicros) = "BRL7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("BRL")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in US`() {
        mockCurrency("MX$", "MXN")

        val (currencyCode, priceAmountMicros) = "MX$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Mexico`() {
        mockCurrency("$", "MXN")

        val (currencyCode, priceAmountMicros) = "MX$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Brazil`() {
        mockCurrency("MX$", "MXN")

        val (currencyCode, priceAmountMicros) = "MX$7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Spain`() {
        mockCurrency("MXN", "MXN")

        val (currencyCode, priceAmountMicros) = "MXN7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("MXN")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace 7 GB in US`() {
        mockCurrency("£", "GBP")

        val (currencyCode, priceAmountMicros) = "£7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace 7 GB in Spain`() {
        mockCurrency("GBP", "GBP")

        val (currencyCode, priceAmountMicros) = "GBP7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in US`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Germany`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Spain`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `ES marketplace 7 Euro in US`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `FR marketplace 7 Euro in US`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IT marketplace 7 Euro in US`() {
        mockCurrency("€", "EUR")

        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in US`() {
        mockCurrency("₹", "INR")

        val (currencyCode, priceAmountMicros) = "₹7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in India`() {
        mockCurrency("₹", "INR")

        val (currencyCode, priceAmountMicros) = "₹7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in Spain`() {
        mockCurrency("INR", "INR")

        val (currencyCode, priceAmountMicros) = "INR7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("INR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in US`() {
        mockCurrency("¥", "JPY")

        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Japan`() {
        mockCurrency("¥", "JPY")

        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Spain`() {
        mockCurrency("¥", "JPY")

        val (currencyCode, priceAmountMicros) = "¥7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("JPY")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in US`() {
        mockCurrency("A$", "AUD")

        val (currencyCode, priceAmountMicros) = "A$7.00".extractPrice(mockCurrency, numberFormatWithDots())
        assertThat(currencyCode).isEqualTo("AUD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in Spain`() {
        mockCurrency("AUD", "AUD")

        val (currencyCode, priceAmountMicros) = "AUD7,00".extractPrice(mockCurrency, numberFormatWithCommas())
        assertThat(currencyCode).isEqualTo("AUD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    private fun numberFormatWithDots() = NumberFormat.getInstance(Locale.US)

    private fun numberFormatWithCommas() = NumberFormat.getInstance(Locale.FRANCE)

    private fun mockCurrency(symbol: String, code: String) {
        every {
            mockCurrency.symbol
        } returns symbol
        every {
            mockCurrency.currencyCode
        } returns code
    }
}
