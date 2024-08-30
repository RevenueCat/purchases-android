package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.models.Period
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import com.amazon.device.iap.model.ProductType as AmazonProductType
import com.revenuecat.purchases.ProductType as RevenueCatProductType

@RunWith(AndroidJUnit4::class)
class ProductConverterTest {

    @Test
    fun `sku is correctly assigned`() {
        val expectedSku = "sku"
        val product = dummyAmazonProduct(expectedSku)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.id).isEqualTo(expectedSku)
    }

    @Test
    fun `product types are correctly assigned`() {
        var product = dummyAmazonProduct(productType = AmazonProductType.CONSUMABLE)
        var storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.type).isEqualTo(RevenueCatProductType.INAPP)

        product = dummyAmazonProduct(productType = AmazonProductType.ENTITLED)
        storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.type).isEqualTo(RevenueCatProductType.INAPP)

        product = dummyAmazonProduct(productType = AmazonProductType.SUBSCRIPTION)
        storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.type).isEqualTo(RevenueCatProductType.SUBS)
    }

    @Test
    fun `price is correctly assigned`() {
        val expectedPrice = "$39.99"
        val product = dummyAmazonProduct(price = expectedPrice)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.price?.formatted).isEqualTo(expectedPrice)
    }

    @Test
    fun `priceAmountMicros is correctly calculated`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.price?.amountMicros).isEqualTo(39_990_000)
    }

    @Test
    fun `priceCurrencyCode is correctly assigned`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.price?.currencyCode).isEqualTo("USD")
    }
    @Test
    fun `title is correctly assigned`() {
        val expectedTitle = "title"
        val product = dummyAmazonProduct(title = expectedTitle)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.title).isEqualTo(expectedTitle)
    }
    @Test
    fun `description is correctly assigned`() {
        val expectedDescription = "description"
        val product = dummyAmazonProduct(description = expectedDescription)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.description).isEqualTo(expectedDescription)
    }

    @Test
    fun `subscription period is null for a non-subscriptions`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.period).isNull()
    }

    @Test
    fun `icon URL is correctly assigned`() {
        val expectedSmallIconUrl = "https://icon.url"
        val product = dummyAmazonProduct(smallIconUrl = expectedSmallIconUrl)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.amazonProduct?.iconUrl).isEqualTo(expectedSmallIconUrl)
    }

    fun `subscription period is non-null for a subscription`() {
        val product = dummyAmazonProduct(
            subscriptionPeriod = "Weekly"
        )
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.period).isEqualTo(
            Period(1, Period.Unit.WEEK, "P1W")
        )
    }

    @Test
    fun `free trial period is null if no free trial period`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.amazonProduct?.freeTrialPeriod).isNull()
    }

    @Test
    fun `free trial period is non-null if a free trial period`() {
        val product = dummyAmazonProduct(
            subscriptionPeriod = "Quarterly",
            freeTrialPeriod = "BiWeekly"
        )
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.amazonProduct?.freeTrialPeriod).isEqualTo(
            Period(2, Period.Unit.WEEK, "P2W")
        )
    }

    @Test
    fun `originalJSON is correctly assigned`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")

        val receivedJSON = storeProduct?.amazonProduct?.originalProductJSON
        val expectedJSON = product.toJSON()

        assertThat(receivedJSON).isNotNull
        assertThat(receivedJSON!!.length()).isEqualTo(expectedJSON.length())
        receivedJSON.keys().forEach {
            assertThat(receivedJSON[it]).isEqualTo(expectedJSON[it])
        }
    }

    @Test
    fun `if price is missing, product is not converted`() {
        val product = dummyAmazonProduct(price = null)
        val storeProduct = product.toStoreProduct("US")

        assertThat(storeProduct).isNull()
    }
}

@RunWith(Parameterized::class)
class PeriodParsingTest(private val periodRaw: String, private val periodExpected: Period?) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() : Collection<Array<Any?>> {
            return listOf(
                arrayOf("Weekly", Period(1, Period.Unit.WEEK, "P1W")),
                arrayOf("BiWeekly", Period(2, Period.Unit.WEEK, "P2W")),
                arrayOf("Monthly", Period(1, Period.Unit.MONTH, "P1M")),
                arrayOf("BiMonthly", Period(2, Period.Unit.MONTH, "P2M")),
                arrayOf("Quarterly", Period(3, Period.Unit.MONTH, "P3M")),
                arrayOf("SemiAnnually", Period(6, Period.Unit.MONTH, "P6M")),
                arrayOf("SemiAnnual", Period(6, Period.Unit.MONTH, "P6M")),
                arrayOf("Annually", Period(1, Period.Unit.YEAR, "P1Y")),
                arrayOf("Annual", Period(1, Period.Unit.YEAR, "P1Y")),
                arrayOf("7 Days", Period(7, Period.Unit.DAY, "P7D")),
                arrayOf("14 Weeks", Period(14, Period.Unit.WEEK, "P14W")),
                arrayOf("1 Year", Period(1, Period.Unit.YEAR, "P1Y")),
                arrayOf("3 Pizzas To Go", null),
                arrayOf("Josh", null)
            )
        }
    }

    @Test
    fun `period string to Period is correct`() {
        val period = periodRaw.createPeriod()
        assertThat(period).isEqualTo(periodExpected)
    }
}