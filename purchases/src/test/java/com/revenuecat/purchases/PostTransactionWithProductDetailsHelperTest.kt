package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostTransactionWithProductDetailsHelperTest {

    private val allowSharingPlayStoreAccount = true
    private val appUserID = "appUserID"
    private val productID = "productID"
    private val subscriptionOptionId = "monthly_base_plan"
    private val initiationSource = PostReceiptInitiationSource.PURCHASE
    private val mockStoreProduct = stubStoreProduct(productID)

    private val mockSubsTransaction = createTransaction(ProductType.SUBS)
    private val mockInAppTransaction = createTransaction(ProductType.INAPP)

    private lateinit var billing: BillingAbstract
    private lateinit var postReceiptHelper: PostReceiptHelper

    private lateinit var postTransactionWithProductDetailsHelper: PostTransactionWithProductDetailsHelper

    @Before
    fun setUp() {
        billing = mockk()
        postReceiptHelper = mockk()

        postTransactionWithProductDetailsHelper = PostTransactionWithProductDetailsHelper(
            billing,
            postReceiptHelper,
        )
    }

    @Test
    fun `if no transactions, no callbacks are called`() {
        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = emptyList(),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ -> fail("Should not be called") },
            transactionPostError = { _, _ -> fail("Should not be called") },
        )
    }

    @Test
    fun `if pending transaction, error callback is called`() {
        val transactions = listOf(
            mockk<StoreTransaction>().apply { every { purchaseState } returns PurchaseState.PENDING }
        )
        var receivedError: PurchasesError? = null
        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = transactions,
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ -> fail("Should not be called") },
            transactionPostError = { _, error -> receivedError = error },
        )

        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PaymentPendingError)
    }

    @Test
    fun `if pending transaction, transaction is not posted`() {
        val transactions = listOf(
            mockk<StoreTransaction>().apply { every { purchaseState } returns PurchaseState.PENDING }
        )

        var errorCallCount = 0
        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = transactions,
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ -> fail("Should not be called") },
            transactionPostError = { _, _ -> errorCallCount++ },
        )

        assertThat(errorCallCount).isEqualTo(1)
        verify(exactly = 0) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `if query product details fails, transaction is posted without product information`() {
        mockQueryProductDetailsError(mockSubsTransaction)
        mockPostReceiptSuccessful(mockSubsTransaction, null)

        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = listOf(mockSubsTransaction),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockSubsTransaction,
                storeProduct = null,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                appInBackground = any(),
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `if INAPP, transaction is posted with product information if found`() {
        mockQueryProductDetailsSuccess(mockInAppTransaction, listOf(mockStoreProduct))
        mockPostReceiptSuccessful(mockInAppTransaction, mockStoreProduct)

        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = listOf(mockInAppTransaction),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockInAppTransaction,
                storeProduct = mockStoreProduct,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                appInBackground = any(),
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `if SUBS, transaction is posted with product information if found`() {
        mockQueryProductDetailsSuccess(mockSubsTransaction, listOf(mockStoreProduct))
        mockPostReceiptSuccessful(mockSubsTransaction, mockStoreProduct)

        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = listOf(mockSubsTransaction),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockSubsTransaction,
                storeProduct = mockStoreProduct,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                appInBackground = any(),
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `success callback is called if post transaction successful`() {
        val mockCustomerInfo = mockk<CustomerInfo>()
        mockQueryProductDetailsSuccess(mockSubsTransaction, listOf(mockStoreProduct))
        mockPostReceiptSuccessful(mockSubsTransaction, mockStoreProduct, mockCustomerInfo)

        var receivedStoreTransaction: StoreTransaction? = null
        var receivedCustomerInfo: CustomerInfo? = null
        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = listOf(mockSubsTransaction),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            appInBackground = false,
            transactionPostSuccess = { storeTransaction, customerInfo ->
                receivedStoreTransaction = storeTransaction
                receivedCustomerInfo = customerInfo
            },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        assertThat(receivedStoreTransaction).isEqualTo(mockSubsTransaction)
        assertThat(receivedCustomerInfo).isEqualTo(mockCustomerInfo)
    }


    private fun createTransaction(
        type: ProductType = ProductType.SUBS,
        productID: String = this.productID
    ): StoreTransaction {
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = listOf(productID),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        return purchase.toStoreTransaction(type, subscriptionOptionId = subscriptionOptionId)
    }

    private fun mockQueryProductDetailsSuccess(
        transaction: StoreTransaction,
        storeProducts: List<StoreProduct>
    ) {
        every {
            billing.queryProductDetailsAsync(
                productType = transaction.type,
                productIds = transaction.productIds.toSet(),
                onReceive = captureLambda(),
                onError = any(),
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }
    }

    private fun mockQueryProductDetailsError(
        transaction: StoreTransaction,
        error: PurchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "Error")
    ) {
        every {
            billing.queryProductDetailsAsync(
                productType = transaction.type,
                productIds = transaction.productIds.toSet(),
                onReceive = any(),
                onError = captureLambda(),
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }
    }

    private fun mockPostReceiptSuccessful(
        transaction: StoreTransaction,
        storeProduct: StoreProduct?,
        customerInfoResult: CustomerInfo = mockk()
    ) {
        every {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transaction,
                storeProduct = storeProduct,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                appInBackground = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(transaction, customerInfoResult)
        }
    }
}
