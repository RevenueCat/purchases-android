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
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.mockProductDetails
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Before
import java.util.Date

open class BillingWrapperTestBase {
    internal var onConnectedCalled: Boolean = false
    internal var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    internal var mockClient: BillingClient = mockk()
    internal var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    internal var billingClientStateListener: BillingClientStateListener? = null
    internal var handler: Handler = mockk()
    internal var mockDeviceCache: DeviceCache = mockk()
    internal var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    internal var mockDateProvider: DateProvider = mockk()

    internal var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    internal var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    internal var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    internal var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    internal var capturedConsumeParams = slot<ConsumeParams>()

    internal lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<ProductDetails>

    internal var storeProducts: List<StoreProduct>? = null

    internal val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    internal val appUserId = "jerry"
    internal var mockActivity = mockk<Activity>()

    internal val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    internal val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!

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

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache, mockDiagnosticsTracker)
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

    internal fun Int.buildResult(): BillingResult {
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

    internal fun getMockedPurchaseList(purchaseToken: String): List<Purchase> {
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
            mockDiagnosticsTracker.trackGoogleQuerySkuDetailsRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(any(), any(), any())
        } just Runs
    }
}
