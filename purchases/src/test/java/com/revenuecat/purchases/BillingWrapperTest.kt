package com.revenuecat.purchases

import android.app.Activity
import android.os.Handler
import android.support.test.runner.AndroidJUnit4
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
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*

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
    private var mockPurchaseHistoryListener: BillingWrapper.PurchaseHistoryResponseListener =
        mockk()

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

        val mockDetails: SkuDetails = mockk()
        mockDetailsList.add(mockDetails)

        wrapper = BillingWrapper(mockClientFactory, handler)
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

        val productIDs = ArrayList<String>()
        productIDs.add("product_a")

        wrapper!!.querySkuDetailsAsync(
            BillingClient.SkuType.SUBS,
            productIDs,
            object : BillingWrapper.SkuDetailsResponseListener {
                override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
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
            override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
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
                override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                    // DO NOTHING
                }
            })

        verify (exactly = 2) {
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
            assertEquals(sku, params.sku)
            assertEquals(skuType, params.skuType)
            assertEquals(oldSkus, params.oldSkus)
            assertEquals(appUserID, params.accountId)
            BillingClient.BillingResponse.OK
        }

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

        val activity: Activity = mockk()

        wrapper!!.makePurchaseAsync(activity, appUserID, sku, oldSkus, skuType)

        verify(exactly = 0) {
            mockClient.launchBillingFlow(eq(activity), any())
        }

    }

    @Test
    fun callsLaunchFlowFromMainThread() {
        setup()

        every {
            mockClient.launchBillingFlow(any(), any())
        }  returns BillingClient.BillingResponse.OK

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
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any())
        } just Runs

        purchasesUpdatedListener!!.onPurchasesUpdated(BillingClient.BillingResponse.OK, null)

        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(
                eq(BillingClient.BillingResponse.ERROR), any()
            )
        }
    }

    @Test
    fun purchaseUpdateFailedCalledIfNotOK() {
        setup()
        every {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any())
        } just Runs
        purchasesUpdatedListener!!.onPurchasesUpdated(
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            null
        )
        verify(exactly = 0) {
            mockPurchasesListener.onPurchasesUpdated(any())
        }
        verify {
            mockPurchasesListener.onPurchasesFailedToUpdate(any(), any())
        }
    }

    @Test
    fun queryHistoryCallsListenerIfOk() {
        setup()
        every {
            mockPurchaseHistoryListener.onReceivePurchaseHistory(any())
        } just Runs
        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            mockPurchaseHistoryListener
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponse.OK,
            ArrayList()
        )

        verify {
            mockPurchaseHistoryListener.onReceivePurchaseHistory(any())
        }
    }

    @Test
    fun queryHistoryNotCalledIfNotOK() {
        setup()

        every {
            mockPurchaseHistoryListener.onReceivePurchaseHistoryError(any(), any())
        } just Runs

        billingClientStateListener!!.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        wrapper!!.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            mockPurchaseHistoryListener
        )
        billingClientPurchaseHistoryListener!!.onPurchaseHistoryResponse(
            BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
            ArrayList()
        )

        verify(exactly = 0) {
            mockPurchaseHistoryListener.onReceivePurchaseHistory(any())
        }

        verify(exactly = 1) {
            mockPurchaseHistoryListener.onReceivePurchaseHistoryError(
                eq(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED),
                any()
            )
        }
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

        wrapper!!.setListener(null)
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

        billingWrapper.setListener(null)
        billingWrapper.consumePurchase("token")

        verify(exactly = 0) {
            billingClient.startConnection(eq(billingWrapper))
        }
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
                override fun onReceiveSkuDetails(skuDetails: List<SkuDetails>) {
                    assertThat(skuDetails).isNotNull
                    assertThat(skuDetails.size).isEqualTo(0)
                }
            })
    }

    @Test
    fun nullifyBillingClientAfterEndingConnection() {
        setup()
        every {
            mockClient.endConnection()
        } just Runs
        wrapper!!.setListener(null)

        assertThat<BillingClient>(wrapper!!.billingClient).isNull()
    }

    @Test
    fun newBillingClientIsCreatedWhenSettingListener() {
        setup()
        wrapper!!.setListener(mockPurchasesListener)

        assertThat<BillingClient>(wrapper!!.billingClient).isNotNull
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
