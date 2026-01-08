package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadata
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataCache
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
    private lateinit var localTransactionMetadataCache: LocalTransactionMetadataCache

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
        localTransactionMetadataCache = mockk<LocalTransactionMetadataCache>().apply {
            every { getAllLocalTransactionMetadata() } returns emptyList()
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
            localTransactionMetadataCache,
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

    // region postNotFoundTransactionMetadata tests

    @Test
    fun `when there are no cached transaction metadata, onNoTransactionsToSync is called`() {
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns emptyList()

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        syncAndAssertResult(SyncPendingPurchaseResult.NoPendingPurchasesToSync)

        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = any(),
                receiptInfo = any(),
                isRestore = any(),
                appUserID = any(),
                initiationSource = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when there is one cached transaction metadata, it is posted successfully`() {
        val metadata = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTokenWithoutConsumingSuccessful(customerInfoMock, listOf(metadata))

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = metadata.appUserID,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when there are multiple cached transaction metadata, all are posted successfully`() {
        val metadata1 = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        val metadata2 = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_2"
        )
        val metadata3 = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_3"
        )
        val allMetadata = listOf(metadata1, metadata2, metadata3)
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns allMetadata

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTokenWithoutConsumingSuccessful(customerInfoMock, allMetadata)

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 3) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = any(),
                receiptInfo = any(),
                isRestore = allowSharingPlayStoreAccount,
                appUserID = any(),
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when posting cached transaction metadata fails, error callback is invoked`() {
        val metadata = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        every {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = metadata.appUserID,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured(error)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(error))
    }

    @Test
    fun `when posting multiple cached metadata and first one fails, error is returned`() {
        val metadata1 = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        val metadata2 = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_2"
        )
        val allMetadata = listOf(metadata1, metadata2)
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns allMetadata

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        val customerInfoMock = mockk<CustomerInfo>()

        val successSlot = slot<(CustomerInfo) -> Unit>()
        val errorSlot = slot<(PurchasesError) -> Unit>()

        every {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = any(),
                receiptInfo = any(),
                isRestore = allowSharingPlayStoreAccount,
                appUserID = any(),
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = capture(successSlot),
                onError = capture(errorSlot)
            )
        } answers {
            val token = firstArg<String>()
            if (token == metadata1.token) {
                errorSlot.captured.invoke(error)
            } else {
                successSlot.captured.invoke(customerInfoMock)
            }
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(error))
    }

    @Test
    fun `when regular transactions succeed, cached metadata is also posted`() {
        val (purchase, activePurchase) = createGooglePurchaseAndTransaction()
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            notInCache = listOf(activePurchase)
        )

        val metadata = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTransactionsSuccessful(customerInfoMock, listOf(activePurchase))
        mockPostTokenWithoutConsumingSuccessful(customerInfoMock, listOf(metadata))

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                transactionPostSuccess = any(),
                transactionPostError = any()
            )
        }

        verify(exactly = 1) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = metadata.appUserID,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
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

        val metadata = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        val regularTransactionError = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                transactionPostSuccess = any(),
                transactionPostError = captureLambda()
            )
        } answers {
            lambda<ErrorPurchaseCallback>().captured.invoke(activePurchase, regularTransactionError)
        }

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTokenWithoutConsumingSuccessful(customerInfoMock, listOf(metadata))

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = metadata.appUserID,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
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

        val metadata = createTransactionMetadata(
            userID = appUserId,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        val regularTransactionError = PurchasesError(PurchasesErrorCode.NetworkError, "Regular transaction failed")
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions = listOf(activePurchase),
                allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
                appUserID = appUserId,
                initiationSource = initiationSource,
                transactionPostSuccess = any(),
                transactionPostError = captureLambda()
            )
        } answers {
            lambda<ErrorPurchaseCallback>().captured.invoke(activePurchase, regularTransactionError)
        }

        val metadataError = PurchasesError(PurchasesErrorCode.NetworkError, "Metadata post failed")
        every {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = metadata.appUserID,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured(metadataError)
        }

        syncAndAssertResult(SyncPendingPurchaseResult.Error(regularTransactionError))
    }

    @Test
    fun `when cached metadata has different user ID than current user, cached user ID is used`() {
        val cachedUserID = "cached-user-id"
        val metadata = createTransactionMetadata(
            userID = cachedUserID,
            token = "cached_token_1"
        )
        every { localTransactionMetadataCache.getAllLocalTransactionMetadata() } returns listOf(metadata)

        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )

        val customerInfoMock = mockk<CustomerInfo>()
        mockPostTokenWithoutConsumingSuccessful(customerInfoMock, listOf(metadata))

        syncAndAssertResult(SyncPendingPurchaseResult.Success(customerInfoMock))

        verify(exactly = 1) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = cachedUserID,  // Verify cached user ID is used, not current user ID
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
            )
        }

        // Verify that the current app user ID is NOT used for cached metadata
        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = metadata.token,
                receiptInfo = metadata.receiptInfo,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserId,  // Should NOT be called with current app user ID
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // endregion

    // Helper methods for cached transaction metadata tests

    private fun createTransactionMetadata(
        userID: String,
        token: String
    ): LocalTransactionMetadata.TransactionMetadata {
        return LocalTransactionMetadata.TransactionMetadata(
            appUserID = userID,
            token = token,
            receiptInfo = mockk(relaxed = true),
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = null
        )
    }

    private fun mockPostTokenWithoutConsumingSuccessful(
        customerInfo: CustomerInfo?,
        metadata: List<LocalTransactionMetadata.TransactionMetadata>
    ) {
        for (transaction in metadata) {
            every {
                postReceiptHelper.postTokenWithoutConsuming(
                    purchaseToken = transaction.token,
                    receiptInfo = transaction.receiptInfo,
                    isRestore = allowSharingPlayStoreAccount,
                    appUserID = transaction.appUserID,
                    initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                    onSuccess = captureLambda(),
                    onError = any()
                )
            } answers {
                lambda<(CustomerInfo) -> Unit>().captured.invoke(customerInfo!!)
            }
        }
    }
}
