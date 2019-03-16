//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
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
            slot.captured.onSkuDetailsResponse(BillingClient.BillingResponse.OK, mockDetailsList)
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

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

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
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

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
        } returns BillingClient.BillingResponse.OK

        val sku = "product_a"

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity: Activity = mockk()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.makePurchaseAsync(activity, "jerry", sku, oldSkus, BillingClient.SkuType.SUBS)

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

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity: Activity = mockk()

        val slot = slot<BillingFlowParams>()
        every {
            mockClient.launchBillingFlow(eq(activity), capture(slot))
        } answers {
            val params = slot.captured
            assertThat(sku).isEqualTo(params.sku)
            assertThat(skuType).isEqualTo(params.skuType)
            assertThat(oldSkus).isEqualTo(params.oldSkus)
            assertThat(appUserID).isEqualTo(params.accountId)
            BillingClient.BillingResponse.OK
        }

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)
    }

    @Test
    fun defersBillingFlowIfNotConnected() {
        setup()

        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponse.OK

        every { mockClient.isReady } returns false

        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity: Activity = mockk()

        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

        verify(exactly = 0) {
            mockClient.launchBillingFlow(eq(activity), any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

        verify(exactly = 1) {
            mockClient.launchBillingFlow(eq(activity), any())
        }
    }

    @Test
    fun callsLaunchFlowFromMainThread() {
        setup()

        every {
            mockClient.launchBillingFlow(any(), any())
        } returns BillingClient.BillingResponse.OK

        every { mockClient.isReady } returns false

        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity: Activity = mockk()

        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

        verify(exactly = 0) {
            handler.post(any())
        }

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

        verify {
            handler.post(any())
        }
    }

    @Test
    fun purchasesUpdatedCallsAreForwarded() {
        setup()
        val purchases = ArrayList<Purchase>()
        every {
            mockPurchasesListener.onPurchasesUpdated(any())
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponse.OK, purchases)

        verify {
            mockPurchasesListener.onPurchasesUpdated(purchases)
        }
    }

    @Test
    fun purchasesUpdatedCallsAreForwardedWithEmptyIfOkNull() {
        setup()

        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any(), any())
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponse.OK, null)

        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(
                null,
                eq(BillingClient.BillingResponse.ERROR),
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
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
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
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
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
            BillingClient.BillingResponse.OK,
            ArrayList()
        )
        assertThat(successCalled).isTrue()
    }

    @Test
    fun queryHistoryNotCalledIfNotOK() {
        setup()

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        var errorCalled = false
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                fail("shouldn't go to on error")
            },
            {
                assertThat(it.code).isEqualTo(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
                errorCalled = true
            }
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            ArrayList()
        )
        assertThat(errorCalled).isTrue()
    }

    @Test
    fun canConsumeAToken() {
        setup()
        val token = "mockToken"

        every {
            mockClient.consumeAsync(eq(token), any())
        } just Runs

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.consumePurchase(token)

        verify {
            mockClient.consumeAsync(eq(token), any())
        }
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
        val clientFactory: BillingWrapper.ClientFactory = mockk()
        val billingClient: BillingClient = mockk()

        every {
            clientFactory.buildClient(any())
        } returns billingClient

        val billingWrapper = BillingWrapper(
            clientFactory,
            mockk()
        )

        billingWrapper.purchasesUpdatedListener = null
        var exceptionCaught = false
        try {
            billingWrapper.consumePurchase("token")
        } catch (e: Exception) {
            exceptionCaught = true
        }

        verify(exactly = 0) {
            billingClient.startConnection(eq(billingWrapper))
        }
        assertThat(exceptionCaught).isTrue()
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
        wrapper!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
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
    fun doNotEndConnectionIfNotConnectedAndRemovingNewListener() {
        setup()
        every {
            mockClient.isReady
        } returns false

        wrapper!!.purchasesUpdatedListener = null
        verify(exactly = 0) {
            mockClient.endConnection()
        }
    }

    @Test
    fun `when billing client is connected, billing is supported`() {
        setup()
        var receivedIsBillingSupported = false
        wrapper!!.isBillingSupported(mockk()) {
            receivedIsBillingSupported = it
        }
        assertThat(receivedIsBillingSupported).isTrue()
    }

    @Test
    fun `when billing client is not connected, start connection to see if it's supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var receivedIsBillingSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isBillingSupported(mockContext) {
            receivedIsBillingSupported = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        assertThat(receivedIsBillingSupported).isTrue()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when billing client is not connected, and try to connect and disconnects occurs, billing not supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var receivedIsBillingSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isBillingSupported(mockContext) {
            receivedIsBillingSupported = it
        }
        listener.captured.onBillingServiceDisconnected()
        assertThat(receivedIsBillingSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when billing client is not connected, and connection is not OK, billing not supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var receivedIsBillingSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isBillingSupported(mockContext) {
            receivedIsBillingSupported = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        assertThat(receivedIsBillingSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when billing client is connected, feature is supported if response is OK`() {
        setup()
        every { mockClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) } returns BillingClient.BillingResponse.OK
        val mockContext = mockk<Context>(relaxed = true)
        var featureSupported = false
        wrapper!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext) {
            featureSupported = it
        }
        assertThat(featureSupported).isTrue()
    }

    @Test
    fun `when billing client is not connected, start connection to see if feature supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var featureSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every { mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) } returns BillingClient.BillingResponse.OK
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext) {
            featureSupported = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        assertThat(featureSupported).isTrue()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when billing client is not connected, and trying to connect, disconnections means not supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var featureSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext) {
            featureSupported = it
        }
        listener.captured.onBillingServiceDisconnected()
        assertThat(featureSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when billing client is not connected, and connection is not OK, feature not supported`() {
        setup()
        every { mockClient.isReady } returns false
        val mockContext = mockk<Context>(relaxed = true)
        var featureSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        wrapper!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext) {
            featureSupported = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        assertThat(featureSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    private fun mockNullSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(BillingClient.BillingResponse.OK, null)
        }
    }
}
