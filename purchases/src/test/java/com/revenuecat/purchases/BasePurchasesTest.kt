//  Purchases
//
//  Copyright © 2023 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Application
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsSynchronizer
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.utils.STUB_PRODUCT_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.After
import org.junit.Before

open class BasePurchasesTest {
    protected val mockBillingAbstract: BillingAbstract = mockk()
    protected val mockBackend: Backend = mockk()
    protected val mockCache: DeviceCache = mockk()
    protected val updatedCustomerInfoListener: UpdatedCustomerInfoListener = mockk()
    private val mockApplication = mockk<Application>(relaxed = true)
    protected val mockContext = mockk<Context>(relaxed = true).apply {
        every {
            applicationContext
        } returns mockApplication
    }
    protected val mockIdentityManager = mockk<IdentityManager>()
    protected val mockSubscriberAttributesManager = mockk<SubscriberAttributesManager>()
    internal val mockCustomerInfoHelper = mockk<CustomerInfoHelper>()
    internal val mockCustomerInfoUpdateHandler = mockk<CustomerInfoUpdateHandler>()
    protected val mockDiagnosticsSynchronizer = mockk<DiagnosticsSynchronizer>()
    protected val mockOfflineEntitlementsManager = mockk<OfflineEntitlementsManager>()
    internal val mockPostReceiptHelper = mockk<PostReceiptHelper>()
    internal val mockPostPendingTransactionsHelper = mockk<PostPendingTransactionsHelper>()
    internal val mockSyncPurchasesHelper = mockk<SyncPurchasesHelper>()
    protected val mockOfferingsManager = mockk<OfferingsManager>()

    protected var capturedPurchasesUpdatedListener = slot<BillingAbstract.PurchasesUpdatedListener>()
    protected var capturedBillingWrapperStateListener = slot<BillingAbstract.StateListener>()
    private val capturedConsumePurchaseWrapper = slot<StoreTransaction>()
    private val capturedShouldTryToConsume = slot<Boolean>()

    protected val randomAppUserId = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    protected val appUserId = "fakeUserID"
    protected lateinit var purchases: Purchases
    protected val mockInfo = mockk<CustomerInfo>()

    @Before
    fun setUp() {
        mockkStatic(ProcessLifecycleOwner::class)

        val productIds = listOf(STUB_PRODUCT_IDENTIFIER)
        mockCache()
        mockPostReceiptHelper()
        mockBackend()
        mockBillingWrapper()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        mockCustomerInfoHelper()
        mockCustomerInfoUpdateHandler()
        mockPostPendingTransactionsHelper()

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

        anonymousSetup(false)
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
                consumeAndSave(capture(capturedShouldTryToConsume), capture(capturedConsumePurchaseWrapper))
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
                postTransactionAndConsumeIfNeeded(any(), any(), any(), any(), captureLambda(), any())
            } answers {
                lambda<SuccessfulPurchaseCallback>().captured.invoke(
                    firstArg(),
                    mockInfo
                )
            }
            every {
                postTokenWithoutConsuming(any(), any(), any(), any(), any(), any(), captureLambda(), any())
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
    protected fun mockCustomerInfoHelper(errorGettingCustomerInfo: PurchasesError? = null) {
        with(mockCustomerInfoHelper) {
            every {
                retrieveCustomerInfo(any(), any(), false, any())
            } answers {
                val callback = arg<ReceiveCustomerInfoCallback?>(3)
                if (errorGettingCustomerInfo == null) {
                    callback?.onReceived(mockInfo)
                } else {
                    callback?.onError(errorGettingCustomerInfo)
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

    protected fun mockOfferingsManagerAppForeground() {
        every {
            mockOfferingsManager.onAppForeground(appUserId)
        } just Runs
    }

    protected fun mockOfferingsManagerFetchOfferings(userId: String = appUserId) {
        every {
            mockOfferingsManager.fetchAndCacheOfferings(userId, any(), any(), any())
        } just Runs
    }

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
                any()
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
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(productId),
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(productType)
    }

    protected fun buildPurchases(anonymous: Boolean, autoSync: Boolean = true) {
        val appConfig = AppConfig(
            context = mockContext,
            observerMode = false,
            platformInfo = PlatformInfo("native", "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            dangerousSettings = DangerousSettings(autoSyncPurchases = autoSync)
        )
        val postTransactionsHelper = PostTransactionWithProductDetailsHelper(mockBillingAbstract, mockPostReceiptHelper)
        purchases = Purchases(
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
            offlineEntitlementsManager = mockOfflineEntitlementsManager,
            postReceiptHelper = mockPostReceiptHelper,
            postTransactionWithProductDetailsHelper = postTransactionsHelper,
            postPendingTransactionsHelper = mockPostPendingTransactionsHelper,
            syncPurchasesHelper = mockSyncPurchasesHelper,
            offeringsManager = mockOfferingsManager
        )
        Purchases.sharedInstance = purchases
        purchases.state = purchases.state.copy(appInBackground = false)
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
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any(), any())
        } just Runs
    }

    // endregion
}
