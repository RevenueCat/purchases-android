package com.revenuecat.purchases.amazon.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.ProductDataResponseBuilder
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.amazon.helpers.MockDeviceCache
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.models.ProductDetails
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDataHandlerTest {

    private lateinit var underTest: ProductDataHandler
    private val apiKey = "api_key"
    private lateinit var cache: MockDeviceCache
    private lateinit var purchasingServiceProvider: PurchasingServiceProviderForTest

    @Before
    fun setup() {
        cache = MockDeviceCache(mockk(), apiKey)
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = ProductDataHandler(purchasingServiceProvider)
    }

    @Test
    fun `send cached product data if available for all skus`() {
        val expectedSkus = setOf("sku_a", "sku_b")

        val expectedProductDataInCache = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        underTest.productDataCache.putAll(expectedProductDataInCache)

        var receivedProductList: List<ProductDetails>? = null
        underTest.getProductData(
            expectedSkus,
            "US",
            onReceive = {
                receivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        assertThat(receivedProductList).isNotNull

        val receivedSkus = receivedProductList!!.map { it.sku }
        assertThat(receivedSkus).isEqualTo(expectedSkus)

        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isZero()
    }

    @Test
    fun `getProductData is called if one of the products is not in cache`() {
        val firstProduct = "sku_a" to dummyAmazonProduct(sku = "sku_a")
        val expectedProductData = mapOf(
            firstProduct,
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        underTest.productDataCache.putAll(mapOf(firstProduct))

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedProductList: List<ProductDetails>? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                receivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        assertThat(receivedProductList).isNotNull

        val expectedSkus = expectedProductData.keys
        val receivedSkus = receivedProductList!!.map { it.sku }
        assertThat(receivedSkus).isEqualTo(expectedSkus)

        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isOne()
    }

    @Test
    fun `products are cached after a successful response`() {
        assertThat(underTest.productDataCache).isEmpty()

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedProductList: List<ProductDetails>? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                receivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        val expectedSkus = expectedProductData.keys
        val receivedSkus = receivedProductList!!.map { it.sku }
        assertThat(receivedSkus).isEqualTo(expectedSkus)

        assertThat(underTest.productDataCache).isNotEmpty
        assertThat(underTest.productDataCache).isEqualTo(expectedProductData)
    }

    @Test
    fun `Product data cache works and a second call to get the same data hits the cache`() {
        assertThat(underTest.productDataCache).isEmpty()

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedProductList: List<ProductDetails>? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                receivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        val secondDummyRequestId = "a_second_request_id"
        purchasingServiceProvider.getProductDataRequestId = secondDummyRequestId

        var secondReceivedProductList: List<ProductDetails>? = null

        underTest.getProductData(expectedProductData.keys, "US",
            onReceive = {
                secondReceivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = secondDummyRequestId,
                productData = expectedProductData
            )
        )

        assertThat(secondReceivedProductList).isEqualTo(receivedProductList)

        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isEqualTo(1)
    }

    @Test
    fun `products have correct marketplace`() {
        assertThat(underTest.productDataCache).isEmpty()

        val marketPlace = "ES"

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a", price = "€3.00")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedProductList: List<ProductDetails>? = null

        underTest.getProductData(
            expectedProductData.keys,
            marketPlace,
            onReceive = {
                receivedProductList = it
            },
            onError = {
                fail("should be success")
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        assertThat(receivedProductList).isNotEmpty
        assertThat(receivedProductList!![0].priceCurrencyCode).isEqualTo("EUR")
    }

    @Test
    fun `products are not cached after an unsuccessful response`() {
        assertThat(underTest.productDataCache).isEmpty()

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedError: PurchasesError? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                fail("should be error")
            },
            onError = {
                receivedError = it
            }
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = emptyMap(),
                requestStatus = ProductDataResponse.RequestStatus.FAILED
            )
        )

        assertThat(receivedError).isNotNull
        assertThat(underTest.productDataCache).isEmpty()
    }

    @Test
    fun `callbacks are invoked just once`() {
        assertThat(underTest.productDataCache).isEmpty()

        val marketPlace = "ES"

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a", price = "€3.00")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var receivedCount = 0

        underTest.getProductData(
            expectedProductData.keys,
            marketPlace,
            onReceive = {
                receivedCount++
            },
            onError = {
                fail("should be success")
            }
        )

        val response = getDummyProductDataResponse(
            requestId = dummyRequestId,
            productData = expectedProductData
        )

        underTest.onProductDataResponse(response)
        underTest.onProductDataResponse(response)

        assertThat(receivedCount).isOne
    }
}

private fun getDummyProductDataResponse(
    requestId: String = "${System.currentTimeMillis()}",
    unavailableSkus: Set<String> = emptySet(),
    requestStatus: ProductDataResponse.RequestStatus = ProductDataResponse.RequestStatus.SUCCESSFUL,
    productData: Map<String, Product> = mapOf("sku_a" to dummyAmazonProduct())
): ProductDataResponse {
    val builder = ProductDataResponseBuilder()
        .setRequestId(RequestId.fromString(requestId))
        .setUnavailableSkus(unavailableSkus)
        .setRequestStatus(requestStatus)
        .setProductData(productData)

    return ProductDataResponse(builder)
}
