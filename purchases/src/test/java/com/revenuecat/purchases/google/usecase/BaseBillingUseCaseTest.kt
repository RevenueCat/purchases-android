package com.revenuecat.purchases.google.usecase

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.utils.MockHandlerFactory
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal open class BaseBillingUseCaseTest {
    private var onConnectedCalled: Boolean = false

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    protected lateinit var handler: Handler

    protected lateinit var wrapper: BillingWrapper

    protected var mockClient: BillingClient = mockk()
    protected var mockDeviceCache: DeviceCache = mockk()
    protected var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    protected var mockDateProvider: DateProvider = mockk()
    protected val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()

    @Before
    open fun setup() {
        handler = MockHandlerFactory.createMockHandler()

        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        val mockClientFactory: BillingWrapper.ClientFactory = mockk()

        val listenerSlot = slot<PurchasesUpdatedListener>()
        every {
            mockClientFactory.buildClient(capture(listenerSlot))
        } answers {
            mockClient
        }

        val billingClientStateListenerSlot = slot<BillingClientStateListener>()
        every {
            mockClient.startConnection(capture(billingClientStateListenerSlot))
        } just Runs

        every {
            mockClient.endConnection()
        } just Runs

        every {
            mockClient.isReady
        } returns false andThen true

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache, mockDiagnosticsTracker, mockDateProvider)
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
    fun tearDown() {
        clearAllMocks()
    }

    protected fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
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
        every {
            mockDiagnosticsTracker.trackProductDetailsNotSupported(any(), any())
        } just Runs
    }

}