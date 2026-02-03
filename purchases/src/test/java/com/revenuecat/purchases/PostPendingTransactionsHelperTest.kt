package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubGooglePurchase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostPendingTransactionsHelperTest {

    private val allowSharingPlayStoreAccount = true
    private val appUserId = "test-app-user-id"
    private val initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES

    private lateinit var appConfig: AppConfig
    private lateinit var deviceCache: DeviceCache
    private lateinit var billing: BillingAbstract
    private lateinit var dispatcher: Dispatcher
    private lateinit var identityManager: IdentityManager
    private lateinit var postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper
    private lateinit var postReceiptHelper: PostReceiptHelper

    private lateinit var postPendingTransactionsHelper: PostPendingTransactionsHelper

    @Before
    fun setUp() {
        appConfig = mockk()
        deviceCache = mockk()
        billing = mockk()
        dispatcher = SyncDispatcher()
        identityManager = mockk<IdentityManager>().apply {
            every { currentAppUserID } returns appUserId
        }
        postTransactionWithProductDetailsHelper = mockk()
        postReceiptHelper = mockk()

        // Default mock for postRemainingCachedTransactionMetadata
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = any(),
                pendingTransactionsTokens = any(),
                onNoTransactionsToSync = captureLambda(),
                onError = any(),
                onSuccess = any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        changeBillingConnected()
        changeAutoSyncEnabled(true)

        postPendingTransactionsHelper = PostPendingTransactionsHelper(
            appConfig,
            deviceCache,
            billing,
            dispatcher,
            identityManager,
            postTransactionWithProductDetailsHelper,
            postReceiptHelper,
        )
    }

    // region syncPendingPurchaseQueue

    @Test
    fun `skip posting pending purchases if autosync is off`() {
        changeAutoSyncEnabled(false)
        syncAndAssertResult(SyncPendingPurchaseResult.AutoSyncDisabled)
        verify(exactly = 0) {
            billing.queryPurchases(
                appUserID = any(),
                onSuccess = any(),
                onError = any()
            )
        }
        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = any(),
                allowSharingPlayStoreAccount = any(),
                appUserID = any(),
                initiationSource = any(),
                sdkOriginated = any(),
                transactionPostSuccess = any(),
                transactionPostError = any()
            )
        }
    }

    @Test
    fun `if autosync is disabled, and sync is called, success callback with null values is called`() {
        changeAutoSyncEnabled(false)
        syncAndAssertResult(SyncPendingPurchaseResult.AutoSyncDisabled)
    }

    @Test
    fun `when updating pending purchases, retrieve purchases`() {
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )
        syncAndAssertResult(SyncPendingPurchaseResult.NoPendingPurchasesToSync)
        verify(exactly = 1) {
            billing.queryPurchases(
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if no purchases to sync, it calls success with null value`() {
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )
        syncAndAssertResult(SyncPendingPurchaseResult.NoPendingPurchasesToSync)
    }

    @Test
    fun `when updating pending purchases, if token has not been sent, send it`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        mockPostTransactionsSuccessful(mockk())

        postPendingTransactionsHelper.syncPendingPurchaseQueue(allowSharingPlayStoreAccount)

        verify(exactly = 1) {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = any(),
                transactionPostError = any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if token sent successfully success callback is called`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock)

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))
    }

    @Test
    fun `when updating pending purchases, if token sent successfully clean previously sent tokens`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock)

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify { deviceCache.cleanPreviouslySentTokens(setOf(purchase.purchaseToken.sha1())) }
    }

    @Test
    fun `when updating pending purchases, if token has been sent, don't send it`() {
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = listOf("product")
        )
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(
                purchase.purchaseToken.sha1() to purchase.toStoreTransaction(
                    ProductType.SUBS,
                    null
                )
            ),
            notInCache = emptyList()
        )

        mockPostTransactionsSuccessful(mockk())

        syncAndAssertResult(SyncPendingPurchaseResult.NoPendingPurchasesToSync)

        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when updating pending purchases, if result from querying purchases is not successful skip`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            billing.queryPurchases(
                appUserID = appUserId,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured(error)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(error))

        verify(exactly = 0) { deviceCache.cleanPreviouslySentTokens(any()) }
        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `all non-pending purchases returned from queryPurchases are posted to backend`() {
        val purchasedPurchase = stubGooglePurchase(
            purchaseToken = "purchasedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchasedPurchase = purchasedPurchase.toStoreTransaction(ProductType.SUBS)

        val pendingPurchase = stubGooglePurchase(
            purchaseToken = "pendingToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PENDING
        )
        val activePendingPurchase = pendingPurchase.toStoreTransaction(ProductType.SUBS)

        val unspecifiedPurchase = stubGooglePurchase(
            purchaseToken = "unspecifiedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.UNSPECIFIED_STATE
        )
        val activeUnspecifiedPurchase = unspecifiedPurchase.toStoreTransaction(ProductType.SUBS)

        val allPurchases = listOf(activePurchasedPurchase, activePendingPurchase, activeUnspecifiedPurchase)
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(
                purchasedPurchase.purchaseToken.sha1() to activePurchasedPurchase,
                pendingPurchase.purchaseToken.sha1() to activePendingPurchase,
                unspecifiedPurchase.purchaseToken.sha1() to activeUnspecifiedPurchase
            ),
            notInCache = allPurchases
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock, allPurchases)

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = allPurchases,
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = captureLambda(),
                transactionPostError = any()
            )
        }
    }

    @Test
    fun `if any post to backend fails, error is called`() {
        val purchasedPurchase = stubGooglePurchase(
            purchaseToken = "purchasedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchasedPurchase = purchasedPurchase.toStoreTransaction(ProductType.SUBS)

        val pendingPurchase = stubGooglePurchase(
            purchaseToken = "pendingToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PENDING
        )
        val activePendingPurchase = pendingPurchase.toStoreTransaction(ProductType.SUBS)

        val unspecifiedPurchase = stubGooglePurchase(
            purchaseToken = "unspecifiedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.UNSPECIFIED_STATE
        )
        val activeUnspecifiedPurchase = unspecifiedPurchase.toStoreTransaction(ProductType.SUBS)

        val allPurchases = listOf(activePurchasedPurchase, activePendingPurchase, activeUnspecifiedPurchase)
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(
                purchasedPurchase.purchaseToken.sha1() to activePurchasedPurchase,
                pendingPurchase.purchaseToken.sha1() to activePendingPurchase,
                unspecifiedPurchase.purchaseToken.sha1() to activeUnspecifiedPurchase
            ),
            notInCache = allPurchases
        )

        val customerInfoMock = mockk<CustomerInfo>()

        val successSlot = slot<SuccessfulPurchaseCallback>()
        val errorSlot = slot<ErrorPurchaseCallback>()
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = allPurchases,
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = capture(successSlot),
                transactionPostError = capture(errorSlot)
            )
        } answers {
            successSlot.captured.invoke(activePurchasedPurchase, customerInfoMock)
            errorSlot.captured.invoke(activeUnspecifiedPurchase, error)
            successSlot.captured.invoke(activePendingPurchase, customerInfoMock)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(error))
    }

    // endregion

    private fun mockPostTransactionsSuccessful(
        customerInfo: CustomerInfo?,
        transactions: List<StoreTransaction>? = null
    ) {
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = transactions ?: any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = captureLambda(),
                transactionPostError = any()
            )
        } answers {
            transactions?.let {
                it.forEach { transaction ->
                    lambda<SuccessfulPurchaseCallback>().captured.invoke(transaction, customerInfo!!)
                }
            } ?: run {
                lambda<SuccessfulPurchaseCallback>().captured.invoke(mockk(), customerInfo!!)
            }
        }
    }

    private fun createGooglePurchaseAndTransaction(): Pair<Purchase, StoreTransaction> {
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchase = purchase.toStoreTransaction(ProductType.SUBS)
        return Pair(purchase, activePurchase)
    }

    private fun mockSuccessfulQueryPurchases(
        purchasesByHashedToken: Map<String, StoreTransaction>,
        notInCache: List<StoreTransaction>
    ) {
        every {
            deviceCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
        } just Runs
        every {
            deviceCache.getActivePurchasesNotInCache(purchasesByHashedToken)
        } returns notInCache

        every {
            billing.queryPurchases(
                appUserId,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured(purchasesByHashedToken)
        }
    }

    private fun changeAutoSyncEnabled(enabled: Boolean = true) {
        every { appConfig.dangerousSettings } returns mockk<DangerousSettings>().apply {
            every { autoSyncPurchases } returns enabled
        }
    }

    private fun changeBillingConnected(isConnected: Boolean = true) {
        every { billing.isConnected() } returns isConnected
    }

    private fun syncAndAssertResult(expectedSyncResult: SyncPendingPurchaseResult) {
        var successCallCount = 0
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            callback = { syncResult ->
                assertThat(syncResult).isEqualTo(expectedSyncResult)
                successCallCount++
            },
        )
        assertThat(successCallCount).isEqualTo(1)
    }

    // region postRemainingCachedTransactionMetadata tests

    @Test
    fun `when there are no cached transaction metadata, onNoTransactionsToSync is called`() {
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = any(),
                onNoTransactionsToSync = captureLambda(),
                onError = any(),
                onSuccess = any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        syncAndAssertResult(SyncPendingPurchaseResult.NoPendingPurchasesToSync)

        verify(exactly = 1) {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `when cached transaction metadata posting succeeds, success callback is invoked`() {
        val customerInfoMock = mockk<CustomerInfo>()
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = captureLambda()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfoMock)
        }

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))
    }

    @Test
    fun `when posting cached transaction metadata fails, error callback is invoked`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = captureLambda(),
                onSuccess = any()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        syncAndAssertResult(SyncPendingPurchaseResult.Error(error))
    }

    @Test
    fun `when regular transactions succeed, cached metadata is also posted`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock, listOf(activePurchase))

        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = captureLambda()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfoMock)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = any(),
                transactionPostError = any()
            )
        }

        verify(exactly = 1) {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `when regular transactions fail, cached metadata is still attempted and can succeed`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val regularTransactionError = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = any(),
                transactionPostError = captureLambda()
            )
        } answers {
            lambda<ErrorPurchaseCallback>().captured.invoke(activePurchase, regularTransactionError)
        }

        val customerInfoMock = mockk<CustomerInfo>()
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = captureLambda()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfoMock)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = any()
            )
        }
    }

    @Test
    fun `when regular transactions fail and cached metadata also fails, regular transaction error is returned`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val regularTransactionError = PurchasesError(PurchasesErrorCode.NetworkError, "Regular transaction failed")
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
                transactionPostSuccess = any(),
                transactionPostError = captureLambda()
            )
        } answers {
            lambda<ErrorPurchaseCallback>().captured.invoke(activePurchase, regularTransactionError)
        }

        val metadataError = PurchasesError(PurchasesErrorCode.NetworkError, "Metadata post failed")
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = emptySet(),
                onNoTransactionsToSync = any(),
                onError = captureLambda(),
                onSuccess = any()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(metadataError)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(regularTransactionError))
    }

    @Test
    fun `pending transaction tokens are correctly passed to postRemainingCachedTransactionMetadata`() {
        val purchasedPurchase = stubGooglePurchase(
            purchaseToken = "purchasedToken",
            productIds = listOf("product1"),
            purchaseState = Purchase.PurchaseState.PURCHASED,
        )
        val activePurchasedPurchase = purchasedPurchase.toStoreTransaction(ProductType.SUBS)

        val pendingPurchase1 = stubGooglePurchase(
            purchaseToken = "pendingToken1",
            productIds = listOf("product2"),
            purchaseState = Purchase.PurchaseState.PENDING,
        )
        val activePendingPurchase1 = pendingPurchase1.toStoreTransaction(ProductType.SUBS)

        val pendingPurchase2 = stubGooglePurchase(
            purchaseToken = "pendingToken2",
            productIds = listOf("product3"),
            purchaseState = Purchase.PurchaseState.PENDING,
        )
        val activePendingPurchase2 = pendingPurchase2.toStoreTransaction(ProductType.SUBS)

        val allPurchases = listOf(activePurchasedPurchase, activePendingPurchase1, activePendingPurchase2)
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(
                purchasedPurchase.purchaseToken.sha1() to activePurchasedPurchase,
                pendingPurchase1.purchaseToken.sha1() to activePendingPurchase1,
                pendingPurchase2.purchaseToken.sha1() to activePendingPurchase2,
            ),
            notInCache = allPurchases
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock, allPurchases)

        val pendingTokensSlot = slot<Set<String>>()
        every {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = capture(pendingTokensSlot),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = captureLambda(),
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfoMock)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postReceiptHelper.postRemainingCachedTransactionMetadata(
                appUserID = any(),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                pendingTransactionsTokens = setOf("pendingToken1", "pendingToken2"),
                onNoTransactionsToSync = any(),
                onError = any(),
                onSuccess = any(),
            )
        }

        // Verify that only the pending transaction tokens are passed
        assertThat(pendingTokensSlot.captured).containsExactlyInAnyOrder("pendingToken1", "pendingToken2")
        assertThat(pendingTokensSlot.captured).doesNotContain("purchasedToken")
    }

    // endregion
}
