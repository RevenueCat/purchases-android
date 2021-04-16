package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Context
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
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
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
import java.util.ArrayList

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperTest {
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

    private var productDetailsList: List<ProductDetails>? = null

    private var skuDetailsResponseCalled = 0

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private val appUserId = "jerry"

    @Before
    fun setup() {
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }

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
        } returns true

        mockDetailsList = listOf(stubSkuDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache)
        wrapper.purchasesUpdatedListener = mockPurchasesListener
        onConnectedCalled = false
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }
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
    fun defersCallingSkuQueryUntilConnected() {
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        val productIDs = setOf("product_a")

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            productIDs,
            {
                this@BillingWrapperTest.productDetailsList = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(productDetailsList).`as`("SKUDetailsList is null").isNull()

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(productDetailsList).`as`("SKUDetailsList is not null").isNotNull
    }

    @Test
    fun canDeferMultipleCalls() {
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        val productIDs = setOf("product_a")

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            productIDs,
            {
                this@BillingWrapperTest.skuDetailsResponseCalled += 1
            },
            {
                fail("shouldn't be an error")
            })
        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            productIDs,
            {
                this@BillingWrapperTest.skuDetailsResponseCalled += 1
            },
            {
                fail("shouldn't be an error")
            })
        assertThat(skuDetailsResponseCalled).isZero()

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(skuDetailsResponseCalled).isEqualTo(2)
    }

    @Test
    fun makingARequestTriggersAConnectionAttempt() {
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            setOf("product_a"),
            {
                // DO NOTHING
            }, {
                // DO NOTHING
            })

        verify(exactly = 2) {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    @Test
    fun canMakeAPurchase() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val skuDetails = stubSkuDetails(productId = "product_a")

        val activity: Activity = mockk()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper.makePurchaseAsync(
            activity,
            appUserId,
            skuDetails.toProductDetails(),
            mockReplaceSkuInfo(),
            "offering_a"
        )

        verify {
            mockClient.launchBillingFlow(
                eq(activity),
                any()
            )
        }
    }

    @Test
    fun properlySetsBillingFlowParams() {
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val upgradeInfo = mockReplaceSkuInfo()
        val activity: Activity = mockk()
        val skuDetails = stubSkuDetails(productId = sku, type = skuType)

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(activity), capture(slot))
        } answers {
            val params = slot.captured
            assertThat(sku).isEqualTo(params.sku)
            assertThat(skuType).isEqualTo(params.skuType)
            assertThat(upgradeInfo.oldPurchase.sku).isEqualTo(params.oldSku)
            assertThat(upgradeInfo.oldPurchase.purchaseToken).isEqualTo(params.oldSkuPurchaseToken)
            assertThat(upgradeInfo.prorationMode).isEqualTo(params.replaceSkusProrationMode)
            billingClientOKResult
        }

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper.makePurchaseAsync(
            activity,
            appUserId,
            skuDetails.toProductDetails(),
            upgradeInfo,
            null
        )
    }

    @Test
    fun `obfuscatedAccountId is set for non-transfer purchases`() {
        val mockBuilder = setUpForObfuscatedAccountIDTests()

        wrapper.makePurchaseAsync(
            mockk(),
            appUserId,
            stubSkuDetails(productId = "product_a").toProductDetails(),
            null,
            null
        )

        verify {
            mockBuilder.setObfuscatedAccountId(appUserId.sha256())
        }

        clearStaticMockk(BillingFlowParams::class)
    }

    @Test
    fun `obfuscatedAccountId is not set for transfer purchases`() {
        val mockBuilder = setUpForObfuscatedAccountIDTests()

        wrapper.makePurchaseAsync(
            mockk(),
            appUserId,
            stubSkuDetails(productId = "product_a").toProductDetails(),
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

        val activity: Activity = mockk()
        val skuDetails = stubSkuDetails(productId = "product_a")

        wrapper.makePurchaseAsync(
            activity,
            appUserId,
            skuDetails.toProductDetails(),
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 0) {
            mockClient.launchBillingFlow(eq(activity), any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        verify(exactly = 1) {
            mockClient.launchBillingFlow(eq(activity), any())
        }
    }

    @Test
    fun callsLaunchFlowFromMainThread() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        every { mockClient.isReady } returns false

        val skuDetails = stubSkuDetails(productId = "product_a")

        val activity: Activity = mockk()

        wrapper.makePurchaseAsync(
            activity,
            appUserId,
            skuDetails.toProductDetails(),
            mockReplaceSkuInfo(),
            null
        )

        verify(exactly = 2) {
            handler.post(any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        verify(exactly = 3) {
            handler.post(any())
        }
    }

    @Test
    fun purchasesUpdatedCallsAreForwarded() {
        val purchases = listOf(stubGooglePurchase())
        val slot = slot<List<PurchaseDetails>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        every {
            mockClient.queryPurchases(BillingClient.SkuType.SUBS)
        } returns Purchase.PurchasesResult(billingClientOKResult, purchases)

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne()
    }

    @Test
    fun `purchasesUpdatedCalls are forwarded with empty list if result is ok but with a null purchase`() {
        val slot = slot<List<PurchaseDetails>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, null)

        assertThat(slot.isCaptured).isTrue()
        assertThat(slot.captured.isEmpty()).isTrue()
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
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        var successCalled = false
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                successCalled = true
            },
            {
                fail("shouldn't go to on error")
            }
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            billingClientOKResult,
            ArrayList()
        )
        assertThat(successCalled).isTrue()
    }

    @Test
    fun queryHistoryNotCalledIfNotOK() {
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                fail("should go to on error")
            },
            {
                assertThat(it.code).isEqualTo(PurchasesErrorCode.PurchaseNotAllowedError)
                errorCalled = true
            }
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            ArrayList()
        )
        assertThat(errorCalled).isTrue()
    }

    @Test
    fun canConsumeAToken() {
        val token = "mockToken"

        val capturingSlot = slot<ConsumeParams>()
        every {
            mockClient.consumeAsync(capture(capturingSlot), any())
        } just Runs

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper.consumePurchase(token) { _, _ -> }

        assertThat(capturingSlot.isCaptured).isTrue()
        assertThat(capturingSlot.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun removingListenerDisconnects() {
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true

        wrapper.purchasesUpdatedListener = null
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
    fun whenSkuDetailsIsNullPassAnEmptyListToTheListener() {
        mockNullSkuDetailsResponse()

        val productIDs = setOf("product_a")

        var receivedList: List<ProductDetails>? = null
        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                fail("shouldn't be an error")
            })
        wrapper.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero()
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

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            setOf("product_a"),
            {},
            {
                fail("shouldn't be an error")
            })

        wrapper.purchasesUpdatedListener = null
        wrapper.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
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

        var receivedPurchases = listOf<PurchaseDetails>()
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
    fun `on successfully connected billing client, listener is called`() {
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        assertThat(onConnectedCalled).isTrue()
    }

    @Test
    fun `when querying anything and billing client returns a null list, returns an empty list`() {
        every {
            mockClient.queryPurchases(any())
        } returns Purchase.PurchasesResult(billingClientOKResult, null)

        var purchasesByHashedToken: Map<String, PurchaseDetails>? = null
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
            productId = sku
        )

        every {
            mockClient.queryPurchases(BillingClient.SkuType.INAPP)
        } returns Purchase.PurchasesResult(billingClientOKResult, listOf(purchase))

        every {
            mockClient.queryPurchases(BillingClient.SkuType.SUBS)
        } returns Purchase.PurchasesResult(billingClientOKResult, emptyList())

        var purchasesByHashedToken: Map<String, PurchaseDetails>? = null
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
        assertThat(purchaseWrapper!!.type).isEqualTo(type.toProductType())
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.sku).isEqualTo(sku)
    }

    @Test
    fun `when querying SUBS result is created properly`() {
        val resultCode = BillingClient.BillingResponseCode.OK
        val token = "token"
        val type = BillingClient.SkuType.SUBS
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productId = sku
        )

        every {
            mockClient.queryPurchases(BillingClient.SkuType.SUBS)
        } returns Purchase.PurchasesResult(resultCode.buildResult(), listOf(purchase))

        every {
            mockClient.queryPurchases(BillingClient.SkuType.INAPP)
        } returns Purchase.PurchasesResult(resultCode.buildResult(), emptyList())

        var purchasesByHashedToken: Map<String, PurchaseDetails>? = null
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
        assertThat(purchaseWrapper!!.type).isEqualTo(type.toProductType())
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.sku).isEqualTo(sku)
    }

    @Test
    fun `Presented offering is properly forwarded`() {
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns billingClientOKResult

        val skuDetails = stubSkuDetails(productId = "product_a")

        val activity: Activity = mockk()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper.makePurchaseAsync(
            activity,
            appUserId,
            skuDetails.toProductDetails(),
            mockReplaceSkuInfo(),
            "offering_a"
        )

        val purchases = listOf(stubGooglePurchase(productId = "product_a"))

        val slot = slot<List<PurchaseDetails>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(billingClientOKResult, purchases)

        assertThat(slot.captured.size).isOne()
        assertThat(slot.captured[0].presentedOfferingIdentifier).isEqualTo("offering_a")
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

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper.acknowledge(token) { _, _ -> }

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue()
        assertThat(capturedAcknowledgePurchaseParams.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun `Getting subscriptions type`() {
        every {
            mockClient.queryPurchases(BillingClient.SkuType.INAPP)
        } returns Purchase.PurchasesResult(
            billingClientOKResult, listOf(mockk(
                relaxed = true
            ) {
                every { this@mockk.purchaseToken } returns "inapp"
            })
        )
        every {
            mockClient.queryPurchases(BillingClient.SkuType.SUBS)
        } returns Purchase.PurchasesResult(
            billingClientOKResult, listOf(mockk(
                relaxed = true
            ) {
                every { this@mockk.purchaseToken } returns "sub"
            })
        )

        val purchaseType = wrapper.getPurchaseType("sub")
        assertThat(purchaseType).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `Getting INAPPs type`() {
        every {
            mockClient.queryPurchases(BillingClient.SkuType.INAPP)
        } returns Purchase.PurchasesResult(
            billingClientOKResult, listOf(mockk(
                relaxed = true
            ) {
                every { this@mockk.purchaseToken } returns "inapp"
            })
        )
        every {
            mockClient.queryPurchases(BillingClient.SkuType.SUBS)
        } returns Purchase.PurchasesResult(
            billingClientOKResult, listOf(mockk(
                relaxed = true
            ) {
                every { this@mockk.purchaseToken } returns "sub"
            })
        )

        val purchaseType = wrapper.getPurchaseType("inapp")
        assertThat(purchaseType).isEqualTo(ProductType.INAPP)
    }

    @Test
    fun `findPurchaseInPurchaseHistory works`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = stubPurchaseHistoryRecord(productId = sku)

        var recordFound: PurchaseDetails? = null
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
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
        )
        assertThat(recordFound).isNotNull
        assertThat(recordFound!!.sku).isEqualTo(purchaseHistoryRecord.sku)
        assertThat(recordFound!!.purchaseTime).isEqualTo(purchaseHistoryRecord.purchaseTime)
        assertThat(recordFound!!.purchaseToken).isEqualTo(purchaseHistoryRecord.purchaseToken)
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error if not found`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = mockk<PurchaseHistoryRecord>(relaxed = true).also {
            every { it.sku } returns sku + "somethingrandom"
        }

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

        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue()
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

        assertThat(capturedAcknowledgeResponseListener.isCaptured).isTrue()
        capturedAcknowledgeResponseListener.captured.onAcknowledgePurchaseResponse(
            billingClientOKResult
        )

        assertThat(capturedAcknowledgePurchaseParams.isCaptured).isTrue()
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

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue()
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

        assertThat(capturedConsumeResponseListener.isCaptured).isTrue()
        capturedConsumeResponseListener.captured.onConsumeResponse(
            billingClientOKResult,
            token
        )

        assertThat(capturedConsumeParams.isCaptured).isTrue()
        val capturedConsumeParams = capturedConsumeParams.captured
        assertThat(capturedConsumeParams.purchaseToken).isEqualTo(token)
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
                this@BillingWrapperTest.productDetailsList = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(slot.isCaptured).isTrue()
        assertThat(slot.captured.skuType).isEqualTo(BillingClient.SkuType.INAPP)
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
    fun `do not query BillingClient for any empty skus`() {
        val skuSet = setOf("abcd", "", "1", "")

        val slot = slot<SkuDetailsParams>()
        every {
            mockClient.querySkuDetailsAsync(capture(slot), any())
        } just Runs

        wrapper.querySkuDetailsAsync(
            ProductType.SUBS,
            skuSet,
            {
                this@BillingWrapperTest.productDetailsList = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(slot.captured.skusList).isEqualTo(skuSet.filter { it.isNotEmpty() })
    }

    @Test
    fun `querySkuDetails with empty list returns empty list and does not query billingclient`() {
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
    fun `querySkuDetails with only empty skus returns empty list and does not query client`() {
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

    private fun mockPurchaseHistoryRecordWrapper(): PurchaseDetails {
        val oldPurchase = stubPurchaseHistoryRecord(
            productId = "product_b",
            purchaseToken = "atoken"
        )

        return oldPurchase.toRevenueCatPurchaseDetails(type = ProductType.SUBS)
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
    ): PurchaseDetails {
        val p = stubGooglePurchase(
            productId = sku,
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return p.toRevenueCatPurchaseDetails(productType, offeringIdentifier)
    }

    private fun getMockedPurchaseHistoryRecordWrapper(
        sku: String,
        purchaseToken: String,
        productType: ProductType
    ): PurchaseDetails {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productId = sku,
            purchaseToken = purchaseToken
        )

        return p.toRevenueCatPurchaseDetails(
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
}
