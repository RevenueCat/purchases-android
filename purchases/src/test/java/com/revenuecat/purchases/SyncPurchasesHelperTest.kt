package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ago
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class SyncPurchasesHelperTest {

    private val appUserID = "test-app-user-id"
    private val initiationSource = PostReceiptInitiationSource.RESTORE
    private val testError = PurchasesError(PurchasesErrorCode.CustomerInfoError)
    private val customerInfoMock = mockk<CustomerInfo>()
    private val appInBackground = false
    private val isRestore = false

    private lateinit var billing: BillingAbstract
    private lateinit var identityManager: IdentityManager
    private lateinit var customerInfoHelper: CustomerInfoHelper
    private lateinit var postReceiptHelper: PostReceiptHelper
    private lateinit var diagnosticsTracker: DiagnosticsTracker

    private lateinit var syncPurchasesHelper: SyncPurchasesHelper

    @Before
    fun setUp() {
        billing = mockk()
        identityManager = mockk()
        customerInfoHelper = mockk()
        postReceiptHelper = mockk()
        diagnosticsTracker = mockk()

        every { identityManager.currentAppUserID } returns appUserID
        every { diagnosticsTracker.trackSyncPurchasesStarted() } just Runs
        every { diagnosticsTracker.trackSyncPurchasesResult(any(), any(), any()) } just Runs

        mockRetrieveCustomerInfoSuccess()

        syncPurchasesHelper = SyncPurchasesHelper(
            billing,
            identityManager,
            customerInfoHelper,
            postReceiptHelper,
            diagnosticsTracker,
        )
    }

    @Test
    fun `does not sync if no purchases`() {
        mockBillingQueryAllPurchasesSuccess(emptyList())

        var receivedCustomerInfo: CustomerInfo? = null
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should not call onError") }
        )

        assertThat(receivedCustomerInfo).isEqualTo(customerInfoMock)
        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `returns error if error getting cached customer info`() {
        mockBillingQueryAllPurchasesSuccess(emptyList())
        mockRetrieveCustomerInfoError()

        var receivedError: PurchasesError? = null
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { fail("Should not call onSuccess") },
            onError = { receivedError = it }
        )

        assertThat(receivedError).isEqualTo(testError)
    }

    @Test
    fun `calls error if error getting purchases`() {
        mockBillingQueryAllPurchasesError()

        var error: PurchasesError? = null
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { fail("Should not call onSuccess") },
            onError = { error = it }
        )

        assertThat(error).isNotNull
        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = any(),
                receiptInfo = any(),
                isRestore = any(),
                appUserID = any(),
                initiationSource = any(),
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `posts all receipts without consuming`() {
        val purchase1 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-1")
            every { purchaseTime } returns 1.hours.ago().time
            every { purchaseToken } returns "test-purchase-token-1"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns null
        }
        val purchase2 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-2")
            every { purchaseTime } returns 1.hours.ago().time
            every { purchaseToken } returns "test-purchase-token-2"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns "test-marketplace"
        }
        mockBillingQueryAllPurchasesSuccess(listOf(purchase1, purchase2))

        every {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = any(),
                receiptInfo = any(),
                isRestore = any(),
                appUserID = any(),
                initiationSource = any(),
                onSuccess = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(mockk())
        }

        var receivedCustomerInfo: CustomerInfo? = null
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should have succeeded") }
        )

        assertThat(receivedCustomerInfo).isEqualTo(customerInfoMock)
        verifyAll {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-1",
                receiptInfo = match { it.storeUserID == "test-store-user-id" },
                isRestore = isRestore,
                appUserID = appUserID,
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-2",
                receiptInfo = match {
                    it.storeUserID == "test-store-user-id" && it.marketplace == "test-marketplace"
                },
                isRestore = isRestore,
                appUserID = appUserID,
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `tries to sync all purchases even if there are errors`() {
        val purchase1 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-1")
            every { purchaseTime } returns 1.hours.ago().time
            every { purchaseToken } returns "test-purchase-token-1"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns null
        }
        val purchase2 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-2")
            every { purchaseTime } returns 1.hours.ago().time
            every { purchaseToken } returns "test-purchase-token-2"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns "test-marketplace"
        }
        mockBillingQueryAllPurchasesSuccess(listOf(purchase1, purchase2))

        every {
            postReceiptHelper.postTokenWithoutConsuming(
                any(), any(), any(), any(), any(), any(), captureLambda(),
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(testError)
        }

        var errorCallCount = 0
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { fail("Should error") },
            onError = { errorCallCount++ }
        )

        assertThat(errorCallCount).isEqualTo(1)
        verifyAll {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-1",
                receiptInfo = match {
                    it.storeUserID == "test-store-user-id"
                },
                isRestore = isRestore,
                appUserID = appUserID,
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-2",
                receiptInfo = match {
                    it.storeUserID == "test-store-user-id" && it.marketplace == "test-marketplace"
                },
                isRestore = isRestore,
                appUserID = appUserID,
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `calls success callback if no purchases to sync`() {
        val purchases = emptyList<StoreTransaction>()
        mockBillingQueryAllPurchasesSuccess(purchases)

        var successCallCount = 0
        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { successCallCount++ },
            onError = { fail("Should have succeeded") }
        )

        assertThat(successCallCount).isEqualTo(1)
    }

    // region diagnostics tracking

    @Test
    fun `calls tracks sync purchases started event`() {
        mockBillingQueryAllPurchasesSuccess(emptyList())

        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { },
            onError = { }
        )

        verify(exactly = 1) {
            diagnosticsTracker.trackSyncPurchasesStarted()
        }
    }

    @Test
    fun `calls tracks sync purchases result event when no purchases`() {
        mockBillingQueryAllPurchasesSuccess(emptyList())

        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { },
            onError = { }
        )

        verify(exactly = 1) {
            diagnosticsTracker.trackSyncPurchasesResult(errorCode = null, errorMessage = null, any())
        }
    }

    @Test
    fun `calls tracks sync purchases result event when error getting purchases`() {
        mockBillingQueryAllPurchasesError()

        syncPurchasesHelper.syncPurchases(
            isRestore = isRestore,
            appInBackground = appInBackground,
            onSuccess = { },
            onError = { }
        )

        verify(exactly = 1) {
            diagnosticsTracker.trackSyncPurchasesResult(
                errorCode = PurchasesErrorCode.UnknownError.code,
                errorMessage = "Unknown error. Check the underlying error for more details.",
                any(),
            )
        }
    }

    // endregion diagnostics tracking

    // region helpers

    private fun mockBillingQueryAllPurchasesSuccess(purchases: List<StoreTransaction>) {
        every {
            billing.queryAllPurchases(
                appUserID = appUserID,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.invoke(purchases)
        }
    }

    private fun mockBillingQueryAllPurchasesError(
        error: PurchasesError = PurchasesError(PurchasesErrorCode.UnknownError)
    ) {
        every {
            billing.queryAllPurchases(
                appUserID = appUserID,
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }
    }

    private fun mockRetrieveCustomerInfoSuccess(
        customerInfo: CustomerInfo = customerInfoMock
    ) {
        val callbackSlot = slot<ReceiveCustomerInfoCallback>()
        every {
            customerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.CACHED_OR_FETCHED,
                appInBackground,
                isRestore,
                callback = capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onReceived(customerInfo)
        }
    }

    private fun mockRetrieveCustomerInfoError(
        error: PurchasesError = testError
    ) {
        val callbackSlot = slot<ReceiveCustomerInfoCallback>()
        every {
            customerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.CACHED_OR_FETCHED,
                appInBackground,
                isRestore,
                callback = capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.onError(error)
        }
    }

    // endregion helpers
}
