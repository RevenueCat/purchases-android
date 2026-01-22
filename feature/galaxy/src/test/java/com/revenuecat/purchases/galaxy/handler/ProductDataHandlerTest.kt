package com.revenuecat.purchases.galaxy.handler

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.listener.OnGetPromotionEligibilityListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class ProductDataHandlerTest : GalaxyStoreTest() {

    private lateinit var iapHelperProvider: IAPHelperProvider
    private lateinit var mainHandler: Handler
    private lateinit var productDataHandler: ProductDataHandler

    private val unexpectedOnReceive: (List<StoreProduct>) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onReceive to be called") }

    @Before
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
        mainHandler = mockk(relaxed = true)
        productDataHandler = ProductDataHandler(iapHelperProvider)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getProductDetails returns empty list for empty request`() {
        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = emptySet(),
            productType = ProductType.INAPP,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        assertThat(receivedProducts).isEmpty()
        verify(exactly = 0) { iapHelperProvider.getProductsDetails(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getProductDetails errors when another request is in flight`() {
        val productIds = setOf("in_flight")
        var receivedError: PurchasesError? = null

        productDataHandler.getProductDetails(
            productIds = productIds,
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )

        productDataHandler.getProductDetails(
            productIds = setOf("second"),
            productType = ProductType.SUBS,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
        verify(exactly = 1) { iapHelperProvider.getProductsDetails(any(), any()) }
        verify(exactly = 0) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful product response caches products and forwards only matching type`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val capturedPromotionEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(any(), capture(capturedPromotionEligibilityListener))
        } returns true

        val productIds = setOf("iap", "sub")
        val inAppProduct = createProductVo(itemId = "iap", type = "item")
        val subProduct = createProductVo(itemId = "sub", type = "subscription")

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = productIds,
            productType = ProductType.SUBS,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedListener.captured.onGetProducts(successErrorVo, arrayListOf(inAppProduct, subProduct))

        verify(exactly = 1) {
            iapHelperProvider.getPromotionEligibility(
                itemIDs = "iap,sub",
                onGetPromotionEligibilityListener = any(),
            )
        }

        capturedPromotionEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(
                createPromotionEligibilityVo(itemId = "iap", pricing = "None"),
                createPromotionEligibilityVo(itemId = "sub", pricing = "None"),
            ),
        )

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactly("sub")
        assertThat(productDataHandler.productVoCache).containsKeys("iap", "sub")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful product response with empty products does not request promotion eligibilities`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = setOf("iap"),
            productType = ProductType.INAPP,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedListener.captured.onGetProducts(successErrorVo, arrayListOf())

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts).isEmpty()
        verify(exactly = 0) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `unsuccessful product response forwards store problem error and clears in flight`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        var receivedError: PurchasesError? = null

        productDataHandler.getProductDetails(
            productIds = setOf("iap"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_PAYMENT_IS_CANCELED.code
            every { errorString } returns "boom"
        }
        capturedListener.captured.onGetProducts(failingErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PurchaseCancelledError)
        verify(exactly = 0) { iapHelperProvider.getPromotionEligibility(any(), any()) }

        // Next request should proceed because the in-flight one was cleared
        productDataHandler.getProductDetails(
            productIds = setOf("next"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `unsuccessful product response maps network errors to NetworkError`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedListener))
        } returns Unit

        var receivedError: PurchasesError? = null

        productDataHandler.getProductDetails(
            productIds = setOf("iap"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        val failingErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NETWORK_NOT_AVAILABLE.code
            every { errorString } returns "no network"
        }
        capturedListener.captured.onGetProducts(failingErrorVo, arrayListOf())

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("no network")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `cached metadata always fetches promotion eligibility again`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
                any(),
                capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListeners = mutableListOf<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListeners))
        } returns true

        val productId = "sub"
        val product = createProductVo(itemId = productId, type = "subscription")

        // Call getProductDetails to populate the internal product cache
        productDataHandler.getProductDetails(
            productIds = setOf(productId),
            productType = ProductType.SUBS,
            onReceive = {},
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(product))
        capturedEligibilityListeners.single().onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(createPromotionEligibilityVo(itemId = productId, pricing = "None")),
        )

        productDataHandler.getProductDetails(
            productIds = setOf(productId),
            productType = ProductType.SUBS,
            onReceive = {},
            onError = unexpectedOnError,
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails(any(), any()) }
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `cached products do not re-request product details`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListeners = mutableListOf<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListeners))
        } returns true

        val productId = "sub"
        val product = createProductVo(itemId = productId, type = "subscription")

        // Cache the product
        productDataHandler.getProductDetails(
            productIds = setOf(productId),
            productType = ProductType.SUBS,
            onReceive = {},
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(product))
        capturedEligibilityListeners.single().onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(createPromotionEligibilityVo(itemId = productId, pricing = "None")),
        )

        productDataHandler.getProductDetails(
            productIds = setOf(productId),
            productType = ProductType.SUBS,
            onReceive = {},
            onError = unexpectedOnError,
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails(any(), any()) }
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `partial cache only requests uncached products from galaxy store and returns full set`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListener))
        } returns true

        val cachedId = "cached"
        val uncachedId = "uncached"
        val cachedProduct = createProductVo(itemId = cachedId, type = "subscription")
        val uncachedProduct = createProductVo(itemId = uncachedId, type = "subscription")
        productDataHandler.productVoCache[cachedId] = cachedProduct

        var receivedProducts: List<StoreProduct>? = null

        // Call getProductDetails to populate the internal product cache
        productDataHandler.getProductDetails(
            productIds = setOf(cachedId, uncachedId),
            productType = ProductType.SUBS,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(uncachedProduct))
        capturedEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(
                createPromotionEligibilityVo(itemId = cachedId, pricing = "None"),
                createPromotionEligibilityVo(itemId = uncachedId, pricing = "None"),
            ),
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails(uncachedId, any()) }
        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactlyInAnyOrder(cachedId, uncachedId)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `extra cached products are ignored when requesting uncached IDs`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListener))
        } returns true

        val cachedRequestedId = "cached_requested"
        val cachedExtraId = "cached_extra"
        val uncachedId = "uncached"
        val cachedRequestedProduct = createProductVo(itemId = cachedRequestedId, type = "subscription")
        val cachedExtraProduct = createProductVo(itemId = cachedExtraId, type = "subscription")
        val uncachedProduct = createProductVo(itemId = uncachedId, type = "subscription")
        productDataHandler.productVoCache[cachedRequestedId] = cachedRequestedProduct
        productDataHandler.productVoCache[cachedExtraId] = cachedExtraProduct

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = setOf(cachedRequestedId, uncachedId),
            productType = ProductType.SUBS,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(uncachedProduct))
        capturedEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(
                createPromotionEligibilityVo(itemId = cachedRequestedId, pricing = "None"),
                createPromotionEligibilityVo(itemId = uncachedId, pricing = "None"),
            ),
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails(uncachedId, any()) }
        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactlyInAnyOrder(cachedRequestedId, uncachedId)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `partial cache returns cached products when samsung response is empty`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListener))
        } returns true

        val cachedId = "cached"
        val uncachedId = "uncached"
        val cachedProduct = createProductVo(itemId = cachedId, type = "subscription")
        productDataHandler.productVoCache[cachedId] = cachedProduct

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = setOf(cachedId, uncachedId),
            productType = ProductType.SUBS,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf())
        capturedEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(createPromotionEligibilityVo(itemId = cachedId, pricing = "None")),
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails(uncachedId, any()) }
        verify(exactly = 1) { iapHelperProvider.getPromotionEligibility(any(), any()) }
        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactly(cachedId)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `extra products returned by samsung are not surfaced to the caller`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListener))
        } returns true

        val requestedId = "requested"
        val extraId = "extra"
        val requestedProduct = createProductVo(itemId = requestedId, type = "subscription")
        val extraProduct = createProductVo(itemId = extraId, type = "subscription")

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = setOf(requestedId),
            productType = ProductType.SUBS,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(requestedProduct, extraProduct))
        capturedEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(
                createPromotionEligibilityVo(itemId = requestedId, pricing = "None"),
                createPromotionEligibilityVo(itemId = extraId, pricing = "None"),
            ),
        )

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactly(requestedId)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `promotion eligibility error clears in flight and forwards error`() {
        val capturedProductListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(
            any(),
            capture(capturedProductListener))
        } returns Unit

        val capturedEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedEligibilityListener))
        } returns true

        val productId = "sub"
        val product = createProductVo(itemId = productId, type = "subscription")
        var receivedError: PurchasesError? = null

        // Build up cache
        productDataHandler.getProductDetails(
            productIds = setOf(productId),
            productType = ProductType.SUBS,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedProductListener.captured.onGetProducts(successErrorVo, arrayListOf(product))

        capturedEligibilityListener.captured.onGetPromotionEligibility(
            mockk<ErrorVo> {
                every { errorCode } returns GalaxyErrorCode.IAP_ERROR_COMMON.code
                every { errorString } returns "eligibility failed"
            },
            arrayListOf(),
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)

        productDataHandler.getProductDetails(
            productIds = setOf("next"),
            productType = ProductType.SUBS,
            onReceive = {},
            onError = unexpectedOnError,
        )

        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
    }
}
