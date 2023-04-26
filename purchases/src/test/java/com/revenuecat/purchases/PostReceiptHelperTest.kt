package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.GoogleProrationMode
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
        subscriptionOptionId,
        prorationMode = GoogleProrationMode.DEFERRED
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
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager

    private lateinit var postReceiptHelper: PostReceiptHelper

    @Before
    fun setUp() {
        appConfig = mockk()
        backend = mockk()
        billing = mockk()
        customerInfoHelper = mockk()
        deviceCache = mockk()
        subscriberAttributesManager = mockk()
        offlineEntitlementsManager = mockk()

        postedReceiptInfoSlot = slot()

        postReceiptHelper = PostReceiptHelper(
            appConfig = appConfig,
            backend = backend,
            billing = billing,
            customerInfoHelper = customerInfoHelper,
            deviceCache = deviceCache,
            subscriberAttributesManager = subscriberAttributesManager,
            offlineEntitlementsManager = offlineEntitlementsManager
        )

        mockUnsyncedSubscriberAttributes()

        every { appConfig.finishTransactions } returns defaultFinishTransactions
    }

    // region postTransactionAndConsumeIfNeeded

    @Test
    fun `postTransactionAndConsumeIfNeeded posts with expected default parameters`() {
        mockPostReceiptSuccess()

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts unsynced subscriber attributes`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded marks unsynced subscriber attributes as synced on success`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded caches customer info on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded sends updated customer info on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with consuming flag true if not observer mode on success`() {
        val expectedShouldConsumeFlag = true
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with consuming flag false if observer mode on success`() {
        val expectedShouldConsumeFlag = false
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls success block with expected parameters on success`() {
        mockPostReceiptSuccess()

        var successTransaction: StoreTransaction? = null
        var successCustomerInfo: CustomerInfo? = null
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded marks unsynced attributes as synced on error if finishable error`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded does not mark unsynced attributes as synced on error if not finishable error`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with consuming flag true if not observer mode on error if finishable error`() {
        every { appConfig.finishTransactions } returns true
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with consuming flag false if observer mode on error if finishable error`() {
        every { appConfig.finishTransactions } returns false
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded does not call consume transaction on error if not finishable error`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded calls error block with expected parameters on error`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        var errorTransaction: StoreTransaction? = null
        var purchasesError: PurchasesError? = null
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
        assertThat(purchasesError?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    // region offline entitlements

    @Test
    fun `postTransactionAndConsumeIfNeeded resets offline consumer info cache on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.resetOfflineCustomerInfoCache()
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not calculate offline entitlements customer info if not server error`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = false)
        }
        verify(exactly = 0) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, any(), any()) }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded calculates offline entitlements customer info if server error`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = true)
        }
        verify(exactly = 1) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, any(), any()) }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded returns offline entitlements customer info if server error and success calculating customer info`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME)

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        var receivedCustomerInfo: CustomerInfo? = null
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, customerInfo -> receivedCustomerInfo = customerInfo },
            onError = { _, _ -> fail("Expected success") }
        )

        assertThat(receivedCustomerInfo).isEqualTo(defaultCustomerInfo)
        verify(exactly = 1) { customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo) }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not cache offline entitlements`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME)

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ ->  },
            onError = { _, _ -> fail("Expected success") }
        )

        verify(exactly = 0) { customerInfoHelper.cacheCustomerInfo(any()) }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not consume if using offline entitlements`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME)

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ ->  },
            onError = { _, _ -> fail("Expected success") }
        )

        verify(exactly = 0) { billing.consumeAndSave(any(), any()) }
        verify(exactly = 0) { deviceCache.addSuccessfullyPostedToken(any()) }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not mark attributes as synced if using offline entitlements`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME)

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ ->  },
            onError = { _, _ -> fail("Expected success") }
        )

        verify(exactly = 0) { subscriberAttributesManager.markAsSynced(any(), any(), any()) }
    }

    // endregion

    @Test
    fun `postTransactionAndConsumeIfNeeded posts product durations`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts proration mode`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            isRestore = true,
            appUserID = appUserID,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.prorationMode).isEqualTo(GoogleProrationMode.DEFERRED)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded posts subscriptionOptionId`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded sends null durations when posting inapps to backend`() {
        mockPostReceiptSuccess()

        val mockInAppProduct = stubINAPPStoreProduct("productId")
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts productIds`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts presentedOfferingIdentifier`() {
        mockPostReceiptSuccess()

        val expectedPresentedOfferingIdentifier = "offering_a"
        val purchase = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            expectedPresentedOfferingIdentifier
        )

        every { billing.consumeAndSave(true, purchase) } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts storeProduct`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded posts price and currency`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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
    fun `postTransactionAndConsumeIfNeeded attributes are marked as synced when post is successful but there are attribute errors`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(jsonBody = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse))

        val expectedSubscriberAttributeErrors = listOf(
            SubscriberAttributeError("invalid_name", "Attribute key name is not valid.")
        )
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
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

    // region postTokenWithoutConsuming

    @Test
    fun `postTokenWithoutConsuming posts with expected default parameters`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming posts unsynced subscriber attributes`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming marks unsynced subscriber attributes as synced on success`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming caches customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming sends updated customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming adds sent token on success`() {
        val expectedShouldConsumeFlag = true
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming calls success block on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        var successCalledCount = 0
        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming marks unsynced attributes as synced on error if finishable error`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming does not mark unsynced attributes as synced on error if not finishable error`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming adds sent token if finishable error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming does not add sent token on error if not finishable error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming calls error block with expected parameters on error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        var purchasesError: PurchasesError? = null
        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Should fail") },
            onError = { purchasesError = it }
        )

        assertThat(purchasesError?.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    @Test
    fun `postTokenWithoutConsuming does not consume on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
    fun `postTokenWithoutConsuming does not consume on non consumable error `() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
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

    // region offline entitlements

    @Test
    fun `postTokenWithoutConsuming resets offline consumer info cache on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
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
            offlineEntitlementsManager.resetOfflineCustomerInfoCache()
        }
    }

    @Test
    fun `postTokenWithoutConsuming does not calculate offline entitlements customer info if not server error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Expected error") },
            onError = { }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = false)
        }
        verify(exactly = 0) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, any(), any()) }
    }

    @Test
    fun `postTokenWithoutConsuming calculates offline entitlements customer info if server error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { fail("Expected error") },
            onError = { }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = true)
        }
        verify(exactly = 1) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, any(), any()) }
    }

    @Test
    fun `postTokenWithoutConsuming returns offline entitlements customer info if server error and success calculating customer info`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        var successCallCount = 0
        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { successCallCount++ },
            onError = { fail("Should succeed") }
        )

        assertThat(successCallCount).isEqualTo(1)
        verify(exactly = 1) { customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo) }
    }

    @Test
    fun `postTokenWithoutConsuming does not cache offline entitlements`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { customerInfoHelper.cacheCustomerInfo(any()) }
    }

    @Test
    fun `postTokenWithoutConsuming does not mark token as consumed if using offline entitlements`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { billing.consumeAndSave(any(), any()) }
        verify(exactly = 0) { deviceCache.addSuccessfullyPostedToken(any()) }
    }

    @Test
    fun `postTokenWithoutConsuming does not mark attributes as synced if using offline entitlements`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            storeUserID = storeUserId,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            marketplace = marketplace,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { subscriberAttributesManager.markAsSynced(any(), any(), any()) }
    }

    // endregion

    // endregion

    // region helpers

    private enum class PostType {
        TRANSACTION_AND_CONSUME,
        TOKEN_WITHOUT_CONSUMING
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
        postType: PostType = PostType.TRANSACTION_AND_CONSUME
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

        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
        every { customerInfoHelper.cacheCustomerInfo(any()) } just Runs
        every { customerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(any()) } just Runs
        if (postType == PostType.TRANSACTION_AND_CONSUME) {
            every { billing.consumeAndSave(any(), mockStoreTransaction) } just Runs
        } else {
            every { deviceCache.addSuccessfullyPostedToken(postToken) } just Runs
        }
    }

    private fun mockPostReceiptError(
        errorHandlingBehavior: PostReceiptErrorHandlingBehavior,
        postType: PostType = PostType.TRANSACTION_AND_CONSUME
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
            val callback = lambda<PostReceiptDataErrorCallback>().captured
            when (errorHandlingBehavior) {
                PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED -> callback.invokeWithFinishableError()
                PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME -> callback.invokeWithNotFinishableError()
                PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME -> callback.invokeWithServerError()
            }
        }

        every {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(any())
        } answers { firstArg() }
        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(appUserID, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.UnknownError)
            )
        }
        if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED) {
            every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
            if (postType == PostType.TRANSACTION_AND_CONSUME) {
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

    private fun PostReceiptDataErrorCallback.invokeWithFinishableError() {
        invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
            PostReceiptErrorHandlingBehavior.SHOULD_BE_CONSUMED,
            JSONObject(Responses.invalidCredentialsErrorResponse)
        )
    }

    private fun PostReceiptDataErrorCallback.invokeWithNotFinishableError() {
        invoke(
            PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
            PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
            JSONObject(Responses.internalServerErrorResponse)
        )
    }

    private fun PostReceiptDataErrorCallback.invokeWithServerError() {
        invoke(
            PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
            PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            JSONObject(Responses.internalServerErrorResponse)
        )
    }
    // endregion
}
