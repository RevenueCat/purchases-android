package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import kotlin.text.Typography.nbsp

@RunWith(AndroidJUnit4::class)
class PriceExtractorTest {

    @Test
    fun `US marketplace $7 dot 12`() {
        val (currencyCode, priceAmountMicros) = "$7.12".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_120_000)
    }

    @Test
    fun `US marketplace $7 comma 12`() {
        val (currencyCode, priceAmountMicros) = "$7,12".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_120_000)
    }

    @Test
    fun `US marketplace $7 dot 1`() {
        val (currencyCode, priceAmountMicros) = "$7.1".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_100_000)
    }

    @Test
    fun `US marketplace $7 comma 1`() {
        val (currencyCode, priceAmountMicros) = "$7,1".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_100_000)
    }

    @Test
    fun `US marketplace $7 dot 123`() {
        val (currencyCode, priceAmountMicros) = "$7.123".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $7 comma 123`() {
        val (currencyCode, priceAmountMicros) = "$7,123".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $7 123`() {
        val (currencyCode, priceAmountMicros) = "$7 123".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_123_000_000)
    }

    @Test
    fun `US marketplace $0 dot 99`() {
        val (currencyCode, priceAmountMicros) = "$0.99".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(990_000)
    }

    @Test
    fun `US marketplace $0 comma 99`() {
        val (currencyCode, priceAmountMicros) = "$0,99".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(990_000)
    }

    @Test
    fun `US marketplace $1`() {
        val (currencyCode, priceAmountMicros) = "$1".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000)
    }

    @Test
    fun `US marketplace $10M`() {
        val (currencyCode, priceAmountMicros) = "$10000000".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(10_000_000_000_000)
    }

    @Test
    fun `US marketplace $1M with decimal dot and commas`() {
        val (currencyCode, priceAmountMicros) = "$1,000,000.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000_000)
    }

    @Test
    fun `US marketplace $1M with decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "$1.000.000,00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000_000)
    }

    @Test
    fun `US marketplace $0 dot 1234`() {
        val (currencyCode, priceAmountMicros) = "$0.1234".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(123_400)
    }

    @Test
    fun `US marketplace $0 comma 1234`() {
        val (currencyCode, priceAmountMicros) = "$0,1234".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(123_400)
    }

    @Test
    fun `US marketplace $1 comma 0000000`() {
        val (currencyCode, priceAmountMicros) = "$1,0000000".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000)
    }

    @Test
    fun `US marketplace $1000 dot`() {
        val (currencyCode, priceAmountMicros) = "$1,000.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace $100000 dot no decimals`() {
        val (currencyCode, priceAmountMicros) = "$100.000".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(100_000_000_000)
    }

    @Test
    fun `US marketplace $100000 comma no decimals`() {
        val (currencyCode, priceAmountMicros) = "$100,000".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(100_000_000_000)
    }

    @Test
    fun `US marketplace $1000 dot without comma`() {
        val (currencyCode, priceAmountMicros) = "$1000.00".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(1_000_000_000)
    }

    @Test
    fun `US marketplace $1000 comma without dot`() {
        val (currencyCode, priceAmountMicros) = "$1000,00".extractPrice("US")
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
    fun `CA marketplace CA$7 dot`() {
        val (currencyCode, priceAmountMicros) = "CA$7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$7 comma`() {
        val (currencyCode, priceAmountMicros) = "CA$7,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 dot`() {
        val (currencyCode, priceAmountMicros) = "CA$7000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 dot and commas`() {
        val (currencyCode, priceAmountMicros) = "CA$7,000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 decimal comma`() {
        val (currencyCode, priceAmountMicros) = "CA$7000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$7000 decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "CA$7.000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7CA$ dot`() {
        val (currencyCode, priceAmountMicros) = "7.00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7CA$ comma`() {
        val (currencyCode, priceAmountMicros) = "7,00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ dot`() {
        val (currencyCode, priceAmountMicros) = "7000.00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ dot and commas`() {
        val (currencyCode, priceAmountMicros) = "7,000.00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ decimal comma`() {
        val (currencyCode, priceAmountMicros) = "7000,00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000CA$ decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "7.000,00CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7 dot`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7 comma`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 dot`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 dot and commas`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7,000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 decimal comma`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace CA$ 7000 decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "CA$ 7.000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7 CA$ dot`() {
        val (currencyCode, priceAmountMicros) = "7.00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7 CA$ comma`() {
        val (currencyCode, priceAmountMicros) = "7,00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ dot`() {
        val (currencyCode, priceAmountMicros) = "7000.00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ dot and commas`() {
        val (currencyCode, priceAmountMicros) = "7,000.00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ decimal comma`() {
        val (currencyCode, priceAmountMicros) = "7000,00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000 CA$ decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "7.000,00 CA$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7 dot`() {
        val (currencyCode, priceAmountMicros) = "$7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7 comma`() {
        val (currencyCode, priceAmountMicros) = "$7,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot`() {
        val (currencyCode, priceAmountMicros) = "$7000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot and commas`() {
        val (currencyCode, priceAmountMicros) = "$7,000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma`() {
        val (currencyCode, priceAmountMicros) = "$7000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "$7.000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7$ dot`() {
        val (currencyCode, priceAmountMicros) = "7.00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7$ comma`() {
        val (currencyCode, priceAmountMicros) = "7,00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot`() {
        val (currencyCode, priceAmountMicros) = "7000.00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot and commas`() {
        val (currencyCode, priceAmountMicros) = "7,000.00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma`() {
        val (currencyCode, priceAmountMicros) = "7000,00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma and dots`() {
        val (currencyCode, priceAmountMicros) = "7.000,00$".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7 dot space`() {
        val (currencyCode, priceAmountMicros) = "$ 7.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7 comma space`() {
        val (currencyCode, priceAmountMicros) = "$ 7,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot space`() {
        val (currencyCode, priceAmountMicros) = "$ 7000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 dot and commas space`() {
        val (currencyCode, priceAmountMicros) = "$ 7,000.00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma space`() {
        val (currencyCode, priceAmountMicros) = "$ 7000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace $7000 decimal comma and dots space`() {
        val (currencyCode, priceAmountMicros) = "$ 7.000,00".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7$ dot space`() {
        val (currencyCode, priceAmountMicros) = "7.00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7$ comma space`() {
        val (currencyCode, priceAmountMicros) = "7,00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot space`() {
        val (currencyCode, priceAmountMicros) = "7000.00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ dot and commas space`() {
        val (currencyCode, priceAmountMicros) = "7,000.00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma space`() {
        val (currencyCode, priceAmountMicros) = "7000,00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
    }

    @Test
    fun `CA marketplace 7000$ decimal comma and dots space`() {
        val (currencyCode, priceAmountMicros) = "7.000,00 $".extractPrice("CA")
        assertThat(currencyCode).isEqualTo("CAD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000_000)
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
    fun `GB marketplace £7 comma`() {
        val (currencyCode, priceAmountMicros) = "£7.00".extractPrice("GB")
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `GB marketplace GBP7 comma`() {
        val (currencyCode, priceAmountMicros) = "GBP7,00".extractPrice("GB")
        assertThat(currencyCode).isEqualTo("GBP")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("DE")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Germany`() {
        val (currencyCode, priceAmountMicros) = "7,00 €".extractPrice("DE")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `DE marketplace 7 Euro in Spain`() {
        val (currencyCode, priceAmountMicros) = "7,00 €".extractPrice("DE")
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
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("FR")
        assertThat(currencyCode).isEqualTo("EUR")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

    @Test
    fun `IT marketplace 7 Euro in US`() {
        val (currencyCode, priceAmountMicros) = "€7.00".extractPrice("IT")
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

    @Test
    fun `US marketplace 7 USD US$ space symbol after and nbsp`() {
        val (currencyCode, priceAmountMicros) = "7.00${nbsp}US$".extractPrice("US")
        assertThat(currencyCode).isEqualTo("USD")
        assertThat(priceAmountMicros).isEqualTo(7_000_000)
    }

}
