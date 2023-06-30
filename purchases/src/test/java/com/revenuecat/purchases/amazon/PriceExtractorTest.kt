package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.text.Typography.nbsp

@RunWith(AndroidJUnit4::class)
class PriceExtractorTest {

    @Test
    fun `US marketplace $7 dot 12`() {
        val price = "$7.12".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_120_000)
    }

    @Test
    fun `US marketplace $7 comma 12`() {
        val price = "$7,12".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_120_000)
    }

    @Test
    fun `US marketplace $7 dot 1`() {
        val price = "$7.1".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_100_000)
    }

    @Test
    fun `US marketplace $7 comma 1`() {
        val price = "$7,1".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_100_000)
    }

    @Test
    fun `US marketplace $7 dot 123`() {
        val price = "$7.123".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $7 comma 123`() {
        val price = "$7,123".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $7 123`() {
        val price = "$7 123".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $0 dot 99`() {
        val price = "$0.99".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(990_000)
    }

    @Test
    fun `US marketplace $0 comma 99`() {
        val price = "$0,99".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(990_000)
    }

    @Test
    fun `US marketplace $1`() {
        val price = "$1".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000)
    }

    @Test
    fun `US marketplace $10M`() {
        val price = "$10000000".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(10_000_000_000_000)
    }

    @Test
    fun `US marketplace $1M with decimal dot and commas`() {
        val price = "$1,000,000.00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000_000_000)
    }

    @Test
    fun `US marketplace $1M with decimal comma and dots`() {
        val price = "$1.000.000,00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000_000_000)
    }

    @Test
    fun `US marketplace $0 dot 1234`() {
        val price = "$0.1234".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(123_400)
    }

    @Test
    fun `US marketplace $0 comma 1234`() {
        val price = "$0,1234".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(123_400)
    }

    @Test
    fun `US marketplace $1 comma 0000000`() {
        val price = "$1,0000000".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000)
    }

    @Test
    fun `US marketplace $1000 dot`() {
        val price = "$1,000.00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace $100000 dot no decimals`() {
        val price = "$100.000".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(100_000_000_000)
    }

    @Test
    fun `US marketplace $100000 comma no decimals`() {
        val price = "$100,000".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(100_000_000_000)
    }

    @Test
    fun `US marketplace $1000 dot without comma`() {
        val price = "$1000.00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace $1000 comma without dot`() {
        val price = "$1000,00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$`() {
        val price = "US$7.00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space`() {
        val price = "US$ 7.00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma`() {
        val price = "US$7,00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space`() {
        val price = "US$ 7,00".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD symbol after`() {
        val price = "7.00$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD comma symbol after`() {
        val price = "7,00$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ symbol after`() {
        val price = "7.00US$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space symbol after`() {
        val price = "7.00 US$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ comma space symbol after`() {
        val price = "7,00 US$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `Inconsistent case with IN marketplace but price in dollars, sets symbol as currency code`() {
        val price = "US$ 7.00".createPrice("IN")
        assertThat(price.currencyCode).isEqualTo("INR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$7 dot`() {
        val price = "CA$7.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$7 comma`() {
        val price = "CA$7,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 dot`() {
        val price = "CA$7000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 dot and commas`() {
        val price = "CA$7,000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 decimal comma`() {
        val price = "CA$7000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 decimal comma and dots`() {
        val price = "CA$7.000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7CA$ dot`() {
        val price = "7.00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7CA$ comma`() {
        val price = "7,00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ dot`() {
        val price = "7000.00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ dot and commas`() {
        val price = "7,000.00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ decimal comma`() {
        val price = "7000,00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ decimal comma and dots`() {
        val price = "7.000,00CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7 dot`() {
        val price = "CA$ 7.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7 comma`() {
        val price = "CA$ 7,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 dot`() {
        val price = "CA$ 7000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 dot and commas`() {
        val price = "CA$ 7,000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 decimal comma`() {
        val price = "CA$ 7000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 decimal comma and dots`() {
        val price = "CA$ 7.000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7 CA$ dot`() {
        val price = "7.00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CA$ comma`() {
        val price = "7,00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ dot`() {
        val price = "7000.00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ dot and commas`() {
        val price = "7,000.00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ decimal comma`() {
        val price = "7000,00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ decimal comma and dots`() {
        val price = "7.000,00 CA$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7 dot`() {
        val price = "$7.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7 comma`() {
        val price = "$7,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot`() {
        val price = "$7000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot and commas`() {
        val price = "$7,000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma`() {
        val price = "$7000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma and dots`() {
        val price = "$7.000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7$ dot`() {
        val price = "7.00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7$ comma`() {
        val price = "7,00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot`() {
        val price = "7000.00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot and commas`() {
        val price = "7,000.00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma`() {
        val price = "7000,00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma and dots`() {
        val price = "7.000,00$".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7 dot space`() {
        val price = "$ 7.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7 comma space`() {
        val price = "$ 7,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot space`() {
        val price = "$ 7000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot and commas space`() {
        val price = "$ 7,000.00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma space`() {
        val price = "$ 7000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma and dots space`() {
        val price = "$ 7.000,00".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7$ dot space`() {
        val price = "7.00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7$ comma space`() {
        val price = "7,00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot space`() {
        val price = "7000.00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot and commas space`() {
        val price = "7,000.00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma space`() {
        val price = "7000,00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma and dots space`() {
        val price = "7.000,00 $".createPrice("CA")
        assertThat(price.currencyCode).isEqualTo("CAD")
        assertThat(price.amountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in US`() {
        val price = "R$7.00".createPrice("BR")
        assertThat(price.currencyCode).isEqualTo("BRL")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Brazil`() {
        val price = "R$7,00".createPrice("BR")
        assertThat(price.currencyCode).isEqualTo("BRL")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `BR marketplace 7 BRL in Spain`() {
        val price = "BRL7,00".createPrice("BR")
        assertThat(price.currencyCode).isEqualTo("BRL")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in US`() {
        val price = "MX$7.00".createPrice("MX")
        assertThat(price.currencyCode).isEqualTo("MXN")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Mexico`() {
        val price = "MX$7,00".createPrice("MX")
        assertThat(price.currencyCode).isEqualTo("MXN")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Brazil`() {
        val price = "MX$7,00".createPrice("MX")
        assertThat(price.currencyCode).isEqualTo("MXN")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `MX marketplace 7 MXN in Spain`() {
        val price = "MXN7,00".createPrice("MX")
        assertThat(price.currencyCode).isEqualTo("MXN")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace £7 comma`() {
        val price = "£7.00".createPrice("GB")
        assertThat(price.currencyCode).isEqualTo("GBP")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace GBP7 comma`() {
        val price = "GBP7,00".createPrice("GB")
        assertThat(price.currencyCode).isEqualTo("GBP")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in US`() {
        val price = "€7.00".createPrice("DE")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Germany`() {
        val price = "7,00 €".createPrice("DE")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Spain`() {
        val price = "7,00 €".createPrice("DE")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `ES marketplace 7 Euro in US`() {
        val price = "€7.00".createPrice("ES")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `FR marketplace 7 Euro in US`() {
        val price = "€7.00".createPrice("FR")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IT marketplace 7 Euro in US`() {
        val price = "€7.00".createPrice("IT")
        assertThat(price.currencyCode).isEqualTo("EUR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in US`() {
        val price = "₹7.00".createPrice("IN")
        assertThat(price.currencyCode).isEqualTo("INR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in India`() {
        val price = "₹7.00".createPrice("IN")
        assertThat(price.currencyCode).isEqualTo("INR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IN marketplace 7 INR in Spain`() {
        val price = "INR7,00".createPrice("IN")
        assertThat(price.currencyCode).isEqualTo("INR")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in US`() {
        val price = "¥7.00".createPrice("JP")
        assertThat(price.currencyCode).isEqualTo("JPY")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Japan`() {
        val price = "¥7.00".createPrice("JP")
        assertThat(price.currencyCode).isEqualTo("JPY")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `JP marketplace 7 JPY in Spain`() {
        val price = "¥7.00".createPrice("JP")
        assertThat(price.currencyCode).isEqualTo("JPY")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in US`() {
        val price = "A$7.00".createPrice("AU")
        assertThat(price.currencyCode).isEqualTo("AUD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `AU marketplace 7 AUD in Spain`() {
        val price = "AUD7,00".createPrice("AU")
        assertThat(price.currencyCode).isEqualTo("AUD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `US marketplace 7 USD US$ space symbol after and nbsp`() {
        // This test was passing in unit test but failing when executed instrumented before the following bugfix
        // https://github.com/RevenueCat/purchases-android/pull/538
        val price = "7.00${nbsp}US$".createPrice("US")
        assertThat(price.currencyCode).isEqualTo("USD")
        assertThat(price.amountMicros).isEqualTo(7_000_000)
        val anotherPriceWithNBSP = "5,80 €"
        val anotherPrice = anotherPriceWithNBSP.createPrice("DE")
        assertThat(anotherPrice.currencyCode).isEqualTo("EUR")
        assertThat(anotherPrice.amountMicros).isEqualTo(5_800_000)
    }

}
