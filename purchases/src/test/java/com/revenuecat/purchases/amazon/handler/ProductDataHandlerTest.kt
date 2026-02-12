package com.revenuecat.purchases.amazon.handler

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.internal.model.ProductDataResponseBuilder
import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.RequestId
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.MockDeviceCache
import com.revenuecat.purchases.amazon.helpers.PurchasingServiceProviderForTest
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var mainHandler: Handler
    private lateinit var purchasingServiceProvider: PurchasingServiceProviderForTest

    private var unexpectedOnSuccess: (List<StoreProduct>) -> Unit = {
        fail("should be error")
    }

    private var unexpectedOnError: (PurchasesError) -> Unit = {
        fail("should be success")
    }

    private val mainHandlerCallbacks: MutableList<Runnable> = ArrayList()

    @Before
    fun setup() {
        cache = MockDeviceCache(mockk(), apiKey)
        mainHandlerCallbacks.clear()
        setupMainHandler()
        purchasingServiceProvider = PurchasingServiceProviderForTest()
        underTest = ProductDataHandler(purchasingServiceProvider, mainHandler)
    }

    @Test
    fun `send cached product data if available for all skus`() {
        val expectedSkus = setOf("sku_a", "sku_b")

        val expectedProductDataInCache = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
            "sku_b" to dummyAmazonProduct(sku = "sku_b")
        )

        underTest.productDataCache.putAll(expectedProductDataInCache)

        var receivedStoreProducts: List<StoreProduct>? = null
        underTest.getProductData(
            expectedSkus,
            "US",
            onReceive = {
                receivedStoreProducts = it
            },
            unexpectedOnError
        )

        assertThat(receivedStoreProducts).isNotNull

        val receivedSkus = receivedStoreProducts!!.map { it.id }
        assertThat(receivedSkus).hasSameElementsAs(expectedSkus)

        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isZero
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

        var receivedStoreProducts: List<StoreProduct>? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                receivedStoreProducts = it
            },
            unexpectedOnError
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        assertThat(receivedStoreProducts).isNotNull

        val expectedSkus = expectedProductData.keys
        val receivedSkus = receivedStoreProducts!!.map { it.id }
        assertThat(receivedSkus).hasSameElementsAs(expectedSkus)

        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isOne
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

        var receivedStoreProducts: List<StoreProduct>? = null

        underTest.getProductData(
            expectedProductData.keys,
            "US",
            onReceive = {
                receivedStoreProducts = it
            },
            unexpectedOnError
        )

        underTest.onProductDataResponse(
            getDummyProductDataResponse(
                requestId = dummyRequestId,
                productData = expectedProductData
            )
        )

        val expectedSkus = expectedProductData.keys
        val receivedSkus = receivedStoreProducts!!.map { it.id }
        assertThat(receivedSkus).hasSameElementsAs(expectedSkus)

        assertThat(underTest.productDataCache).isNotEmpty
        assertThat(underTest.productDataCache).isEqualTo(expectedProductData)
    }

    // TODOBC5: fix
//    @Test
//    fun `Product data cache works and a second call to get the same data hits the cache`() {
//        assertThat(underTest.productDataCache).isEmpty()
//
//        val expectedProductData = mapOf(
//            "sku_a" to dummyAmazonProduct(sku = "sku_a"),
//            "sku_b" to dummyAmazonProduct(sku = "sku_b")
//        )
//
//        val dummyRequestId = "a_request_id"
//        purchasingServiceProvider.getProductDataRequestId = dummyRequestId
//
//        var receivedStoreProducts: List<StoreProduct>? = null
//
//        underTest.getProductData(
//            expectedProductData.keys,
//            "US",
//            onReceive = {
//                receivedStoreProducts = it
//            },
//            unexpectedOnError
//        )
//
//        underTest.onProductDataResponse(
//            getDummyProductDataResponse(
//                requestId = dummyRequestId,
//                productData = expectedProductData
//            )
//        )
//
//        val secondDummyRequestId = "a_second_request_id"
//        purchasingServiceProvider.getProductDataRequestId = secondDummyRequestId
//
//        var secondReceivedStoreProducts: List<StoreProduct>? = null
//
//        underTest.getProductData(expectedProductData.keys, "US",
//            onReceive = {
//                secondReceivedStoreProducts = it
//            },
//            unexpectedOnError
//        )
//
//        underTest.onProductDataResponse(
//            getDummyProductDataResponse(
//                requestId = secondDummyRequestId,
//                productData = expectedProductData
//            )
//        )
//
//        assertThat(secondReceivedStoreProducts).hasSameElementsAs(receivedStoreProducts)
//
//        assertThat(purchasingServiceProvider.getProductDataCalledTimes).isEqualTo(1)
//    }
//
//    @Test
//    fun `products have correct marketplace`() {
//        assertThat(underTest.productDataCache).isEmpty()
//
//        val marketPlace = "ES"
//
//        val expectedProductData = mapOf(
//            "sku_a" to dummyAmazonProduct(sku = "sku_a", price = "€3.00")
//        )
//
//        val dummyRequestId = "a_request_id"
//        purchasingServiceProvider.getProductDataRequestId = dummyRequestId
//
//        var receivedStoreProducts: List<StoreProduct>? = null
//
//        underTest.getProductData(
//            expectedProductData.keys,
//            marketPlace,
//            onReceive = {
//                receivedStoreProducts = it
//            },
//            unexpectedOnError
//        )
//
//        underTest.onProductDataResponse(
//            getDummyProductDataResponse(
//                requestId = dummyRequestId,
//                productData = expectedProductData
//            )
//        )
//
//        assertThat(receivedStoreProducts).isNotEmpty
//        assertThat(receivedStoreProducts!![0].priceCurrencyCode).isEqualTo("EUR")
//    }

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
            unexpectedOnSuccess,
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
            unexpectedOnError
        )

        val response = getDummyProductDataResponse(
            requestId = dummyRequestId,
            productData = expectedProductData
        )

        underTest.onProductDataResponse(response)
        underTest.onProductDataResponse(response)

        assertThat(receivedCount).isOne
    }

    @Test
    fun `Exceptions are logged so they are not swallowed by Amazon`() {
        assertThat(underTest.productDataCache).isEmpty()

        val marketPlace = "ES"

        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a", price = "€3.00")
        )
        val expectedException = RuntimeException("")
        var receivedException: Throwable? = null
        var receivedLoggedException: Throwable? = null
        Purchases.logHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {
            }

            override fun d(tag: String, msg: String) {
            }

            override fun i(tag: String, msg: String) {
            }

            override fun w(tag: String, msg: String) {
            }

            override fun e(tag: String, msg: String, throwable: Throwable?) {
                receivedLoggedException = throwable
            }
        }
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        underTest.getProductData(
            expectedProductData.keys,
            marketPlace,
            onReceive = { throw expectedException },
            unexpectedOnError
        )

        val response = getDummyProductDataResponse(
            requestId = dummyRequestId,
            productData = expectedProductData
        )

        try {
            underTest.onProductDataResponse(response)
        } catch (e: Exception) {
            receivedException = e
        }

        assertThat(receivedException).isNotNull
        assertThat(receivedLoggedException).isNotNull
        assertThat(expectedException).isEqualTo(receivedException)
        assertThat(expectedException).isEqualTo(receivedLoggedException)
    }

    @Test
    fun `timeout millis when getting products is correct`() {
        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        underTest.getProductData(
            setOf("sku_a", "sku_b"),
            "US",
            unexpectedOnSuccess,
            unexpectedOnError
        )

        verify(exactly = 1) { mainHandler.postDelayed(any(), 10_000L) }
        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
    }

    @Test
    fun `request fails with timeout if did not receive response`() {
        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var resultError: PurchasesError? = null
        underTest.getProductData(
            expectedProductData.keys,
            "US",
            unexpectedOnSuccess
        ) { resultError = it }

        assertThat(resultError).isNull()

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
        mainHandlerCallbacks[0].run()

        assertThat(resultError).isNotNull
        assertThat(resultError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(resultError?.underlyingErrorMessage).isEqualTo(
            "Timeout error trying to get Amazon product data for SKUs: [sku_a]. Please check that the SKUs are correct."
        )
    }

    @Test
    fun `request does not succeed if received response after timeout`() {
        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var resultError: PurchasesError? = null
        underTest.getProductData(
            expectedProductData.keys,
            "US",
            unexpectedOnSuccess
        ) { resultError = it }

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)
        mainHandlerCallbacks[0].run()

        val response = getDummyProductDataResponse(
            requestId = dummyRequestId,
            productData = expectedProductData
        )

        underTest.onProductDataResponse(response)

        assertThat(resultError).isNotNull
    }

    @Test
    fun `request succeeds if received response before timeout`() {
        val expectedProductData = mapOf(
            "sku_a" to dummyAmazonProduct(sku = "sku_a")
        )

        val dummyRequestId = "a_request_id"
        purchasingServiceProvider.getProductDataRequestId = dummyRequestId

        var resultProducts: List<StoreProduct>? = null
        underTest.getProductData(
            expectedProductData.keys,
            "US",
            { resultProducts = it },
            unexpectedOnError
        )

        assertThat(mainHandlerCallbacks.size).isEqualTo(1)

        val response = getDummyProductDataResponse(
            requestId = dummyRequestId,
            productData = expectedProductData
        )

        underTest.onProductDataResponse(response)

        mainHandlerCallbacks[0].run()

        assertThat(resultProducts).isNotNull
        assertThat(resultProducts?.size).isEqualTo(1)
    }

    private fun setupMainHandler() {
        mainHandler = mockk()
        every {
            mainHandler.postDelayed(any(), any())
        } answers {
            mainHandlerCallbacks.add(firstArg())
        }
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
