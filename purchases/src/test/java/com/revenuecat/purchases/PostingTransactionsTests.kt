package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.attributes.SubscriberAttribute
import com.revenuecat.purchases.attributes.SubscriberAttributesManager
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
import java.util.concurrent.ExecutorService

@RunWith(AndroidJUnit4::class)
class PostingTransactionsTests {

    private lateinit var underTest: Purchases
    private val appUserId = "juan"
    private val subscriberAttributesManagerMock = mockk<SubscriberAttributesManager>()
    private val backendMock = mockk<Backend>(relaxed = true)
    private val billingWrapperMock = mockk<BillingWrapper>(relaxed = true)
    private var postReceiptError: PostReceiptErrorContainer? = null
    private var postReceiptSuccess: PostReceiptSuccessContainer? = null
    private var subscriberAttribute = SubscriberAttribute("key", "value")
    private var expectedAttributes = mapOf(
        subscriberAttribute.key.backendKey to subscriberAttribute
    )

    private val attributesToMarkAsSyncSlot = slot<Map<String, SubscriberAttribute>>()
    private val attributesErrorsSlot = slot<List<SubscriberAttributeError>>()
    private val postedProductInfoSlot = slot<ProductInfo>()

    internal data class PostReceiptErrorContainer(
        val error: PurchasesError,
        val shouldConsumePurchase: Boolean,
        val subscriberAttributesErrors: List<SubscriberAttributeError>
    )

    internal data class PostReceiptSuccessContainer(
        val info: PurchaserInfo = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo(),
        val subscriberAttributeErrors: List<SubscriberAttributeError> = emptyList()
    )

    @Before
    fun setup() {
        postReceiptError = null
        postReceiptSuccess = null

        every {
            billingWrapperMock.queryAllPurchases(captureLambda(), any())
        } answers {
            lambda<(List<PurchaseHistoryRecordWrapper>) -> Unit>().captured.also {
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
                productInfo = capture(postedProductInfoSlot),
                onSuccess = capture(successSlot),
                onError = capture(errorSlot)
            )
        } answers {
            postReceiptError?.let {
                errorSlot.captured(it.error, it.shouldConsumePurchase, it.subscriberAttributesErrors)
            } ?: postReceiptSuccess?.let {
                successSlot.captured(it.info, it.subscriberAttributeErrors)
            }
        }

        every {
            subscriberAttributesManagerMock.getUnsyncedSubscriberAttributes(appUserId)
        } answers {
            expectedAttributes
        }
        every {
            subscriberAttributesManagerMock.markAsSynced(appUserId, capture(attributesToMarkAsSyncSlot), capture(attributesErrorsSlot))
        } just runs

        underTest = Purchases(
            application = mockk(relaxed = true),
            backingFieldAppUserID = appUserId,
            backend = backendMock,
            billingWrapper = billingWrapperMock,
            deviceCache = mockk(relaxed = true),
            executorService = mockk<ExecutorService>().apply {
                val capturedRunnable = slot<Runnable>()
                every { execute(capture(capturedRunnable)) } answers { capturedRunnable.captured.run() }
            },
            identityManager = mockk<IdentityManager>(relaxed = true).apply {
                every { currentAppUserID } returns appUserId
            },
            subscriberAttributesManager = subscriberAttributesManagerMock,
            appConfig = AppConfig("en-US", "1.0", PlatformInfo("native", "3.2.0"), true)
        )
    }

    @Test
    fun `durations are sent when posting to backend`() {
        postReceiptSuccess = PostReceiptSuccessContainer()

        val mockSkuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns "P1M"
            every { it.introductoryPricePeriod } returns "P2M"
            every { it.freeTrialPeriod } returns "P3M"
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockSkuDetails,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.duration).isEqualTo("P1M")
        assertThat(postedProductInfoSlot.captured.introDuration).isEqualTo("P2M")
        assertThat(postedProductInfoSlot.captured.trialDuration).isEqualTo("P3M")
        verify (exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes,
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `inapps send null durations when posting to backend`() {
        postReceiptSuccess = PostReceiptSuccessContainer()

        val mockSkuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_id"
            every { it.priceAmountMicros } returns 2000000
            every { it.priceCurrencyCode } returns "USD"
            every { it.subscriptionPeriod } returns ""
            every { it.introductoryPricePeriod } returns ""
            every { it.freeTrialPeriod } returns ""
        }
        underTest.postToBackend(
            purchase = mockk(relaxed = true),
            skuDetails = mockSkuDetails,
            allowSharingPlayStoreAccount = true,
            consumeAllTransactions = true,
            appUserID = appUserId,
            onSuccess = {_,_ -> },
            onError = {_,_ -> }
        )
        assertThat(postedProductInfoSlot.isCaptured).isTrue()
        assertThat(postedProductInfoSlot.captured.duration).isNull()
        assertThat(postedProductInfoSlot.captured.introDuration).isNull()
        assertThat(postedProductInfoSlot.captured.trialDuration).isNull()
        verify (exactly = 1) {
            backendMock.postReceiptData(
                purchaseToken = any(),
                appUserID = appUserId,
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = expectedAttributes,
                productInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // endregion

}