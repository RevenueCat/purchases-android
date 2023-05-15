package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncPurchasesHelperTest {

    private val appUserID = "test-app-user-id"

    private lateinit var billing: BillingAbstract
    private lateinit var identityManager: IdentityManager
    private lateinit var postReceiptHelper: PostReceiptHelper

    private lateinit var syncPurchasesHelper: SyncPurchasesHelper

    @Before
    fun setUp() {
        billing = mockk()
        identityManager = mockk()
        postReceiptHelper = mockk()

        every { identityManager.currentAppUserID } returns appUserID

        syncPurchasesHelper = SyncPurchasesHelper(
            billing,
            identityManager,
            postReceiptHelper
        )
    }

    @Test
    fun `does not sync if no purchases`() {
        mockBillingQueryAllPurchasesSuccess(emptyList())

        var successCallCount = 0
        syncPurchasesHelper.syncPurchases(
            isRestore = false,
            onSuccess = { successCallCount++ },
            onError = { fail("Should not call onError") }
        )

        assertThat(successCallCount).isEqualTo(1)
        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `calls error if error getting purchases`() {
        mockBillingQueryAllPurchasesError()

        var error: PurchasesError? = null
        syncPurchasesHelper.syncPurchases(
            isRestore = false,
            onSuccess = { fail("Should not call onSuccess") },
            onError = { error = it }
        )

        assertThat(error).isNotNull
        verify(exactly = 0) {
            postReceiptHelper.postTokenWithoutConsuming(
                any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `posts all receipts without consuming`() {
        val purchase1 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-1")
            every { purchaseToken } returns "test-purchase-token-1"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns null
        }
        val purchase2 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-2")
            every { purchaseToken } returns "test-purchase-token-2"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns "test-marketplace"
        }
        mockBillingQueryAllPurchasesSuccess(listOf(purchase1, purchase2))

        every {
            postReceiptHelper.postTokenWithoutConsuming(any(), any(), any(), any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        var successCallCount = 0
        syncPurchasesHelper.syncPurchases(
            isRestore = false,
            onSuccess = { successCallCount++ },
            onError = { fail("Should have succeeded") }
        )

        assertThat(successCallCount).isEqualTo(1)
        verifyAll {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-1",
                storeUserID = "test-store-user-id",
                receiptInfo = any(),
                isRestore = false,
                appUserID = appUserID,
                marketplace = null,
                onSuccess = any(),
                onError = any()
            )
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-2",
                storeUserID = "test-store-user-id",
                receiptInfo = any(),
                isRestore = false,
                appUserID = appUserID,
                marketplace = "test-marketplace",
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `tries to sync all purchases even if there are errors`() {
        val purchase1 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-1")
            every { purchaseToken } returns "test-purchase-token-1"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns null
        }
        val purchase2 = mockk<StoreTransaction>().apply {
            every { productIds } returns listOf("test-product-id-2")
            every { purchaseToken } returns "test-purchase-token-2"
            every { storeUserID } returns "test-store-user-id"
            every { marketplace } returns "test-marketplace"
        }
        mockBillingQueryAllPurchasesSuccess(listOf(purchase1, purchase2))

        every {
            postReceiptHelper.postTokenWithoutConsuming(any(), any(), any(), any(), any(), any(), any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError))
        }

        var errorCallCount = 0
        syncPurchasesHelper.syncPurchases(
            isRestore = false,
            onSuccess = { fail("Should error") },
            onError = { errorCallCount++ }
        )

        assertThat(errorCallCount).isEqualTo(1)
        verifyAll {
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-1",
                storeUserID = "test-store-user-id",
                receiptInfo = any(),
                isRestore = false,
                appUserID = appUserID,
                marketplace = null,
                onSuccess = any(),
                onError = any()
            )
            postReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = "test-purchase-token-2",
                storeUserID = "test-store-user-id",
                receiptInfo = any(),
                isRestore = false,
                appUserID = appUserID,
                marketplace = "test-marketplace",
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
            isRestore = false,
            onSuccess = { successCallCount++ },
            onError = { fail("Should have succeeded") }
        )

        assertThat(successCallCount).isEqualTo(1)
    }

    // region helpers

    private fun mockBillingQueryAllPurchasesSuccess(purchases: List<StoreTransaction>) {
        every {
            billing.queryAllPurchases(appUserID, captureLambda(), any())
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.invoke(purchases)
        }
    }

    private fun mockBillingQueryAllPurchasesError(
        error: PurchasesError = PurchasesError(PurchasesErrorCode.UnknownError)
    ) {
        every {
            billing.queryAllPurchases(appUserID, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }
    }

    // endregion helpers
}
