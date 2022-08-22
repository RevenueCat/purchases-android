package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import com.amazon.device.iap.model.ProductType as AmazonProductType
import com.revenuecat.purchases.ProductType as RevenueCatProductType

@RunWith(AndroidJUnit4::class)
class ProductConverterTest {

    @Test
    fun `sku is correctly assigned`() {
        val expectedSku = "sku"
        val product = dummyAmazonProduct(expectedSku)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.sku).isEqualTo(expectedSku)
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
        assertThat(storeProduct?.price).isEqualTo(expectedPrice)
    }

    @Test
    fun `priceAmountMicros is correctly calculated`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.priceAmountMicros).isEqualTo(39_990_000)
    }

    @Test
    fun `priceCurrencyCode is correctly assigned`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.priceCurrencyCode).isEqualTo("USD")
    }

    @Test
    fun `originalPrice is null`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.originalPrice).isNull()
        assertThat(storeProduct?.originalPriceAmountMicros).isZero()
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
    fun `subscription period is null`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.subscriptionPeriod).isNull()
    }

    @Test
    fun `introductory price and trial periods are not available for Amazon`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.freeTrialPeriod).isNull()
        assertThat(storeProduct?.introductoryPrice).isNull()
        assertThat(storeProduct?.introductoryPriceAmountMicros).isZero
        assertThat(storeProduct?.introductoryPricePeriod).isNull()
        assertThat(storeProduct?.introductoryPriceCycles).isZero
    }

    @Test
    fun `icon URL is correctly assigned`() {
        val expectedSmallIconUrl = "https://icon.url"
        val product = dummyAmazonProduct(smallIconUrl = expectedSmallIconUrl)
        val storeProduct = product.toStoreProduct("US")
        assertThat(storeProduct?.iconUrl).isEqualTo(expectedSmallIconUrl)
    }

    @Test
    fun `originalJSON is correctly assigned`() {
        val product = dummyAmazonProduct()
        val storeProduct = product.toStoreProduct("US")

        val receivedJSON = storeProduct?.originalJson
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
