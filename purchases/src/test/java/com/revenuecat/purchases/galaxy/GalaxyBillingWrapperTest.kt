package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import android.os.Looper
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
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.galaxy.listener.AcknowledgePurchaseResponseListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import com.revenuecat.purchases.galaxy.constants.GalaxyConsumeOrAcknowledgeStatusCode
import com.samsung.android.sdk.iap.lib.vo.AcknowledgeVo

class GalaxyBillingWrapperTest : GalaxyStoreTest() {

    private val stateProvider = mockk<PurchasesStateProvider>(relaxed = true)
    private var handler = mockk<Handler>(relaxed = true)
    private var context = mockk<Context>()
    private var deviceCache = mockk<DeviceCache>()
    private val iapHelperProvider = mockk<IAPHelperProvider>(relaxed = true)
    private val productDataHandler = mockk<ProductDataResponseListener>(relaxed = true)
    private val purchaseHandlerMock = mockk<PurchaseResponseListener>(relaxed = true)
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
            iapHelper = iapHelperProvider,
            productDataHandler = productDataHandler,
            deviceCache = deviceCache,
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
            billingMode = customBillingMode,
            iapHelper = customIapHelperProvider,
            productDataHandler = customProductDataHandler,
            purchaseHandler = customPurchaseHandler,
            deviceCache = deviceCache,
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

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync errors for non-Galaxy purchasing data`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val unexpectedPurchasingData = mockk<com.revenuecat.purchases.models.PurchasingData>()

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = unexpectedPurchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        verify(exactly = 0) {
            purchaseHandlerMock.purchase(any(), any(), any(), any())
        }

        val errorSlot = slot<PurchasesError>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesFailedToUpdate(capture(errorSlot)) }
        assertThat(errorSlot.captured.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(errorSlot.captured.underlyingErrorMessage)
            .isEqualTo(PurchaseStrings.INVALID_PURCHASE_TYPE.format("Galaxy", "GalaxyPurchasingData"))
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync exits when replaceProductInfo is provided`() {
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = mockk(relaxed = true)

        val storeProduct = createStoreProduct()
        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = mockk<ReplaceProductInfo>(),
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        verify(exactly = 0) {
            purchaseHandlerMock.purchase(any(), any(), any(), any())
        }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync dispatches purchase and forwards success`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val storeProduct = createStoreProduct(presentedOfferingContext = PresentedOfferingContext("offering"))
        val onSuccessSlot = slot<(PurchaseVo) -> Unit>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()

        every {
            purchaseHandlerMock.purchase(any(), any(), capture(onSuccessSlot), capture(onErrorSlot))
        } answers { /* captured */ }

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = null,
            presentedOfferingContext = storeProduct.presentedOfferingContext,
            isPersonalizedPrice = null,
        )

        val purchaseVo = createPurchaseVo(
            paymentId = "paymentId",
            purchaseId = "purchaseId",
            orderId = "orderId",
            purchaseDate = "2024-01-15 13:45:20",
            type = "subscription",
            itemId = storeProduct.id,
        )
        onSuccessSlot.captured(purchaseVo)

        verify(exactly = 1) {
            purchaseHandlerMock.purchase(
                appUserID = "user",
                productId = storeProduct.id,
                onSuccess = any(),
                onError = any(),
            )
        }

        val transactionsSlot = slot<List<StoreTransaction>>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesUpdated(capture(transactionsSlot)) }
        val transaction = transactionsSlot.captured.single()
        assertThat(transaction.productIds).containsExactly(storeProduct.id)
        assertThat(transaction.presentedOfferingContext).isEqualTo(storeProduct.presentedOfferingContext)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync forwards errors from purchase handler`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val storeProduct = createStoreProduct()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()

        every {
            purchaseHandlerMock.purchase(any(), any(), any(), capture(onErrorSlot))
        } answers { /* captured */ }

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        val expectedError = PurchasesError(PurchasesErrorCode.StoreProblemError, "failure")
        onErrorSlot.captured(expectedError)

        verify(exactly = 1) {
            purchaseHandlerMock.purchase(
                appUserID = "user",
                productId = storeProduct.id,
                onSuccess = any(),
                onError = any(),
            )
        }

        val errorSlot = slot<PurchasesError>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesFailedToUpdate(capture(errorSlot)) }
        assertThat(errorSlot.captured).isEqualTo(expectedError)
    }
    
    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync errors when purchasing OTP`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener


        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product("productId", productType = ProductType.INAPP),
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        val errorSlot = slot<PurchasesError>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesFailedToUpdate(capture(errorSlot)) }
        assertThat(errorSlot.captured.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(errorSlot.captured.underlyingErrorMessage).isEqualTo(GalaxyStrings.GALAXY_OTPS_NOT_SUPPORTED)

        verify(exactly = 0) { purchaseHandlerMock.purchase(any(), any(), any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumeAndSave caches token when finishTransactions is false`() {
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>(relaxed = true)
        val wrapper = createWrapper(acknowledgePurchaseHandler = acknowledgePurchaseHandler)
        every { deviceCache.addSuccessfullyPostedToken("token") } returns Unit

        wrapper.consumeAndSave(
            finishTransactions = false,
            purchase = storeTransaction("token"),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.RESTORE,
        )

        verify(exactly = 0) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        verify(exactly = 1) { deviceCache.addSuccessfullyPostedToken("token") }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumeAndSave caches token for unknown product type`() {
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>(relaxed = true)
        val wrapper = createWrapper(acknowledgePurchaseHandler = acknowledgePurchaseHandler)
        every { deviceCache.addSuccessfullyPostedToken("token") } returns Unit

        wrapper.consumeAndSave(
            finishTransactions = true,
            purchase = storeTransaction("token", type = ProductType.UNKNOWN),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        verify(exactly = 0) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        verify(exactly = 1) { deviceCache.addSuccessfullyPostedToken("token") }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumeAndSave does nothing for pending purchases`() {
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>(relaxed = true)
        val wrapper = createWrapper(acknowledgePurchaseHandler = acknowledgePurchaseHandler)
        every { deviceCache.addSuccessfullyPostedToken(any()) } returns Unit

        wrapper.consumeAndSave(
            finishTransactions = true,
            purchase = storeTransaction("token", state = PurchaseState.PENDING),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        verify(exactly = 0) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        verify(exactly = 0) { deviceCache.addSuccessfullyPostedToken(any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumeAndSave acknowledges subscriptions`() {
        val ackTransactionSlot = slot<StoreTransaction>()
        val ackCallbackSlot = slot<(AcknowledgeVo) -> Unit>()
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>()
        every { deviceCache.addSuccessfullyPostedToken(any()) } returns Unit
        every {
            acknowledgePurchaseHandler.acknowledgePurchase(
                capture(ackTransactionSlot),
                capture(ackCallbackSlot),
                any(),
            )
        } answers {
            val acknowledgementResult = mockk<AcknowledgeVo> {
                every { statusCode } returns GalaxyConsumeOrAcknowledgeStatusCode.SUCCESS.code
                every { statusString } returns "Success"
            }
            ackCallbackSlot.captured(acknowledgementResult)
        }
        val wrapper = createWrapper(acknowledgePurchaseHandler = acknowledgePurchaseHandler)

        wrapper.consumeAndSave(
            finishTransactions = true,
            purchase = storeTransaction("token-sub", type = ProductType.SUBS),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        verify(exactly = 1) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        verify(exactly = 1) { deviceCache.addSuccessfullyPostedToken("token-sub") }
    }

    private fun createWrapper(
        finishTransactions: Boolean = true,
        billingMode: GalaxyBillingMode = GalaxyBillingMode.TEST,
        purchaseHandler: PurchaseResponseListener = purchaseHandlerMock,
        acknowledgePurchaseHandler: AcknowledgePurchaseResponseListener = mockk(relaxed = true),
    ): GalaxyBillingWrapper {
        return GalaxyBillingWrapper(
            stateProvider,
            context = context,
            billingMode = billingMode,
            iapHelper = iapHelperProvider,
            productDataHandler = productDataHandler,
            purchaseHandler = purchaseHandler,
            deviceCache = deviceCache,
            acknowledgePurchaseHandler = acknowledgePurchaseHandler,
        )
    }

    private fun createStoreProduct(
        id: String = "productId",
        type: ProductType = ProductType.SUBS,
        presentedOfferingContext: PresentedOfferingContext? = null,
    ): GalaxyStoreProduct = GalaxyStoreProduct(
        id = id,
        type = type,
        price = Price(
            formatted = "$1.00",
            amountMicros = 1_000_000,
            currencyCode = "USD",
        ),
        name = "name",
        title = "title",
        description = "description",
        period = Period.create("P1M"),
        subscriptionOptions = null,
        defaultOption = null,
        presentedOfferingContext = presentedOfferingContext,
    )

    private fun storeTransaction(
        token: String,
        type: ProductType = ProductType.INAPP,
        state: PurchaseState = PurchaseState.PURCHASED,
    ) = mockk<StoreTransaction> {
        every { purchaseToken } returns token
        every { this@mockk.type } returns type
        every { purchaseState } returns state
        every { purchaseType } returns PurchaseType.GALAXY_PURCHASE
        every { signature } returns null
    }
}
