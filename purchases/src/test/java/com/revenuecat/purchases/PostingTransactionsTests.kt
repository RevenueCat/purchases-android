package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostingTransactionsTests {

    private lateinit var underTest: Purchases
    private val appUserId = "juan"
    private val subscriberAttributesManagerMock = mockk<SubscriberAttributesManager>()
    private val backendMock = mockk<Backend>(relaxed = true)
    private val billingWrapperMock = mockk<BillingWrapper>(relaxed = true)
    private val customerInfoHelperMock = mockk<CustomerInfoHelper>()
    private val offlineEntitlementsManagerMock = mockk<OfflineEntitlementsManager>()
    private var postReceiptError: PostReceiptErrorContainer? = null
    private var postReceiptSuccess: PostReceiptCompletionContainer? = null
    private var subscriberAttribute = SubscriberAttribute("key", "value")
    private var expectedAttributes = mapOf(
        subscriberAttribute.key.backendKey to subscriberAttribute
    )

    private val attributesToMarkAsSyncSlot = slot<Map<String, SubscriberAttribute>>()
    private val attributesErrorsSlot = slot<List<SubscriberAttributeError>>()
    private val postedReceiptInfoSlot = slot<ReceiptInfo>()

    private val subscriptionOptionId = "subscriptionOptionId"
    private val mockStoreProduct = stubStoreProduct("productId")
    private val mockGooglePurchase = stubGooglePurchase(
        productIds = listOf("uno", "dos")
    )
    private val mockStoreTransaction = mockGooglePurchase.toStoreTransaction(
        ProductType.SUBS,
        null,
        subscriptionOptionId
    )

    internal data class PostReceiptErrorContainer(
        val error: PurchasesError,
        val shouldConsumePurchase: Boolean,
        val isServerError: Boolean,
        val body: JSONObject?
    )

    internal data class PostReceiptCompletionContainer(
        val info: CustomerInfo = CustomerInfoFactory.buildCustomerInfo(
            JSONObject(Responses.validFullPurchaserResponse),
            null,
            VerificationResult.NOT_REQUESTED
        ),
        val body: JSONObject = JSONObject(Responses.validFullPurchaserResponse)
    )

    @Before
    fun setup() {
        every {
            billingWrapperMock.queryAllPurchases(appUserId, captureLambda(), any())
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }
        val successSlot = slot<PostReceiptDataSuccessCallback>()
        val errorSlot = slot<PostReceiptDataErrorCallback>()
        every {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = any(),
                receiptInfo = capture(postedReceiptInfoSlot),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = capture(successSlot),
                onError = capture(errorSlot)
            )
        } answers {
            postReceiptError?.let {
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.isServerError, it.body)
            } ?: postReceiptSuccess?.let {
                successSlot.captured(it.info, it.body)
            }
        }

        every {
            subscriberAttributesManagerMock.getUnsyncedSubscriberAttributes(appUserId, captureLambda())
        } answers {
            lambda<(Map<String, SubscriberAttribute>) -> Unit>().captured.also {
                it.invoke(expectedAttributes)
            }
        }

        every {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                capture(attributesToMarkAsSyncSlot),
                capture(attributesErrorsSlot)
            )
        } just runs

        every {
            customerInfoHelperMock.cacheCustomerInfo(any())
        } just runs
        every {
            customerInfoHelperMock.sendUpdatedCustomerInfoToDelegateIfChanged(any())
        } just runs
        every {
            offlineEntitlementsManagerMock.updateProductEntitlementMappingCacheIfStale()
        } just runs

        underTest = Purchases(
            application = mockk(relaxed = true),
            backingFieldAppUserID = appUserId,
            backend = backendMock,
            billing = billingWrapperMock,
            deviceCache = mockk(relaxed = true),
            dispatcher = SyncDispatcher(),
            identityManager = mockk<com.revenuecat.purchases.identity.IdentityManager>(relaxed = true).apply {
                every { currentAppUserID } returns appUserId
            },
            subscriberAttributesManager = subscriberAttributesManagerMock,
            appConfig = AppConfig(
                context = mockk(relaxed = true),
                observerMode = false,
                platformInfo = PlatformInfo(
                    flavor = "native",
                    version = "3.2.0"
                ),
                proxyURL = null,
                store = Store.PLAY_STORE
            ),
            customerInfoHelper = customerInfoHelperMock,
            offeringParser = OfferingParserFactory.createOfferingParser(Store.PLAY_STORE),
            diagnosticsSynchronizer = null,
            offlineEntitlementsManager = offlineEntitlementsManagerMock
        )
    }

    @After
    fun tearDown() {
        postReceiptError = null
        postReceiptSuccess = null
        clearMocks(customerInfoHelperMock)
    }

    @Test
    fun `durations are sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.duration).isEqualTo(mockStoreProduct.period?.iso8601)
    }

    @Test
    fun `subscriptionOptionId is sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.subscriptionOptionId).isEqualTo(subscriptionOptionId)
    }

    @Test
    fun `inapps send null durations when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val mockInAppProduct = stubINAPPStoreProduct("productId")
        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockInAppProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.duration).isNull()
        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `productIds are sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.productIDs).isEqualTo(mockStoreTransaction.productIds)
    }

    @Test
    fun `presentedOfferingIdentifier is sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val expectedPresentedOfferingIdentifier = "offering_a"
        val purchase = mockGooglePurchase.toStoreTransaction(
            ProductType.SUBS,
            expectedPresentedOfferingIdentifier
        )

        underTest.postToBackend(
            purchase = purchase,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.offeringIdentifier).isEqualTo(expectedPresentedOfferingIdentifier)
    }

    @Test
    fun `storeProduct is sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedReceiptInfoSlot.isCaptured).isTrue
        assertThat(postedReceiptInfoSlot.captured.storeProduct).isEqualTo(mockStoreProduct)
    }

    @Test
    fun `allowSharingPlayStoreAccount is sent as isRestore when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val expectedAllowSharingPlayStoreAccount = true
        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = expectedAllowSharingPlayStoreAccount,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = expectedAllowSharingPlayStoreAccount,
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `observerMode parameter is sent as false when transactions are consumed`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val expectedConsumeAllTransactions = true
        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = expectedConsumeAllTransactions,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = !expectedConsumeAllTransactions,
                subscriberAttributes = expectedAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `customer info cache is updated when purchasing`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        underTest.postToBackend(
            purchase = mockStoreTransaction,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )

        verify(exactly = 1) {
            customerInfoHelperMock.cacheCustomerInfo(any())
        }
        verify(exactly = 1) {
            customerInfoHelperMock.sendUpdatedCustomerInfoToDelegateIfChanged(any())
        }
    }

}
