package com.revenuecat.purchases.galaxy

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.galaxy.constants.GalaxyConsumeOrAcknowledgeStatusCode
import com.revenuecat.purchases.galaxy.conversions.toSamsungIAPOperationMode
import com.revenuecat.purchases.galaxy.listener.AcknowledgePurchaseResponseListener
import com.revenuecat.purchases.galaxy.listener.ChangeSubscriptionPlanResponseListener
import com.revenuecat.purchases.galaxy.listener.GetOwnedListResponseListener
import com.revenuecat.purchases.galaxy.listener.ProductDataResponseListener
import com.revenuecat.purchases.galaxy.listener.PurchaseResponseListener
import com.revenuecat.purchases.galaxy.logging.currentLogHandler
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.vo.AcknowledgeVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.fail

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
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
        mockkStatic(Base64::class)
        val mockMainLooper = mockk<Looper> {
            every { thread } returns Thread.currentThread()
        }
        every { Looper.getMainLooper() } returns mockMainLooper

        every { Base64.encode(any(), any()) } answers {
            val data = it.invocation.args[0] as ByteArray
            java.util.Base64.getEncoder().encode(data)
        }

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
        unmockkStatic(Base64::class)
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
    fun `queryAllPurchases requests owned list and returns mapped transactions`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = capture(onErrorSlot),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null
        wrapper.queryAllPurchases(
            appUserID = "app_user",
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it },
        )

        val ownedProduct = createOwnedProductVo(
            itemId = "product",
            purchaseId = "token",
            type = "subscription",
            purchaseDate = "2023-02-01 00:00:00",
        )
        onSuccessSlot.captured.invoke(arrayListOf(ownedProduct))

        assertThat(receivedError).isNull()
        val transactions = receivedTransactions
        assertThat(transactions).isNotNull
        assertThat(transactions!!.map { it.purchaseToken }).containsExactly("token")
        assertThat(transactions.map { it.type }).containsExactly(ProductType.SUBS)
        verify(exactly = 1) { getOwnedListHandler.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryAllPurchases forwards errors from getOwnedList`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = any(),
                onError = capture(onErrorSlot),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var receivedError: PurchasesError? = null
        wrapper.queryAllPurchases(
            appUserID = "app_user",
            onReceivePurchaseHistory = { fail("Expected error callback") },
            onReceivePurchaseHistoryError = { receivedError = it },
        )

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        onErrorSlot.captured.invoke(error)

        assertThat(receivedError).isEqualTo(error)
        verify(exactly = 1) { getOwnedListHandler.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryAllPurchases returns InvalidReceiptError when conversion fails`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var receivedError: PurchasesError? = null
        wrapper.queryAllPurchases(
            appUserID = "app_user",
            onReceivePurchaseHistory = { fail("Expected error callback") },
            onReceivePurchaseHistoryError = { receivedError = it },
        )

        val invalidOwnedProduct = createOwnedProductVo(
            itemId = "product",
            purchaseId = "token",
            type = "subscription",
            purchaseDate = "invalid-date",
        )
        onSuccessSlot.captured.invoke(arrayListOf(invalidOwnedProduct))

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidReceiptError)
        assertThat(receivedError?.underlyingErrorMessage)
            .contains(GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_DATE.format("invalid-date"))
        verify(exactly = 1) { getOwnedListHandler.getOwnedList(any(), any()) }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `findPurchaseInPurchaseHistory returns matching transaction`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var receivedTransaction: StoreTransaction? = null
        var receivedError: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = "app_user",
            productType = ProductType.SUBS,
            productId = "target_product",
            onCompletion = { receivedTransaction = it },
            onError = { receivedError = it },
        )

        onSuccessSlot.captured.invoke(
            arrayListOf(
                createOwnedProductVo(
                    itemId = "other_product",
                    purchaseId = "other_token",
                    type = "subscription",
                    purchaseDate = "2023-02-01 00:00:00",
                ),
                createOwnedProductVo(
                    itemId = "target_product",
                    purchaseId = "match_token",
                    type = "subscription",
                    purchaseDate = "2023-02-02 00:00:00",
                ),
            ),
        )

        assertThat(receivedError).isNull()
        val transaction = receivedTransaction ?: fail("Expected transaction")
        assertThat(transaction.purchaseToken).isEqualTo("match_token")
        assertThat(transaction.productIds).containsExactly("target_product")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `findPurchaseInPurchaseHistory returns PurchaseInvalidError when no match`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var completionCalled = false
        var receivedError: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = "app_user",
            productType = ProductType.SUBS,
            productId = "missing_product",
            onCompletion = { completionCalled = true },
            onError = { receivedError = it },
        )

        onSuccessSlot.captured.invoke(
            arrayListOf(
                createOwnedProductVo(
                    itemId = "other_product",
                    purchaseId = "other_token",
                    type = "subscription",
                    purchaseDate = "2023-02-01 00:00:00",
                ),
            ),
        )

        assertThat(completionCalled).isFalse()
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(receivedError?.underlyingErrorMessage)
            .isEqualTo(PurchaseStrings.NO_EXISTING_PURCHASE.format("missing_product"))
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `findPurchaseInPurchaseHistory forwards errors from queryAllPurchases`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = any(),
                onError = capture(onErrorSlot),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var completionCalled = false
        var receivedError: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = "app_user",
            productType = ProductType.SUBS,
            productId = "product",
            onCompletion = { completionCalled = true },
            onError = { receivedError = it },
        )

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        onErrorSlot.captured.invoke(error)

        assertThat(completionCalled).isFalse()
        assertThat(receivedError).isEqualTo(error)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryPurchases filters expired subscriptions and hashes tokens`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val now = parseGalaxyDate("2024-02-10 00:00:00")
        val wrapper = createWrapper(
            getOwnedListHandler = getOwnedListHandler,
            dateProvider = FixedDateProvider(now),
        )

        var receivedMap: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "app_user",
            onSuccess = { receivedMap = it },
            onError = { fail("Expected success") },
        )

        val activeOwnedProduct = createOwnedProductVo(
            itemId = "active_product",
            purchaseId = "active_token",
            type = "subscription",
            purchaseDate = "2024-02-01 00:00:00",
        ).also {
            every { it.subscriptionEndDate } returns "2024-02-20 00:00:00"
        }
        val expiredOwnedProduct = createOwnedProductVo(
            itemId = "expired_product",
            purchaseId = "expired_token",
            type = "subscription",
            purchaseDate = "2024-01-01 00:00:00",
        ).also {
            every { it.subscriptionEndDate } returns "2024-02-05 00:00:00"
        }

        onSuccessSlot.captured.invoke(arrayListOf(activeOwnedProduct, expiredOwnedProduct))

        val purchases = receivedMap ?: fail("Expected purchases")
        assertThat(purchases.keys).containsExactly("active_token".sha1())
        assertThat(purchases.values.map { it.purchaseToken }).containsExactly("active_token")
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryPurchases returns empty map when all subscriptions are expired`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val now = parseGalaxyDate("2024-02-10 00:00:00")
        val wrapper = createWrapper(
            getOwnedListHandler = getOwnedListHandler,
            dateProvider = FixedDateProvider(now),
        )

        var receivedMap: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "app_user",
            onSuccess = { receivedMap = it },
            onError = { fail("Expected success") },
        )

        val expiredOwnedProduct = createOwnedProductVo(
            itemId = "expired_product",
            purchaseId = "expired_token",
            type = "subscription",
            purchaseDate = "2024-01-01 00:00:00",
        ).also {
            every { it.subscriptionEndDate } returns "2024-02-01 00:00:00"
        }
        onSuccessSlot.captured.invoke(arrayListOf(expiredOwnedProduct))

        val purchases = receivedMap ?: fail("Expected purchases")
        assertThat(purchases).isEmpty()
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryPurchases forwards errors from getOwnedList`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = any(),
                onError = capture(onErrorSlot),
            )
        } answers { }
        val wrapper = createWrapper(getOwnedListHandler = getOwnedListHandler)

        var receivedError: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "app_user",
            onSuccess = { fail("Expected error callback") },
            onError = { receivedError = it },
        )

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        onErrorSlot.captured.invoke(error)

        assertThat(receivedError).isEqualTo(error)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `queryPurchases returns InvalidReceiptError when conversion fails`() {
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        val onSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(onSuccessSlot),
                onError = any(),
            )
        } answers { }
        val now = parseGalaxyDate("2024-02-10 00:00:00")
        val wrapper = createWrapper(
            getOwnedListHandler = getOwnedListHandler,
            dateProvider = FixedDateProvider(now),
        )

        var receivedError: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "app_user",
            onSuccess = { fail("Expected error callback") },
            onError = { receivedError = it },
        )

        val invalidOwnedProduct = createOwnedProductVo(
            itemId = "product",
            purchaseId = "token",
            type = "subscription",
            purchaseDate = "invalid-date",
        ).also {
            every { it.subscriptionEndDate } returns "2024-02-20 00:00:00"
        }
        onSuccessSlot.captured.invoke(arrayListOf(invalidOwnedProduct))

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.InvalidReceiptError)
        assertThat(receivedError?.underlyingErrorMessage)
            .contains(GalaxyStrings.ERROR_CANNOT_PARSE_PURCHASE_DATE.format("invalid-date"))
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync errors for non-Galaxy purchasing data`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val wrapper = createWrapper()
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val unexpectedPurchasingData = mockk<PurchasingData>()

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
    fun `makePurchaseAsync dispatches change subscription plan when replaceProductInfo is provided`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val changeSubscriptionPlanHandler = mockk<ChangeSubscriptionPlanResponseListener>(relaxed = true)
        val wrapper = createWrapper(changeSubscriptionPlanHandler = changeSubscriptionPlanHandler)
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val storeProduct = createStoreProduct()
        val oldPurchase = storeTransaction(
            token = "old-token",
            type = ProductType.SUBS,
            productId = "old-product",
        )
        val replacementMode = GalaxyReplacementMode.INSTANT_PRORATED_CHARGE
        val onSuccessSlot = slot<(PurchaseVo) -> Unit>()
        val onErrorSlot = slot<(PurchasesError) -> Unit>()

        every {
            changeSubscriptionPlanHandler.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                capture(onSuccessSlot),
                capture(onErrorSlot),
            )
        } answers { }

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = ReplaceProductInfo(
                oldPurchase = oldPurchase,
                replacementMode = replacementMode,
            ),
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        verify(exactly = 1) {
            changeSubscriptionPlanHandler.changeSubscriptionPlan(
                appUserID = "user",
                oldPurchase = oldPurchase,
                newProductId = storeProduct.id,
                prorationMode = replacementMode,
                onSuccess = any(),
                onError = any(),
            )
        }
        verify(exactly = 0) {
            purchaseHandlerMock.purchase(any(), any(), any(), any())
        }

        val purchaseVo = createPurchaseVo(
            paymentId = "paymentId",
            purchaseId = "purchaseId",
            orderId = "orderId",
            purchaseDate = "2024-01-15 13:45:20",
            type = "subscription",
            itemId = storeProduct.id,
        )
        onSuccessSlot.captured(purchaseVo)

        val transactionsSlot = slot<List<StoreTransaction>>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesUpdated(capture(transactionsSlot)) }
        val transaction = transactionsSlot.captured.single()
        assertThat(transaction.productIds).containsExactly(storeProduct.id)
        assertThat(transaction.replacementMode).isEqualTo(replacementMode)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync defaults to GalaxyReplacementMode default when non-Galaxy replacement mode provided`() {
        val changeSubscriptionPlanHandler = mockk<ChangeSubscriptionPlanResponseListener>(relaxed = true)
        val wrapper = createWrapper(changeSubscriptionPlanHandler = changeSubscriptionPlanHandler)
        wrapper.purchasesUpdatedListener = mockk(relaxed = true)

        val storeProduct = createStoreProduct()
        val oldPurchase = storeTransaction(
            token = "old-token",
            type = ProductType.SUBS,
            productId = "old-product",
        )
        val prorationModeSlot = slot<GalaxyReplacementMode>()

        every {
            changeSubscriptionPlanHandler.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                capture(prorationModeSlot),
                any(),
                any(),
            )
        } answers { }

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = ReplaceProductInfo(
                oldPurchase = oldPurchase,
                replacementMode = GoogleReplacementMode.WITHOUT_PRORATION,
            ),
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        assertThat(prorationModeSlot.captured).isEqualTo(GalaxyReplacementMode.default)
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `makePurchaseAsync forwards errors from change subscription plan handler`() {
        val purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val changeSubscriptionPlanHandler = mockk<ChangeSubscriptionPlanResponseListener>(relaxed = true)
        val wrapper = createWrapper(changeSubscriptionPlanHandler = changeSubscriptionPlanHandler)
        wrapper.purchasesUpdatedListener = purchasesUpdatedListener

        val storeProduct = createStoreProduct()
        val oldPurchase = storeTransaction(
            token = "old-token",
            type = ProductType.SUBS,
            productId = "old-product",
        )
        val onErrorSlot = slot<(PurchasesError) -> Unit>()

        every {
            changeSubscriptionPlanHandler.changeSubscriptionPlan(
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(onErrorSlot),
            )
        } answers { }

        wrapper.makePurchaseAsync(
            activity = mockk<Activity>(),
            appUserID = "user",
            purchasingData = GalaxyPurchasingData.Product(
                productId = storeProduct.id,
                productType = storeProduct.type,
            ),
            replaceProductInfo = ReplaceProductInfo(oldPurchase = oldPurchase),
            presentedOfferingContext = null,
            isPersonalizedPrice = null,
        )

        val expectedError = PurchasesError(PurchasesErrorCode.StoreProblemError, "failure")
        onErrorSlot.captured(expectedError)

        verify(exactly = 0) {
            purchaseHandlerMock.purchase(any(), any(), any(), any())
        }

        val errorSlot = slot<PurchasesError>()
        verify(exactly = 1) { purchasesUpdatedListener.onPurchasesFailedToUpdate(capture(errorSlot)) }
        assertThat(errorSlot.captured).isEqualTo(expectedError)
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
        val ownedListSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>()
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        every { deviceCache.addSuccessfullyPostedToken(any()) } returns Unit
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(ownedListSuccessSlot),
                onError = any(),
            )
        } answers { }
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
        val wrapper = createWrapper(
            acknowledgePurchaseHandler = acknowledgePurchaseHandler,
            getOwnedListHandler = getOwnedListHandler,
        )

        val productId = "productId"
        wrapper.consumeAndSave(
            finishTransactions = true,
            purchase = storeTransaction("token-sub", type = ProductType.SUBS, productId = productId),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        ownedListSuccessSlot.captured.invoke(
            arrayListOf(
                createOwnedProductVo(
                    itemId = productId,
                    purchaseId = "token-sub",
                    type = "subscription",
                    purchaseDate = "2024-02-02 00:00:00",
                ),
            ),
        )

        verify(exactly = 1) { getOwnedListHandler.getOwnedList(any(), any()) }
        verify(exactly = 1) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        assertThat(ackTransactionSlot.captured.purchaseToken).isEqualTo("token-sub")
        verify(exactly = 1) { deviceCache.addSuccessfullyPostedToken("token-sub") }
    }

    @OptIn(GalaxySerialOperation::class)
    @Test
    fun `consumeAndSave does not acknowledge already acknowledged subscriptions`() {
        val ownedListSuccessSlot = slot<(ArrayList<OwnedProductVo>) -> Unit>()
        val acknowledgePurchaseHandler = mockk<AcknowledgePurchaseResponseListener>(relaxed = true)
        val getOwnedListHandler = mockk<GetOwnedListResponseListener>()
        every { deviceCache.addSuccessfullyPostedToken(any()) } returns Unit
        every {
            getOwnedListHandler.getOwnedList(
                onSuccess = capture(ownedListSuccessSlot),
                onError = any(),
            )
        } answers { }
        val wrapper = createWrapper(
            acknowledgePurchaseHandler = acknowledgePurchaseHandler,
            getOwnedListHandler = getOwnedListHandler,
        )

        val productId = "productId"
        wrapper.consumeAndSave(
            finishTransactions = true,
            purchase = storeTransaction("token-sub", type = ProductType.SUBS, productId = productId),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        ownedListSuccessSlot.captured.invoke(
            arrayListOf(
                createOwnedProductVo(
                    itemId = productId,
                    purchaseId = "token-sub",
                    type = "subscription",
                    purchaseDate = "2024-02-02 00:00:00",
                    acknowledgedStatus = HelperDefine.AcknowledgedStatus.ACKNOWLEDGED,
                ),
            ),
        )

        verify(exactly = 1) { getOwnedListHandler.getOwnedList(any(), any()) }
        verify(exactly = 0) { acknowledgePurchaseHandler.acknowledgePurchase(any(), any(), any()) }
        verify(exactly = 0) { deviceCache.addSuccessfullyPostedToken(any()) }
    }

    private fun createWrapper(
        finishTransactions: Boolean = true,
        billingMode: GalaxyBillingMode = GalaxyBillingMode.TEST,
        dateProvider: DateProvider = DefaultDateProvider(),
        purchaseHandler: PurchaseResponseListener = purchaseHandlerMock,
        acknowledgePurchaseHandler: AcknowledgePurchaseResponseListener = mockk(relaxed = true),
        getOwnedListHandler: GetOwnedListResponseListener = mockk(relaxed = true),
        changeSubscriptionPlanHandler: ChangeSubscriptionPlanResponseListener = mockk(relaxed = true),
    ): GalaxyBillingWrapper {
        return GalaxyBillingWrapper(
            stateProvider,
            context = context,
            billingMode = billingMode,
            iapHelper = iapHelperProvider,
            productDataHandler = productDataHandler,
            purchaseHandler = purchaseHandler,
            deviceCache = deviceCache,
            dateProvider = dateProvider,
            acknowledgePurchaseHandler = acknowledgePurchaseHandler,
            getOwnedListHandler = getOwnedListHandler,
            changeSubscriptionPlanHandler = changeSubscriptionPlanHandler,
        )
    }

    private fun parseGalaxyDate(dateString: String): Date {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse(dateString)!!
    }

    private class FixedDateProvider(
        override val now: Date,
    ) : DateProvider

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
        period = Period.Factory.create("P1M"),
        subscriptionOptions = null,
        defaultOption = null,
        presentedOfferingContext = presentedOfferingContext,
    )

    private fun storeTransaction(
        token: String,
        type: ProductType = ProductType.INAPP,
        state: PurchaseState = PurchaseState.PURCHASED,
        productId: String = "productId",
    ) = mockk<StoreTransaction> {
        every { purchaseToken } returns token
        every { this@mockk.type } returns type
        every { purchaseState } returns state
        every { purchaseType } returns PurchaseType.GALAXY_PURCHASE
        every { signature } returns null
        every { productIds } returns listOf(productId)
    }
}
