package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerInfoUpdateHandlerTest {

    private lateinit var deviceCache: DeviceCache
    private lateinit var identityManager: IdentityManager
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var appConfig: AppConfig
    private lateinit var diagnosticsTracker: DiagnosticsTracker

    private lateinit var customerInfoUpdateHandler: CustomerInfoUpdateHandler

    private val appUserId = "test-app-user-id"
    private val mockInfo = mockk<CustomerInfo>()

    @Before
    fun setUp() {
        deviceCache = mockk()
        identityManager = mockk()
        offlineEntitlementsManager = mockk()
        appConfig = mockk()
        diagnosticsTracker = mockk()

        every { identityManager.currentAppUserID } returns appUserId
        every { deviceCache.getCachedCustomerInfo(appUserId) } returns mockInfo
        every { deviceCache.cacheCustomerInfo(appUserId, mockInfo) } just Runs
        every { offlineEntitlementsManager.offlineCustomerInfo } returns null
        every { appConfig.customEntitlementComputation } returns false
        every { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(any()) } just Runs

        customerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            identityManager,
            offlineEntitlementsManager,
            appConfig = appConfig,
            diagnosticsTracker = diagnosticsTracker,
        )
    }


    // region updatedCustomerInfoListener (legacy)

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener sends cached value if it exists`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener doesn't send cached value if custom entitlements computation enabled`() {
        every { appConfig.customEntitlementComputation } returns true
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 0) { listenerMock.onReceived(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener sends offline customer info cached value if it exists over cached value`() {
        val mockCustomerInfo2 = mockk<CustomerInfo>()
        every { offlineEntitlementsManager.offlineCustomerInfo } returns mockCustomerInfo2
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockCustomerInfo2) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener does not send cached value if it does not exist`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        every { deviceCache.getCachedCustomerInfo(any()) } returns null
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 0) { listenerMock.onReceived(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener tracks customer info verification result`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting listener after prior broadcast still receives cached customer info`() {
        customerInfoUpdateHandler.notifyListeners(mockInfo)

        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) }
    }

    // endregion

    // region cacheAndNotifyListeners

    @Test
    fun `caching and notifying listeners caches customer info with correct parameters`() {
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo)

        verify(exactly = 1) { deviceCache.cacheCustomerInfo(appUserId, mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `caching and notifying listeners does not notify listeners if same than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

    @Suppress("DEPRECATION")
    @Test
    fun `caching and notifying listeners notifies listeners if different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.cacheAndNotifyListeners(newCustomerInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    // endregion

    // region notifyListeners

    @Suppress("DEPRECATION")
    @Test
    fun `does not update listener if customer info same as previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.notifyListeners(mockInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

    @Suppress("DEPRECATION")
    @Test
    fun `updates listener if customer info different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `does not update listener if customer info same one in several calls`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) } // From setting the listener
    }

    @Suppress("DEPRECATION")
    @Test
    fun `tracks customer info verification result if customer info different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(newCustomerInfo) }
    }

    // endregion

    // region addUpdatedCustomerInfoListener

    @Test
    fun `added listener receives cached customer info immediately`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listenerMock)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) }
    }

    @Test
    fun `added listener does not receive cached info if custom entitlements computation enabled`() {
        every { appConfig.customEntitlementComputation } returns true
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listenerMock)

        verify(exactly = 0) { listenerMock.onReceived(any()) }
    }

    @Test
    fun `multiple added listeners all get notified on change`() {
        val listener1 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val listener2 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener1)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener2)

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { listener1.onReceived(newCustomerInfo) }
        verify(exactly = 1) { listener2.onReceived(newCustomerInfo) }
    }

    @Test
    fun `adding the same listener twice does not duplicate callbacks`() {
        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener)

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { listener.onReceived(mockInfo) }
        verify(exactly = 1) { listener.onReceived(newCustomerInfo) }
    }

    @Test
    fun `removing a specific listener stops its notifications`() {
        val listener1 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val listener2 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener1)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener2)

        customerInfoUpdateHandler.removeUpdatedCustomerInfoListener(listener1)

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 0) { listener1.onReceived(newCustomerInfo) }
        verify(exactly = 1) { listener2.onReceived(newCustomerInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `legacy property and added listeners coexist`() {
        val legacyListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val addedListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)

        customerInfoUpdateHandler.updatedCustomerInfoListener = legacyListener
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(addedListener)

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { legacyListener.onReceived(newCustomerInfo) }
        verify(exactly = 1) { addedListener.onReceived(newCustomerInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting legacy to null does not affect added listeners`() {
        val addedListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(addedListener)

        customerInfoUpdateHandler.updatedCustomerInfoListener = null

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        verify(exactly = 1) { addedListener.onReceived(newCustomerInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `setting legacy listener does not redeliver cached info to added listeners`() {
        val addedListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(addedListener)

        val legacyListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = legacyListener

        verify(exactly = 1) { addedListener.onReceived(mockInfo) }
        verify(exactly = 1) { legacyListener.onReceived(mockInfo) }
    }

    @Test
    fun `new listener gets cached info immediately even after prior broadcasts`() {
        val listener1 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener1)

        // listener1 already got mockInfo. Now add a second listener — it should also get mockInfo.
        val listener2 = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener2)

        verify(exactly = 1) { listener2.onReceived(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `removeAllListeners clears everything`() {
        val legacyListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val addedListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = legacyListener
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(addedListener)

        customerInfoUpdateHandler.removeAllListeners()

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo)

        // Only the initial cached info call, nothing after removeAllListeners
        verify(exactly = 1) { legacyListener.onReceived(mockInfo) }
        verify(exactly = 1) { addedListener.onReceived(mockInfo) }
        verify(exactly = 0) { legacyListener.onReceived(newCustomerInfo) }
        verify(exactly = 0) { addedListener.onReceived(newCustomerInfo) }
    }

    @Test
    fun `adding a new listener does not cause double delivery to existing listeners`() {
        // Simulate: notifyListeners(newInfo) has already run, so lastSentCustomerInfo = newInfo.
        // Then a second listener is added while the cache still holds an older value.
        // The existing listener must NOT receive newInfo a second time.
        val existingListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(existingListener)
        // existingListener received mockInfo (initial cached delivery)

        val newInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newInfo)
        // existingListener received newInfo; lastSentCustomerInfo = newInfo

        // Cache still returns the older mockInfo (e.g. not yet updated)
        every { deviceCache.getCachedCustomerInfo(appUserId) } returns mockInfo

        val newListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(newListener)
        // newListener should receive the stale mockInfo as its initial delivery
        // but existingListener must NOT receive newInfo again

        verify(exactly = 1) { newListener.onReceived(mockInfo) }
        verify(exactly = 1) { existingListener.onReceived(newInfo) } // exactly once, not twice
    }

    @Test
    fun `new listener does not receive stale cached info after newer notification wins the race`() {
        val queuedHandler = QueuedHandler()
        customerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            identityManager,
            offlineEntitlementsManager,
            appConfig = appConfig,
            diagnosticsTracker = diagnosticsTracker,
            handler = queuedHandler.handler,
        )

        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener)

        val newInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newInfo) } just Runs
        customerInfoUpdateHandler.notifyListeners(newInfo)

        queuedHandler.runPostedActions()

        verify(exactly = 0) { listener.onReceived(mockInfo) }
        verify(exactly = 1) { listener.onReceived(newInfo) }
    }

    @Test
    fun `new listener does not receive stale cached info if notify happens during registration before initial dispatch queues`() {
        val queuedHandler = QueuedHandler()
        customerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            identityManager,
            offlineEntitlementsManager,
            appConfig = appConfig,
            diagnosticsTracker = diagnosticsTracker,
            handler = queuedHandler.handler,
        )

        val newInfo = mockk<CustomerInfo>()
        every { deviceCache.getCachedCustomerInfo(appUserId) } answers {
            customerInfoUpdateHandler.notifyListeners(newInfo)
            mockInfo
        }

        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener)

        queuedHandler.runPostedActions()

        verify(exactly = 0) { listener.onReceived(mockInfo) }
        verify(exactly = 1) { listener.onReceived(newInfo) }
    }

    // endregion

    // region legacy property and reference-based removal interplay

    @Suppress("DEPRECATION")
    @Test
    fun `listener registered through both the legacy property and add is only notified once`() {
        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listener
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(listener)

        val newInfo = mockk<CustomerInfo>()
        customerInfoUpdateHandler.notifyListeners(newInfo)

        verify(exactly = 1) { listener.onReceived(mockInfo) }
        verify(exactly = 1) { listener.onReceived(newInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `removing by reference also removes a listener set through the legacy property`() {
        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listener

        customerInfoUpdateHandler.removeUpdatedCustomerInfoListener(listener)

        assertThat(customerInfoUpdateHandler.updatedCustomerInfoListener).isNull()

        val newInfo = mockk<CustomerInfo>()
        customerInfoUpdateHandler.notifyListeners(newInfo)

        verify(exactly = 0) { listener.onReceived(newInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `re-assigning the same legacy listener instance does not redeliver cached info`() {
        val listener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listener
        customerInfoUpdateHandler.updatedCustomerInfoListener = listener
        customerInfoUpdateHandler.updatedCustomerInfoListener = listener

        verify(exactly = 1) { listener.onReceived(mockInfo) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `replacing the legacy listener stops notifying the previous one`() {
        val firstListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val secondListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = firstListener
        customerInfoUpdateHandler.updatedCustomerInfoListener = secondListener

        val newInfo = mockk<CustomerInfo>()
        customerInfoUpdateHandler.notifyListeners(newInfo)

        verify(exactly = 0) { firstListener.onReceived(newInfo) }
        verify(exactly = 1) { secondListener.onReceived(newInfo) }
    }

    // endregion

    // region diagnostics deduplication

    @Suppress("DEPRECATION")
    @Test
    fun `tracks verification result only once when cached info is later broadcast`() {
        customerInfoUpdateHandler.updatedCustomerInfoListener =
            mockk<UpdatedCustomerInfoListener>(relaxed = true)

        // The backend returns the very same customer info the cache already held.
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo)

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) }
    }

    @Test
    fun `tracks verification result only once when several listeners register for the same info`() {
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(
            mockk<UpdatedCustomerInfoListener>(relaxed = true),
        )
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(
            mockk<UpdatedCustomerInfoListener>(relaxed = true),
        )

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) }
    }

    // endregion

    // region isolation between listeners

    /**
     * Characterization test. A listener that throws aborts the broadcast for the listeners
     * registered behind it, which is a known limitation of delivering inline on the looper thread
     * and is deliberately not swallowed here: hiding an app's exception would be worse than the
     * missed update. What this pins is that the failure stays confined to that one broadcast and
     * does not corrupt the delivery bookkeeping of the listeners behind it.
     */
    @Test
    fun `a listener that throws does not corrupt delivery for the listeners behind it`() {
        // No cached info, so registration does not deliver and the test isolates broadcasting.
        every { deviceCache.getCachedCustomerInfo(appUserId) } returns null

        val throwingListener = UpdatedCustomerInfoListener { error("listener blew up") }
        val healthyListener = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(throwingListener)
        customerInfoUpdateHandler.addUpdatedCustomerInfoListener(healthyListener)

        val firstInfo = mockk<CustomerInfo>()
        val secondInfo = mockk<CustomerInfo>()

        // The throwing listener is registered first, so it aborts this broadcast before the
        // healthy listener behind it is reached.
        runCatching { customerInfoUpdateHandler.notifyListeners(firstInfo) }
        verify(exactly = 0) { healthyListener.onReceived(firstInfo) }

        customerInfoUpdateHandler.removeUpdatedCustomerInfoListener(throwingListener)

        // Later updates must still reach the healthy listener, including firstInfo once the
        // shared gate has moved off it.
        customerInfoUpdateHandler.notifyListeners(secondInfo)
        customerInfoUpdateHandler.notifyListeners(firstInfo)

        verify(exactly = 1) { healthyListener.onReceived(secondInfo) }
        verify(exactly = 1) { healthyListener.onReceived(firstInfo) }
    }

    // endregion

    private class QueuedHandler {
        private val looper = mockk<Looper>()
        private val postedActions = mutableListOf<Runnable>()

        val handler = mockk<Handler>()

        init {
            every { looper.thread } returns Thread()
            every { handler.looper } returns looper
            every { handler.post(any()) } answers {
                postedActions.add(firstArg())
                true
            }
        }

        fun runPostedActions() {
            val actions = postedActions.toList()
            postedActions.clear()
            actions.forEach(Runnable::run)
        }
    }
}
