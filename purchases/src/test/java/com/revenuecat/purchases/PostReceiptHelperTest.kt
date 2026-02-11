package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.ago
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadata
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataStore
import com.revenuecat.purchases.common.networking.PostReceiptProductInfo
import com.revenuecat.purchases.common.networking.PostReceiptResponse
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
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
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class PostReceiptHelperTest {

    private val appUserID = "test-app-user-id"
    private val mockStoreProduct = stubStoreProduct("productId")
    private val mockGooglePurchase = stubGooglePurchase(
        productIds = listOf("lifetime_product", "dos")
    )
    private val mockPendingPurchase = stubGooglePurchase(
        productIds = listOf("lifetime_product", "dos"),
        purchaseToken = "pending-purchase-token",
        purchaseState = Purchase.PurchaseState.PENDING,
    )
    private val subscriptionOptionId = "mock-base-plan-id:mock-offer-id"
    private val postToken = "test-post-token"
    private val storeUserId = "test-store-user-id"
    private val initiationSource = PostReceiptInitiationSource.PURCHASE
    private val marketplace = "test-marketplace"
    private val mockStoreTransaction = mockGooglePurchase.toStoreTransaction(
        ProductType.SUBS,
        null,
        subscriptionOptionId,
        replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE
    )
    private val mockPendingStoreTransaction = mockPendingPurchase.toStoreTransaction(
        ProductType.SUBS,
        null,
        subscriptionOptionId,
        replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE
    )
    private val testReceiptInfo = ReceiptInfo(
        productIDs = listOf("test-product-id-1", "test-product-id-2"),
        purchaseTime = Date().time,
        presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "test-offering-identifier"),
        price = mockStoreProduct.price.amountMicros.div(SharedConstants.MICRO_MULTIPLIER),
        currency = mockStoreProduct.price.currencyCode,
        period = mockStoreProduct.period,
        pricingPhases = mockStoreProduct.defaultOption?.pricingPhases,
        replacementMode = null,
        platformProductIds = emptyList(),
        storeUserID = storeUserId,
        marketplace = marketplace,
    )
    private val defaultFinishTransactions = true
    private val defaultCustomerInfo = CustomerInfoFactory.buildCustomerInfo(
        JSONObject(Responses.validFullPurchaserResponse),
        null,
        VerificationResult.NOT_REQUESTED
    )
    private val unsyncedSubscriberAttributes = getUnsyncedSubscriberAttributes()
    private val event = PaywallEvent(
        creationData = PaywallEvent.CreationData(UUID.randomUUID(), 1.hours.ago()),
        data = PaywallEvent.Data(
            paywallIdentifier = "paywall_id",
            PresentedOfferingContext("offering_id"),
            10,
            UUID.randomUUID(),
            "footer",
            "es_ES",
            false,
            packageIdentifier = "test-package-id",
            productIdentifier = mockGooglePurchase.products.first(),
        ),
        type = PaywallEventType.PURCHASE_INITIATED,
    )

    private lateinit var postedReceiptInfoSlot: CapturingSlot<ReceiptInfo>

    private lateinit var appConfig: AppConfig
    private lateinit var backend: Backend
    private lateinit var billing: BillingAbstract
    private lateinit var customerInfoUpdateHandler: CustomerInfoUpdateHandler
    private lateinit var deviceCache: DeviceCache
    private lateinit var subscriberAttributesManager: SubscriberAttributesManager
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var paywallPresentedCache: PaywallPresentedCache
    private lateinit var localTransactionMetadataStore: LocalTransactionMetadataStore

    private lateinit var postReceiptHelper: PostReceiptHelper

    @Before
    public fun setUp() {
        appConfig = mockk()
        backend = mockk()
        billing = mockk()
        customerInfoUpdateHandler = mockk()
        deviceCache = mockk()
        subscriberAttributesManager = mockk()
        offlineEntitlementsManager = mockk()
        paywallPresentedCache = PaywallPresentedCache()
        localTransactionMetadataStore = mockk()

        postedReceiptInfoSlot = slot()

        postReceiptHelper = PostReceiptHelper(
            appConfig = appConfig,
            backend = backend,
            billing = billing,
            customerInfoUpdateHandler = customerInfoUpdateHandler,
            deviceCache = deviceCache,
            subscriberAttributesManager = subscriberAttributesManager,
            offlineEntitlementsManager = offlineEntitlementsManager,
            paywallPresentedCache = paywallPresentedCache,
            localTransactionMetadataStore = localTransactionMetadataStore,
        )

        mockUnsyncedSubscriberAttributes()

        every { localTransactionMetadataStore.getLocalTransactionMetadata(any()) } returns null
        every { localTransactionMetadataStore.cacheLocalTransactionMetadata(any(), any()) } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(any()) } just Runs

        every { appConfig.finishTransactions } returns defaultFinishTransactions
        every { appConfig.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
    }

    // region postTransactionAndConsumeIfNeeded

    @Test
    fun `postTransactionAndConsumeIfNeeded posts with expected default parameters`() {
        mockPostReceiptSuccess()

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        val expectedReceiptInfo = ReceiptInfo(
            productIDs = mockStoreTransaction.productIds,
            purchaseTime = mockStoreTransaction.purchaseTime,
            presentedOfferingContext = mockStoreTransaction.presentedOfferingContext,
            price = mockStoreProduct.price.amountMicros.div(SharedConstants.MICRO_MULTIPLIER),
            formattedPrice = mockStoreProduct.price.formatted,
            currency = mockStoreProduct.price.currencyCode,
            period = mockStoreProduct.period,
            pricingPhases = null,
            replacementMode = mockStoreTransaction.replacementMode,
            platformProductIds = listOf(
                mapOf("product_id" to "lifetime_product"),
                mapOf("product_id" to "dos"),
            ),
            sdkOriginated = false,
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = mockStoreTransaction.purchaseToken,
                appUserID = appUserID,
                isRestore = allowSharingPlayStoreAccount,
                finishTransactions = defaultFinishTransactions,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                subscriberAttributes = emptyMap(),
                receiptInfo = expectedReceiptInfo,
                initiationSource = PostReceiptInitiationSource.PURCHASE,
                paywallPostReceiptData = null,
                onSuccess = any(),
                onError = any()
            )
        }
        assertThat(expectedReceiptInfo.storeUserID).isEqualTo(mockStoreTransaction.storeUserID)
        assertThat(expectedReceiptInfo.marketplace).isEqualTo(mockStoreTransaction.marketplace)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded posts unsynced subscriber attributes`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                purchasesAreCompletedBy = any(),
                subscriberAttributes = unsyncedSubscriberAttributes.toBackendMap(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = null,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
    fun `postTransactionAndConsumeIfNeeded caches and notifies listeners on success`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with finish transactions flag true if not observer mode on success`() {
        val expectedFinishTransactionsFlag = true
        every { appConfig.finishTransactions } returns expectedFinishTransactionsFlag

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = expectedFinishTransactionsFlag,
                purchase = mockStoreTransaction,
                shouldConsume = false,
                initiationSource = initiationSource,
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with finish transactions flag false if observer mode on success`() {
        val expectedFinishTransactionsFlag = false
        every { appConfig.finishTransactions } returns expectedFinishTransactionsFlag
        every { appConfig.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = expectedFinishTransactionsFlag,
                purchase = mockStoreTransaction,
                shouldConsume = false,
                initiationSource = initiationSource,
            )
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with finish transactions flag true and shouldConsume flag false if not observer mode on error if finishable error`() {
        every { appConfig.finishTransactions } returns true
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = true,
                purchase = mockStoreTransaction,
                shouldConsume = false,
                initiationSource = initiationSource
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded calls consume transaction with finish transactions flag false and shouldConsume flag false if observer mode on error if finishable error`() {
        every { appConfig.finishTransactions } returns false
        every { appConfig.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = false,
                purchase = mockStoreTransaction,
                shouldConsume = false,
                initiationSource = initiationSource,
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not call consume transaction on error if not finishable error`() {
        mockPostReceiptError(errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(
                finishTransactions = any(),
                purchase = any(),
                shouldConsume = any(),
                initiationSource = initiationSource
            )
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Expected error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = true)
        }
        verify(exactly = 1) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserId = appUserID,
            onSuccess = any(),
            onError = any()
        ) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        var receivedCustomerInfo: CustomerInfo? = null
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, customerInfo -> receivedCustomerInfo = customerInfo },
            onError = { _, _ -> fail("Expected success") }
        )

        assertThat(receivedCustomerInfo).isEqualTo(defaultCustomerInfo)
        verify(exactly = 1) { customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ ->  },
            onError = { _, _ -> fail("Expected success") }
        )

        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
        verify(exactly = 1) { customerInfoUpdateHandler.notifyListeners(any()) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ ->  },
            onError = { _, _ -> fail("Expected success") }
        )

        verify(exactly = 0) { billing.consumeAndSave(
            finishTransactions = any(),
            purchase = any(),
            shouldConsume = any(),
            initiationSource = initiationSource
        ) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.duration).isEqualTo("P1M")
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded posts replacement mode`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_FULL_PRICE)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded sends null durations when posting inapps to backend`() {
        mockPostReceiptSuccess()

        val mockInAppProduct = stubINAPPStoreProduct("productId")
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockInAppProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.productIDs).isEqualTo(listOf("lifetime_product", "dos"))
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded posts presentedOfferingIdentifier`() {
        mockPostReceiptSuccess()

        val expectedPresentedOfferingContext = PresentedOfferingContext("offering_a")
        val purchase = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            expectedPresentedOfferingContext,
        )

        every { billing.consumeAndSave(
            finishTransactions = true,
            purchase = purchase,
            shouldConsume = false,
            initiationSource = initiationSource
        )
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = purchase,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.presentedOfferingContext).isEqualTo(expectedPresentedOfferingContext)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded posts storeProduct info`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.price).isEqualTo(4.99)
        assertThat(postedReceiptInfoSlot.captured.currency).isEqualTo("USD")
        assertThat(postedReceiptInfoSlot.captured.period).isEqualTo(
            Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")
        )
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
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
        mockPostReceiptSuccess(
            postType = PostType.TOKEN_WITHOUT_CONSUMING,
            postReceiptInitiationSource = PostReceiptInitiationSource.RESTORE,
        )

        val allowSharingPlayStoreAccount = true

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = allowSharingPlayStoreAccount,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.RESTORE,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = postToken,
                appUserID = appUserID,
                isRestore = allowSharingPlayStoreAccount,
                finishTransactions = defaultFinishTransactions,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.RESTORE,
                paywallPostReceiptData = null,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                purchasesAreCompletedBy = any(),
                subscriberAttributes = unsyncedSubscriberAttributes.toBackendMap(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = null,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
    fun `postTokenWithoutConsuming caches and notifies listeners with new customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 1) {
            customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo)
        }
    }

    @Test
    fun `postTokenWithoutConsuming returns customer info on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        var receivedCustomerInfo: CustomerInfo? = null
        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { receivedCustomerInfo = it },
            onError = { fail("Should succeed") }
        )

        assertThat(receivedCustomerInfo).isEqualTo(defaultCustomerInfo)
    }

    @Test
    fun `postTokenWithoutConsuming adds sent token on success`() {
        val expectedShouldConsumeFlag = true
        every { appConfig.finishTransactions } returns expectedShouldConsumeFlag

        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { successCalledCount++ },
            onError = { fail("Should succeed") }
        )

        assertThat(successCalledCount).isEqualTo(1)
    }

    @Test
    fun `postTokenWithoutConsuming marks unsynced attributes as synced on error if finishable error`() {
        mockUnsyncedSubscriberAttributes(unsyncedSubscriberAttributes)
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(
                finishTransactions = any(),
                purchase = any(),
                shouldConsume = any(),
                initiationSource = initiationSource
            )
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
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { fail("Should fail") },
            onError = { }
        )

        verify(exactly = 0) {
            billing.consumeAndSave(
                finishTransactions = any(),
                purchase = any(),
                shouldConsume = any(),
                initiationSource = initiationSource
            )
        }
    }

    // region offline entitlements

    @Test
    fun `postTokenWithoutConsuming resets offline consumer info cache on success`() {
        mockPostReceiptSuccess(postType = PostType.TOKEN_WITHOUT_CONSUMING)

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { fail("Expected error") },
            onError = { }
        )

        verify(exactly = 1) {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(isServerError = false)
        }
        verify(exactly = 0) { offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
            appUserId = appUserID,
            onSuccess = any(),
            onError = any()
        ) }
    }

    @Test
    fun `postTokenWithoutConsuming calculates offline entitlements customer info if server error`() {
        mockPostReceiptError(
            errorHandlingBehavior = PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME,
            postType = PostType.TOKEN_WITHOUT_CONSUMING
        )

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        var successCallCount = 0
        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { successCallCount++ },
            onError = { fail("Should succeed") }
        )

        assertThat(successCallCount).isEqualTo(1)
        verify(exactly = 1) { customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) }
        verify(exactly = 1) { customerInfoUpdateHandler.notifyListeners(any()) }
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
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { billing.consumeAndSave(
            finishTransactions = any(),
            purchase = any(),
            shouldConsume = any(),
            initiationSource = initiationSource
        ) }
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
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
                appUserId = appUserID,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured(defaultCustomerInfo)
        }
        every {
            customerInfoUpdateHandler.notifyListeners(defaultCustomerInfo)
        } just Runs

        postReceiptHelper.postTokenWithoutConsuming(
            purchaseToken = postToken,
            receiptInfo = testReceiptInfo,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { },
            onError = { fail("Should succeed") }
        )

        verify(exactly = 0) { subscriberAttributesManager.markAsSynced(any(), any(), any()) }
    }

    // endregion

    // endregion

    // region paywall data

    @Test
    fun `postReceipt posts paywall data if cached`() {
        val expectedPaywallData = event.toPaywallPostReceiptData()

        mockPostReceiptSuccess()

        paywallPresentedCache.receiveEvent(event)
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                purchasesAreCompletedBy = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = expectedPaywallData,
                onSuccess = any(),
                onError = any()
            )
        }
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
    }

    @Test
    fun `postReceipt does not post paywall data if purchase product does not match paywall event`() {
        mockPostReceiptSuccess()

        paywallPresentedCache.receiveEvent(event)

        val mockGooglePurchase2 = stubGooglePurchase(productIds = listOf("other_product"))
        val mockStoreTransaction2 = mockGooglePurchase2.toStoreTransaction(
            ProductType.SUBS,
            null,
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        every { billing.consumeAndSave(
            finishTransactions = any(),
            purchase = mockStoreTransaction2,
            shouldConsume = any(),
            initiationSource = initiationSource,
        )
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction2,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                purchasesAreCompletedBy = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = null,
                onSuccess = any(),
                onError = any()
            )
        }
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue
    }

    @Test
    fun `postReceipt does not post paywall data if purchase timestamp before purchase initiated event`() {
        mockPostReceiptSuccess()

        paywallPresentedCache.receiveEvent(event)

        val mockGooglePurchase2 = stubGooglePurchase(
            productIds = listOf("lifetime_product", "dos"),
            purchaseTime = 1.days.ago().time,
        )
        val mockStoreTransaction2 = mockGooglePurchase2.toStoreTransaction(
            ProductType.SUBS,
            null,
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        every { billing.consumeAndSave(
            finishTransactions = any(),
            purchase = mockStoreTransaction2,
            shouldConsume = any(),
            initiationSource = initiationSource,
        )
        } just Runs

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction2,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                purchasesAreCompletedBy = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = null,
                onSuccess = any(),
                onError = any()
            )
        }
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue
    }

    // endregion paywall data

    // region purchased products data

    @Test
    fun `postTransactionAndConsumeIfNeeded tries to consume products if product data indicates it should consume`() {
        mockPostReceiptSuccess(
            purchasedProductsInfo = mapOf("lifetime_product" to true)
        )

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = true,
                purchase = mockStoreTransaction,
                shouldConsume = true,
                initiationSource = initiationSource
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not try to consume products if product data indicates it should not consume`() {
        mockPostReceiptSuccess(
            purchasedProductsInfo = mapOf("lifetime_product" to false)
        )

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = true,
                purchase = mockStoreTransaction,
                shouldConsume = false,
                initiationSource = initiationSource
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded tries to consume products if product data not available`() {
        mockPostReceiptSuccess(
            purchasedProductsInfo = mapOf()
        )

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = true,
                purchase = mockStoreTransaction,
                shouldConsume = true,
                initiationSource = initiationSource
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded tries to consume products if product data product id does not match transactions`() {
        mockPostReceiptSuccess(
            purchasedProductsInfo = mapOf("other_product" to false)
        )

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = false,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            billing.consumeAndSave(
                finishTransactions = true,
                purchase = mockStoreTransaction,
                shouldConsume = true,
                initiationSource = initiationSource
            )
        }
    }

    // endregion purchased products data

    // region cached purchase data

    @Test
    fun `postTransactionAndConsumeIfNeeded clears purchaseData after successful post`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = null,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(mockStoreTransaction.purchaseToken, any())
        }
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(mockStoreTransaction.purchaseToken))
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not clear purchaseData after failed post`() {
        mockPostReceiptError(PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = null,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should error") },
            onError = { _, _ -> }
        )
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(mockStoreTransaction.purchaseToken, any())
        }
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(any())
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded caches transaction metadata before posting`() {
        val expectedPaywallData = event.toPaywallPostReceiptData()
        val transaction = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            PresentedOfferingContext("offering_id"),
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        paywallPresentedCache.receiveEvent(event)
        mockPostReceiptSuccess(storeTransaction = transaction)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = transaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )
        val expectedTransactionMetadata = LocalTransactionMetadata(
            token = transaction.purchaseToken,
            receiptInfo = ReceiptInfo.from(transaction, mockStoreProduct, emptyMap(), sdkOriginated = false),
            paywallPostReceiptData = expectedPaywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(
                mockStoreTransaction.purchaseToken,
                expectedTransactionMetadata,
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded caches transaction metadata for pending purchases`() {
        every {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(any())
        } returns false
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockPendingStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should fail") },
            onError = { _, _ -> }
        )
        val expectedTransactionMetadata = LocalTransactionMetadata(
            token = mockPendingStoreTransaction.purchaseToken,
            receiptInfo = ReceiptInfo.from(mockPendingStoreTransaction, mockStoreProduct, emptyMap(), sdkOriginated = false),
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(
                mockPendingStoreTransaction.purchaseToken,
                expectedTransactionMetadata,
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not cache if metadata already exists but clears it on success`() {
        // Mock that metadata already exists for this token
        val existingMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = ReceiptInfo.from(mockStoreTransaction, mockStoreProduct, emptyMap()),
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns existingMetadata

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 0) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(any(), any())
        }
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(mockStoreTransaction.purchaseToken))
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded clears cache on SHOULD_BE_MARKED_SYNCED error`() {
        mockPostReceiptError(PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = null,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should error") },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(mockStoreTransaction.purchaseToken, any())
        }
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(mockStoreTransaction.purchaseToken))
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded clears cache on SHOULD_BE_MARKED_SYNCED error if metadata was already cached`() {
        // Mock that metadata already exists for this token (from a previous attempt)
        val existingMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = ReceiptInfo.from(mockStoreTransaction, mockStoreProduct, emptyMap()),
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns existingMetadata

        mockPostReceiptError(PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should error") },
            onError = { _, _ -> }
        )

        // Should clear cache if metadata was already cached from a previous attempt
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(mockStoreTransaction.purchaseToken))
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not cache transaction metadata when initiationSource is RESTORE`() {
        mockPostReceiptSuccess(postReceiptInitiationSource = PostReceiptInitiationSource.RESTORE)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.RESTORE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 0) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(any(), any())
        }
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(any())
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded does not cache transaction metadata when initiationSource is UNSYNCED_ACTIVE_PURCHASES`() {
        mockPostReceiptSuccess(postReceiptInitiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 0) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(any(), any())
        }
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(any())
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded uses cached paywall data when present paywall is null`() {
        val cachedPaywallData = event.toPaywallPostReceiptData()
        val cachedMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = cachedPaywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns cachedMetadata

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = cachedPaywallData,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded uses cached data over presented when both exist and does not remove cached`() {
        val cachedPaywallData = event.toPaywallPostReceiptData()
        val cachedMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = cachedPaywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns cachedMetadata

        val presentedEvent = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), 1.minutes.ago()),
            data = PaywallEvent.Data(
                paywallIdentifier = "presented_paywall_id",
                PresentedOfferingContext("different_offering") ,
                20,
                UUID.randomUUID(),
                "header",
                "en_US",
                true,
                packageIdentifier = "test-package-id",
                productIdentifier = mockStoreTransaction.productIds.first(),
            ),
            type = PaywallEventType.PURCHASE_INITIATED,
        )
        paywallPresentedCache.receiveEvent(presentedEvent)

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        // Verify we have not removed presented cache event when using cached value
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = cachedPaywallData,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded uses cached observer mode value if available`() {
        val cachedPaywallData = event.toPaywallPostReceiptData()
        val cachedMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = cachedPaywallData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns cachedMetadata

        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded uses cached receipt info if available`() {
        val cachedReceiptInfo = ReceiptInfo(
            productIDs = listOf("cached-product"),
            presentedOfferingContext = PresentedOfferingContext("cached-offering"),
            price = 9.99,
            formattedPrice = "$9.99",
            currency = "USD",
            period = Period(1, Period.Unit.YEAR, "P1Y"),
            pricingPhases = null,
            replacementMode = GoogleReplacementMode.DEFERRED,
            platformProductIds = listOf(mapOf("product_id" to "cached-product")),
        )
        val cachedMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = cachedReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns cachedMetadata

        mockPostReceiptSuccess(postReceiptInitiationSource = PostReceiptInitiationSource.RESTORE)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.RESTORE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = cachedReceiptInfo,
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded passes PurchasesAreCompletedBy from cached metadata`() {
        val cachedMetadata = LocalTransactionMetadata(
            token = mockStoreTransaction.purchaseToken,
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
        )
        every { localTransactionMetadataStore.getLocalTransactionMetadata(mockStoreTransaction.purchaseToken) } returns cachedMetadata

        mockPostReceiptSuccess(postReceiptInitiationSource = PostReceiptInitiationSource.RESTORE)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = PostReceiptInitiationSource.RESTORE,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded uses paywall presentedOfferingContext when receiptInfo context is null`() {
        // Create a transaction without presentedOfferingContext
        val transactionWithoutContext = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            null, // No presentedOfferingContext
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        // Create a paywall event with presentedOfferingContext
        val paywallOfferingContext = PresentedOfferingContext("paywall_offering_id")
        val paywallEvent = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), 1.hours.ago()),
            data = PaywallEvent.Data(
                paywallIdentifier = "paywall_id",
                paywallOfferingContext,
                10,
                UUID.randomUUID(),
                "footer",
                "es_ES",
                false,
                packageIdentifier = "test-package-id",
                productIdentifier = mockGooglePurchase.products.first(),
            ),
            type = PaywallEventType.PURCHASE_INITIATED,
        )

        paywallPresentedCache.receiveEvent(paywallEvent)
        mockPostReceiptSuccess(storeTransaction = transactionWithoutContext)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = transactionWithoutContext,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        // Verify that the cached transaction metadata uses the paywall's presentedOfferingContext
        val capturedMetadata = slot<LocalTransactionMetadata>()
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(
                transactionWithoutContext.purchaseToken,
                capture(capturedMetadata),
            )
        }
        assertThat(capturedMetadata.captured.receiptInfo.presentedOfferingContext)
            .isEqualTo(paywallOfferingContext)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded keeps receiptInfo as-is when both contexts are null`() {
        // Create a transaction without presentedOfferingContext
        val transactionWithoutContext = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            null, // No presentedOfferingContext
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        // Don't add any paywall event to the cache (no paywall context available)
        mockPostReceiptSuccess(storeTransaction = transactionWithoutContext)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = transactionWithoutContext,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        // Verify that the cached transaction metadata has null presentedOfferingContext
        val capturedMetadata = slot<LocalTransactionMetadata>()
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(
                transactionWithoutContext.purchaseToken,
                capture(capturedMetadata),
            )
        }
        assertThat(capturedMetadata.captured.receiptInfo.presentedOfferingContext).isNull()
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded preserves receiptInfo context when it already exists`() {
        // Create a transaction with presentedOfferingContext
        val transactionContext = PresentedOfferingContext("transaction_offering_id")
        val transactionWithContext = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            transactionContext,
            subscriptionOptionId,
            replacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )

        // Create a paywall event with a different presentedOfferingContext
        val paywallOfferingContext = PresentedOfferingContext("paywall_offering_id")
        val paywallEvent = PaywallEvent(
            creationData = PaywallEvent.CreationData(UUID.randomUUID(), 1.hours.ago()),
            data = PaywallEvent.Data(
                paywallIdentifier = "paywall_id",
                paywallOfferingContext,
                10,
                UUID.randomUUID(),
                "footer",
                "es_ES",
                false,
                packageIdentifier = "test-package-id",
                productIdentifier = mockGooglePurchase.products.first(),
            ),
            type = PaywallEventType.PURCHASE_INITIATED,
        )

        paywallPresentedCache.receiveEvent(paywallEvent)
        mockPostReceiptSuccess(storeTransaction = transactionWithContext)

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = transactionWithContext,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        // Verify that the cached transaction metadata preserves the transaction's presentedOfferingContext
        // (not overwritten by paywall's context)
        val capturedMetadata = slot<LocalTransactionMetadata>()
        verify(exactly = 1) {
            localTransactionMetadataStore.cacheLocalTransactionMetadata(
                transactionWithContext.purchaseToken,
                capture(capturedMetadata),
            )
        }
        assertThat(capturedMetadata.captured.receiptInfo.presentedOfferingContext)
            .isEqualTo(transactionContext)
    }

    @Test
    fun `postTransactionAndConsumeIfNeeded passes current PurchasesAreCompletedBy when no cached metadata`() {
        mockPostReceiptSuccess()

        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> },
            onError = { _, _ -> fail("Should succeed") }
        )

        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // endregion cached purchase data

    // region pending transactions

    @Test
    fun `if pending transaction, error callback is called`() {
        var receivedError: PurchasesError? = null
        every {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(any())
        } returns false
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockPendingStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should error") },
            onError = { _, error -> receivedError = error }
        )

        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.PaymentPendingError)
    }

    @Test
    fun `if pending transaction, transaction is not posted`() {
        every {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(any())
        } returns false

        var errorCallCount = 0
        postReceiptHelper.postTransactionAndConsumeIfNeeded(
            purchase = mockPendingStoreTransaction,
            storeProduct = mockStoreProduct,
            subscriptionOptionForProductIDs = null,
            isRestore = true,
            appUserID = appUserID,
            initiationSource = initiationSource,
            onSuccess = { _, _ -> fail("Should error") },
            onError = { _, _ -> errorCallCount++ }
        )

        assertThat(errorCallCount).isEqualTo(1)
        verify(exactly = 0) {
            backend.postReceiptData(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    // endregion pending transactions

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
        purchasedProductsInfo: Map<String, Boolean> = mapOf("lifetime_product" to false),
        jsonBody: JSONObject = JSONObject(Responses.createFullCustomerResponse(productsInfo = purchasedProductsInfo)),
        postType: PostType = PostType.TRANSACTION_AND_CONSUME,
        postReceiptInitiationSource: PostReceiptInitiationSource = initiationSource,
        storeTransaction: StoreTransaction = mockStoreTransaction,
    ) {
        every {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = capture(postedReceiptInfoSlot),
                initiationSource = postReceiptInitiationSource,
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            val purchasedProductsById = purchasedProductsInfo.mapValues {
                PostReceiptProductInfo(it.value)
            }
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(customerInfo, purchasedProductsById, jsonBody)
            )
        }

        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(any()) } just Runs
        if (postType == PostType.TRANSACTION_AND_CONSUME) {
            every { billing.consumeAndSave(
                finishTransactions = any(),
                purchase = storeTransaction,
                shouldConsume = any(),
                initiationSource = postReceiptInitiationSource
            )
            } just Runs
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
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = capture(postedReceiptInfoSlot),
                initiationSource = initiationSource,
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            val callback = lambda<PostReceiptDataErrorCallback>().captured
            when (errorHandlingBehavior) {
                PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED -> callback.invokeWithFinishableError()
                PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME -> callback.invokeWithNotFinishableError()
                PostReceiptErrorHandlingBehavior.SHOULD_USE_OFFLINE_ENTITLEMENTS_AND_NOT_CONSUME -> callback.invokeWithServerError()
            }
        }

        every {
            offlineEntitlementsManager.shouldCalculateOfflineCustomerInfoInPostReceipt(any())
        } answers { firstArg() }
        every {
            offlineEntitlementsManager.calculateAndCacheOfflineCustomerInfo(
                appUserId = appUserID,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.UnknownError)
            )
        }
        if (errorHandlingBehavior == PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED) {
            every { subscriberAttributesManager.markAsSynced(appUserID, any(), any()) } just Runs
            if (postType == PostType.TRANSACTION_AND_CONSUME) {
                every { billing.consumeAndSave(
                    finishTransactions = any(),
                    purchase = mockStoreTransaction,
                    shouldConsume = false,
                    initiationSource = initiationSource
                ) } just Runs
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
            PostReceiptErrorHandlingBehavior.SHOULD_BE_MARKED_SYNCED,
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

    // region postRemainingCachedTransactionMetadata tests

    @Test
    fun `postRemainingCachedTransactionMetadata calls onNoTransactionsToSync when no cached metadata`() {
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns emptyList()

        var onNoTransactionsToSyncCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = {
                onNoTransactionsToSyncCalled = true
            },
            onError = { fail("Should not call onError") },
            onSuccess = { fail("Should not call onSuccess") }
        )

        assertThat(onNoTransactionsToSyncCalled).isTrue
    }

    @Test
    fun `postRemainingCachedTransactionMetadata posts cached metadata with paywallPostReceiptData`() {
        val paywallPostReceiptData = event.toPaywallPostReceiptData()
        val metadata = LocalTransactionMetadata(
            token = "cached-token",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = paywallPostReceiptData,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata)
        mockUnsyncedSubscriberAttributes()
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, emptyMap(), emptyList()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo) } just Runs
        every { deviceCache.addSuccessfullyPostedToken("cached-token") } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token")) } just Runs

        every {
            backend.postReceiptData(
                purchaseToken = "cached-token",
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = paywallPostReceiptData,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(defaultCustomerInfo, emptyMap(), JSONObject())
            )
        }

        var successCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { fail("Should not call onError") },
            onSuccess = { customerInfo ->
                assertThat(customerInfo).isEqualTo(defaultCustomerInfo)
                successCalled = true
            }
        )

        assertThat(successCalled).isTrue
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token"))
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata posts cached metadata with MY_APP purchasesAreCompletedBy`() {
        val metadata = LocalTransactionMetadata(
            token = "cached-token",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata)
        mockUnsyncedSubscriberAttributes()
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, emptyMap(), emptyList()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo) } just Runs
        every { deviceCache.addSuccessfullyPostedToken("cached-token") } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token")) } just Runs

        every {
            backend.postReceiptData(
                purchaseToken = "cached-token",
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.MY_APP,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(defaultCustomerInfo, emptyMap(), JSONObject())
            )
        }

        var successCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { fail("Should not call onError") },
            onSuccess = { customerInfo ->
                assertThat(customerInfo).isEqualTo(defaultCustomerInfo)
                successCalled = true
            }
        )

        assertThat(successCalled).isTrue
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token"))
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata clears cache only on success`() {
        val metadata = LocalTransactionMetadata(
            token = "cached-token",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata)
        mockUnsyncedSubscriberAttributes()
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, emptyMap(), emptyList()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo) } just Runs
        every { deviceCache.addSuccessfullyPostedToken("cached-token") } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token")) } just Runs

        every {
            backend.postReceiptData(
                purchaseToken = "cached-token",
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(defaultCustomerInfo, emptyMap(), JSONObject())
            )
        }

        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { fail("Should not call onError") },
            onSuccess = { }
        )

        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token"))
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata does not clear cache on error`() {
        val metadata = LocalTransactionMetadata(
            token = "cached-token",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata)
        mockUnsyncedSubscriberAttributes()

        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        every {
            backend.postReceiptData(
                purchaseToken = "cached-token",
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<PostReceiptDataErrorCallback>().captured.invoke(
                error,
                PostReceiptErrorHandlingBehavior.SHOULD_NOT_CONSUME,
                JSONObject()
            )
        }

        var errorCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { receivedError ->
                assertThat(receivedError).isEqualTo(error)
                errorCalled = true
            },
            onSuccess = { fail("Should not call onSuccess") }
        )

        assertThat(errorCalled).isTrue
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(any())
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata posts all cached metadata`() {
        val metadata1 = LocalTransactionMetadata(
            token = "cached-token-1",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        val metadata2 = LocalTransactionMetadata(
            token = "cached-token-2",
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata1, metadata2)
        mockUnsyncedSubscriberAttributes()
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, emptyMap(), emptyList()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo) } just Runs
        every { deviceCache.addSuccessfullyPostedToken(any()) } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(any()) } just Runs

        // Mock backend to respond successfully for both tokens
        every {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(defaultCustomerInfo, emptyMap(), JSONObject())
            )
        }

        var successCount = 0
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = emptySet(),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { fail("Should not call onError") },
            onSuccess = {
                successCount++
            }
        )

        assertThat(successCount).isEqualTo(1)
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token-1"))
        }
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf("cached-token-2"))
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata filters out pending transaction tokens`() {
        val pendingToken = "pending-token"
        val metadata = LocalTransactionMetadata(
            token = pendingToken,
            receiptInfo = testReceiptInfo,
            paywallPostReceiptData = null,
            purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
        )
        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns listOf(metadata)

        var onNoTransactionsToSyncCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = setOf(pendingToken),
            onNoTransactionsToSync = {
                onNoTransactionsToSyncCalled = true
            },
            onError = { fail("Should not call onError") },
            onSuccess = { fail("Should not call onSuccess") }
        )

        assertThat(onNoTransactionsToSyncCalled).isTrue
        verify(exactly = 0) {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `postRemainingCachedTransactionMetadata handles multiple pending and non-pending transactions`() {
        val pendingToken1 = "pending-token-1"
        val pendingToken2 = "pending-token-2"
        val nonPendingToken1 = "non-pending-token-1"
        val nonPendingToken2 = "non-pending-token-2"

        val metadata = listOf(
            LocalTransactionMetadata(
                token = pendingToken1,
                receiptInfo = testReceiptInfo,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
            ),
            LocalTransactionMetadata(
                token = nonPendingToken1,
                receiptInfo = testReceiptInfo,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
            ),
            LocalTransactionMetadata(
                token = pendingToken2,
                receiptInfo = testReceiptInfo,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
            ),
            LocalTransactionMetadata(
                token = nonPendingToken2,
                receiptInfo = testReceiptInfo,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT
            )
        )

        every { localTransactionMetadataStore.getAllLocalTransactionMetadata() } returns metadata
        mockUnsyncedSubscriberAttributes()
        every { offlineEntitlementsManager.resetOfflineCustomerInfoCache() } just Runs
        every { subscriberAttributesManager.markAsSynced(appUserID, emptyMap(), emptyList()) } just Runs
        every { customerInfoUpdateHandler.cacheAndNotifyListeners(defaultCustomerInfo) } just Runs
        every { deviceCache.addSuccessfullyPostedToken(any()) } just Runs
        every { localTransactionMetadataStore.clearLocalTransactionMetadata(any()) } just Runs

        every {
            backend.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserID,
                isRestore = true,
                finishTransactions = defaultFinishTransactions,
                subscriberAttributes = emptyMap(),
                receiptInfo = testReceiptInfo,
                initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
                paywallPostReceiptData = null,
                purchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                PostReceiptResponse(defaultCustomerInfo, emptyMap(), JSONObject())
            )
        }

        var successCalled = false
        postReceiptHelper.postRemainingCachedTransactionMetadata(
            appUserID = appUserID,
            allowSharingPlayStoreAccount = true,
            pendingTransactionsTokens = setOf(pendingToken1, pendingToken2),
            onNoTransactionsToSync = { fail("Should not call onNoTransactionsToSync") },
            onError = { fail("Should not call onError") },
            onSuccess = {
                successCalled = true
            }
        )

        assertThat(successCalled).isTrue

        // Verify only non-pending tokens were posted
        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = nonPendingToken1,
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = any()
            )
        }
        verify(exactly = 1) {
            backend.postReceiptData(
                purchaseToken = nonPendingToken2,
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = any()
            )
        }

        // Verify pending tokens were not posted
        verify(exactly = 0) {
            backend.postReceiptData(
                purchaseToken = pendingToken1,
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = any()
            )
        }
        verify(exactly = 0) {
            backend.postReceiptData(
                purchaseToken = pendingToken2,
                appUserID = any(),
                isRestore = any(),
                finishTransactions = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                initiationSource = any(),
                paywallPostReceiptData = any(),
                purchasesAreCompletedBy = any(),
                onSuccess = any(),
                onError = any()
            )
        }

        // Verify only non-pending tokens were cleared
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(nonPendingToken1))
        }
        verify(exactly = 1) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(nonPendingToken2))
        }
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(pendingToken1))
        }
        verify(exactly = 0) {
            localTransactionMetadataStore.clearLocalTransactionMetadata(setOf(pendingToken2))
        }
    }

    // endregion
}
