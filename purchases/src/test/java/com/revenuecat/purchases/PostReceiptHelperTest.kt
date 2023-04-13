package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PostReceiptHelperTest {

    private val appUserID = "test-app-user-id"
    private val mockStoreProduct = stubStoreProduct("productId")
    private val mockGooglePurchase = stubGooglePurchase(
        productIds = listOf("uno", "dos")
    )
    private val subscriptionOptionId = "mock-base-plan-id:mock-offer-id"
    private val postToken = "test-post-token"
    private val storeUserId = "test-store-user-id"
    private val marketplace = "test-marketplace"
    private val mockStoreTransaction = mockGooglePurchase.toStoreTransaction(
        ProductType.SUBS,
        null,
        subscriptionOptionId
    )
    private val testReceiptInfo = ReceiptInfo(
        productIDs = listOf("test-product-id-1", "test-product-id-2"),
        offeringIdentifier = "test-offering-identifier",
        subscriptionOptionId = subscriptionOptionId,
        storeProduct = mockStoreProduct
    )
    private val defaultFinishTransactions = true
    private val defaultCustomerInfo = CustomerInfoFactory.buildCustomerInfo(
        JSONObject(Responses.validFullPurchaserResponse),
        null,
        VerificationResult.NOT_REQUESTED
    )
    private val unsyncedSubscriberAttributes = getUnsyncedSubscriberAttributes()

    private lateinit var postedReceiptInfoSlot: CapturingSlot<ReceiptInfo>

    private lateinit var appConfig: AppConfig
    private lateinit var backend: Backend
    private lateinit var billing: BillingAbstract
    private lateinit var customerInfoHelper: CustomerInfoHelper
    private lateinit var deviceCache: DeviceCache
    private lateinit var subscriberAttributesManager: SubscriberAttributesManager

    private lateinit var postReceiptHelper: PostReceiptHelper

    @Before
    fun setUp() {
        appConfig = mockk()
        backend = mockk()
        billing = mockk()
        customerInfoHelper = mockk()
        deviceCache = mockk()
        subscriberAttributesManager = mockk()

        postedReceiptInfoSlot = slot()

        postReceiptHelper = PostReceiptHelper(
            appConfig = appConfig,
            backend = backend,
            billing = billing,
            customerInfoHelper = customerInfoHelper,
            deviceCache = deviceCache,
            subscriberAttributesManager = subscriberAttributesManager
        )

        mockUnsyncedSubscriberAttributes()

        every { appConfig.finishTransactions } returns defaultFinishTransactions
    }

    // region postTransactionToBackend

    @Test
    fun `postTransactionToBackend posts with expected default parameters`() {
        mockPostReceiptSuccess()

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        val expectedReceiptInfo = ReceiptInfo(
            productIDs = mockStoreTransaction.productIds,
            offeringIdentifier = mockStoreTransaction.presentedOfferingIdentifier,
            subscriptionOptionId = mockStoreTransaction.subscriptionOptionId,
            storeProduct = mockStoreProduct
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = mockStoreTransaction.purchaseToken,
                appUserID = appUserID,
                isRestore = allowSharingPlayStoreAccount,
                observerMode = !defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = expectedReceiptInfo,
                storeAppUserID = mockStoreTransaction.storeUserID,
                marketplace = mockStoreTransaction.marketplace,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionToBackend posts unsynced subscriber attributes`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = unsyncedSubscriberAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionToBackend marks unsynced subscriber attributes as synced on success`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            subscriberAttributesManager.markAsSynced(
                appUserID = appUserID,
                attributesToMarkAsSynced = unsyncedSubscriberAttributes,
                attributeErrors = emptyList()
            )
        }
    }

    @Test
    fun `postTransactionToBackend caches customer info on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoHelper.cacheCustomerInfo(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTransactionToBackend sends updated customer info on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTransactionToBackend calls consume transaction with consuming flag true if not observer mode on success`() {
        val expectedShouldConsumeFlag = true
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(expectedShouldConsumeFlag, mockStoreTransaction)
        }
    }

    @Test
    fun `postTransactionToBackend calls consume transaction with consuming flag false if observer mode on success`() {
        val expectedShouldConsumeFlag = false
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(expectedShouldConsumeFlag, mockStoreTransaction)
        }
    }

    @Test
    fun `postTransactionToBackend calls success block with expected parameters on success`() {
        mockPostReceiptSuccess()

        var successTransaction: StoreTransaction? = null
        var successCustomerInfo: CustomerInfo? = null
        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { transaction, customerInfo ->
                successTransaction = transaction
                successCustomerInfo = customerInfo
            },
            onError = { _, _ -> fail("Should succeed") }
        )

        assertThat(successTransaction).isEqualTo(mockStoreTransaction)
        assertThat(successCustomerInfo).isEqualTo(defaultCustomerInfo)
    }

    @Test
    fun `postTransactionToBackend marks unsynced attributes as synced on error if it should consume purchase`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = true, isServerError = false, body = null)

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            subscriberAttributesManager.markAsSynced(
                appUserID = appUserID,
                attributesToMarkAsSynced = unsyncedSubscriberAttributes,
                attributeErrors = emptyList()
            )
        }
    }

    @Test
    fun `postTransactionToBackend does not mark unsynced attributes as synced on error if it should not consume purchase`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = false, isServerError = false, body = null)

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 0) {
            subscriberAttributesManager.markAsSynced(
                appUserID = any(),
                attributesToMarkAsSynced = any(),
                attributeErrors = any()
            )
        }
    }

    @Test
    fun `postTransactionToBackend calls consume transaction with consuming flag true if not observer mode on error if it should consume purchase`() {
        every { appConfig.finishTransactions } returns true
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = true, isServerError = false, body = null)

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(true, mockStoreTransaction)
        }
    }

    @Test
    fun `postTransactionToBackend calls consume transaction with consuming flag false if observer mode on error if it should consume purchase`() {
        every { appConfig.finishTransactions } returns false
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = true, isServerError = false, body = null)

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(false, mockStoreTransaction)
        }
    }

    @Test
    fun `postTransactionToBackend does not call consume transaction on error if it should not consume purchase`() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = false, isServerError = false, body = null)

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(any(), any())
        }
    }

    @Test
    fun `postTransactionToBackend calls error block with expected parameters on error`() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(expectedPurchasesError, shouldConsumePurchase = false, isServerError = false, body = null)

        var errorTransaction: StoreTransaction? = null
        var purchasesError: PurchasesError? = null
        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { transaction, error ->
                errorTransaction = transaction
                purchasesError = error
            }
        )

        assertThat(errorTransaction).isEqualTo(mockStoreTransaction)
        assertThat(purchasesError).isEqualTo(expectedPurchasesError)
    }

    @Test
    fun `postTransactionToBackend posts product durations`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.duration).isEqualTo("P1M")
    }

    @Test
    fun `postTransactionToBackend posts subscriptionOptionId`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.subscriptionOptionId).isEqualTo(subscriptionOptionId)
    }

    @Test
    fun `postTransactionToBackend sends null durations when posting inapps to backend`() {
        mockPostReceiptSuccess()

        val mockInAppProduct = stubINAPPStoreProduct("productId")
        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockInAppProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.duration).isNull()
    }

    @Test
    fun `postTransactionToBackend posts productIds`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.productIDs).isEqualTo(listOf("uno", "dos"))
    }

    @Test
    fun `postTransactionToBackend posts presentedOfferingIdentifier`() {
        mockPostReceiptSuccess()

        val expectedPresentedOfferingIdentifier = "offering_a"
        val purchase = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            expectedPresentedOfferingIdentifier
        )

        every { billing.consumeAndSave(true, purchase) } just Runs

        postReceiptHelper.postTransactionToBackend(
            purchase = purchase,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.offeringIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `postTransactionToBackend posts storeProduct`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.storeProduct).isEqualTo(mockStoreProduct)
    }

    @Test
    fun `postTransactionToBackend posts price and currency`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.price).isEqualTo(4.99)
        assertThat(postedReceiptInfoSlot.captured.currency).isEqualTo("USD")
    }

    @Test
    fun `postTransactionToBackend attributes are marked as synced when post is successful but there are attribute errors`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(jsonBody = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse))

        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("invalid_name", "Attribute key name is not valid.")
        )
        postReceiptHelper.postTransactionToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            subscriberAttributesManager.markAsSynced(
                appUserID,
                unsyncedSubscriberAttributes,
                expectedSubscriberAttributeErrors
            )
        }
    }

    // endregion

    // region postTokenAndReceiptInfoToBackend

    @Test
    fun `postTokenAndReceiptInfoToBackend posts with expected default parameters`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = postToken,
                appUserID = appUserID,
                isRestore = allowSharingPlayStoreAccount,
                observerMode = !defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                storeAppUserID = storeUserId,
                marketplace = marketplace,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend posts unsynced subscriber attributes`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = unsyncedSubscriberAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend marks unsynced subscriber attributes as synced on success`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            subscriberAttributesManager.markAsSynced(
                appUserID = appUserID,
                attributesToMarkAsSynced = unsyncedSubscriberAttributes,
                attributeErrors = emptyList()
            )
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend caches customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoHelper.cacheCustomerInfo(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend sends updated customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend adds sent token on success`() {
        val expectedShouldConsumeFlag = true
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            deviceCache.addSuccessfullyPostedToken(postToken)
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend calls success block on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        var successCalledCount = 0
        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { successCalledCount++ },
            onError = { fail("Should succeed") }
        )

        assertThat(successCalledCount).isEqualTo(1)
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend marks unsynced attributes as synced on error if it should consume purchase`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = true,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 1) {
            subscriberAttributesManager.markAsSynced(
                appUserID = appUserID,
                attributesToMarkAsSynced = unsyncedSubscriberAttributes,
                attributeErrors = emptyList()
            )
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend does not mark unsynced attributes as synced on error if it should not consume purchase`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = false,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 0) {
            subscriberAttributesManager.markAsSynced(
                appUserID = any(),
                attributesToMarkAsSynced = any(),
                attributeErrors = any()
            )
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend adds sent token if it should consume purchase`() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = true,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 1) {
            deviceCache.addSuccessfullyPostedToken(postToken)
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend does not add sent token on error if it should not consume purchase`() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = false,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 0) {
            deviceCache.addSuccessfullyPostedToken(postToken)
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend calls error block with expected parameters on error`() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = false,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        var purchasesError: PurchasesError? = null
        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { purchasesError = it }
        )

        assertThat(purchasesError).isEqualTo(expectedPurchasesError)
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend does not consume on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_AND_RECEIPT_INFO)

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(any(), any())
        }
    }

    @Test
    fun `postTokenAndReceiptInfoToBackend does not consume on error `() {
        val expectedPurchasesError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        mockPostReceiptError(
            error = expectedPurchasesError,
            shouldConsumePurchase = false,
            isServerError = false,
            body = null,
            postType = PostType.TOKEN_AND_RECEIPT_INFO
        )

        postReceiptHelper.postTokenAndReceiptInfoToBackend(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(any(), any())
        }
    }

    // endregion

    // region helpers

    private enum class PostType {
        TRANSACTION,
        TOKEN_AND_RECEIPT_INFO
    }

    private fun mockUnsyncedSubscriberAttributes(
        unsyncedSubscriberAttributes: Map<String, SubscriberAttribute> = emptyMap()
    ) {
        every { subscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserID, captureLambda()) } answers {
            lambda<(Map<String, SubscriberAttribute>) -> Unit>().captured.invoke(unsyncedSubscriberAttributes)
        }
    }

    private fun mockPostReceiptSuccess(
        customerInfo: CustomerInfo = defaultCustomerInfo,
        jsonBody: JSONObject = JSONObject(Responses.validFullPurchaserResponse),
        postType: PostType = PostType.TRANSACTION
    ) {
        every {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = any(),
                receiptInfo = capture(postedReceiptInfoSlot),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(customerInfo, jsonBody)
        }

        every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
        every { customerInfoHelper.cacheCustomerInfo(any()) } just Runs
        every { customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(any()) } just Runs
        if (postType == PostType.TRANSACTION) {
            every { billing.consumeAndSave(any(), mockStoreTransaction) } just Runs
        } else {
            every { deviceCache.addSuccessfullyPostedToken(postToken) } just Runs
        }
    }

    private fun mockPostReceiptError(
        error: PurchasesError,
        shouldConsumePurchase: Boolean,
        isServerError: Boolean,
        body: JSONObject?,
        postType: PostType = PostType.TRANSACTION
    ) {
        every {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = any(),
                receiptInfo = capture(postedReceiptInfoSlot),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<PostReceiptDataErrorCallback>().captured.invoke(error, shouldConsumePurchase, isServerError, body)
        }

        if (shouldConsumePurchase) {
            every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
            if (postType == PostType.TRANSACTION) {
                every { billing.consumeAndSave(any(), mockStoreTransaction) } just Runs
            } else {
                every { deviceCache.addSuccessfullyPostedToken(postToken) } just Runs
            }
        }
    }

    private fun getUnsyncedSubscriberAttributes(): Map<String, SubscriberAttribute> {
        val subscriberAttribute = SubscriberAttribute("key", "value")
        return mapOf(
            subscriberAttribute.key.backendKey to subscriberAttribute
        )
    }
    // endregion
}
