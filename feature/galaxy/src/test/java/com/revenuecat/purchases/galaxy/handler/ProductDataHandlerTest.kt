package com.revenuecat.purchases.galaxy.handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Config
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.listener.PromotionEligibilityResponseListener
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.currentLogHandler
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
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
    private lateinit var productDataHandler: ProductDataHandler

    private val unexpectedOnReceive: (List<StoreProduct>) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onReceive to be called") }

    @Before
    fun setup() {
        iapHelperProvider = mockk(relaxed = true)
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
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `successful product response forwards only matching type and does not cache`() {
        val capturedListeners = mutableListOf<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListeners)) } returns Unit

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
        capturedListeners[0].onGetProducts(successErrorVo, arrayListOf(inAppProduct, subProduct))

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

        productDataHandler.getProductDetails(
            productIds = productIds,
            productType = ProductType.SUBS,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )
        capturedListeners[1].onGetProducts(successErrorVo, arrayListOf(inAppProduct, subProduct))
        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
        verify(exactly = 2) { iapHelperProvider.getPromotionEligibility(any(), any()) }
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
    fun `extra products returned by samsung are not surfaced to the caller`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val capturedPromotionEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(
                any(),
                capture(capturedPromotionEligibilityListener))
        } returns true

        val productIds = setOf("iap")
        val requestedProduct = createProductVo(itemId = "iap", type = "item")
        val extraProduct = createProductVo(itemId = "extra", type = "item")

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = productIds,
            productType = ProductType.INAPP,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedListener.captured.onGetProducts(successErrorVo, arrayListOf(requestedProduct, extraProduct))

        capturedPromotionEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(
                createPromotionEligibilityVo(itemId = "iap", pricing = "None"),
                createPromotionEligibilityVo(itemId = "extra", pricing = "None"),
            ),
        )

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactly("iap")
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
    fun `promotion eligibility error clears in flight and forwards error`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val promotionEligibilityListener = mockk<PromotionEligibilityResponseListener>()
        val capturedPromotionOnError = slot<(PurchasesError) -> Unit>()
        every {
            promotionEligibilityListener.getPromotionEligibilities(any(), any(), capture(capturedPromotionOnError))
        } returns Unit

        productDataHandler = ProductDataHandler(
            iapHelper = iapHelperProvider,
            promotionEligibilityResponseListener = promotionEligibilityListener,
        )

        var receivedError: PurchasesError? = null

        productDataHandler.getProductDetails(
            productIds = setOf("iap"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        capturedListener.captured.onGetProducts(successErrorVo, arrayListOf(createProductVo(itemId = "iap")))

        val promotionError = PurchasesError(
            code = PurchasesErrorCode.StoreProblemError,
            underlyingErrorMessage = "promotion failed",
        )
        capturedPromotionOnError.captured.invoke(promotionError)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo("promotion failed")

        productDataHandler.getProductDetails(
            productIds = setOf("next"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class, InternalRevenueCatAPI::class)
    @Test
    fun `missing products are logged`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val promotionEligibilityListener = mockk<PromotionEligibilityResponseListener>()
        val capturedPromotionOnSuccess = slot<(List<com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo>) -> Unit>()
        every {
            promotionEligibilityListener.getPromotionEligibilities(any(), capture(capturedPromotionOnSuccess), any())
        } returns Unit

        productDataHandler = ProductDataHandler(
            iapHelper = iapHelperProvider,
            promotionEligibilityResponseListener = promotionEligibilityListener,
        )

        val previousLogHandler = currentLogHandler
        val previousLogLevel = Config.logLevel
        val loggedMessages = mutableListOf<String>()
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {
                loggedMessages.add(msg)
            }
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        Config.logLevel = LogLevel.VERBOSE

        try {
            productDataHandler.getProductDetails(
                productIds = setOf("iap", "missing"),
                productType = ProductType.INAPP,
                onReceive = { },
                onError = unexpectedOnError,
            )

            val successErrorVo = mockk<ErrorVo> {
                every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
            }
            capturedListener.captured.onGetProducts(
                successErrorVo,
                arrayListOf(createProductVo(itemId = "iap", type = "item")),
            )
            capturedPromotionOnSuccess.captured.invoke(
                listOf(createPromotionEligibilityVo(itemId = "iap", pricing = "None")),
            )
        } finally {
            currentLogHandler = previousLogHandler
            Config.logLevel = previousLogLevel
        }

        val expectedMessage = "${LogIntent.GALAXY_WARNING.emojiList.joinToString("")} " +
            GalaxyStrings.GET_PRODUCT_DETAILS_RESPONSE_MISSING_PRODUCTS.format(
                "iap, missing",
                "missing",
            )
        assertThat(loggedMessages).contains(expectedMessage)
    }

    @OptIn(GalaxySerialOperation::class, InternalRevenueCatAPI::class)
    @Test
    fun `no missing products warning logged when all products are returned`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val promotionEligibilityListener = mockk<PromotionEligibilityResponseListener>()
        val capturedPromotionOnSuccess = slot<(List<com.samsung.android.sdk.iap.lib.vo.PromotionEligibilityVo>) -> Unit>()
        every {
            promotionEligibilityListener.getPromotionEligibilities(any(), capture(capturedPromotionOnSuccess), any())
        } returns Unit

        productDataHandler = ProductDataHandler(
            iapHelper = iapHelperProvider,
            promotionEligibilityResponseListener = promotionEligibilityListener,
        )

        val previousLogHandler = currentLogHandler
        val previousLogLevel = Config.logLevel
        val loggedMessages = mutableListOf<String>()
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {
                loggedMessages.add(msg)
            }
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        Config.logLevel = LogLevel.VERBOSE

        try {
            productDataHandler.getProductDetails(
                productIds = setOf("iap", "sub"),
                productType = ProductType.INAPP,
                onReceive = { },
                onError = unexpectedOnError,
            )

            val successErrorVo = mockk<ErrorVo> {
                every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
            }
            capturedListener.captured.onGetProducts(
                successErrorVo,
                arrayListOf(
                    createProductVo(itemId = "iap", type = "item"),
                    createProductVo(itemId = "sub", type = "item"),
                ),
            )
            capturedPromotionOnSuccess.captured.invoke(
                listOf(
                    createPromotionEligibilityVo(itemId = "iap", pricing = "None"),
                    createPromotionEligibilityVo(itemId = "sub", pricing = "None"),
                ),
            )
        } finally {
            currentLogHandler = previousLogHandler
            Config.logLevel = previousLogLevel
        }

        assertThat(loggedMessages).noneMatch { it.contains("returned product details for only") }
    }
}
