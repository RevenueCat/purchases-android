package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.buildPurchaserInfo
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
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
        val info: PurchaserInfo = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo(),
        val body: JSONObject = JSONObject(Responses.validFullPurchaserResponse)
    )

    @Before
    fun setup() {
        postReceiptError = null
        postReceiptSuccess = null

        every {
            billingWrapperMock.queryAllPurchases(appUserId, captureLambda(), any())
        } answers {
            lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
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
                onSuccess = capture(successSlot),
                onError = capture(errorSlot)
            )
        } answers {
            postReceiptError?.let {
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.body)
            } ?: postReceiptSuccess?.let {
                successSlot.captured(it.info, it.body)
            }
        }

        every {
            subscriberAttributesManagerMock.getUnsyncedSubscriberAttributes(appUserId)
        } answers {
            expectedAttributes
        }
        every {
            subscriberAttributesManagerMock.markAsSynced(
                appUserId,
                capture(attributesToMarkAsSyncSlot),
                capture(attributesErrorsSlot)
            )
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
            )
        )
    }

    @Test
    fun `durations are sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val expectedSubscriptionPeriod = "P1M"
        val expectedIntroPricePeriod = "P2M"
        val expectedFreeTrialPeriod = "P3M"
        val mockProductDetails = mockk<ProductDetails>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns expectedSubscriptionPeriod
            every { it.introductoryPricePeriod } returns expectedIntroPricePeriod
            every { it.freeTrialPeriod } returns expectedFreeTrialPeriod
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            productDetails = mockProductDetails,
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
                storeAppUserID = any(),
                receiptInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `inapps send null durations when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val mockProductDetails = mockk<ProductDetails>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            productDetails = mockProductDetails,
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
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `store user id is sent when posting to backend`() {
        postReceiptSuccess = PostReceiptCompletionContainer()

        val mockProductDetails = mockk<ProductDetails>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }
        val purchase: PurchaseDetails = mockk(relaxed = true)
        val expectedStoreUserID = "a_store_user_id"
        every {
            purchase.storeUserID
        } returns expectedStoreUserID

        underTest.postToBackend(
            purchase = purchase,
            productDetails = mockProductDetails,
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
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // endregion
}
