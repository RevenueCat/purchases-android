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
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.google.toRevenueCatProductType
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.stubSkuDetails
import io.mockk.Runs
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperBC4Test {
    private var onConnectedCalled: Boolean = false
    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var billingClientPurchaseHistoryListener: PurchaseHistoryResponseListener? = null
    private var handler: Handler = mockk()
    private var mockDeviceCache: DeviceCache = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<SkuDetails>

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private val appUserId = "jerry"
    private var mockActivity = mockk<Activity>()

    @Before
    fun setup() {
        setupRunnables()

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

        val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
                capture(billingClientPurchaseHistoryListenerSlot)
            )
        } answers {
            billingClientPurchaseHistoryListener = billingClientPurchaseHistoryListenerSlot.captured
        }

        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs

        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } just Runs

        every {
            mockClient.isReady
        } returns false andThen true

        mockDetailsList = listOf(stubSkuDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache)
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
    fun canMakeAPurchase() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val skuDetails = stubSkuDetails(productId = "product_a")

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            skuDetails.toStoreProduct(),
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
    fun properlySetsBillingFlowParams() {
        mockkStatic(BillingFlowParams::class)
        mockkStatic(BillingFlowParams.SubscriptionUpdateParams::class)

        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        val skuDetailsSlot = slot<SkuDetails>()
        every {
            mockBuilder.setSkuDetails(capture(skuDetailsSlot))
        } returns mockBuilder

        val mockSubscriptionUpdateParamsBuilder =
            mockk<BillingFlowParams.SubscriptionUpdateParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
        } returns mockSubscriptionUpdateParamsBuilder

        val oldSkuPurchaseTokenSlot = slot<String>()
        every {
            mockSubscriptionUpdateParamsBuilder.setOldSkuPurchaseToken(capture(oldSkuPurchaseTokenSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val prorationModeSlot = slot<Int>()
        every {
            mockSubscriptionUpdateParamsBuilder.setReplaceSkusProrationMode(capture(prorationModeSlot))
        } returns mockSubscriptionUpdateParamsBuilder

        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val upgradeInfo = mockReplaceSkuInfo()
        val skuDetails = stubSkuDetails(productId = sku, type = skuType)

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(mockActivity), capture(slot))
        } answers {
            val capturedSkuDetails = skuDetailsSlot.captured

            assertThat(sku).isEqualTo(capturedSkuDetails.sku)
            assertThat(skuType).isEqualTo(capturedSkuDetails.type)

            assertThat(upgradeInfo.oldPurchase.purchaseToken).isEqualTo(oldSkuPurchaseTokenSlot.captured)
            assertThat(upgradeInfo.prorationMode).isEqualTo(prorationModeSlot.captured)
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            skuDetails.toStoreProduct(),
            upgradeInfo,
            null
        )
    }

    @Test
    fun `obfuscatedAccountId is set for non-transfer purchases`() {
        val mockBuilder = setUpForObfuscatedAccountIDTests()

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            stubSkuDetails(productId = "product_a").toStoreProduct(),
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

        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            stubSkuDetails(productId = "product_a").toStoreProduct(),
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockBuilder.setObfuscatedAccountId(any())
        }

        clearStaticMockk(BillingFlowParams::class)
    }

    @Test
    fun whenSkuDetailsIsNullPassAnEmptyListToTheListener() {
        mockNullSkuDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<StoreProduct>? = null
        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                fail("shouldn't be an error")
            })
        wrapper.onBillingSetupFinished(billingClientOKResult)
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero()
    }

    @Test
    fun `getting all purchases gets both subs and inapps`() {
        val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
                capture(billingClientPurchaseHistoryListenerSlot)
            )
        } answers {
            billingClientPurchaseHistoryListenerSlot.captured.onPurchaseHistoryResponse(
                billingClientOKResult,
                listOf(stubPurchaseHistoryRecord())
            )
        }

        var receivedPurchases = listOf<StoreTransaction>()
        wrapper.queryAllPurchases("appUserID", {
            receivedPurchases = it
        }, { fail("Shouldn't be error") })

        assertThat(receivedPurchases.size).isNotZero()

        verify(exactly = 1) {
            mockClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, any())
        }

        verify(exactly = 1) {
            mockClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, any())
        }
    }

    @Test
    fun `when querying anything and billing client returns an empty list, returns an empty list`() {
        mockQueryPurchasesAsyncResult(null, billingClientOKResult, listOf())

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
        val type = BillingClient.SkuType.INAPP
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.INAPP,
            billingClientOKResult,
            listOf(purchase)
        )
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.SUBS,
            billingClientOKResult,
            listOf())

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
        assertThat(purchaseWrapper!!.type).isEqualTo(type.toRevenueCatProductType())
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.skus[0]).isEqualTo(sku)
    }

    @Test
    fun `when querying SUBS result is created properly`() {
        val token = "token"
        val type = BillingClient.SkuType.SUBS
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockQueryPurchasesAsyncResult(BillingClient.SkuType.SUBS, billingClientOKResult, listOf(purchase))
        mockQueryPurchasesAsyncResult(BillingClient.SkuType.INAPP, billingClientOKResult, emptyList())

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
        assertThat(purchaseWrapper!!.type).isEqualTo(type.toRevenueCatProductType())
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.skus[0]).isEqualTo(sku)
    }

    @Test
    fun `Presented offering is properly forwarded`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val skuDetails = stubSkuDetails(productId = "product_a")

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)
        wrapper.makePurchaseAsync(
            mockActivity,
            appUserId,
            skuDetails.toStoreProduct(),
            mockReplaceSkuInfo(),
            "offering_a"
        )

        val purchases = listOf(stubGooglePurchase(productIds = listOf("product_a")))

        val slot = slot<List<StoreTransaction>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne()
        assertThat(slot.captured[0].presentedOfferingIdentifier).isEqualTo("offering_a")
    }

    @Test
    fun `Getting subscriptions type`() {
        val inAppToken = "inAppToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.INAPP,
            billingClientOKResult,
            getMockedPurchaseList(inAppToken)
        )

        val subsToken = "subsToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.SUBS,
            billingClientOKResult,
            getMockedPurchaseList(subsToken)

        )

        wrapper.getPurchaseType(subsToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.SUBS)
        }
    }

    @Test
    fun `Getting INAPPs type`() {
        val inAppToken = "inAppToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.INAPP,
            billingClientOKResult,
            getMockedPurchaseList(inAppToken)
        )

        val subToken = "subToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.SUBS,
            billingClientOKResult,
            getMockedPurchaseList(subToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.INAPP)
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub and inapps response not OK`() {
        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val subToken = "subToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.SUBS,
            errorResult,
            getMockedPurchaseList(subToken)
        )

        val inAppToken = "abcd"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.INAPP,
            errorResult,
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub not found and inapp responses not OK`() {
        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val inAppPurchaseToken = "inAppToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.INAPP,
            errorResult,
            getMockedPurchaseList(inAppPurchaseToken)
        )

        val subPurchaseToken = "subToken"
        mockQueryPurchasesAsyncResult(
            BillingClient.SkuType.SUBS,
            billingClientOKResult,
            getMockedPurchaseList(subPurchaseToken)
        )

        wrapper.getPurchaseType(inAppPurchaseToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
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

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

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

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
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

        wrapper.consumeAndSave(true, googlePurchaseWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
            token
        )

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

        wrapper.consumeAndSave(true, historyRecordWrapper)

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
            token
        )

        verify(exactly = 0) {
            mockDeviceCache.addSuccessfullyPostedToken(token)
        }
    }

    @Test
    fun `product type defaults to INAPP when querying sku details`() {
        val slot = slot<SkuDetailsParams>()
        every {
            mockClient.querySkuDetailsAsync(
                capture(slot),
                any()
            )
        } just Runs

        val productIDs = setOf("product_a")

        wrapper.querySkuDetailsAsync(
            ProductType.UNKNOWN,
            productIDs,
            {
                this@BillingWrapperBC4Test.storeProducts = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(slot.isCaptured).isTrue()
        assertThat(slot.captured.skuType).isEqualTo(BillingClient.SkuType.INAPP)
    }

    @Test
    fun `querySkuDetails filters empty skus before querying BillingClient`() {
        val skuSet = setOf("abcd", "", "1", "")

        val slot = slot<SkuDetailsParams>()
        every {
            mockClient.querySkuDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            skuSet,
            {}, {
                fail("shouldn't be an error")
            })

        assertThat(slot.captured.skusList).isEqualTo(skuSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `querySkuDetails with empty list returns empty list and does not query BillingClient`() {
        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            emptySet(),
            {
                assertThat(it.isEmpty())
            }, {
                fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.querySkuDetailsAsync(any(), any())
        }
    }

    @Test
    fun `querySkuDetails with only empty skus returns empty list and does not query BillingClient`() {
        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            setOf("", ""),
            {
                assertThat(it.isEmpty())
            }, {
                fail("shouldn't be an error")
            })

        verify(exactly = 0) {
            mockClient.querySkuDetailsAsync(any(), any())
        }
    }

    @Test
    fun `querySkuDetailsAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
            slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
        }

        wrapper.querySkuDetailsAsync(
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
    fun `querySkuDetailsAsync only calls one response when BillingClient responds twice in separate threads`() {
        var numCallbacks = 0

        val slot = slot<SkuDetailsResponseListener>()
        val lock = CountDownLatch(2)
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()
        }

        wrapper.querySkuDetailsAsync(
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
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                numCallbacks++
            }, {
                fail("shouldn't be an error")
            })

        assertThat(numCallbacks == 1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        val lock = CountDownLatch(2)
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
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
            BillingClient.SkuType.SUBS,
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

    private fun mockQueryPurchasesAsyncResult(
        @BillingClient.SkuType skuType: String?,
        result: BillingResult,
        purchases: List<Purchase>
    ) {
        val queryPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                skuType ?: any(),
                capture(queryPurchasesListenerSlot)
            )
        } answers {
            queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                result,
                purchases
            )
        }
    }

    private fun getMockedPurchaseList(purchaseToken: String): List<Purchase> {
        return listOf(mockk(
            relaxed = true
        ) {
            every { this@mockk.purchaseToken } returns purchaseToken
        })
    }

    private fun mockNullSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
        }
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockPurchaseHistoryRecordWrapper(): StoreTransaction {
        val oldPurchase = stubPurchaseHistoryRecord(
            productIds = listOf("product_b"),
            purchaseToken = "atoken"
        )

        return oldPurchase.toStoreTransaction(type = ProductType.SUBS)
    }

    private fun mockReplaceSkuInfo(): ReplaceSkuInfo {
        val oldPurchase = mockPurchaseHistoryRecordWrapper()
        return ReplaceSkuInfo(oldPurchase, BillingFlowParams.ProrationMode.DEFERRED)
    }

    private fun getMockedPurchaseWrapper(
        sku: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false
    ): StoreTransaction {
        val p = stubGooglePurchase(
            productIds = listOf(sku),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return p.toStoreTransaction(productType, offeringIdentifier)
    }

    private fun getMockedPurchaseHistoryRecordWrapper(
        sku: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(sku),
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
            mockBuilder.setSkuDetails(any())
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

    private fun mockStandardSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(billingClientOKResult, mockDetailsList)
        }
    }

    private fun setupRunnables() {
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
}
