package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class PriceExtensionsTest {
    @Test
    fun `formattedPricePerMonth correctly formats USD in US locale`() {
        val price = Price("$59.99", 59_990_000, "USD")
        val billingPeriod = Period.create("P1Y")
        val locale = Locale.US
        assertThat(price.formattedPricePerMonth(billingPeriod, locale)).isEqualTo("$5.00")
    }

    @Test
    fun `formattedPricePerMonth correctly formats USD in ES locale`() {
        val price = Price("$59.99", 59_990_000, "USD")
        val billingPeriod = Period.create("P1Y")
        val locale = Locale("es", "ES")
        assertThat(price.formattedPricePerMonth(billingPeriod, locale)).isEqualTo("5,00 US$")
    }

    @Test
    fun `formattedPricePerMonth correctly formats EUR in ES locale`() {
        val price = Price("59.99€", 59_990_000, "EUR")
        val billingPeriod = Period.create("P1Y")
        val locale = Locale("es", "ES")
        assertThat(price.formattedPricePerMonth(billingPeriod, locale)).isEqualTo("5,00 €")
    }

    @Test
    fun `formattedPricePerMonth correctly formats EUR in US locale`() {
        val price = Price("59.99€", 59_990_000, "EUR")
        val billingPeriod = Period.create("P1Y")
        val locale = Locale.US
        assertThat(price.formattedPricePerMonth(billingPeriod, locale)).isEqualTo("€5.00")
    }

    @Test
    fun `formattedPricePerMonth correctly formats monthly price`() {
        val price = Price("9.99€", 9_990_000, "USD")
        val billingPeriod = Period.create("P1M")
        val locale = Locale.US
        assertThat(price.formattedPricePerMonth(billingPeriod, locale)).isEqualTo("$9.99")
    }
}
