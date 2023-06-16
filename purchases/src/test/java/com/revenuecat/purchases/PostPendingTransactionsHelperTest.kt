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
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostPendingTransactionsHelperTest {

    private val allowSharingPlayStoreAccount = true
    private val appUserId = "test-app-user-id"

    private lateinit var appConfig: AppConfig
    private lateinit var deviceCache: DeviceCache
    private lateinit var billing: BillingAbstract
    private lateinit var dispatcher: Dispatcher
    private lateinit var identityManager: IdentityManager
    private lateinit var postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper

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

        changeBillingConnected()
        changeAutoSyncEnabled(true)

        postPendingTransactionsHelper = PostPendingTransactionsHelper(
            appConfig,
            deviceCache,
            billing,
            dispatcher,
            identityManager,
            postTransactionWithProductDetailsHelper,
        )
    }

    // region syncPendingPurchaseQueue

    @Test
    fun `skip posting pending purchases if autosync is off`() {
        changeAutoSyncEnabled(false)
        syncAndAssertSuccessful(null)
        verify(exactly = 0) {
            billing.queryPurchases(any(), any(), any())
        }
        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if autosync is disabled, and sync is called, success callback with null value is called`() {
        changeAutoSyncEnabled(false)
        syncAndAssertSuccessful(null)
    }

    @Test
    fun `when updating pending purchases, retrieve purchases`() {
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )
        syncAndAssertSuccessful(null)
        verify(exactly = 1) {
            billing.queryPurchases(appUserId, any(), any())
        }
    }

    @Test
    fun `when updating pending purchases, if no purchases to sync, it calls success with null value`() {
        mockSuccessfulQueryPurchases(
            purchasesByHashedToken = emptyMap(),
            notInCache = emptyList()
        )
        syncAndAssertSuccessful(null)
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
                listOf(activePurchase),
                allowSharingPlayStoreAccount,
                appUserId,
                any(),
                any()
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

        syncAndAssertSuccessful(customerInfoMock)
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

        syncAndAssertSuccessful(customerInfoMock)

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

        syncAndAssertSuccessful(null)

        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `when updating pending purchases, if result from querying purchases is not successful skip`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            billing.queryPurchases(appUserId, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured(error)
        }

        syncAndAssertError(error)

        verify(exactly = 0) { deviceCache.cleanPreviouslySentTokens(any()) }
        verify(exactly = 0) {
            postTransactionWithProductDetailsHelper.postTransactions(any(), any(), any(), any(), any())
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

        syncAndAssertSuccessful(customerInfoMock)

        verify(exactly = 1) {
            postTransactionWithProductDetailsHelper.postTransactions(
                allPurchases,
                allowSharingPlayStoreAccount,
                appUserId,
                captureLambda(),
                any()
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
                allPurchases,
                allowSharingPlayStoreAccount,
                appUserId,
                capture(successSlot),
                capture(errorSlot)
            )
        } answers {
            successSlot.captured.invoke(activePurchasedPurchase, customerInfoMock)
            errorSlot.captured.invoke(activeUnspecifiedPurchase, error)
            successSlot.captured.invoke(activePendingPurchase, customerInfoMock)
        }

        syncAndAssertError(error)
    }

    // endregion

    private fun mockPostTransactionsSuccessful(
        customerInfo: CustomerInfo?,
        transactions: List<StoreTransaction>? = null
    ) {
        every {
            postTransactionWithProductDetailsHelper.postTransactions(
                transactions ?: any(),
                allowSharingPlayStoreAccount,
                appUserId,
                captureLambda(),
                any()
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
            billing.queryPurchases(appUserId, captureLambda(), any())
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

    private fun syncAndAssertSuccessful(resultCustomerInfo: CustomerInfo?) {
        var successCallCount = 0
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            onError = { fail("Should be success") },
            onSuccess = {
                assertThat(it).isEqualTo(resultCustomerInfo)
                successCallCount++
            }
        )
        assertThat(successCallCount).isEqualTo(1)
    }

    private fun syncAndAssertError(purchasesError: PurchasesError) {
        var receivedError: PurchasesError? = null
        postPendingTransactionsHelper.syncPendingPurchaseQueue(
            allowSharingPlayStoreAccount,
            onError = { receivedError = it },
            onSuccess = { fail("Should be error") },
        )
        assertThat(receivedError?.code).isEqualTo(purchasesError.code)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo(purchasesError.underlyingErrorMessage)
    }
}
