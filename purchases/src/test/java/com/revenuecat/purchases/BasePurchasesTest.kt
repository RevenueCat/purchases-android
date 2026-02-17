//  Purchases
//
//  Copyright © 2023 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.backup.BackupManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT
import com.revenuecat.purchases.blockstore.BlockstoreHelper
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DefaultLocaleProvider
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.deeplinks.WebPurchaseRedemptionHelper
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.FontLoader
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.utils.PurchaseParamsValidator
import com.revenuecat.purchases.utils.Result
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencyManager
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.After
import org.junit.Before
import org.robolectric.Shadows.shadowOf
import java.util.Date

internal open class BasePurchasesTest {
    protected val mockBillingAbstract: BillingAbstract = mockk()
    protected val mockBackend: Backend = mockk()
    protected val mockCache: DeviceCache = mockk()
    protected val updatedCustomerInfoListener: UpdatedCustomerInfoListener = mockk()
    protected val mockContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val mockApplication = (mockContext.applicationContext as Application).apply {
        shadowOf(this).grantPermissions(Manifest.permission.INTERNET)
    }
    protected val mockIdentityManager = mockk<IdentityManager>()
    protected val mockSubscriberAttributesManager = mockk<SubscriberAttributesManager>()
    internal val mockCustomerInfoHelper = mockk<CustomerInfoHelper>()
    internal val mockCustomerInfoUpdateHandler = mockk<CustomerInfoUpdateHandler>()
    protected val mockDiagnosticsSynchronizer = mockk<DiagnosticsSynchronizer>()
    protected val mockDiagnosticsTracker = mockk<DiagnosticsTracker>(relaxUnitFun = true)
    protected val mockDateProvider = mockk<DateProvider>()
    protected val mockOfflineEntitlementsManager = mockk<OfflineEntitlementsManager>()
    internal val mockPostReceiptHelper = mockk<PostReceiptHelper>()
    internal val mockPostPendingTransactionsHelper = mockk<PostPendingTransactionsHelper>()
    internal val mockSyncPurchasesHelper = mockk<SyncPurchasesHelper>()
    protected val mockOfferingsManager = mockk<OfferingsManager>()
    protected val mockBackupManager = mockk<BackupManager>()
    internal val mockEventsManager = mockk<EventsManager>()
    internal val mockAdEventsManager = mockk<EventsManager>()
    internal val mockWebPurchasesRedemptionHelper = mockk<WebPurchaseRedemptionHelper>()
    internal val mockLifecycleOwner = mockk<LifecycleOwner>()
    internal val mockLifecycle = mockk<Lifecycle>()
    internal val mockFontLoader = mockk<FontLoader>()
    internal val mockVirtualCurrencyManager = mockk<VirtualCurrencyManager>()
    internal val mockPurchaseParamsValidator = mockk<PurchaseParamsValidator>()
    private val mockBlockstoreHelper = mockk<BlockstoreHelper>()
    private val purchasesStateProvider = PurchasesStateCache(PurchasesState())

    protected lateinit var appConfig: AppConfig

    protected var capturedPurchasesUpdatedListener = slot<BillingAbstract.PurchasesUpdatedListener>()
    protected var capturedBillingWrapperStateListener = slot<BillingAbstract.StateListener>()
    private val capturedConsumePurchaseWrapper = slot<StoreTransaction>()
    private val capturedFinishedTransactions = slot<Boolean>()
    private val capturedShouldConsume = slot<Boolean>()

    protected lateinit var paywallPresentedCache: PaywallPresentedCache

    protected val randomAppUserId = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    protected val appUserId = "fakeUserID"
    protected lateinit var purchases: Purchases
    protected val mockInfo = mockk<CustomerInfo>()
    protected val mockOfferings = mockk<Offerings>()
    protected val mockActivity: Activity = mockk()
    protected val subscriptionOptionId = "mock-base-plan-id:mock-offer-id"

    protected open val shouldConfigureOnSetUp: Boolean
        get() = true

    @Before
    fun setUp() {
        val productIds = listOf(STUB_PRODUCT_IDENTIFIER)
        mockCache()
        mockPostReceiptHelper()
        mockBackend()
        mockBillingWrapper()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        mockCustomerInfo()
        mockCustomerInfoHelper()
        mockCustomerInfoUpdateHandler()
        mockPostPendingTransactionsHelper()
        paywallPresentedCache = PaywallPresentedCache()

        every {
            updatedCustomerInfoListener.onReceived(any())
        } just Runs
        every {
            mockIdentityManager.configure(any())
        } just Runs
        every {
            mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded()
        } just Runs
        every {
            mockOfflineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        } just Runs
        every {
            mockEventsManager.flushEvents(any())
        } just Runs
        every {
            mockAdEventsManager.flushEvents(any())
        } just Runs
        every {
            mockEventsManager.debugEventListener = any()
        } just Runs
        every {
            mockEventsManager.debugEventListener
        } returns null
        every {
            mockLifecycleOwner.lifecycle
        } returns mockLifecycle

        every { mockBlockstoreHelper.storeUserIdIfNeeded(any()) } just Runs
        every {
            mockBlockstoreHelper.aliasCurrentAndStoredUserIdsIfNeeded(captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every {
            mockBlockstoreHelper.clearUserIdBackupIfNeeded(captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every { mockBackupManager.dataChanged() } just Runs

        every { mockLifecycle.addObserver(any()) } just Runs
        every { mockLifecycle.removeObserver(any()) } just Runs

        every { mockDateProvider.now } returns Date()

        every { mockPurchaseParamsValidator.validate(any()) } returns Result.Success(Unit)

        if (shouldConfigureOnSetUp) {
            anonymousSetup(false)
        }
    }

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance = null
        clearMocks(
            mockCustomerInfoHelper,
            mockPostReceiptHelper,
            mockSyncPurchasesHelper,
            mockOfferingsManager,
            mockCustomerInfoUpdateHandler,
            mockPostPendingTransactionsHelper,
            mockEventsManager,
            mockAdEventsManager,
            mockWebPurchasesRedemptionHelper,
            mockLifecycleOwner,
            mockLifecycle,
            mockFontLoader,
            mockBlockstoreHelper,
            mockBackupManager,
        )
    }

    // region Private Methods
    private fun mockBillingWrapper() {
        with(mockBillingAbstract) {
            every {
                makePurchaseAsync(any(), any(), any(), any(), any(), any())
            } just Runs
            every {
                purchasesUpdatedListener = capture(capturedPurchasesUpdatedListener)
            } just Runs
            every {
                consumeAndSave(
                    capture(capturedFinishedTransactions),
                    capture(capturedConsumePurchaseWrapper),
                    capture(capturedShouldConsume),
                    any(),
                )
            } just Runs
            every {
                purchasesUpdatedListener = null
            } just Runs
            every {
                stateListener = capture(capturedBillingWrapperStateListener)
            } just Runs
            every {
                isConnected()
            } returns true

            every {
                close()
            } answers {
                purchasesUpdatedListener = null
            }

            every {
                startConnectionOnMainThread()
            } just Runs
        }
    }

    private fun mockBackend() {
        with(mockBackend) {
            every {
                close()
            } just Runs
            every {
                clearCaches()
            } just Runs
        }
    }

    private fun mockPostReceiptHelper() {
        with(mockPostReceiptHelper) {
            every {
                postTransactionAndConsumeIfNeeded(
                    purchase = any(),
                    storeProduct = any(),
                    subscriptionOptionForProductIDs = any(),
                    isRestore = any(),
                    appUserID = any(),
                    initiationSource = any(),
                    sdkOriginated = any(),
                    onSuccess = captureLambda(),
                    onError = any(),
                )
            } answers {
                lambda<SuccessfulPurchaseCallback>().captured.invoke(
                    firstArg(),
                    mockInfo
                )
            }
            every {
                postTokenWithoutConsuming(
                    purchaseToken = any(),
                    receiptInfo = any(),
                    isRestore = any(),
                    appUserID = any(),
                    initiationSource = any(),
                    onSuccess = captureLambda(),
                    onError = any(),
                )
            } answers {
                lambda<(CustomerInfo) -> Unit>().captured.invoke(mockInfo)
            }
        }
    }

    private fun mockCache() {
        with(mockCache) {
            every {
                getCachedAppUserID()
            } returns null
            every {
                getCachedCustomerInfo(any())
            } returns mockInfo
            every {
                cacheCustomerInfo(any(), any())
            } just Runs
            every {
                cacheAppUserID(any())
            } just Runs
            every {
                setCustomerInfoCacheTimestampToNow(appUserId)
            } just Runs
            every {
                clearCustomerInfoCacheTimestamp(appUserId)
            } just Runs
            every {
                clearCustomerInfoCache(appUserId)
            } just Runs
            every {
                isCustomerInfoCacheStale(appUserId, any())
            } returns false
            every {
                addSuccessfullyPostedToken(any())
            } just Runs
        }
    }

    private fun mockSubscriberAttributesManager() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
    }
    // endregion

    // region Protected methods

    protected fun mockCustomerInfo(verificationResult: VerificationResult = VerificationResult.VERIFIED) {
        val mockEntitlements = mockk<EntitlementInfos>()
        every { mockEntitlements.verification } returns verificationResult
        every { mockInfo.entitlements } returns mockEntitlements
    }

    protected fun mockCustomerInfoHelper(
        errorGettingCustomerInfo: PurchasesError? = null,
        mockedCustomerInfo: CustomerInfo = mockInfo
    ) {
        with(mockCustomerInfoHelper) {
            val slotList = mutableListOf<ReceiveCustomerInfoCallback?>()
            every {
                retrieveCustomerInfo(
                    any(),
                    any(),
                    appInBackground = false,
                    allowSharingPlayStoreAccount = false,
                    any(),
                    callback = captureNullable(slotList),
                )
            } answers {
                if (errorGettingCustomerInfo == null) {
                    slotList.firstOrNull()?.onReceived(mockedCustomerInfo)
                } else {
                    slotList.firstOrNull()?.onError(errorGettingCustomerInfo)
                }
            }
        }
    }

    protected fun mockCustomerInfoUpdateHandler() {
        with(mockCustomerInfoUpdateHandler) {
            every {
                cacheAndNotifyListeners(any())
            } just Runs
            every {
                notifyListeners(any())
            } just Runs
            every {
                updatedCustomerInfoListener = any()
            } just Runs
            every {
                updatedCustomerInfoListener
            } returns null
        }
    }

    // region mockOfferingsManager
    protected fun mockOfferingsManagerAppForeground() {
        every {
            mockOfferingsManager.onAppForeground(appUserId)
        } just Runs
    }

    protected fun mockOfferingsManagerGetOfferings(errorGettingOfferings: PurchasesError? = null): Offerings {
        val offerings: Offerings = mockk()
        every {
            mockOfferingsManager.getOfferings(
                appUserId,
                appInBackground = false,
                onError = errorGettingOfferings?.let { captureLambda() } ?: any(),
                onSuccess = errorGettingOfferings?.let { any() } ?: captureLambda()
            )
        } answers {
            errorGettingOfferings?.let {
                lambda<(PurchasesError) -> Unit>().captured.invoke(it)
            } ?: lambda<(Offerings) -> Unit>().captured.invoke(offerings)
        }
        return offerings
    }
    // endregion

    protected fun mockStoreProduct(
        productIds: List<String>,
        productIdsSuccessfullyFetched: List<String>,
        type: ProductType
    ): List<StoreProduct> {
        val storeProducts = productIdsSuccessfullyFetched.map { productId ->
            if (type == ProductType.SUBS) stubStoreProduct(productId, stubSubscriptionOption("p1m", "P1M"))
            else createMockOneTimeProductDetails(productId).toInAppStoreProduct()
        }.mapNotNull { it }

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                type,
                productIds.toSet(),
                captureLambda(),
                any(),
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }
        return storeProducts
    }

    protected fun getMockedStoreTransaction(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        return getMockedStoreTransaction(listOf(productId), purchaseToken, productType)
    }

    protected fun getMockedStoreTransaction(
        productIds: List<String>,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: Purchase = stubGooglePurchase(
            productIds = productIds,
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(productType)
    }

    protected fun buildPurchases(
        anonymous: Boolean,
        autoSync: Boolean = true,
        customEntitlementComputation: Boolean = false,
        showInAppMessagesAutomatically: Boolean = false,
        apiKeyValidationResult: APIKeyValidator.ValidationResult = APIKeyValidator.ValidationResult.VALID,
        enableSimulatedStore: Boolean = false,
        store: Store = Store.PLAY_STORE,
    ) {
        appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = showInAppMessagesAutomatically,
            platformInfo = PlatformInfo("native", "3.2.0"),
            proxyURL = null,
            store = store,
            isDebugBuild = false,
            apiKeyValidationResult = apiKeyValidationResult,
            dangerousSettings = DangerousSettings(
                autoSyncPurchases = autoSync,
                customEntitlementComputation = customEntitlementComputation,
            )
        )
        val postTransactionsHelper = PostTransactionWithProductDetailsHelper(mockBillingAbstract, mockPostReceiptHelper)
        val purchasesOrchestrator = PurchasesOrchestrator(
            mockApplication,
            if (anonymous) null else appUserId,
            mockBackend,
            mockBillingAbstract,
            mockCache,
            identityManager = mockIdentityManager,
            subscriberAttributesManager = mockSubscriberAttributesManager,
            appConfig = appConfig,
            customerInfoHelper = mockCustomerInfoHelper,
            customerInfoUpdateHandler = mockCustomerInfoUpdateHandler,
            diagnosticsSynchronizer = mockDiagnosticsSynchronizer,
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            dateProvider = mockDateProvider,
            offlineEntitlementsManager = mockOfflineEntitlementsManager,
            postReceiptHelper = mockPostReceiptHelper,
            postTransactionWithProductDetailsHelper = postTransactionsHelper,
            postPendingTransactionsHelper = mockPostPendingTransactionsHelper,
            syncPurchasesHelper = mockSyncPurchasesHelper,
            offeringsManager = mockOfferingsManager,
            eventsManager = mockEventsManager,
            adEventsManager = mockAdEventsManager,
            paywallPresentedCache = paywallPresentedCache,
            purchasesStateCache = purchasesStateProvider,
            dispatcher = SyncDispatcher(),
            initialConfiguration = PurchasesConfiguration.Builder(mockContext, "api_key").build(),
            webPurchaseRedemptionHelper = mockWebPurchasesRedemptionHelper,
            processLifecycleOwnerProvider = { mockLifecycleOwner },
            fontLoader = mockFontLoader,
            localeProvider = DefaultLocaleProvider(),
            virtualCurrencyManager = mockVirtualCurrencyManager,
            blockstoreHelper = mockBlockstoreHelper,
            backupManager = mockBackupManager,
            purchaseParamsValidator = mockPurchaseParamsValidator,
        )

        purchases = Purchases(
            purchasesOrchestrator = purchasesOrchestrator,
        )
        Purchases.sharedInstance = purchases
        purchasesOrchestrator.state = purchasesOrchestrator.state.copy(appInBackground = false)
    }

    protected fun anonymousSetup(anonymous: Boolean) {
        val userIdToUse = if (anonymous) randomAppUserId else appUserId

        every {
            mockIdentityManager.currentAppUserID
        } returns userIdToUse

        every {
            mockIdentityManager.currentUserIsAnonymous()
        } returns anonymous

        buildPurchases(anonymous)
        mockSubscriberAttributesManager()
    }

    protected fun mockCacheStale(
        customerInfoStale: Boolean = false,
        appInBackground: Boolean = false
    ) {
        every {
            mockCache.isCustomerInfoCacheStale(appUserId, appInBackground)
        } returns customerInfoStale
    }

    protected fun mockPostPendingTransactionsHelper() {
        every {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any())
        } just Runs
    }

    protected fun getPurchaseParams(
        purchaseable: Any,
        oldProductId: String? = null,
        isPersonalizedPrice: Boolean? = null,
        googleReplacementMode: GoogleReplacementMode? = null
    ): PurchaseParams {
        val builder = when (purchaseable) {
            is SubscriptionOption -> PurchaseParams.Builder(mockActivity, purchaseable)
            is Package -> PurchaseParams.Builder(mockActivity, purchaseable)
            is StoreProduct -> PurchaseParams.Builder(mockActivity, purchaseable)
            else -> null
        }

        oldProductId?.let {
            builder!!.oldProductId(it)
        }

        isPersonalizedPrice?.let {
            builder!!.isPersonalizedPrice(it)
        }

        googleReplacementMode?.let {
            builder!!.googleReplacementMode(googleReplacementMode)
        }
        return builder!!.build()
    }

    protected fun getMockedPurchaseList(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        presentedOfferingContext: PresentedOfferingContext? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): List<StoreTransaction> {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        val subscriptionOptionIdForProductIDs =
            subscriptionOptionId
                ?.takeIf { productType == ProductType.SUBS }
                ?.let { mapOf(productId to it) }

        return listOf(
            p.toStoreTransaction(
                productType,
                presentedOfferingContext,
                if (productType == ProductType.SUBS) subscriptionOptionId else null,
                subscriptionOptionIdForProductIDs
            )
        )
    }

    protected fun mockOfferingsManagerFetchOfferings(userId: String = appUserId) {
        every {
            mockOfferingsManager.fetchAndCacheOfferings(userId, any(), any(), any())
        } just Runs
    }

    // endregion
}
