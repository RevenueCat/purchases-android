package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.InAppMessageResponseListener
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.InAppMessageResult.InAppMessageResponseCode
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.assertDebugLog
import com.revenuecat.purchases.assertErrorLog
import com.revenuecat.purchases.assertVerboseLog
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.firstProductId
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.google.history.PurchaseHistoryManager
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.InstallmentsInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.strings.BillingStrings
import com.revenuecat.purchases.utils.createMockProductDetailsNoOffers
import com.revenuecat.purchases.utils.mockInstallmentPlandetails
import com.revenuecat.purchases.utils.mockOneTimePurchaseOfferDetails
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.mockSubscriptionOfferDetails
import com.revenuecat.purchases.utils.stubGooglePurchase
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class BillingWrapperTest {

    internal companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
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
    private var mockPurchaseHistoryManager: PurchaseHistoryManager = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<ProductDetails>
    private lateinit var testScope: TestScope

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private val appUserId = "jerry"
    private var mockActivity = mockk<Activity>()

    private val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    private val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!
    private val purchasesStateProvider = PurchasesStateCache(PurchasesState())

    @Before
    public fun setup() {
        testScope = TestScope()
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

        mockConsumeAsync(billingClientOKResult)

        every {
            mockClient.isReady
        } returns false andThen true

        val featureSlot = slot<String>()
        every {
            mockClient.isFeatureSupported(capture(featureSlot))
        } returns billingClientOKResult

        mockDetailsList = listOf(mockProductDetails())

        setupBillingWrapper()

        every {
            mockActivity.intent
        } returns Intent()
    }

    private fun setupBillingWrapper(
        isAIDLEnabled: Boolean = false,
    ) {
        wrapper = BillingWrapper(
            mockClientFactory,
            handler,
            mockDeviceCache,
            mockDiagnosticsTracker,
            purchasesStateProvider,
            mockPurchaseHistoryManager,
            mockDateProvider,
            coroutineScope = testScope,
            isAIDLEnabled = isAIDLEnabled,
        )
        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.startConnectionOnMainThread()
        onConnectedCalled = false
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }
    }

    @After
    public fun tearDown() {
        clearAllMocks()
    }

    @Test
    public fun canBeCreated() {
        assertThat(wrapper).`as`("Wrapper is not null").isNotNull
    }

    @Test
    public fun callsBuildOnTheFactory() {
        verify {
            mockClientFactory.buildClient(purchasesUpdatedListener!!)
        }
    }

    @Test
    public fun connectsToPlayBilling() {
        verify {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    @Test
    public fun defersCallUntilConnected() {
        every { mockClient.isReady } returns false

        val token = "token"
        var consumePurchaseCompleted = false
        wrapper.consumePurchase(
            token = token,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) {
            consumePurchaseCompleted = true
        }

        assertThat(consumePurchaseCompleted).isFalse

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        assertThat(consumePurchaseCompleted).isTrue
    }

    @Test
    public fun canDeferMultipleCallsUntilConnected() {
        every { mockClient.isReady } returns false

        val token = "token"

        var consumePurchaseResponse1Called = false
        wrapper.consumePurchase(
            token = token,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) {
            consumePurchaseResponse1Called = true
        }

        var consumePurchaseResponse2Called = false
        wrapper.consumePurchase(
            token = token,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) {
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
    public fun makingARequestTriggersAConnectionAttempt() {
        every { mockClient.isReady } returns false

        val token = "token"
        wrapper.consumePurchase(
            token = token,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) {  }

        verify(exactly = 2) {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    @Test
    fun `If starting connection throws an IllegalStateException, error is forwarded`() {
        every { mockClient.isReady } returns false
        every {
            mockClient.startConnection(any())
        } throws IllegalStateException("Too many bind requests(999+) for service intent.")

        var error: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                fail("should be an error")
            },
            onError = {
                error = it
            }
        )

        verify {
            mockClient.startConnection(billingClientStateListener!!)
        }
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If starting connection throws a SecurityException, error is forwarded`() {
        every { mockClient.isReady } returns false
        every {
            mockClient.startConnection(any())
        } throws SecurityException("get package info: UID XXXXXXX requires android.permission.INTERACT_ACROSS_USERS_FULL or android.permission.INTERACT_ACROSS_USERS to access user 0.")

        var error: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                fail("should be an error")
            },
            onError = {
                error = it
            }
        )

        verify {
            mockClient.startConnection(billingClientStateListener!!)
        }
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `can make a purchase`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val storeProduct = createStoreProductWithoutOffers()
        val purchasingData = storeProduct.subscriptionOptions!!.first().purchasingData

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            purchasingData,
            mockReplaceSkuInfo(),
            PresentedOfferingContext("offering_a"),
        )

        verify {
            mockClient.launchBillingFlow(
                eq(mockActivity),
                any()
            )
        }

        assertThat(wrapper.purchaseContext.size).isEqualTo(1)
        val purchaseContext = wrapper.purchaseContext[purchasingData.productId]
        assertThat(purchaseContext).isNotNull
        assertThat(purchaseContext?.productType).isEqualTo(ProductType.SUBS)
        assertThat(purchaseContext?.presentedOfferingContext).isEqualTo(PresentedOfferingContext("offering_a"))
        assertThat(purchaseContext?.selectedSubscriptionOptionId).isEqualTo(storeProduct.subscriptionOptions!!.first().id)
        assertThat(purchaseContext?.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_FULL_PRICE)
    }

    @Test
    fun `making a deferred purchase uses previous product id cached context`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val storeProduct = createStoreProductWithoutOffers()
        val purchasingData = storeProduct.subscriptionOptions!!.first().purchasingData
        val oldPurchase = mockPurchaseRecordWrapper()
        val replaceInfo = ReplaceProductInfo(oldPurchase, GoogleReplacementMode.DEFERRED)

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            purchasingData,
            replaceInfo,
            PresentedOfferingContext("offering_a"),
        )

        verify {
            mockClient.launchBillingFlow(
                eq(mockActivity),
                any()
            )
        }

        assertThat(wrapper.purchaseContext.size).isEqualTo(1)
        val purchaseContext = wrapper.purchaseContext[oldPurchase.productIds.first()]
        assertThat(purchaseContext).isNotNull
        assertThat(purchaseContext?.productType).isEqualTo(ProductType.SUBS)
        assertThat(purchaseContext?.presentedOfferingContext).isEqualTo(PresentedOfferingContext("offering_a"))
        assertThat(purchaseContext?.selectedSubscriptionOptionId).isEqualTo(storeProduct.subscriptionOptions!!.first().id)
        assertThat(purchaseContext?.replacementMode).isEqualTo(GoogleReplacementMode.DEFERRED)
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

        val replacementModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setSubscriptionReplacementMode(capture(replacementModeSlot))
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
            assertThat((upgradeInfo.replacementMode as GoogleReplacementMode?)?.playBillingClientMode).isEqualTo(replacementModeSlot.captured)

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
    fun `skips setting on BillingFlowPrams when replacementmode or personalized price null for subscription purchase`() {
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

        val replacementModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setSubscriptionReplacementMode(capture(replacementModeSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val isPersonalizedPriceSlot = slot<Boolean>()
        every {
            mockBuilder.setIsOfferPersonalized(capture(isPersonalizedPriceSlot))
        } returns mockBuilder

        val productId = "product_a"

        val replaceProductInfo = ReplaceProductInfo(mockPurchaseRecordWrapper())
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
                mockSubscriptionUpdateParamsBuilder.setSubscriptionReplacementMode(any())
                !replacementModeSlot.isCaptured
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
        mockkStatic(ProductDetailsParams::class)

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
        mockkStatic(ProductDetailsParams::class)
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
        mockkStatic(ProductDetailsParams::class)

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
            null,
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
            null,
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
            null,
        )

        verify(exactly = 0) {
            mockBuilder.setObfuscatedAccountId(any())
        }

        clearStaticMockk(BillingFlowParams::class)
    }

    @Test
    public fun defersBillingFlowIfNotConnected() {
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
            null,
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
    public fun callsLaunchFlowFromMainThread() {
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
            null,
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
                get() = presentedOfferingContext?.offeringIdentifier
            override val presentedOfferingContext: PresentedOfferingContext?
                get() = null

            override val purchasingData: PurchasingData
                get() = object: PurchasingData {
                    override val productId: String
                        get() = ""
                    override val productType: ProductType
                        get() = ProductType.SUBS
                }

            override val installmentsInfo: InstallmentsInfo?
                get() = null
        }

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            nonGoogleSubscriptionOption.purchasingData,
            mockReplaceSkuInfo(),
            null,
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
            override val name: String
                get() = ""
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
                        get() = presentedOfferingContext?.offeringIdentifier
                    override val presentedOfferingContext: PresentedOfferingContext?
                        get() = null
                    override val purchasingData: PurchasingData
                        get() = purchasingData
                    override val installmentsInfo: InstallmentsInfo?
                        get() = null
                }
            override val purchasingData: PurchasingData
                get() = purchasingData
            override val presentedOfferingIdentifier: String?
                get() = presentedOfferingContext?.offeringIdentifier
            override val presentedOfferingContext: PresentedOfferingContext?
                get() = null

            @Deprecated("Replaced with id", replaceWith = ReplaceWith("id"))
            override val sku: String
                get() = id

            override fun copyWithOfferingId(offeringId: String): StoreProduct {
                return this // this is wrong, just doing for test
            }

            override fun copyWithPresentedOfferingContext(presentedOfferingContext: PresentedOfferingContext?): StoreProduct {
                return this // this is wrong, just doing for test
            }

            override fun formattedPricePerMonth(locale: Locale): String? {
                error("not implemented")
            }
        }

        val slot = slot<PurchasesError>()

        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(capture(slot))
        } just Runs

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions.first().purchasingData,
            mockReplaceSkuInfo(),
            null,
        )

        verify(exactly = 0) {
            mockClient.launchBillingFlow(any(), any())
        }

        assertThat(slot.isCaptured).isTrue
        assertThat(slot.captured.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    public fun purchasesUpdatedCallsAreForwarded() {
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
    fun `purchasesUpdatedCalls call the error callback if result is ok but with a null purchase`() {
        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any())
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(
            BillingClient.BillingResponseCode.OK.buildResult(),
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
    public fun purchaseUpdateFailedCalledIfNotOK() {
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
    public fun whenSettingListenerStartConnection() {
        verify {
            mockClient.startConnection(eq(wrapper))
        }

        assertThat(wrapper.purchasesUpdatedListener).isNotNull
    }

    @Test
    public fun whenExecutingRequestAndThereIsNoListenerDoNotTryToStartConnection() {
        every {
            mockClient.endConnection()
        } just Runs
        wrapper.purchasesUpdatedListener = null
        wrapper.consumePurchase(
            token = "token",
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        ) { }

        verify(exactly = 1) { // Just the original connection
            mockClient.startConnection(wrapper)
        }
    }

    @Test
    public fun closingBillingClientAfterEndingConnection() {
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true

        wrapper.close()

        assertThat<BillingClient>(wrapper.billingClient).isNull()
    }

    @Test
    public fun newBillingClientIsCreatedWhenSettingListener() {
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

        wrapper.close()
        verify {
            mockClient.endConnection()
        }
    }

    @Test
    fun `on successfully connected billing client, listener is called`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        assertThat(onConnectedCalled).isTrue
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
        val expectedOfferingId = "offering_a"
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            storeProduct.subscriptionOptions!!.first().purchasingData,
            null,
            PresentedOfferingContext(expectedOfferingId)
        )

        val purchases = listOf(stubGooglePurchase(productIds = listOf("product_a")))

        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne
        assertThat(slot.captured[0].presentedOfferingIdentifier).isEqualTo(expectedOfferingId)
        assertThat(slot.captured[0].presentedOfferingContext?.offeringIdentifier).isEqualTo(expectedOfferingId)
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
            PresentedOfferingContext("offering_a"),
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
    fun `installment plan details are properly forwarded`() {
        val productDetails = mockProductDetails(subscriptionOfferDetails = listOf(
            mockSubscriptionOfferDetails(installmentDetails = mockInstallmentPlandetails())
        ))
        val storeProduct = productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!
        assertThat(storeProduct.subscriptionOptions?.size).isOne
        val subscriptionOption = storeProduct.subscriptionOptions!!.first()
        assertThat(subscriptionOption.installmentsInfo).isNotNull
        assertThat(subscriptionOption.installmentsInfo?.commitmentPaymentsCount).isEqualTo(3)
        assertThat(subscriptionOption.installmentsInfo?.renewalCommitmentPaymentsCount).isEqualTo(1)
    }

    @Test
    fun `When building the BillingClient enabledPendingPurchases for OTP and prepaid are called`() {
        val context = mockk<Context>()
        mockkStatic(BillingClient::class)
        mockkStatic(PendingPurchasesParams::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every {
            BillingClient.newBuilder(context)
        } returns mockBuilder
        val mockPendingParamsBuilder = mockk<PendingPurchasesParams.Builder>(relaxed = true)
        val mockPendingParams = mockk<PendingPurchasesParams>()
        every {
            PendingPurchasesParams.newBuilder()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.enableOneTimeProducts()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.enablePrepaidPlans()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.build()
        } returns mockPendingParams
        BillingWrapper.ClientFactory(context, pendingTransactionsForPrepaidPlansEnabled = true).buildClient(mockk())

        verify(exactly = 1) {
            mockPendingParamsBuilder.enableOneTimeProducts()
        }
        verify(exactly = 1) {
            mockPendingParamsBuilder.enablePrepaidPlans()
        }
        verify(exactly = 1) {
            mockBuilder.enablePendingPurchases(mockPendingParams)
        }
    }

    @Test
    fun `Can disable pending prepaid subs`() {
        val context = mockk<Context>()
        mockkStatic(BillingClient::class)
        mockkStatic(PendingPurchasesParams::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every {
            BillingClient.newBuilder(context)
        } returns mockBuilder
        val mockPendingParamsBuilder = mockk<PendingPurchasesParams.Builder>(relaxed = true)
        val mockPendingParams = mockk<PendingPurchasesParams>()
        every {
            PendingPurchasesParams.newBuilder()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.enableOneTimeProducts()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.enablePrepaidPlans()
        } returns mockPendingParamsBuilder
        every {
            mockPendingParamsBuilder.build()
        } returns mockPendingParams
        BillingWrapper.ClientFactory(context, pendingTransactionsForPrepaidPlansEnabled = false).buildClient(mockk())

        verify(exactly = 1) {
            mockPendingParamsBuilder.enableOneTimeProducts()
        }
        verify(exactly = 0) {
            mockPendingParamsBuilder.enablePrepaidPlans()
        }
        verify(exactly = 1) {
            mockBuilder.enablePendingPurchases(mockPendingParams)
        }
    }

    fun `if billing setup returns recoverable error code, will try to reconnect with exponential backoff`() {
        val runnableSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(runnableSlot), any())
        } returns true

        val errorCodes = listOf(
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR
        )
        var currentCallback = 1 // we get one call before triggering it manually
        for (errorCode in errorCodes) {
            currentCallback += 1
            wrapper.onBillingSetupFinished(errorCode.buildResult())
            verify(exactly = currentCallback) { handler.postDelayed(any(), any()) }
            runnableSlot.captured.run()
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
    fun `trackProductDetailsNotSupported is called when receiving a FEATURE_NOT_SUPPORTED error from isFeatureSupported after setup`() {
        val featureSlot = slot<String>()
        every {
            mockClient.isFeatureSupported(capture(featureSlot))
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        verify(exactly = 1) {
            mockDiagnosticsTracker.trackProductDetailsNotSupported(
                billingResponseCode = -2,
                billingDebugMessage = ""
            )
        }
    }

    @Test
    fun `startConnectionOnMainThread tracks diagnostics call with correct parameters`() {
        // Arrange, Act, Assert
        // Our test setup() method calls startConnectionOnMainThread().
        verify(exactly = 1) { mockDiagnosticsTracker.trackGoogleBillingStartConnection() }
    }

    @Test
    fun `onBillingSetupFinished tracks diagnostics call with correct parameters`() {
        // Arrange
        every { mockClient.queryProductDetailsAsync(any(), any()) } just runs
        // BillingClient is not connected, to check pendingRequestCount.
        every { mockClient.isReady } returns false
        val billingResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val expectedResponseCode = billingResult.responseCode
        val expectedDebugMessage = billingResult.debugMessage
        // We expect this to be capped at 100, even though we have 200 pending requests.
        val expectedPendingRequestCount = 100

        // Act
        repeat(200) {
            wrapper.queryProductDetailsAsync(
                productType = ProductType.SUBS,
                productIds = setOf("product_a"),
                onReceive = { },
                onError = { fail("shouldn't be an error") }
            )
        }
        wrapper.onBillingSetupFinished(billingResult)

        // Assert
        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleBillingSetupFinished(
                responseCode = expectedResponseCode,
                debugMessage = expectedDebugMessage,
                pendingRequestCount = expectedPendingRequestCount,
            )
        }
    }

    @Test
    fun `onBillingServiceDisconnected tracks diagnostics call with correct parameters`() {
        // Arrange, Act
        wrapper.onBillingServiceDisconnected()

        // Assert
        verify(exactly = 1) { mockDiagnosticsTracker.trackGoogleBillingServiceDisconnected() }
    }

    @Test
    fun `make a purchase tracks start purchase event`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val storeProduct = createStoreProductWithoutOffers()
        val purchasingData = storeProduct.subscriptionOptions!!.first().purchasingData
        val replaceSkuInfo = mockReplaceSkuInfo()

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            purchasingData,
            replaceSkuInfo,
            PresentedOfferingContext("offering_a"),
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGooglePurchaseStarted(
                productId = purchasingData.productId,
                oldProductId = replaceSkuInfo.oldPurchase.productIds.first(),
                hasIntroTrial = false,
                hasIntroPrice = false,
            )
        }
    }

    @Test
    fun `tracks purchase update received event`() {
        val purchases = listOf(stubGooglePurchase())
        every {
            mockPurchasesListener.onPurchasesUpdated(any())
        } just Runs

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            purchases,
            emptyList()
        )

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGooglePurchaseUpdateReceived(
                productIds = listOf(purchases.first().firstProductId),
                purchaseStatuses = listOf("PURCHASED"),
                billingResponseCode = BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
            )
        }

    }

    // endregion diagnostics tracking

    // region inapp messages

    @Test
    fun `showing inapp messages does nothing and logs if no types passed`() {
        assertErrorLog(BillingStrings.BILLING_UNSPECIFIED_INAPP_MESSAGE_TYPES) {
            wrapper.showInAppMessagesIfNeeded(mockk(), emptyList()) {
                error("Unexpected subscription status change")
            }
        }
        wrapper.showInAppMessagesIfNeeded(mockk(), emptyList()) {
            error("Unexpected subscription status change")
        }

        // This is the initial start connection
        verify(exactly = 1) { mockClient.startConnection(any()) }
    }

    @Test
    fun `showing inapp messages triggers connection if not connected`() {
        every { mockClient.isReady } returns false

        wrapper.showInAppMessagesIfNeeded(mockk(), InAppMessageType.values().toList()) {
            error("Unexpected subscription status change")
        }

        // One for the initial, another for this test since isReady is false
        verify(exactly = 2) { mockClient.startConnection(any()) }
    }

    @Test
    fun `showing inapp messages calls show inapp messages correctly`() {
        val activity = mockk<Activity>()
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns false
        every { mockClient.showInAppMessages(activity, any(), any()) } returns billingClientOKResult

        wrapper.showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList()) {
            error("Unexpected subscription status change")
        }

        verify(exactly = 1) { mockClient.showInAppMessages(activity, any(), any()) }
    }

    @Test
    fun `showing inapp messages handles inapp messages listener response correctly when no messages`() {
        val activity = mockk<Activity>()
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns false
        val listenerSlot = slot<InAppMessageResponseListener>()
        every { mockClient.showInAppMessages(activity, any(), capture(listenerSlot)) } returns billingClientOKResult

        wrapper.showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList()) {
            error("Unexpected subscription status change")
        }

        assertThat(listenerSlot.captured).isNotNull
        val purchaseToken = null
        assertVerboseLog(BillingStrings.BILLING_INAPP_MESSAGE_NONE) {
            listenerSlot.captured.onInAppMessageResponse(
                InAppMessageResult(InAppMessageResponseCode.NO_ACTION_NEEDED, purchaseToken)
            )
        }
    }

    @Test
    fun `showing inapp messages handles inapp messages listener response correctly when subscription updated`() {
        val activity = mockk<Activity>()
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns false
        val listenerSlot = slot<InAppMessageResponseListener>()
        every { mockClient.showInAppMessages(activity, any(), capture(listenerSlot)) } returns billingClientOKResult

        var subscriptionStatusChanged = false
        wrapper.showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList()) {
            subscriptionStatusChanged = true
        }

        assertThat(listenerSlot.captured).isNotNull
        val purchaseToken = null
        assertDebugLog(BillingStrings.BILLING_INAPP_MESSAGE_UPDATE) {
            listenerSlot.captured.onInAppMessageResponse(
                InAppMessageResult(InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED, purchaseToken)
            )
        }
        assertThat(subscriptionStatusChanged).isTrue
    }

    @Test
    fun `showing inapp messages does not show inapp messages when the Activity is finishing`() {
        val activity = mockk<Activity>()
        every { activity.isFinishing } returns true
        every { activity.isDestroyed } returns false
        every { mockClient.showInAppMessages(activity, any(), any()) } returns billingClientOKResult

        wrapper.showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList()) {
            error("Unexpected subscription status change")
        }

        verify(exactly = 0) { mockClient.showInAppMessages(activity, any(), any()) }
    }

    @Test
    fun `showing inapp messages does not show inapp messages when the Activity is destroyed`() {
        val activity = mockk<Activity>()
        every { activity.isFinishing } returns false
        every { activity.isDestroyed } returns true
        every { mockClient.showInAppMessages(activity, any(), any()) } returns billingClientOKResult

        wrapper.showInAppMessagesIfNeeded(activity, InAppMessageType.values().toList()) {
            error("Unexpected subscription status change")
        }

        verify(exactly = 0) { mockClient.showInAppMessages(activity, any(), any()) }
    }

    // endregion inapp messages

    // region BILLING_UNAVAILABLE

    @Test
    fun `BILLING_UNAVAILABLE errors are forwarded to billing client calls`() {
        every { mockClient.isReady } returns false

        var receivedError: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "abc",
            onSuccess = {
                error("Unexpected success")
            },
            onError = { error ->
                receivedError = error
            }
        )
        val billingResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            .setDebugMessage(IN_APP_BILLING_LESS_THAN_3_ERROR_MESSAGE)
            .build()

        billingClientStateListener!!.onBillingSetupFinished(billingResult)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    // region Multi-line subscriptions
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `can make a multi-line subscription purchase`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val productId2 = "productId2"
        val storeProduct2 = createStoreProductWithoutOffers(productId = productId2)
        val optionId = "base_plan_1"
        val productDetails = createMockProductDetailsNoOffers()
        val purchasingData = GooglePurchasingData.Subscription(
            productId = productDetails.productId,
            optionId = optionId,
            productDetails = productDetails,
            token = "mock-subscription-offer-token",
            billingPeriod = mockk(),
            addOnProducts = listOf(storeProduct2.purchasingData as GooglePurchasingData),
        )

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            purchasingData,
            mockReplaceSkuInfo(),
            PresentedOfferingContext("offering_a"),
        )

        verify {
            mockClient.launchBillingFlow(
                eq(mockActivity),
                any()
            )
        }

        assertThat(wrapper.purchaseContext.size).isEqualTo(1)
        val purchaseContext = wrapper.purchaseContext[purchasingData.productId]
        assertThat(purchaseContext).isNotNull
        assertThat(purchaseContext?.productType).isEqualTo(ProductType.SUBS)
        assertThat(purchaseContext?.presentedOfferingContext).isEqualTo(PresentedOfferingContext("offering_a"))
        assertThat(purchaseContext?.selectedSubscriptionOptionId).isEqualTo(optionId)
        assertThat(purchaseContext?.replacementMode).isEqualTo(GoogleReplacementMode.CHARGE_FULL_PRICE)

        val subscriptionOptionIdForProductIDs = purchaseContext?.subscriptionOptionIdForProductIDs
        assertThat(subscriptionOptionIdForProductIDs).isNotNull
        assertThat(subscriptionOptionIdForProductIDs!!.size).isEqualTo(2)
        assertThat(subscriptionOptionIdForProductIDs[productDetails.productId]).isEqualTo(optionId)
        assertThat(subscriptionOptionIdForProductIDs[productId2]).isEqualTo("mock-base-plan-id:mock-offer-id")
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `properly sets billingFlowParams for subscription purchase with add-ons`() {
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

        val replacementModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setSubscriptionReplacementMode(capture(replacementModeSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val isPersonalizedPriceSlot = slot<Boolean>()
        every {
            mockBuilder.setIsOfferPersonalized(capture(isPersonalizedPriceSlot))
        } returns mockBuilder

        val productId = "productId1"
        val productId2 = "productId2"

        val upgradeInfo = mockReplaceSkuInfo()
        val productDetails = mockProductDetails(productId = productId, type = subsGoogleProductType)
        val purchasingData = GooglePurchasingData.Subscription(
            productId = productDetails.productId,
            optionId = "base_plan",
            productDetails = productDetails,
            token = "mock-subscription-offer-token",
            billingPeriod = mockk(),
            addOnProducts = listOf(
                createStoreProductWithoutOffers(productId = productId2).purchasingData as GooglePurchasingData
            ),
        )
        val isPersonalizedPrice = true

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(mockActivity), capture(slot))
        } answers {
            val capturedProductDetailsParams = productDetailsParamsSlot.captured

            assertThat(capturedProductDetailsParams.size).isEqualTo(2)
            assertThat(capturedProductDetailsParams[0].zza().productId).isEqualTo(productId)
            assertThat(capturedProductDetailsParams[0].zza().productType).isEqualTo(subsGoogleProductType)
            assertThat(capturedProductDetailsParams[1].zza().productId).isEqualTo(productId2)
            assertThat(capturedProductDetailsParams[1].zza().productType).isEqualTo(subsGoogleProductType)
            assertThat(upgradeInfo.oldPurchase.purchaseToken).isEqualTo(oldPurchaseTokenSlot.captured)
            assertThat((upgradeInfo.replacementMode as GoogleReplacementMode?)?.playBillingClientMode).isEqualTo(replacementModeSlot.captured)

            assertThat(isPersonalizedPrice).isEqualTo(isPersonalizedPriceSlot.captured)
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            purchasingData,
            upgradeInfo,
            null,
            isPersonalizedPrice
        )
    }
    // endregion

    // endregion

    // region queryPurchaseHistoryAsync with PurchaseHistoryManager tests

    @Test
    fun `queryPurchaseHistoryAsync with AIDL for INAPP uses PurchaseHistoryManager`() = runTest {
        setupBillingWrapper(isAIDLEnabled = true)

        val mockTransactions = listOf(
            mockk<StoreTransaction>().apply {
                every { productIds } returns listOf("inapp1")
                every { purchaseToken } returns "token1"
            }
        )

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns true

        coEvery {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        } returns mockTransactions

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } just Runs

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        assertThat(receivedTransactions).isEqualTo(mockTransactions)
        assertThat(receivedError).isNull()

        coVerify(exactly = 1) {
            mockPurchaseHistoryManager.connect()
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
            mockPurchaseHistoryManager.disconnect()
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync with AIDL for INAPP falls back to billing library when query exception occurs`() = runTest {
        assumeRunningBc8()

        setupBillingWrapper(isAIDLEnabled = true)

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns true

        coEvery {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        } throws RuntimeException("Test exception")

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } just Runs

        val fallbackPurchases = listOf(stubGooglePurchase(productIds = listOf("fallback_product")))
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            fallbackPurchases
        )

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        assertThat(receivedError).isNull()
        assertThat(receivedTransactions).isNotNull
        assertThat(receivedTransactions?.size).isEqualTo(1)
        assertThat(receivedTransactions?.first()?.productIds).contains("fallback_product")

        coVerify(exactly = 1) {
            mockPurchaseHistoryManager.connect()
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        }
        coVerify {
            mockPurchaseHistoryManager.disconnect()
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync with AIDL for INAPP disconnects even when disconnect fails and fallback works`() = runTest {
        assumeRunningBc8()

        setupBillingWrapper(isAIDLEnabled = true)

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns true

        coEvery {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        } throws RuntimeException("Query exception")

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } throws RuntimeException("Disconnect exception")

        // Mock the fallback to return successful results
        val fallbackPurchases = listOf(stubGooglePurchase(productIds = listOf("fallback_product")))
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            fallbackPurchases
        )


        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        // Fallback should work even when disconnect fails
        assertThat(receivedError).isNull()
        assertThat(receivedTransactions).isNotNull
        assertThat(receivedTransactions?.size).isEqualTo(1)

        coVerify(exactly = 2) {
            mockPurchaseHistoryManager.disconnect()
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync for AIDL for INAPP returns empty list successfully`() = runTest {
        setupBillingWrapper(isAIDLEnabled = true)

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns true

        coEvery {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        } returns emptyList()

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } just Runs

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        assertThat(receivedTransactions).isNotNull
        assertThat(receivedTransactions).isEmpty()
        assertThat(receivedError).isNull()

        coVerify(exactly = 1) {
            mockPurchaseHistoryManager.connect()
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
            mockPurchaseHistoryManager.disconnect()
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync with AIDL does fallback when connection fails`() = runTest {
        assumeRunningBc8()

        setupBillingWrapper(isAIDLEnabled = true)

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns false

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } just Runs

        // Mock the fallback to return successful results
        val fallbackPurchases = listOf(stubGooglePurchase(productIds = listOf("fallback_product")))
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            fallbackPurchases
        )

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        // Fallback should work even when disconnect fails
        assertThat(receivedError).isNull()
        assertThat(receivedTransactions).isNotNull
        assertThat(receivedTransactions?.size).isEqualTo(1)

        coVerify(exactly = 1) {
            mockPurchaseHistoryManager.connect()
        }
        coVerify(exactly = 0) {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync with AIDL fallback handles billing library errors`() = runTest {
        assumeRunningBc8()

        setupBillingWrapper(isAIDLEnabled = true)

        coEvery {
            mockPurchaseHistoryManager.connect()
        } returns true

        coEvery {
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        } throws RuntimeException("Test exception")

        coEvery {
            mockPurchaseHistoryManager.disconnect()
        } just Runs

        // Mock the fallback to return error from billing library
        val billingError = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult()
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingError,
            emptyList(),
            emptyList()
        )

        var receivedTransactions: List<StoreTransaction>? = null
        var receivedError: PurchasesError? = null

        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.INAPP,
            onReceivePurchaseHistory = { receivedTransactions = it },
            onReceivePurchaseHistoryError = { receivedError = it }
        )

        // Wait for coroutine to complete
        testScope.advanceUntilIdle()

        // Should return error from the fallback
        assertThat(receivedTransactions).isNull()
        assertThat(receivedError).isNotNull
        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)

        coVerify(exactly = 1) {
            mockPurchaseHistoryManager.connect()
            mockPurchaseHistoryManager.queryAllPurchaseHistory(any())
        }
        // Disconnect is called twice: once in finally block, once in catch block
        coVerify(exactly = 2) {
            mockPurchaseHistoryManager.disconnect()
        }
    }

    // endregion

    private fun mockPurchaseRecordWrapper(): StoreTransaction {
        val oldPurchase = stubGooglePurchase(
            productIds = listOf("product_b"),
            purchaseToken = "atoken"
        )

        return oldPurchase.toStoreTransaction(productType = ProductType.SUBS)
    }

    private fun mockReplaceSkuInfo(): ReplaceProductInfo {
        val oldPurchase = mockPurchaseRecordWrapper()
        return ReplaceProductInfo(oldPurchase, GoogleReplacementMode.CHARGE_FULL_PRICE)
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
        val capturedConsumeResponseListener = slot<ConsumeResponseListener>()
        val capturedConsumeParams = slot<ConsumeParams>()

        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }

    private fun createStoreProductWithoutOffers(
        productId: String? = null
    ): StoreProduct {
        val productDetails = if (productId != null) {
            mockProductDetails(productId = productId)
        } else {
            createMockProductDetailsNoOffers()
        }
        return productDetails.toStoreProduct(
            productDetails.subscriptionOfferDetails!!
        )!!
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
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(any(), any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(any(), any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackProductDetailsNotSupported(any(), any())
        } just Runs
        every { mockDiagnosticsTracker.trackGoogleBillingStartConnection() } just runs
        every { mockDiagnosticsTracker.trackGoogleBillingSetupFinished(any(), any(), any()) } just runs
        every { mockDiagnosticsTracker.trackGoogleBillingServiceDisconnected() } just runs
        every { mockDiagnosticsTracker.trackGooglePurchaseStarted(any(), any(), any(), any()) } just runs
        every { mockDiagnosticsTracker.trackGooglePurchaseUpdateReceived(any(), any(), any(), any()) } just runs
    }

    private fun assumeRunningBc8() {
        assumeTrue(!BuildConfig.BILLING_CLIENT_VERSION.startsWith("7.") )
    }
}
