package com.revenuecat.purchases.galaxy.handler

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.galaxy.GalaxyStoreTest
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.constants.GalaxyErrorCode
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import io.mockk.CapturingSlot
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

    private lateinit var timeoutRunnableSlot: CapturingSlot<Runnable>

    private val unexpectedOnReceive: (List<StoreProduct>) -> Unit = { fail("Expected onError to be called") }
    private val unexpectedOnError: (PurchasesError) -> Unit = { fail("Expected onReceive to be called") }

    @Before
    fun setup() {
        timeoutRunnableSlot = slot()
        iapHelperProvider = mockk(relaxed = true)
        mainHandler = mockk(relaxed = true)
        every { mainHandler.postDelayed(capture(timeoutRunnableSlot), any()) } returns true
        productDataHandler = ProductDataHandler(iapHelperProvider, mainHandler)
    }

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

    @Test
    fun `successful product response caches products and forwards only matching type`() {
        val capturedListener = slot<OnGetProductsDetailsListener>()
        every { iapHelperProvider.getProductsDetails(any(), capture(capturedListener)) } returns Unit

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

        assertThat(receivedProducts).isNotNull
        assertThat(receivedProducts!!.map { it.id }).containsExactly("sub")
        assertThat(productDataHandler.productDataCache).containsKeys("iap", "sub")
    }

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

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)

        // Next request should proceed because the in-flight one was cleared
        productDataHandler.getProductDetails(
            productIds = setOf("next"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
    }

    @Test
    fun `timeout invokes onError with unknown error and clears request`() {
        var receivedError: PurchasesError? = null
        productDataHandler.getProductDetails(
            productIds = setOf("timeout"),
            productType = ProductType.INAPP,
            onReceive = unexpectedOnReceive,
            onError = { receivedError = it },
        )

        val timeoutRunnable = timeoutRunnableSlot.captured
        timeoutRunnable.run()

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnknownError)

        productDataHandler.getProductDetails(
            productIds = setOf("next"),
            productType = ProductType.SUBS,
            onReceive = unexpectedOnReceive,
            onError = unexpectedOnError,
        )
        verify(exactly = 2) { iapHelperProvider.getProductsDetails(any(), any()) }
    }
}
