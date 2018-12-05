package com.revenuecat.purchases

import android.app.Activity
import android.os.Handler
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.ArrayList

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BillingWrapperTest {
    private var mockClientFactory: BillingWrapper.ClientFactory? = null
    private var mockClient: BillingClient? = null
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var billingClientPurchaseHistoryListener: PurchaseHistoryResponseListener? = null
    private var handler: Handler? = null

    private var mockPurchasesListener: BillingWrapper.PurchasesUpdatedListener? = null
    private var mockPurchaseHistoryListener: BillingWrapper.PurchaseHistoryResponseListener? = null

    private var wrapper: BillingWrapper? = null

    private val mockDetailsList = ArrayList<SkuDetails>()

    private var skuDetailsList: List<SkuDetails>? = null

    private var skuDetailsResponseCalled = 0

    private fun setup() {
        mockClientFactory = mock(BillingWrapper.ClientFactory::class.java)
        mockClient = mock(BillingClient::class.java)
        mockPurchasesListener = mock(BillingWrapper.PurchasesUpdatedListener::class.java)
        mockPurchaseHistoryListener =
                mock(BillingWrapper.PurchaseHistoryResponseListener::class.java)

        handler = mock(Handler::class.java)
        
        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val r = invocation.getArgument<Runnable>(0)
                r.run()
                return null
            }
        }).`when`<Handler>(handler).post(any(Runnable::class.java))

        `when`(mockClientFactory!!.buildClient(any(PurchasesUpdatedListener::class.java))).thenAnswer { invocation ->
            purchasesUpdatedListener = invocation.getArgument<PurchasesUpdatedListener>(0)
            mockClient
        }

        doAnswer { invocation ->
            billingClientStateListener = invocation.getArgument<BillingClientStateListener>(0)
            null
        }.`when`<BillingClient>(mockClient)
            .startConnection(any(BillingClientStateListener::class.java))

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                billingClientPurchaseHistoryListener =
                        invocation.getArgument<PurchaseHistoryResponseListener>(1)
                return null
            }
        }).`when`<BillingClient>(mockClient).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(PurchaseHistoryResponseListener::class.java)
        )

        val mockDetails = mock(SkuDetails::class.java)
        mockDetailsList.add(mockDetails)

        wrapper = BillingWrapper(mockClientFactory!!, handler!!)
        wrapper!!.setListener(mockPurchasesListener)
    }

    @Test
    fun canBeCreated() {
        setup()
        assertNotNull(wrapper)
    }

    @Test
    fun callsBuildOnTheFactory() {
        setup()
        verify<ClientFactory>(mockClientFactory).buildClient(purchasesUpdatedListener!!)
    }

    @Test
    fun connectsToPlayBilling() {
        setup()
        verify<BillingClient>(mockClient).startConnection(billingClientStateListener!!)
    }

    private fun mockStandardSkuDetailsResponse() {
        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val listener = invocation.getArgument<SkuDetailsResponseListener>(1)

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, mockDetailsList)
                return null
            }
        }).`when`<BillingClient>(mockClient).querySkuDetailsAsync(
            any(SkuDetailsParams::class.java),
            any(SkuDetailsResponseListener::class.java)
        )
    }

    @Test
    fun defersCallingSkuQueryUntilConnected() {
        setup()

        mockStandardSkuDetailsResponse()

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            object : BillingWrapper.SkuDetailsResponseListener {
                fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                    this@BillingWrapperTest.skuDetailsList = skuDetails
                }
            })

        assertNull(skuDetailsList)

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

        assertNotNull(skuDetailsList)
    }

    @Test
    fun canDeferMultipleCalls() {
        setup()
        mockStandardSkuDetailsResponse()

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")
        val listener = object : BillingWrapper.SkuDetailsResponseListener {
            fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                this@BillingWrapperTest.skuDetailsResponseCalled += 1
            }
        }

        wrapper!!.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, listener)
        wrapper!!.querySkuDetailsAsync(BillingClient.SkuType.SUBS, productIDs, listener)

        assertEquals(0, skuDetailsResponseCalled)

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

        assertEquals(2, skuDetailsResponseCalled)
    }

    @Test
    fun makingARequestTriggersAConnectionAttempt() {
        setup()
        mockStandardSkuDetailsResponse()

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            object : BillingWrapper.SkuDetailsResponseListener {
                fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                    // DO NOTHING
                }
            })

        verify<BillingClient>(mockClient, times(2)).startConnection(billingClientStateListener!!)
    }

    @Test
    fun canMakeAPurchase() {
        setup()

        val sku = "product_a"

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity = mock(Activity::class.java)

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.makePurchaseAsync(activity, "jerry", sku, oldSkus, BillingClient.SkuType.SUBS)

        verify<BillingClient>(mockClient).launchBillingFlow(
            eq(activity),
            any(BillingFlowParams::class.java)
        )
    }

    @Test
    fun properlySetsBillingFlowParams() {
        setup()
        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity = mock(Activity::class.java)

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val params = invocation.getArgument<BillingFlowParams>(1)
                assertEquals(sku, params.sku)
                assertEquals(skuType, params.skuType)
                assertEquals(oldSkus, params.oldSkus)
                assertEquals(appUserID, params.accountId)
                return null
            }
        }).`when`<BillingClient>(mockClient)
            .launchBillingFlow(eq(activity), any(BillingFlowParams::class.java))

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)
    }

    @Test
    fun defersBillingFlowIfNotConnected() {
        setup()
        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity = mock(Activity::class.java)

        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

        verify<BillingClient>(mockClient, times(0)).launchBillingFlow(
            eq(activity),
            any(BillingFlowParams::class.java)
        )
    }

    @Test
    fun callsLaunchFlowFromMainThread() {
        setup()
        val appUserID = "jerry"
        val sku = "product_a"
        @BillingClient.SkuType val skuType = BillingClient.SkuType.SUBS

        val oldSkus = ArrayList<String>()
        oldSkus.add("product_b")

        val activity = mock(Activity::class.java)

        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

        verify<Handler>(handler, times(0)).post(any(Runnable::class.java))

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)

        verify<Handler>(handler).post(any(Runnable::class.java))
    }

    @Test
    fun purchasesUpdatedCallsAreForwarded() {
        setup()
        val purchases = ArrayList<Purchase>()

        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponse.OK, purchases)

        verify<PurchasesUpdatedListener>(mockPurchasesListener).onPurchasesUpdated(purchases)
    }

    @Test
    fun purchasesUpdatedCallsAreForwardedWithEmptyIfOkNull() {
        setup()

        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponse.OK, null)

        verify<PurchasesUpdatedListener>(mockPurchasesListener).onPurchasesFailedToUpdate(
            eq(
                BillingClient.BillingResponse.ERROR
            ), any(String::class.java)
        )
    }

    @Test
    fun purchaseUpdateFailedCalledIfNotOK() {
        setup()
        purchasesUpdatedListener!!.onPurchasesUpdated(
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            null
        )

        verify<PurchasesUpdatedListener>(
            mockPurchasesListener,
            times(0)
        ).onPurchasesUpdated(any<Any>() as List<Purchase>)
        verify<BillingWrapper.PurchasesUpdatedListener>(mockPurchasesListener).onPurchasesFailedToUpdate(
            anyInt(),
            any(String::class.java)
        )
    }

    @Test
    fun queryHistoryCallsListenerIfOk() {
        setup()
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            mockPurchaseHistoryListener!!
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponse.OK,
            ArrayList()
        )

        verify<PurchaseHistoryResponseListener>(mockPurchaseHistoryListener).onReceivePurchaseHistory(
            any<Any>() as List<Purchase>
        )
    }

    @Test
    fun queryHistoryNotCalledIfNotOK() {
        setup()
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            mockPurchaseHistoryListener!!
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            ArrayList()
        )

        verify<PurchaseHistoryResponseListener>(
            mockPurchaseHistoryListener,
            times(0)
        ).onReceivePurchaseHistory(any<Any>() as List<Purchase>)
        verify<PurchaseHistoryResponseListener>(
            mockPurchaseHistoryListener,
            times(1)
        ).onReceivePurchaseHistoryError(
            eq(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED),
            any(String::class.java)
        )
    }

    @Test
    fun canConsumeAToken() {
        setup()
        val token = "mockToken"

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.consumePurchase(token)

        verify<BillingClient>(mockClient).consumeAsync(
            eq(token),
            any(ConsumeResponseListener::class.java)
        )
    }

    @Test
    fun removingListenerDisconnects() {
        setup()
        wrapper!!.setListener(null)
        verify<BillingClient>(mockClient).endConnection()
        assertThat(wrapper!!.getPurchasesUpdatedListener()).isNull()
    }

    @Test
    fun whenSettingListenerStartConnection() {
        setup()
        verify<BillingClient>(mockClient).startConnection(eq<BillingWrapper>(wrapper))
        assertThat(wrapper!!.getPurchasesUpdatedListener()).isNotNull()
    }

    @Test
    fun whenExecutingRequestAndThereIsNoListenerDoNotTryToStartConnection() {
        val clientFactory = mock(BillingWrapper.ClientFactory::class.java)
        val billingClient = mock(BillingClient::class.java)

        `when`(clientFactory.buildClient(any(PurchasesUpdatedListener::class.java)))
            .thenReturn(billingClient)

        val billingWrapper = BillingWrapper(
            clientFactory,
            mock(Handler::class.java)
        )

        billingWrapper.setListener(null)
        billingWrapper.consumePurchase("token")

        verify(billingClient, never()).startConnection(eq(billingWrapper))
    }

    @Test
    fun whenSkuDetailsIsNullPassAnEmptyListToTheListener() {
        setup()
        mockNullSkuDetailsResponse()

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            object : BillingWrapper.SkuDetailsResponseListener {
                fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                    assertThat(skuDetails).isNotNull
                    assertThat(skuDetails.size).isEqualTo(0)
                }
            })
    }

    @Test
    fun nullifyBillingClientAfterEndingConnection() {
        setup()
        wrapper!!.setListener(null)

        assertThat(wrapper!!.getBillingClient()).isNull()
    }

    @Test
    fun newBillingClientIsCreatedWhenSettingListener() {
        setup()
        wrapper!!.setListener(mockPurchasesListener)

        assertThat(wrapper!!.getBillingClient()).isNotNull()
    }

    private fun mockNullSkuDetailsResponse() {
        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val listener = invocation.getArgument<SkuDetailsResponseListener>(1)

                listener.onSkuDetailsResponse(BillingClient.BillingResponse.OK, null)
                return null
            }
        }).`when`<BillingClient>(mockClient).querySkuDetailsAsync(
            any(SkuDetailsParams::class.java),
            any(SkuDetailsResponseListener::class.java)
        )
    }
}
