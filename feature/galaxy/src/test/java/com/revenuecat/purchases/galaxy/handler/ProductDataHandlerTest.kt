package com.revenuecat.purchases.galaxy.handler

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.galaxy.conversions.toStoreProduct
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
        assertThat(productDataHandler.productsCache).containsKeys("iap", "sub")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `getProductDetails only requests missing products and returns cached results`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

        val capturedPromotionEligibilityListener = slot<OnGetPromotionEligibilityListener>()
        every {
            iapHelperProvider.getPromotionEligibility(any(), capture(capturedPromotionEligibilityListener))
        } returns true

        val cachedProduct = createProductVo(itemId = "cached", type = "item").toStoreProduct()
        productDataHandler.productsCache[cachedProduct.id] = cachedProduct

        var receivedProducts: List<StoreProduct>? = null

        productDataHandler.getProductDetails(
            productIds = setOf("cached", "new"),
            productType = ProductType.INAPP,
            onReceive = { receivedProducts = it },
            onError = unexpectedOnError,
        )

        verify(exactly = 1) { iapHelperProvider.getProductsDetails("new", any()) }

        val successErrorVo = mockk<ErrorVo> {
            every { errorCode } returns GalaxyErrorCode.IAP_ERROR_NONE.code
        }
        val newProduct = createProductVo(itemId = "new", type = "item")
        capturedListener.captured.onGetProducts(successErrorVo, arrayListOf(newProduct))

        verify(exactly = 1) {
            iapHelperProvider.getPromotionEligibility(
                itemIDs = "new",
                onGetPromotionEligibilityListener = any(),
            )
        }

        capturedPromotionEligibilityListener.captured.onGetPromotionEligibility(
            successErrorVo,
            arrayListOf(createPromotionEligibilityVo(itemId = "new", pricing = "None")),
        )

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactlyInAnyOrder("cached", "new")
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
}
