package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PriceFactoryTest {

    @Test
    fun `creates price with US locale correctly`() {
        val price = PriceFactory.createPrice(9990000L, "USD", Locale.US)

        assertThat(price.formatted).isEqualTo("$9.99")
        assertThat(price.amountMicros).isEqualTo(9990000L)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `creates price with EUR currency correctly`() {
        val price = PriceFactory.createPrice(4990000L, "EUR", Locale.GERMANY)

        assertThat(price.formatted).isEqualTo("4,99 €")
        assertThat(price.amountMicros).isEqualTo(4990000L)
        assertThat(price.currencyCode).isEqualTo("EUR")
    }

    @Test
    fun `creates price with JPY currency correctly`() {
        val price = PriceFactory.createPrice(12345678L, "JPY", Locale.JAPAN)

        assertThat(price.formatted).isEqualTo("￥12")
        assertThat(price.amountMicros).isEqualTo(12345678L)
        assertThat(price.currencyCode).isEqualTo("JPY")
    }

    @Test
    fun `creates zero price correctly`() {
        val price = PriceFactory.createPrice(0L, "USD", Locale.US)

        assertThat(price.formatted).isEqualTo("$0.00")
        assertThat(price.amountMicros).isEqualTo(0L)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `creates price with large amount correctly`() {
        val price = PriceFactory.createPrice(99999990000L, "USD", Locale.US)

        assertThat(price.formatted).isEqualTo("$99,999.99")
        assertThat(price.amountMicros).isEqualTo(99999990000L)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `creates price with different locale formats USD correctly`() {
        val priceUS = PriceFactory.createPrice(9990000L, "USD", Locale.US)
        val priceUK = PriceFactory.createPrice(9990000L, "USD", Locale.UK)

        assertThat(priceUS.formatted).isEqualTo("$9.99")
        assertThat(priceUK.formatted).isEqualTo("US$9.99")
        assertThat(priceUS.amountMicros).isEqualTo(priceUK.amountMicros)
        assertThat(priceUS.currencyCode).isEqualTo(priceUK.currencyCode)
    }

    @Test
    fun `creates price with GBP currency correctly`() {
        val price = PriceFactory.createPrice(7990000L, "GBP", Locale.UK)

        assertThat(price.formatted).isEqualTo("£7.99")
        assertThat(price.amountMicros).isEqualTo(7990000L)
        assertThat(price.currencyCode).isEqualTo("GBP")
    }

    @Test
    fun `creates price with CAD currency correctly`() {
        val price = PriceFactory.createPrice(12990000L, "CAD", Locale.CANADA)

        assertThat(price.formatted).isEqualTo("$12.99")
        assertThat(price.amountMicros).isEqualTo(12990000L)
        assertThat(price.currencyCode).isEqualTo("CAD")
    }

    @Test
    fun `creates price with BRL currency correctly`() {
        val price = PriceFactory.createPrice(24990000L, "BRL", Locale("pt", "BR"))

        // BRL formatting varies by locale - just check structure
        assertThat(price.formatted).contains("24,99")
        assertThat(price.formatted).contains("R$")
        assertThat(price.amountMicros).isEqualTo(24990000L)
        assertThat(price.currencyCode).isEqualTo("BRL")
    }

    @Test
    fun `creates price with KRW currency correctly`() {
        val price = PriceFactory.createPrice(15000000000L, "KRW", Locale.KOREA)

        assertThat(price.formatted).isEqualTo("₩15,000")
        assertThat(price.amountMicros).isEqualTo(15000000000L)
        assertThat(price.currencyCode).isEqualTo("KRW")
    }

    @Test
    fun `creates price with very small amount correctly`() {
        val price = PriceFactory.createPrice(10000L, "USD", Locale.US)

        assertThat(price.formatted).isEqualTo("$0.01")
        assertThat(price.amountMicros).isEqualTo(10000L)
        assertThat(price.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `creates price with fraction digits for different currencies`() {
        val usdPrice = PriceFactory.createPrice(9990000L, "USD", Locale.US)
        val jpyPrice = PriceFactory.createPrice(999000000L, "JPY", Locale.JAPAN)

        // USD has 2 fraction digits
        assertThat(usdPrice.formatted).isEqualTo("$9.99")
        
        // JPY has 0 fraction digits
        assertThat(jpyPrice.formatted).isEqualTo("￥999")
    }
}
