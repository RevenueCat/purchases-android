package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Intent
import android.os.Handler
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.stubSkuDetails
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Before

abstract class BillingWrapperTestBase {
    internal var onConnectedCalled: Boolean = false
    internal var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    internal var mockClient: BillingClient = mockk()
    internal var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    internal var billingClientStateListener: BillingClientStateListener? = null
    internal var handler: Handler = mockk()
    internal var mockDeviceCache: DeviceCache = mockk()

    internal var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    internal var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    internal var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    internal var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    internal var capturedConsumeParams = slot<ConsumeParams>()

    internal lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<SkuDetails>

    internal var storeProducts: List<StoreProduct>? = null

    internal val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    internal val appUserId = "jerry"
    internal var mockActivity = mockk<Activity>()

    @Before
    fun setup() {
        mockRunnables()

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

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    internal fun mockConsumeAsync(billingResult: BillingResult) {
        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }
}
