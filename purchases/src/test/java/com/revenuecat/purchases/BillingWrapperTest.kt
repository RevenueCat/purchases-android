//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsResponseListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
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

    private var mockPurchasesListener: BillingWrapper.PurchasesUpdatedListener = mockk()

    private var wrapper: BillingWrapper? = null

    private val mockDetailsList = ArrayList<SkuDetails>()

    private var skuDetailsList: List<SkuDetails>? = null

    private var skuDetailsResponseCalled = 0

    private fun setup() {
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

        every{
            mockClient.isReady
        } returns true

        val mockDetails: SkuDetails = mockk(relaxed = true)
        mockDetailsList.add(mockDetails)

        wrapper = BillingWrapper(mockClientFactory, handler)
        wrapper!!.purchasesUpdatedListener = mockPurchasesListener
        onConnectedCalled = false
        wrapper!!.stateListener = object : BillingWrapper.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }
    }

    @Test
    fun canBeCreated() {
        setup()
        assertThat(wrapper).`as`("Wrapper is not null").isNotNull
    }

    @Test
    fun callsBuildOnTheFactory() {
        setup()
        verify {
            mockClientFactory.buildClient(purchasesUpdatedListener!!)
        }
    }

    @Test
    fun connectsToPlayBilling() {
        setup()
        verify {
            mockClient.startConnection(billingClientStateListener!!)
        }
    }

    private fun mockStandardSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(BillingClient.BillingResponseCode.OK.buildResult(), mockDetailsList)
        }
    }

    @Test
    fun defersCallingSkuQueryUntilConnected() {
        setup()
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            {
                this@BillingWrapperTest.skuDetailsList = it
            }, {
                fail("shouldn't be an error")
            })

        assertThat(skuDetailsList).`as`("SKUDetailsList is null").isNull()

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(skuDetailsList).`as`("SKUDetailsList is not null").isNotNull
    }

    @Test
    fun canDeferMultipleCalls() {
        setup()
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            {
                this@BillingWrapperTest.skuDetailsResponseCalled += 1
            },
            {
                fail("shouldn't be an error")
            })
        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs
            , {
                this@BillingWrapperTest.skuDetailsResponseCalled += 1
            }, {
                fail("shouldn't be an error")
            })
        assertThat(skuDetailsResponseCalled).isZero()

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(skuDetailsResponseCalled).isEqualTo(2)
    }

    @Test
    fun makingARequestTriggersAConnectionAttempt() {
        setup()
        mockStandardSkuDetailsResponse()
        every { mockClient.isReady } returns false

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            listOf("product_a"),
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
        setup()
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_a"
            every { it.type } returns BillingClient.SkuType.SUBS
        }
        val oldSku = "product_b"

        val activity: Activity = mockk()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper!!.makePurchaseAsync(
            activity,
            "jerry",
            skuDetails,
            oldSku,
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
        setup()
        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSku = "product_b"
        val activity: Activity = mockk()
        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns sku
            every { it.type } returns skuType
        }

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(activity), capture(slot))
        } answers {
            val params = slot.captured
            assertThat(sku).isEqualTo(params.sku)
            assertThat(skuType).isEqualTo(params.skuType)
            assertThat(oldSku).isEqualTo(params.oldSku)
            assertThat(appUserID).isEqualTo(params.accountId)
            BillingClient.BillingResponseCode.OK.buildResult()
        }

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper!!.makePurchaseAsync(
            activity,
            appUserID,
            skuDetails,
            oldSku,
            null
        )
    }

    @Test
    fun defersBillingFlowIfNotConnected() {
        setup()

        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        every { mockClient.isReady } returns false

        val appUserID = "jerry"

        val activity: Activity = mockk()
        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_a"
            every { it.type } returns BillingClient.SkuType.SUBS
        }

        wrapper!!.makePurchaseAsync(
            activity,
            appUserID,
            skuDetails,
            "product_b",
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
        setup()

        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        every { mockClient.isReady } returns false

        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_a"
            every { it.type } returns BillingClient.SkuType.SUBS
        }
        val appUserID = "jerry"

        val oldSku = "product_b"

        val activity: Activity = mockk()

        wrapper!!.makePurchaseAsync(
            activity,
            appUserID,
            skuDetails,
            oldSku,
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
        setup()
        val purchases = listOf(mockk<Purchase>(relaxed = true))
        val slot = slot<List<PurchaseWrapper>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponseCode.OK.buildResult(), purchases)

        assertThat(slot.captured.size).isOne()
    }

    @Test
    fun purchasesUpdatedCallsAreForwardedWithEmptyIfOkNull() {
        setup()

        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any(), any())
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponseCode.OK.buildResult(), null)

        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(
                null,
                eq(BillingClient.BillingResponseCode.ERROR),
                any()
            )
        }
    }

    @Test
    fun purchaseUpdateFailedCalledIfNotOK() {
        setup()
        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any(), any())
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            null
        )
        verify(exactly = 0) {
            mockPurchasesListener.onPurchasesUpdated(any())
        }
        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any(), any())
        }
    }

    @Test
    fun queryHistoryCallsListenerIfOk() {
        setup()
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        var successCalled = false
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                successCalled = true
            },
            {
                fail("shouldn't go to on error")
            }
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponseCode.OK.buildResult(),
            ArrayList()
        )
        assertThat(successCalled).isTrue()
    }

    @Test
    fun queryHistoryNotCalledIfNotOK() {
        setup()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        var errorCalled = false
        wrapper!!.queryPurchaseHistoryAsync(
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
        setup()
        val token = "mockToken"

        val capturingSlot = slot<ConsumeParams>()
        every {
            mockClient.consumeAsync(capture(capturingSlot), any())
        } just Runs

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper!!.consumePurchase(token) { _, _ -> }

        assertThat(capturingSlot.isCaptured).isTrue()
        assertThat(capturingSlot.captured.purchaseToken).isEqualTo(token)
    }

    @Test
    fun removingListenerDisconnects() {
        setup()
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true

        wrapper!!.purchasesUpdatedListener = null
        verify {
            mockClient.endConnection()
        }
        assertThat(wrapper!!.purchasesUpdatedListener).isNull()
    }

    @Test
    fun whenSettingListenerStartConnection() {
        setup()
        verify {
            mockClient.startConnection(eq(wrapper!!))
        }
        assertThat(wrapper!!.purchasesUpdatedListener).isNotNull
    }

    @Test
    fun whenExecutingRequestAndThereIsNoListenerDoNotTryToStartConnection() {
        setup()
        every {
            mockClient.endConnection()
        } just Runs
        wrapper!!.purchasesUpdatedListener = null
        wrapper!!.consumePurchase("token") { _, _ -> }

        verify(exactly = 1) { // Just the original connection
            mockClient.startConnection(wrapper!!)
        }
    }

    @Test
    fun whenSkuDetailsIsNullPassAnEmptyListToTheListener() {
        setup()
        mockNullSkuDetailsResponse()

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        var receivedList: List<SkuDetails>? = null
        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs, {
                receivedList = it
            }, {
                fail("shouldn't be an error")
            })
        wrapper!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isZero()
    }

    @Test
    fun nullifyBillingClientAfterEndingConnection() {
        setup()
        every {
            mockClient.endConnection()
        } just Runs
        every {
            mockClient.isReady
        } returns true
        wrapper!!.purchasesUpdatedListener = null

        assertThat<BillingClient>(wrapper!!.billingClient).isNull()
    }

    @Test
    fun newBillingClientIsCreatedWhenSettingListener() {
        setup()
        wrapper!!.purchasesUpdatedListener = mockPurchasesListener

        assertThat<BillingClient>(wrapper!!.billingClient).isNotNull
    }

    @Test
    fun `calling close before setup finishes doesn't crash`() {
        setup()
        every {
            mockClient.isReady
        } returns false

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            listOf("product_a"),
            {},
            {
                fail("shouldn't be an error")
            })

        wrapper!!.purchasesUpdatedListener = null
        wrapper!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
    }

    @Test
    fun `calling close before purchase completes doesn't crash`() {
        setup()
        every {
            mockClient.isReady
        } returns false

        wrapper!!.purchasesUpdatedListener = null
        wrapper!!.onPurchasesUpdated(BillingClient.BillingResponseCode.DEVELOPER_ERROR.buildResult(), emptyList())
    }

    @Test
    fun `calling end connection before client is ready ends connection`() {
        setup()
        every {
            mockClient.isReady
        } returns false

        wrapper!!.purchasesUpdatedListener = null
        verify {
            mockClient.endConnection()
        }
    }

    @Test
    fun `getting all purchases gets both subs and inapps`() {
        setup()

        val billingClientPurchaseHistoryListenerSlot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
                capture(billingClientPurchaseHistoryListenerSlot)
            )
        } answers {
            billingClientPurchaseHistoryListenerSlot.captured.onPurchaseHistoryResponse(
                BillingClient.BillingResponseCode.OK.buildResult(),
                listOf(mockk(relaxed = true))
            )
        }

        var receivedPurchases = listOf<PurchaseWrapper>()
        wrapper!!.queryAllPurchases({
            receivedPurchases = it
        }, { fail("Shouldn't be error") })

        assertThat(receivedPurchases.size).isNotZero()

        verify (exactly = 1){
            mockClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, any())
        }

        verify (exactly = 1){
            mockClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, any())
        }
    }

    @Test
    fun `on successfully connected billing client, listener is called`() {
        setup()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        assertThat(onConnectedCalled).isTrue()
    }

    @Test
    fun `when querying INAPPs and there is no billing client, don't return anything`() {
        wrapper = BillingWrapper(mockClientFactory, handler)

        assertThat(wrapper!!.queryPurchases(BillingClient.SkuType.INAPP)).isNull()
    }

    @Test
    fun `when querying SUBs and there is no billing client, don't return anything`() {
        wrapper = BillingWrapper(mockClientFactory, handler)
        assertThat(wrapper!!.queryPurchases(BillingClient.SkuType.SUBS)).isNull()
    }

    @Test
    fun `when querying anything and billing client returns a null list, returns an empty list`() {
        setup()

        every {
            mockClient.queryPurchases(any())
        } returns Purchase.PurchasesResult(BillingClient.BillingResponseCode.OK.buildResult(), null)

        assertThat(wrapper!!.queryPurchases(BillingClient.SkuType.SUBS)!!.purchasesByHashedToken).isNotNull
    }

    @Test
    fun `when querying INAPPs result is created properly`() {
        setup()
        val resultCode = BillingClient.BillingResponseCode.OK
        val token = "token"
        val type = BillingClient.SkuType.INAPP
        val time = System.currentTimeMillis()
        val sku = "sku"
        val purchase = mockk<Purchase>().also {
            every { it.purchaseToken } returns token
            every { it.purchaseTime } returns time
            every { it.sku } returns sku
        }
        every {
            mockClient.queryPurchases(type)
        } returns Purchase.PurchasesResult(resultCode.buildResult(), listOf(purchase))
        val queryPurchasesResult = wrapper!!.queryPurchases(type)
        assertThat(queryPurchasesResult).isNotNull
        assertThat(queryPurchasesResult!!.responseCode).isEqualTo(resultCode)
        assertThat(queryPurchasesResult.isSuccessful()).isTrue()
        assertThat(queryPurchasesResult.purchasesByHashedToken.isNotEmpty()).isTrue()
        val purchaseWrapper = queryPurchasesResult.purchasesByHashedToken[token.sha1()]
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(type)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.sku).isEqualTo(sku)
    }

    @Test
    fun `when querying SUBS result is created properly`() {
        setup()
        val resultCode = BillingClient.BillingResponseCode.OK
        val token = "token"
        val type = BillingClient.SkuType.SUBS
        val time = System.currentTimeMillis()
        val sku = "sku"
        val purchase = mockk<Purchase>().also {
            every { it.purchaseToken } returns token
            every { it.purchaseTime } returns time
            every { it.sku } returns sku
        }
        every {
            mockClient.queryPurchases(type)
        } returns Purchase.PurchasesResult(resultCode.buildResult(), listOf(purchase))
        val queryPurchasesResult = wrapper!!.queryPurchases(type)
        assertThat(queryPurchasesResult).isNotNull
        assertThat(queryPurchasesResult!!.responseCode).isEqualTo(resultCode)
        assertThat(queryPurchasesResult.isSuccessful()).isTrue()
        assertThat(queryPurchasesResult.purchasesByHashedToken.isNotEmpty()).isTrue()
        val purchaseWrapper = queryPurchasesResult.purchasesByHashedToken[token.sha1()]
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(type)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.sku).isEqualTo(sku)
    }

    @Test
    fun `Presented offering is properly forwarded`() {
        setup()
        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "product_a"
            every { it.type } returns BillingClient.SkuType.SUBS
        }
        val oldSku = "product_b"

        val activity: Activity = mockk()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper!!.makePurchaseAsync(
            activity,
            "jerry",
            skuDetails,
            oldSku,
            "offering_a"
        )
        val purchases = listOf(mockk<Purchase>(relaxed = true).also {
            every { it.sku } returns "product_a"
        })
        val slot = slot<List<PurchaseWrapper>>()
        every {
            mockPurchasesListener.onPurchasesUpdated(capture(slot))
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponseCode.OK.buildResult(), purchases)

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
        verify (exactly = 1) {
            mockBuilder.enablePendingPurchases()
        }
    }

    @Test
    fun `Acknowledge works`() {
        setup()
        val token = "token"

        val capturingSlot = slot<AcknowledgePurchaseParams>()
        every {
            mockClient.acknowledgePurchase(capture(capturingSlot), any())
        } just Runs

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        wrapper!!.acknowledge(token) { _, _ -> }

        assertThat(capturingSlot.isCaptured).isTrue()
        assertThat(capturingSlot.captured.purchaseToken).isEqualTo(token)
    }

    private fun mockNullSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(BillingClient.BillingResponseCode.OK.buildResult(), null)
        }
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }
}
