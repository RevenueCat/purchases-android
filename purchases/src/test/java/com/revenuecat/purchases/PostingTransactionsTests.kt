package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.buildCustomerInfo
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubGooglePurchase
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
    private var postReceiptError: PostReceiptErrorContainer? = null
    private var postReceiptSuccess: PostReceiptCompletionContainer? = null
    private var subscriberAttribute = SubscriberAttribute("key", "value")
    private var expectedAttributes = mapOf(
        subscriberAttribute.key.backendKey to subscriberAttribute
    )

    private val attributesToMarkAsSyncSlot = slot<Map<String, SubscriberAttribute>>()
    private val attributesErrorsSlot = slot<List<SubscriberAttributeError>>()
    private val postedProductInfoSlot = slot<ReceiptInfo>()

    internal data class PostReceiptErrorContainer(
        val error: PurchasesError,
        val shouldConsumePurchase: Boolean,
        val body: JSONObject?
    )

    internal data class PostReceiptCompletionContainer(
        val info: CustomerInfo = JSONObject(Responses.validFullPurchaserResponse).buildCustomerInfo(),
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
                receiptInfo = capture(postedProductInfoSlot),
                storeAppUserID = any(),
                marketplace = any(),
                onSuccess = capture(successSlot),
                onError = capture(errorSlot),
                storeProduct = storeProduct
            )
        } answers {
            postReceiptError?.let {
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.body)
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
            customerInfoHelper = customerInfoHelperMock
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

        val expectedSubscriptionPeriod = "P1M"
        val expectedIntroPricePeriod = "P2M"
        val expectedFreeTrialPeriod = "P3M"
        val mockStoreProduct = mockk<StoreProduct>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns expectedSubscriptionPeriod
            every { it.introductoryPricePeriod } returns expectedIntroPricePeriod
            every { it.freeTrialPeriod } returns expectedFreeTrialPeriod
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.duration).isEqualTo(expectedSubscriptionPeriod)
        assertThat(postedProductInfoSlot.captured.introDuration).isEqualTo(expectedIntroPricePeriod)
        assertThat(postedProductInfoSlot.captured.trialDuration).isEqualTo(expectedFreeTrialPeriod)
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
                onError = any(),
                storeProduct = storeProduct
            )
        }
    }

    @Test
    fun `inapps send null durations when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val mockStoreProduct = mockk<StoreProduct>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.duration).isNull()
        assertThat(postedProductInfoSlot.captured.introDuration).isNull()
        assertThat(postedProductInfoSlot.captured.trialDuration).isNull()
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
                onError = any(),
                storeProduct = storeProduct
            )
        }
    }

    @Test
    fun `store user id is sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val mockStoreProduct = mockk<StoreProduct>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }
        val purchase: StoreTransaction = mockk(relaxed = true)
        val expectedStoreUserID = "a_store_user_id"
        every {
            purchase.storeUserID
        } returns expectedStoreUserID

        underTest.postToBackend(
            purchase = purchase,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.duration).isNull()
        assertThat(postedProductInfoSlot.captured.introDuration).isNull()
        assertThat(postedProductInfoSlot.captured.trialDuration).isNull()
        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = expectedStoreUserID,
                marketplace = any(),
                onSuccess = any(),
                onError = any(),
                storeProduct = storeProduct
            )
        }
    }

    @Test
    fun `product_ids are sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()
        val productIds = listOf("uno", "dos")
        val purchase =
            stubGooglePurchase(productIds = productIds).toStoreTransaction(ProductType.SUBS, null)
        val mockStoreProduct = mockk<StoreProduct>().also {
            every { it.sku } returns "uno"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }

        underTest.postToBackend(
            purchase = purchase,
            storeProduct = mockStoreProduct,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = { _, _ -> },
            onError = { _, _ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.productIDs).isEqualTo(productIds)
        verify(exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes.toBackendMap(),
                receiptInfo = any(),
                storeAppUserID = any(),
                onSuccess = any(),
                onError = any(),
                storeProduct = storeProduct
            )
        }
    }

    @Test
    fun `customer info cache is updated when purchasing`() {
        postReceiptSuccess = PostReceiptCompletionContainer()
        val productIds = listOf("uno", "dos")
        val purchase =
            stubGooglePurchase(productIds = productIds).toStoreTransaction(ProductType.SUBS, null)
        val mockStoreProduct = mockk<StoreProduct>().also {
            every { it.sku } returns "uno"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }

        underTest.postToBackend(
            purchase = purchase,
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
