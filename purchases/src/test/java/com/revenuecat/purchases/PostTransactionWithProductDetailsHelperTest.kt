package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class PostTransactionWithProductDetailsHelperTest {

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
    public fun setUp() {
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
            sdkOriginated = true,
            transactionPostSuccess = { _, _ -> fail("Should not be called") },
            transactionPostError = { _, _ -> fail("Should not be called") },
        )
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
            sdkOriginated = true,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockSubsTransaction,
                storeProduct = null,
                subscriptionOptionForProductIDs = null,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                sdkOriginated = true,
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
            sdkOriginated = true,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockInAppTransaction,
                storeProduct = mockStoreProduct,
                subscriptionOptionForProductIDs = null,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                sdkOriginated = true,
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
            sdkOriginated = true,
            transactionPostSuccess = { _, _ ->  },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockSubsTransaction,
                storeProduct = mockStoreProduct,
                subscriptionOptionForProductIDs = null,
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                sdkOriginated = true,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `if multi-line subscription, txn is posted with product information and correct subscription options`() {
        val product1 = mockGoogleStoreProduct(productID, subscriptionOptionId)
        val productID2 = productID + "2"
        val subscriptionOptionId2 = subscriptionOptionId + "2"
        val product2 = mockGoogleStoreProduct(productID2, subscriptionOptionId2)

        val mockTransaction = createTransaction(
            ProductType.SUBS,
            productIDs = listOf(productID, productID2),
            subscriptionOptionIdForProductIDs = mapOf(
                productID to subscriptionOptionId,
                productID2 to subscriptionOptionId2
            )
        )
        mockQueryProductDetailsSuccess(mockTransaction, listOf(product1, product2))
        mockPostReceiptSuccessful(mockTransaction, product1)

        every {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockTransaction,
                storeProduct = product1,
                subscriptionOptionForProductIDs = any(),
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(mockTransaction, mockk())
        }

        postTransactionWithProductDetailsHelper.postTransactions(
            transactions = listOf(mockTransaction),
            allowSharingPlayStoreAccount = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = initiationSource,
            sdkOriginated = true,
            transactionPostSuccess = { _, _ -> },
            transactionPostError = { _, _ -> fail("Should be success") },
        )

        val subscriptionOptionsForProductIDsSlot = slot<Map<String, SubscriptionOption>>()

        verify(exactly = 1) {
            postReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockTransaction,
                storeProduct = product1,
                subscriptionOptionForProductIDs = capture(subscriptionOptionsForProductIDsSlot),
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                sdkOriginated = true,
                onSuccess = any(),
                onError = any(),
            )
        }

        val capturedSubscriptionOptionsForProductIDs = subscriptionOptionsForProductIDsSlot.captured
        assertThat(capturedSubscriptionOptionsForProductIDs.size).isEqualTo(2)

        val product1Option = capturedSubscriptionOptionsForProductIDs[productID]
        assertThat(product1Option).isNotNull
        assertThat(product1Option!!.id).isEqualTo(subscriptionOptionId)

        val product2Option = capturedSubscriptionOptionsForProductIDs[productID2]
        assertThat(product2Option).isNotNull
        assertThat(product2Option!!.id).isEqualTo(subscriptionOptionId2)
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
            sdkOriginated = true,
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
        productID: String = this.productID,
        productIDs: List<String>? = null,
        subscriptionOptionIdForProductIDs: Map<String, String>? = null,
    ): StoreTransaction {
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = productIDs.takeUnless { it.isNullOrEmpty() } ?: listOf(productID),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        return purchase.toStoreTransaction(
            type,
            subscriptionOptionId = subscriptionOptionId,
            subscriptionOptionIdForProductIDs = subscriptionOptionIdForProductIDs
        )
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
                subscriptionOptionForProductIDs = any(),
                isRestore = allowSharingPlayStoreAccount,
                appUserID = appUserID,
                initiationSource = initiationSource,
                sdkOriginated = true,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(transaction, customerInfoResult)
        }
    }

    private fun mockGoogleStoreProduct(
        productID: String,
        basePlanID: String
    ): GoogleStoreProduct {
        val googleStoreProduct: GoogleStoreProduct = mockk()
        every { googleStoreProduct.productId } returns productID
        every { googleStoreProduct.basePlanId } returns basePlanID

        val mockSubscriptionOption: SubscriptionOption = mockk()
        every { mockSubscriptionOption.id } returns basePlanID
        val subOptions = SubscriptionOptions(
            subscriptionOptions = listOf(mockSubscriptionOption)
        )
        every { googleStoreProduct.subscriptionOptions } returns subOptions

        return googleStoreProduct
    }
}
