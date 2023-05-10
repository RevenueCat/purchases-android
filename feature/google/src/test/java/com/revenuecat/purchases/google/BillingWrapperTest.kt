package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.utils.createMockProductDetailsNoOffers
import com.revenuecat.purchases.utils.mockOneTimePurchaseOfferDetails
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockQueryPurchaseHistory
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.verifyQueryPurchaseHistoryCalledWithType
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.Thread.sleep
import java.util.Date
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperTest {

    private companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
        const val timestamp500 = 1676379370500 // Tuesday, February 14, 2023 12:56:10.500 PM GMT
        const val timestamp900 = 1676379370900 // Tuesday, February 14, 2023 12:56:10.900 PM GMT
    }

    private var onConnectedCalled: Boolean = false
    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var handler: Handler = mockk()
    private var mockDeviceCache: DeviceCache = mockk()
    private var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    private var mockDateProvider: DateProvider = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<ProductDetails>

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private val billingClientErrorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
    private val appUserId = "jerry"
    private var mockActivity = mockk<Activity>()

    private val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    private val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!

    @Before
    fun setup() {
        clearAllMocks()
        storeProducts = null
        purchasesUpdatedListener = null
        billingClientStateListener = null

        mockRunnables()
        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        val listenerSlot = slot<PurchasesUpdatedListener>()
        every {
            mockClientFactory.buildClient(capture(listenerSlot))
        } answers {
            purchasesUpdatedListener = listenerSlot.captured
            mockClient
        }

        val billingClientStateListenerSlot = slot<BillingClientStateListener>()
        every {
            mockClient.startConnection(capture(billingClientStateListenerSlot))
        } answers {
            billingClientStateListener = billingClientStateListenerSlot.captured
        }

        every {
            mockClient.endConnection()
        } just runs

        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs

        mockConsumeAsync(billingClientOKResult)

        every {
            mockClient.isReady
        } returns false andThen true

        mockDetailsList = listOf(mockProductDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache, mockDiagnosticsTracker, mockDateProvider)
        wrapper.purchasesUpdatedListener = mockPurchasesListener
        onConnectedCalled = false
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }

        every {
            mockActivity.intent
        } returns Intent()
    }

    @Test
    fun canBeCreated() {
        assertThat(wrapper).`as`("Wrapper is not null").isNotNull
    }

    @Test
    fun callsBuildOnTheFactory() {
        verify {
            mockClientFactory.buildClient(purchasesUpdatedListener!!)
        }
    }

    @Test
    fun connectsToPlayBilling() {
        verify {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    @Test
    fun defersCallUntilConnected() {
        every { mockClient.isReady } returns false

        val token = "token"
        var consumePurchaseCompleted = false
        wrapper.consumePurchase(token) { _, _ ->
            consumePurchaseCompleted = true
        }

        assertThat(consumePurchaseCompleted).isFalse

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        assertThat(consumePurchaseCompleted).isTrue
    }

    @Test
    fun canDeferMultipleCallsUntilConnected() {
        every { mockClient.isReady } returns false

        val token = "token"

        var consumePurchaseResponse1Called = false
        wrapper.consumePurchase(token) { _, _ ->
            consumePurchaseResponse1Called = true
        }

        var consumePurchaseResponse2Called = false
        wrapper.consumePurchase(token) { _, _ ->
            consumePurchaseResponse2Called = true
        }
        assertThat(consumePurchaseResponse1Called).isFalse
        assertThat(consumePurchaseResponse2Called).isFalse

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        assertThat(consumePurchaseResponse1Called).isTrue
        assertThat(consumePurchaseResponse2Called).isTrue
    }

    @Test
    fun makingARequestTriggersAConnectionAttempt() {
        every { mockClient.isReady } returns false

        val token = "token"
        wrapper.consumePurchase(token) { _, _ -> }

        verify(exactly = 2) {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync fails if sent invalid type`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )
        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            "notValid",
            {
                fail("call should not succeed")
            },
            {
                errorCalled = true
            }
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            {
                numCallbacks++
            }, {
                fail("shouldn't be an error")
            })

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        val lock = CountDownLatch(2)
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                handler.post {
                    numCallbacks++
                }
            }, {
                fail("shouldn't be an error")
            })

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `can make a purchase`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val storeProduct = createStoreProductWithoutOffers()

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            mockReplaceSkuInfo(),
            "offering_a"
        )

        verify {
            mockClient.launchBillingFlow(
                eq(mockActivity),
                any()
            )
        }
    }

    @Test
    fun `properly sets billingFlowParams for subscription purchase`() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.SubscriptionUpdateParams::class)

        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        val productDetailsParamsSlot = slot<List<ProductDetailsParams>>()
        every {
            mockBuilder.setProductDetailsParamsList(capture(productDetailsParamsSlot))
        } returns mockBuilder

        every {
            mockBuilder.setIsOfferPersonalized(any())
        } returns mockBuilder

        val mockSubscriptionUpdateParamsBuilder =
            mockk<BillingFlowParams.SubscriptionUpdateParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
        } returns mockSubscriptionUpdateParamsBuilder

        val oldPurchaseTokenSlot = slot<String>()
        every {
            mockSubscriptionUpdateParamsBuilder.setOldPurchaseToken(capture(oldPurchaseTokenSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val prorationModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setSubscriptionReplacementMode(capture(prorationModeSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val isPersonalizedPriceSlot = slot<Boolean>()
        every {
            mockBuilder.setIsOfferPersonalized(capture(isPersonalizedPriceSlot))
        } returns mockBuilder

        val productId = "product_a"

        val upgradeInfo = mockReplaceSkuInfo()
        val productDetails = mockProductDetails(productId = productId, type = subsGoogleProductType)
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!
        val isPersonalizedPrice = true

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(mockActivity), capture(slot))
        } answers {
            val capturedProductDetailsParams = productDetailsParamsSlot.captured

            assertThat(1).isEqualTo(capturedProductDetailsParams.size)
            assertThat(productId).isEqualTo(capturedProductDetailsParams[0].zza().productId)
            assertThat(subsGoogleProductType).isEqualTo(capturedProductDetailsParams[0].zza().productType)

            assertThat(upgradeInfo.oldPurchase.purchaseToken).isEqualTo(oldPurchaseTokenSlot.captured)
            assertThat((upgradeInfo.replacementMode as GoogleReplacementMode?)?.playBillingClientMode).isEqualTo(prorationModeSlot.captured)

            assertThat(isPersonalizedPrice).isEqualTo(isPersonalizedPriceSlot.captured)
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            upgradeInfo,
            null,
            isPersonalizedPrice
        )
    }

    @Test
    fun `skips setting on BillingFlowPrams when prorationmode or personalized price null for subscription purchase`() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.SubscriptionUpdateParams::class)

        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        val productDetailsParamsSlot = slot<List<ProductDetailsParams>>()
        every {
            mockBuilder.setProductDetailsParamsList(capture(productDetailsParamsSlot))
        } returns mockBuilder

        every {
            mockBuilder.setIsOfferPersonalized(any())
        } returns mockBuilder

        val mockSubscriptionUpdateParamsBuilder =
            mockk<BillingFlowParams.SubscriptionUpdateParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
        } returns mockSubscriptionUpdateParamsBuilder

        val oldPurchaseTokenSlot = slot<String>()
        every {
            mockSubscriptionUpdateParamsBuilder.setOldPurchaseToken(capture(oldPurchaseTokenSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val prorationModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setReplaceProrationMode(capture(prorationModeSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val isPersonalizedPriceSlot = slot<Boolean>()
        every {
            mockBuilder.setIsOfferPersonalized(capture(isPersonalizedPriceSlot))
        } returns mockBuilder

        val productId = "product_a"

        val replaceProductInfo = ReplaceProductInfo(mockPurchaseHistoryRecordWrapper())
        val productDetails = mockProductDetails(productId = productId, type = subsGoogleProductType)
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!
        val isPersonalizedPrice = null

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(mockActivity), capture(slot))
        } answers {
            val capturedProductDetailsParams = productDetailsParamsSlot.captured

            assertThat(1).isEqualTo(capturedProductDetailsParams.size)
            assertThat(productId).isEqualTo(capturedProductDetailsParams[0].zza().productId)
            assertThat(subsGoogleProductType).isEqualTo(capturedProductDetailsParams[0].zza().productType)

            assertThat(replaceProductInfo.oldPurchase.purchaseToken).isEqualTo(oldPurchaseTokenSlot.captured)

            verify(exactly = 0) {
                mockBuilder.setIsOfferPersonalized(any())
                !isPersonalizedPriceSlot.isCaptured
            }

            verify(exactly = 0) {
                mockSubscriptionUpdateParamsBuilder.setReplaceProrationMode(any())
                !prorationModeSlot.isCaptured
            }
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            replaceProductInfo,
            null,
            isPersonalizedPrice
        )
    }

    @Test
    fun `properly sets billingFlowParams for inapp purchase`() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.ProductDetailsParams::class)

        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        val productDetailsParamsSlot = slot<List<ProductDetailsParams>>()
        every {
            mockBuilder.setProductDetailsParamsList(capture(productDetailsParamsSlot))
        } returns mockBuilder

        every {
            mockBuilder.setObfuscatedAccountId(any())
        } returns mockBuilder

        val isPersonalizedPriceSlot = slot<Boolean>()
        every {
            mockBuilder.setIsOfferPersonalized(capture(isPersonalizedPriceSlot))
        } returns mockBuilder

        val productId = "product_a"

        val oneTimePurchaseOfferDetails = mockOneTimePurchaseOfferDetails()
        val productDetails = mockProductDetails(
            productId = productId,
            type = inAppGoogleProductType,
            oneTimePurchaseOfferDetails = oneTimePurchaseOfferDetails,
            subscriptionOfferDetails = null
        )
        every {
            oneTimePurchaseOfferDetails.zza()
        } returns productId
        
        val storeProduct = productDetails.toInAppStoreProduct()!!
        val isPersonalizedPrice = true

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(mockActivity), capture(slot))
        } answers {
            assertThat(isPersonalizedPrice).isEqualTo(isPersonalizedPriceSlot.captured)
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.purchasingData,
            null,
            null,
            isPersonalizedPrice
        )
    }

    @Test
    fun `properly sets ProductDetailsParams for subscription product`() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.ProductDetailsParams::class)
        mockkStatic(BillingFlowParams.SubscriptionUpdateParams::class)

        val mockProductDetailsBuilder = mockk<ProductDetailsParams.Builder>(relaxed = true)
        every {
            ProductDetailsParams.newBuilder()
        } returns mockProductDetailsBuilder

        val tokenSlot = slot<String>()
        every {
            mockProductDetailsBuilder.setOfferToken(capture(tokenSlot))
        } returns mockProductDetailsBuilder

        val productDetailsSlot = slot<ProductDetails>()
        every {
            mockProductDetailsBuilder.setProductDetails(capture(productDetailsSlot))
        } returns mockProductDetailsBuilder

        val productId = "product_a"

        val upgradeInfo = mockReplaceSkuInfo()
        val productDetails = mockProductDetails(productId = productId, type = subsGoogleProductType)
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!,
        )!!

        every {
            mockClient.launchBillingFlow(eq(mockActivity), any())
        } answers {
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            upgradeInfo,
            null
        )

        assertThat(tokenSlot.isCaptured).isTrue
        assertThat(tokenSlot.captured).isEqualTo("mock-subscription-offer-token")
        assertThat(productDetailsSlot.isCaptured).isTrue
        assertThat(productDetailsSlot.captured).isEqualTo(productDetails)
    }

    @Test
    fun `properly sets ProductDetailsParams for inapp product`() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.ProductDetailsParams::class)

        val mockProductDetailsBuilder = mockk<ProductDetailsParams.Builder>(relaxed = true)
        every {
            ProductDetailsParams.newBuilder()
        } returns mockProductDetailsBuilder

        val productDetailsSlot = slot<ProductDetails>()
        every {
            mockProductDetailsBuilder.setProductDetails(capture(productDetailsSlot))
        } returns mockProductDetailsBuilder

        val productId = "product_a"

        val productDetails = mockProductDetails(
            productId = productId,
            type = inAppGoogleProductType,
            oneTimePurchaseOfferDetails = mockOneTimePurchaseOfferDetails(),
            subscriptionOfferDetails = null
        )
        val storeProduct = productDetails.toInAppStoreProduct()!!

        every {
            mockClient.launchBillingFlow(eq(mockActivity), any())
        } answers {
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.purchasingData,
            null,
            null
        )

        verify(exactly = 0) {
            mockProductDetailsBuilder.setOfferToken(any())
        }

        assertThat(productDetailsSlot.isCaptured).isTrue
        assertThat(productDetailsSlot.captured).isEqualTo(productDetails)
    }

    @Test
    fun `obfuscatedAccountId is set for non-transfer purchases`() {
        val mockBuilder = setUpForObfuscatedAccountIDTests()

        val storeProduct = createStoreProductWithoutOffers()
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            null,
            null
        )

        val expectedUserId = appUserId.sha256()
        verify {
            mockBuilder.setObfuscatedAccountId(expectedUserId)
        }

        clearStaticMockk(BillingFlowParams::class)
    }

    @Test
    fun `obfuscatedAccountId is not set for transfer purchases`() {
        val mockBuilder = setUpForObfuscatedAccountIDTests()

        val storeProduct = createStoreProductWithoutOffers()

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockBuilder.setObfuscatedAccountId(any())
        }

        clearStaticMockk(BillingFlowParams::class)
    }

    @Test
    fun defersBillingFlowIfNotConnected() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        every { mockClient.isReady } returns false

        val storeProduct = createStoreProductWithoutOffers()

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockClient.launchBillingFlow(eq(mockActivity), any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        verify(exactly = 1) {
            mockClient.launchBillingFlow(eq(mockActivity), any())
        }
    }

    @Test
    fun callsLaunchFlowFromMainThread() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        every { mockClient.isReady } returns false

        val storeProduct = createStoreProductWithoutOffers()

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            mockReplaceSkuInfo(),
            null
        )

        // ensure calls to startConnection - 1 happens in setup, 1 more here
        verify(exactly = 2) {
            handler.postDelayed(any(), any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        // ensure calls to launchBillingFlow - 1 in setup, 1 here
        verify(exactly = 2) {
            handler.post(any())
        }
    }

    @Test
    fun `subscription purchase fails if subscription option is not GoogleSubscriptionOption`() {
        val slot = slot<PurchasesError>()

        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(capture(slot))
        } just Runs

        val nonGoogleSubscriptionOption = object : SubscriptionOption {
            override val id: String
                get() = "subscriptionOption"
            override val pricingPhases: List<PricingPhase>
                get() = listOf(PricingPhase(
                    billingPeriod = Period.create("P1M"),
                    recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
                    billingCycleCount = 0,
                    price = Price(
                        formatted = "",
                        amountMicros = 0L,
                        currencyCode = "",
                    )
                ))
            override val tags: List<String>
                get() = emptyList()
            override val presentedOfferingIdentifier: String?
                get() = null

            override val purchasingData: PurchasingData
                get() = object: PurchasingData {
                    override val productId: String
                        get() = ""
                    override val productType: ProductType
                        get() = ProductType.SUBS
                }
        }

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            nonGoogleSubscriptionOption.purchasingData,
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockClient.launchBillingFlow(any(), any())
        }

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `purchase fails if store product is not GoogleStoreProduct`() {
        val purchasingData = object: PurchasingData {
            override val productId: String
                get() = ""
            override val productType: ProductType
                get() = ProductType.SUBS
        }

        val storeProduct = object : StoreProduct {
            override val id: String
                get() = "mock-sku"
            override val type: ProductType
                get() = ProductType.SUBS
            override val price: Price
                get() = Price("$0.00", 0, "USD")
            override val title: String
                get() = ""
            override val description: String
                get() = ""
            override val period: Period?
                get() = null
            override val subscriptionOptions: SubscriptionOptions
                get() = SubscriptionOptions(listOf(defaultOption))
            override val defaultOption: SubscriptionOption
                get() = object : SubscriptionOption {
                    override val id: String
                        get() = ""
                    override val pricingPhases: List<PricingPhase>
                        get() = emptyList()
                    override val tags: List<String>
                        get() = emptyList()
                    override val presentedOfferingIdentifier: String?
                        get() = null
                    override val purchasingData: PurchasingData
                        get() = purchasingData
                }
            override val purchasingData: PurchasingData
                get() = purchasingData
            override val presentedOfferingIdentifier: String?
                get() = null
            override val sku: String
                get() = id

            override fun copyWithOfferingId(offeringId: String): StoreProduct {
                return this // this is wrong, just doing for test
            }
        }

        val slot = slot<PurchasesError>()

        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(capture(slot))
        } just Runs

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockClient.launchBillingFlow(any(), any())
        }

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun purchasesUpdatedCallsAreForwarded() {
        val purchases = listOf(stubGooglePurchase())
        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            purchases,
            emptyList()
        )

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne
    }

    @Test
    fun `purchasesUpdatedCalls are forwarded with empty list if result is ok but with a null purchase`() {
        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, null)

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.isEmpty()).isTrue
    }

    @Test
    fun purchaseUpdateFailedCalledIfNotOK() {
        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any())
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            null
        )
        verify(exactly = 0) {
            mockPurchasesListener.onPurchasesUpdated(any())
        }
        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(any())
        }
    }

    @Test
    fun queryHistoryCallsListenerIfOk() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        var successCalled = false
        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {
                successCalled = true
            },
            {
                fail("shouldn't go to on error")
            }
        )
        assertThat(successCalled).isTrue
    }

    @Test
    fun queryHistoryErrorCalledIfNotOK() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            emptyList()
        )

        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {
                fail("should go to on error")
            },
            {
                assertThat(it.code).isEqualTo(PurchasesErrorCode.PurchaseNotAllowedError)
                errorCalled = true
            }
        )

        assertThat(errorCalled).isTrue
    }

    @Test
    fun canConsumeAToken() {
        val token = "mockToken"

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.consumePurchase(token) { _, _ -> }

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        assertThat(capturedConsumeParams.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `calling billing close() sets purchasesUpdatedListener to null and disconnects from BillingClient`() {
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true

        wrapper.close()
        verify {
            mockClient.endConnection()
        }
        assert(wrapper.purchasesUpdatedListener == null)
    }

    @Test
    fun whenSettingListenerStartConnection() {
        verify {
            mockClient.startConnection(eq(wrapper))
        }

        assertThat(wrapper.purchasesUpdatedListener).isNotNull
    }

    @Test
    fun whenExecutingRequestAndThereIsNoListenerDoNotTryToStartConnection() {
        every {
            mockClient.endConnection()
        } just Runs
        wrapper.purchasesUpdatedListener = null
        wrapper.consumePurchase("token") { _, _ -> }

        verify(exactly = 1) { // Just the original connection
            mockClient.startConnection(wrapper)
        }
    }

    @Test
    fun whenProductDetailsIsEmptyPassAnEmptyListToTheListener() {
        mockEmptyProductDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<StoreProduct>? = null
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                fail("shouldn't be an error")
            })
        wrapper.onBillingSetupFinished(billingClientOKResult)
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero
    }

    @Test
    fun nullifyBillingClientAfterEndingConnection() {
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true
        wrapper.purchasesUpdatedListener = null

        assertThat<BillingClient>(wrapper.billingClient).isNull()
    }

    @Test
    fun newBillingClientIsCreatedWhenSettingListener() {
        wrapper.purchasesUpdatedListener = mockPurchasesListener

        assertThat<BillingClient>(wrapper.billingClient).isNotNull
    }

    @Test
    fun `calling close before setup finishes doesn't crash`() {
        every {
            mockClient.isReady
        } returns false

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("product_a"),
            {},
            {
                fail("shouldn't be an error")
            })

        wrapper.purchasesUpdatedListener = null
        wrapper.onBillingSetupFinished(billingClientOKResult)
    }

    @Test
    fun `calling close before purchase completes doesn't crash`() {
        every {
            mockClient.isReady
        } returns false

        wrapper.purchasesUpdatedListener = null
        wrapper.onPurchasesUpdated(BillingClient.BillingResponseCode.DEVELOPER_ERROR.buildResult(), emptyList())
    }

    @Test
    fun `calling end connection before client is ready ends connection`() {
        every {
            mockClient.isReady
        } returns false

        wrapper.purchasesUpdatedListener = null
        verify {
            mockClient.endConnection()
        }
    }

    @Test
    fun `getting all purchases gets both subs and inapps`() {
        val builder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            listOf(stubPurchaseHistoryRecord())
        )

        var receivedPurchases = listOf<StoreTransaction>()
        wrapper.queryAllPurchases("appUserID", {
            receivedPurchases = it
        }, { fail("Shouldn't be error") })

        assertThat(receivedPurchases.size).isNotZero
        mockClient.verifyQueryPurchaseHistoryCalledWithType(subsGoogleProductType, builder)
        mockClient.verifyQueryPurchaseHistoryCalledWithType(inAppGoogleProductType, builder)
    }

    @Test
    fun `on successfully connected billing client, listener is called`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        assertThat(onConnectedCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync sets correct type`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        val subsBuilder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {},
            {}
        )

        mockClient.verifyQueryPurchaseHistoryCalledWithType(subsGoogleProductType, subsBuilder)

        val inAppBuilder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        wrapper.queryPurchaseHistoryAsync(
            inAppGoogleProductType,
            {},
            {}
        )

        mockClient.verifyQueryPurchaseHistoryCalledWithType(inAppGoogleProductType, inAppBuilder)
    }

    @Test
    fun `when querying anything and billing client returns an empty list, returns an empty list`() {
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            emptyList()
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isEmpty()
    }

    @Test
    fun `when querying INAPPs result is created properly`() {
        val token = "token"
        val type = ProductType.INAPP
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            listOf(purchase)
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isNotEmpty

        val purchaseWrapper = purchasesByHashedToken?.get(token.sha1())
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(type)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.productIds[0]).isEqualTo(sku)
        assertThat(purchasesByHashedToken?.size == 1)

    }

    @Test
    fun `when querying SUBS result is created properly`() {
        val token = "token"
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            listOf(purchase),
            emptyList()
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isNotEmpty

        val purchaseWrapper = purchasesByHashedToken?.get(token.sha1())
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(ProductType.SUBS)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.productIds[0]).isEqualTo(sku)
    }

    @Test
    fun `Presented offering is properly forwarded`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val productDetails = mockProductDetails(productId = "product_a")
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            null,
            "offering_a"
        )

        val purchases = listOf(stubGooglePurchase(productIds = listOf("product_a")))

        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne
        assertThat(slot.captured[0].presentedOfferingIdentifier).isEqualTo("offering_a")
    }

    @Test
    fun `subscriptionOptionId is properly forwarded`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val productDetails = mockProductDetails(productId = "product_a")
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        val subscriptionOption = storeProduct.subscriptionOptions!!.first()
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            subscriptionOption.purchasingData,
            null,
            "offering_a"
        )

        val purchases = listOf(stubGooglePurchase(productIds = listOf("product_a")))

        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne
        assertThat(slot.captured[0].subscriptionOptionId).isEqualTo(subscriptionOption.id)
    }

    @Test
    fun `When building the BillingClient enabledPendingPurchases is called`() {
        val context = mockk<Context>()
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every {
            BillingClient.newBuilder(context)
        } returns mockBuilder
        BillingWrapper.ClientFactory(context).buildClient(mockk())
        verify(exactly = 1) {
            mockBuilder.enablePendingPurchases()
        }
    }

    @Test
    fun `Acknowledge works`() {
        val token = "token"

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.acknowledge(token) { _, _ -> }

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        assertThat(capturedAcknowledgePurchaseParams.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `Getting SUBS type`() {
        val inAppToken = "inAppToken"
        val subsToken = "subsToken"

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            getMockedPurchaseList(subsToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(subsToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.SUBS)
        }
    }

    @Test
    fun `Getting INAPPs type`() {
        val inAppToken = "inAppToken"
        val subToken = "subToken"

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            getMockedPurchaseList(subToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.INAPP)
        }

    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub and inapps response not OK`() {
        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val subToken = "subToken"
        val inAppToken = "abcd"

        mockClient.mockQueryPurchasesAsync(
            errorResult,
            errorResult,
            getMockedPurchaseList(subToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub not found and inapp responses not OK`() {
        val subPurchaseToken = "subToken"
        val inAppPurchaseToken = "inAppToken"

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientErrorResult,
            subPurchases = getMockedPurchaseList(subPurchaseToken),
            inAppPurchases = getMockedPurchaseList(inAppPurchaseToken)
        )

        wrapper.getPurchaseType(inAppPurchaseToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    @Test
    fun `findPurchaseInPurchaseHistory works`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = stubPurchaseHistoryRecord(productIds = listOf(sku))

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
        )

        var recordFound: StoreTransaction? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserId,
            ProductType.SUBS,
            sku,
            onCompletion = {
                recordFound = it
            },
            onError = {
                fail("should be success")
            }
        )

        assertThat(recordFound).isNotNull
        assertThat(recordFound!!.productIds[0]).isEqualTo(purchaseHistoryRecord.firstSku)
        assertThat(recordFound!!.purchaseTime).isEqualTo(purchaseHistoryRecord.purchaseTime)
        assertThat(recordFound!!.purchaseToken).isEqualTo(purchaseHistoryRecord.purchaseToken)
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error if not found`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = mockk<PurchaseHistoryRecord>(relaxed = true).also {
            every { it.firstSku } returns sku + "somethingrandom"
        }

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
        )
        var errorReturned: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserId,
            ProductType.SUBS,
            sku,
            onCompletion = {
                fail("should be error")
            },
            onError = {
                errorReturned = it
            }
        )

        assertThat(errorReturned).isNotNull
        assertThat(errorReturned!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
    }

    @Test
    fun `tokens are saved in cache when acknowledging`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are saved in cache when acknowledging`() {
        val sku = "sub"
        val token = "token_sub"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.SUBS
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens are saved in cache when consuming`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are saved in cache when consuming`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens are not saved in cache if acknowledge fails`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a"
        )

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult()
        )

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are not save in cache if acknowledge fails`() {
        val sku = "sub"
        val token = "token_sub"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.SUBS
        )

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult()
        )

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `tokens are not save in cache if consuming fails`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        mockConsumeAsync(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult())

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `restored tokens are not save in cache if consuming fails`() {
        val sku = "consumable"
        val token = "token_consumable"

        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        mockConsumeAsync(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult())

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `subscriptions are acknowledged`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        val capturedAcknowledgeParams = capturedAcknowledgePurchaseParams.captured
        assertThat(capturedAcknowledgeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `restored subscriptions are acknowledged`() {
        val sku = "sub"
        val token = "token_sub"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.SUBS
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue
        val capturedAcknowledgeParams = capturedAcknowledgePurchaseParams.captured
        assertThat(capturedAcknowledgeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `consumables are consumed`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue
        val capturedConsumeParams = capturedConsumeParams.captured
        assertThat(capturedConsumeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `restored consumables are consumed`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue
        val capturedConsumeParams = capturedConsumeParams.captured
        assertThat(capturedConsumeParams.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `product type defaults to INAPP when querying product details`() {
        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(
                capture(slot),
                any()
            )
        } just Runs

        val productIDs = setOf("product_a")

        wrapper.queryProductDetailsAsync(
            ProductType.UNKNOWN,
            productIDs,
            {
                this@BillingWrapperTest.storeProducts = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.productList[0].productType).isEqualTo(BillingClient.ProductType.INAPP)
    }

    @Test
    fun `if it shouldn't consume transactions, don't consume and save it in cache`() {
        val sku = "consumable"
        val token = "token_consumable"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.INAPP,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = false, googlePurchaseWrapper)

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `if it shouldn't consume restored transactions, don't consume and save it in cache`() {
        val sku = "consumable"
        val token = "token_consumable"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.INAPP
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = false, historyRecordWrapper)

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `if it shouldn't consume transactions, don't acknowledge and save it in cache`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a"
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = false, googlePurchaseWrapper)

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `if it shouldn't consume restored transactions, don't acknowledge and save it in cache`() {
        val sku = "sub"
        val token = "token_sub"
        val historyRecordWrapper = getMockedPurchaseHistoryRecordWrapper(
            sku,
            token,
            ProductType.SUBS
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = false, historyRecordWrapper)

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `Do not acknowledge purchases that are already acknowledged`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a",
            acknowledged = true
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = true, googlePurchaseWrapper)

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 1) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `Do not consume nor acknowledge pending purchases`() {
        val sku = "sub"
        val token = "token_sub"
        val googlePurchaseWrapper = getMockedPurchaseWrapper(
            sku,
            token,
            ProductType.SUBS,
            "offering_a",
            purchaseState = Purchase.PurchaseState.PENDING
        )

        every {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        } just Runs

        wrapper.consumeAndSave(shouldTryToConsume = true, googlePurchaseWrapper)

        verify(exactly = 0) {
            mockClient.acknowledgePurchase(any(), any())
        }

        verify(exactly = 0) {
            mockClient.consumeAsync(any(), any())
        }

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `queryProductDetails filters empty productIds before querying BillingClient`() {
        val productIdsSet = setOf("abcd", "", "1", "")

        val slot = slot<QueryProductDetailsParams>()
        every {
            mockClient.queryProductDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            productIdsSet,
            {}, {
                fail("shouldn't be an error")
            })

        assertThat(slot.captured).isNotNull
        val queryProductDetailsParamsProductList = slot.captured.productList
        val queriedProductIds = queryProductDetailsParamsProductList.map { it.productId }
        assertThat(queriedProductIds).isEqualTo(productIdsSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `queryProductDetails with empty list returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            emptySet(),
            {
                assertThat(it.isEmpty())
            }, {
                fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetails with only empty productIds returns empty list and does not query BillingClient`() {
        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("", ""),
            {
                assertThat(it.isEmpty())
            }, {
                fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.queryProductDetailsAsync(any(), any())
        }
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("asdf", "asdf"),
            {
                sleep(200)
                numCallbacks++
            }, {
                numCallbacks++
            })

        assertThat(numCallbacks == 1)
    }

    @Test
    fun `queryProductDetailsAsync only calls one response when BillingClient responds twice in separate threads`() {
        var numCallbacks = 0

        val slot = slot<ProductDetailsResponseListener>()
        val lock = CountDownLatch(2)
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()
        }

        wrapper.queryProductDetailsAsync(
            ProductType.SUBS,
            setOf("asdf"),
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                handler.post {
                    numCallbacks++
                }
            }, {
                fail("shouldn't be an error")
            })

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `if BillingService disconnects, will try to reconnect with exponential backoff`() {
        // ensure delay on first retry
        val firstRetryMillisecondsSlot = slot<Long>()
        every {
            handler.postDelayed(any(), capture(firstRetryMillisecondsSlot))
        } returns true

        wrapper.onBillingServiceDisconnected()

        assertThat(firstRetryMillisecondsSlot.isCaptured).isTrue
        assertThat(firstRetryMillisecondsSlot.captured).isNotEqualTo(0)

        // ensure 2nd retry has longer delay
        val secondRetryMillisecondsSlot = slot<Long>()
        every {
            handler.postDelayed(any(), capture(secondRetryMillisecondsSlot))
        } returns true
        wrapper.onBillingServiceDisconnected()

        assertThat(secondRetryMillisecondsSlot.isCaptured).isTrue
        assertThat(secondRetryMillisecondsSlot.captured).isGreaterThan(firstRetryMillisecondsSlot.captured)

        // ensure milliseconds backoff gets reset to default after successful connection
        wrapper.onBillingSetupFinished(billingClientOKResult)
        val afterSuccessfulConnectionRetryMillisecondsSlot = slot<Long>()
        every {
            handler.postDelayed(any(), capture(afterSuccessfulConnectionRetryMillisecondsSlot))
        } returns true
        wrapper.onBillingServiceDisconnected()

        assertThat(afterSuccessfulConnectionRetryMillisecondsSlot.isCaptured).isTrue
        assertThat(afterSuccessfulConnectionRetryMillisecondsSlot.captured == firstRetryMillisecondsSlot.captured)
    }

    @Test
    fun `if billing setup returns recoverable error code, will try to reconnect with exponential backoff`() {
        every {
            handler.postDelayed(any(), any())
        } returns true

        val errorCodes = listOf(
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
        )
        var currentCallback = 1 // we get one call before triggering it manually
        for (errorCode in errorCodes) {
            currentCallback += 1
            wrapper.onBillingSetupFinished(errorCode.buildResult())
            verify(exactly = currentCallback) { handler.postDelayed(any(), any()) }
        }
    }

    @Test
    fun `if billing setup returns code that doesnt merit retry, will not try to reconnect`() {
        every {
            handler.postDelayed(any(), any())
        } returns true

        val errorCodes = listOf(
            BillingClient.BillingResponseCode.OK,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR
        )
        for (errorCode in errorCodes) {
            wrapper.onBillingSetupFinished(errorCode.buildResult())
        }
        verify(exactly = 1) { handler.postDelayed(any(), any()) }
    }

    @Test
    fun `setting purchasesUpdatedListener will connect to BillingService with no delay`() {
        val retryMillisecondsSlot = slot<Long>()
        every {
            handler.postDelayed(any(), capture(retryMillisecondsSlot))
        } returns true

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        assertThat(retryMillisecondsSlot.captured == 0L)
    }

    @Test
    fun `normalizing Google purchase returns correct product ID and null store user ID`() {
        val expectedProductID = "expectedProductID"

        var receivedProductID: String? = null

        wrapper.normalizePurchaseData(
            expectedProductID,
            "purchaseToken",
            "nothingshouldbepassedherebutjustincase",
            { normalizedProductID ->
                receivedProductID = normalizedProductID
            },
            {
                fail("shouldn't be an error")
            }
        )

        assertThat(receivedProductID).isEqualTo(expectedProductID)
    }

    // region diagnostics tracking

    @Test
    fun `queryPurchaseHistoryAsync tracks diagnostics call with correct parameters`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.ProductType.SUBS, {}, { fail("shouldn't be an error") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync tracks diagnostics call with correct parameters on error`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.ProductType.SUBS, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `querySkuDetailsAsync tracks diagnostics call with correct parameters`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(result, emptyList())
        }

        wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("test-sku"), {}, { fail("shouldn't be an error") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `querySkuDetailsAsync tracks diagnostics call with correct parameters on error`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(result, emptyList())
        }

        wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("test-sku"), { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `queryPurchases tracks query purchases diagnostics calls for subs and inapp with correct parameters`() {
        every {
            mockDateProvider.now
        } returnsMany listOf(
            Date(timestamp0),
            Date(timestamp123),
            Date(timestamp500),
            Date(timestamp900)
        )

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, {}, { fail("shouldn't be an error") })

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.INAPP,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 400.milliseconds
            )
        }
    }

    @Test
    fun `queryPurchases tracks query purchases diagnostics only for subs query if it fails`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `getPurchaseType tracks query purchases diagnostics calls for subs and inapp`() {
        every {
            mockDateProvider.now
        } returnsMany listOf(
            Date(timestamp0),
            Date(timestamp123),
            Date(timestamp500),
            Date(timestamp900)
        )

        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.SUBS,
            getMockedPurchaseList("subToken")
        )
        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.INAPP,
            getMockedPurchaseList("inappToken")
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("inappToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.INAPP)

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.INAPP,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 400.milliseconds
            )
        }
    }

    @Test
    fun `getPurchaseType tracks query purchases diagnostics calls for subs if found as subs`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.SUBS,
            getMockedPurchaseList("subToken")
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("subToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.SUBS)

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
        }
    }

    // endregion

    private fun mockEmptyProductDetailsResponse() {
        val slot = slot<ProductDetailsResponseListener>()
        every {
            mockClient.queryProductDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onProductDetailsResponse(billingClientOKResult, emptyList())
        }
    }

    private fun mockPurchaseHistoryRecordWrapper(): StoreTransaction {
        val oldPurchase = stubPurchaseHistoryRecord(
            productIds = listOf("product_b"),
            purchaseToken = "atoken"
        )

        return oldPurchase.toStoreTransaction(type = ProductType.SUBS)
    }

    private fun mockReplaceSkuInfo(): ReplaceProductInfo {
        val oldPurchase = mockPurchaseHistoryRecordWrapper()
        return ReplaceProductInfo(oldPurchase, GoogleReplacementMode.CHARGE_FULL_PRICE)
    }

    private fun mockQueryPurchasesAsyncResponse(
        @BillingClient.ProductType productType: String,
        purchasesToReturn: List<Purchase>,
        billingResult: BillingResult = billingClientOKResult
    ) {
        val queryPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                productType.buildQueryPurchasesParams()!!,
                capture(queryPurchasesListenerSlot)
            )
        } answers {
            queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                billingResult,
                purchasesToReturn
            )
        }
    }

    private fun getMockedPurchaseWrapper(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false
    ): StoreTransaction {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return p.toStoreTransaction(productType, offeringIdentifier)
    }

    private fun getMockedPurchaseHistoryRecordWrapper(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(productId),
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(
            type = productType
        )
    }

    private fun setUpForObfuscatedAccountIDTests(): BillingFlowParams.Builder {
        mockkStatic(BillingFlowParams::class)
        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        every {
            mockBuilder.setProductDetailsParamsList(any())
        } returns mockBuilder

        val params = mockk<BillingFlowParams>(relaxed = true)
        every {
            mockBuilder.build()
        } returns params

        every {
            mockClient.launchBillingFlow(any(), params)
        } returns billingClientOKResult

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        return mockBuilder
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockConsumeAsync(billingResult: BillingResult) {
        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }

    private fun createStoreProductWithoutOffers(): StoreProduct {
        val productDetails = createMockProductDetailsNoOffers()
        return productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!
    }

    private fun getMockedPurchaseList(purchaseToken: String): List<Purchase> {
        return listOf(mockk(
            relaxed = true
        ) {
            every { this@mockk.purchaseToken } returns purchaseToken
        })
    }

    private fun mockRunnables() {
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }

        val delayedSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(delayedSlot), any())
        } answers {
            delayedSlot.captured.run()
            true
        }
    }

    private fun mockDiagnosticsTracker() {
        every {
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(any(), any(), any(), any())
        } just Runs
    }
}
