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
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.sku).isEqualTo(expectedSku)
    }

    @Test
    fun `product types are correctly assigned`() {
        var product = dummyAmazonProduct(productType = AmazonProductType.CONSUMABLE)
        var productDetails = product.toProductDetails("US")
        assertThat(productDetails.type).isEqualTo(RevenueCatProductType.INAPP)

        product = dummyAmazonProduct(productType = AmazonProductType.ENTITLED)
        productDetails = product.toProductDetails("US")
        assertThat(productDetails.type).isEqualTo(RevenueCatProductType.ENTITLED)

        product = dummyAmazonProduct(productType = AmazonProductType.SUBSCRIPTION)
        productDetails = product.toProductDetails("US")
        assertThat(productDetails.type).isEqualTo(RevenueCatProductType.SUBS)
    }

    @Test
    fun `price is correctly assigned`() {
        val expectedPrice = "$39.99"
        val product = dummyAmazonProduct(price = expectedPrice)
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.price).isEqualTo(expectedPrice)
    }

    @Test
    fun `priceAmountMicros is correctly calculated`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.priceAmountMicros).isEqualTo(39_990_000)
    }

    @Test
    fun `priceCurrencyCode is correctly assigned`() {
        val product = dummyAmazonProduct(price = "$39.99")
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.priceCurrencyCode).isEqualTo("USD")
    }

    @Test
    fun `originalPrice is null`() {
        val product = dummyAmazonProduct()
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.originalPrice).isNull()
        assertThat(productDetails.originalPriceAmountMicros).isZero()
    }

    @Test
    fun `title is correctly assigned`() {
        val expectedTitle = "title"
        val product = dummyAmazonProduct(title = expectedTitle)
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `description is correctly assigned`() {
        val expectedDescription = "description"
        val product = dummyAmazonProduct(description = expectedDescription)
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.description).isEqualTo(expectedDescription)
    }

    @Test
    fun `subscription period is null`() {
        val product = dummyAmazonProduct()
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.subscriptionPeriod).isNull()
    }

    @Test
    fun `introductory price and trial periods are not available for Amazon`() {
        val product = dummyAmazonProduct()
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.freeTrialPeriod).isNull()
        assertThat(productDetails.introductoryPrice).isNull()
        assertThat(productDetails.introductoryPriceAmountMicros).isZero
        assertThat(productDetails.introductoryPricePeriod).isNull()
        assertThat(productDetails.introductoryPriceCycles).isZero
    }

    @Test
    fun `icon URL is correctly assigned`() {
        val expectedSmallIconUrl = "https://icon.url"
        val product = dummyAmazonProduct(smallIconUrl = expectedSmallIconUrl)
        val productDetails = product.toProductDetails("US")
        assertThat(productDetails.iconUrl).isEqualTo(expectedSmallIconUrl)
    }

    @Test
    fun `originalJSON is correctly assigned`() {
        val product = dummyAmazonProduct()
        val productDetails = product.toProductDetails("US")

        val receivedJSON = productDetails.originalJson
        val expectedJSON = product.toJSON()

        assertThat(receivedJSON.length()).isEqualTo(expectedJSON.length())
        receivedJSON.keys().forEach {
            assertThat(receivedJSON[it]).isEqualTo(expectedJSON[it])
        }
    }


}
