package com.revenuecat.purchases.galaxy

import android.content.Context
import android.os.Handler
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.galaxy.IAPHelperProvider
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import android.os.Looper
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import io.mockk.mockk
import io.mockk.slot
import io.mockk.every
import io.mockk.verify
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.fail
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.galaxy.conversions.toSamsungIAPOperationMode
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener

class GalaxyBillingWrapperTest {

    private val stateProvider = mockk<PurchasesStateProvider>(relaxed = true)
    private var handler = mockk<Handler>(relaxed = true)
    private var context = mockk<Context>()
    private val iapHelperProvider = mockk<IAPHelperProvider>(relaxed = true)
    private val productDataHandler = mockk<ProductDataResponseListener>(relaxed = true)
    private lateinit var wrapper: GalaxyBillingWrapper
    private var previousLogHandler: LogHandler? = null

    @Before
    fun setUp() {
        previousLogHandler = currentLogHandler
        currentLogHandler = mockk(relaxed = true)

        mockkStatic(Looper::class)
        val mockMainLooper = mockk<Looper> {
            every { thread } returns Thread.currentThread()
        }
        every { Looper.getMainLooper() } returns mockMainLooper

        every { handler.post(any()) } answers {
            (it.invocation.args[0] as Runnable).run()
            true
        }

        wrapper = GalaxyBillingWrapper(
            stateProvider,
            context = context,
            billingMode = GalaxyBillingMode.TEST,
            finishTransactions = true,
            iapHelperProvider = iapHelperProvider,
            productDataHandler = productDataHandler,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
        previousLogHandler?.let { currentLogHandler = it }
    }

    @Test
    fun `init sets operation mode with billing mode`() {
        val customBillingMode = GalaxyBillingMode.ALWAYS_FAIL
        val customIapHelperProvider = mockk<IAPHelperProvider>(relaxed = true)
        val customProductDataHandler = mockk<ProductDataResponseListener>(relaxed = true)
        val customPurchaseHandler = mockk<PurchaseResponseListener>(relaxed = true)

        GalaxyBillingWrapper(
            stateProvider = stateProvider,
            context = context,
            finishTransactions = true,
            billingMode = customBillingMode,
            iapHelperProvider = customIapHelperProvider,
            productDataHandler = customProductDataHandler,
            purchaseHandler = customPurchaseHandler,
        )

        verify(exactly = 1) {
            customIapHelperProvider.setOperationMode(
                mode = customBillingMode.toSamsungIAPOperationMode(),
            )
        }
    }

    @Test
    fun `getStorefront returns unsupported error`() {
        var receivedError: PurchasesError? = null

        wrapper.getStorefront(
            onSuccess = { fail("Expected getStorefront to be unsupported") },
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo(GalaxyStrings.STOREFRONT_NOT_SUPPORTED)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryProductDetailsAsync delegates to ProductDataHandler when listener set`() {
        wrapper.purchasesUpdatedListener = mockk(relaxed = true)

        val productIds = setOf("prod_1", "prod_2")
        val expectedType = ProductType.SUBS
        val onReceive = mockk<(List<StoreProduct>) -> Unit>(relaxed = true)
        val onError = mockk<(PurchasesError) -> Unit>(relaxed = true)

        val idsSlot = slot<Set<String>>()
        val typeSlot = slot<ProductType>()
        val onReceiveSlot = slot<(List<StoreProduct>) -> Unit>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()

        wrapper.queryProductDetailsAsync(
            productType = expectedType,
            productIds = productIds,
            onReceive = onReceive,
            onError = onError,
        )

        verify(exactly = 1) {
            productDataHandler.getProductDetails(
                productIds = capture(idsSlot),
                productType = capture(typeSlot),
                onReceive = capture(onReceiveSlot),
                onError = capture(onErrorSlot),
            )
        }

        assertThat(idsSlot.captured).containsExactlyInAnyOrderElementsOf(productIds)
        assertThat(typeSlot.captured).isEqualTo(expectedType)

        val expectedProducts = listOf(mockk<StoreProduct>())
        onReceiveSlot.captured(expectedProducts)
        verify(exactly = 1) { onReceive(expectedProducts) }

        val expectedError = mockk<PurchasesError>()
        onErrorSlot.captured(expectedError)
        verify(exactly = 1) { onError(expectedError) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryProductDetailsAsync is ignored when purchasesUpdatedListener is null`() {
        wrapper.purchasesUpdatedListener = null

        wrapper.queryProductDetailsAsync(
            productType = ProductType.INAPP,
            productIds = setOf("prod_1"),
            onReceive = { fail("should be ignored") },
            onError = { fail("should be ignored") },
        )

        verify(exactly = 0) { productDataHandler.getProductDetails(any(), any(), any(), any()) }
    }
}
