package com.revenuecat.purchases

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class CustomerInfoUpdateHandlerTest {

    private lateinit var deviceCache: DeviceCache
    private lateinit var subscriberAttributesManager: SubscriberAttributesManager
    private lateinit var identityManager: IdentityManager
    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager
    private lateinit var appConfig: AppConfig
    private lateinit var diagnosticsTracker: DiagnosticsTracker
    private lateinit var promotionalEntitlementExpirationScheduler: PromotionalEntitlementExpirationScheduler

    private lateinit var customerInfoUpdateHandler: CustomerInfoUpdateHandler

    private val appUserId = "test-app-user-id"
    private val mockInfo = mockk<CustomerInfo>()

    @Before
    fun setUp() {
        deviceCache = mockk()
        subscriberAttributesManager = mockk()
        identityManager = mockk()
        offlineEntitlementsManager = mockk()
        appConfig = mockk()
        diagnosticsTracker = mockk()
        promotionalEntitlementExpirationScheduler = mockk(relaxed = true)

        every { identityManager.currentAppUserID } returns appUserId
        every { deviceCache.getCachedCustomerInfo(appUserId) } returns mockInfo
        every { deviceCache.cacheCustomerInfo(appUserId, mockInfo) } just Runs
        every { subscriberAttributesManager.mergeAttributes(any(), any()) } just Runs
        every { offlineEntitlementsManager.offlineCustomerInfo } returns null
        every { appConfig.customEntitlementComputation } returns false
        every { appConfig.uiPreviewMode } returns false
        every { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(any()) } just Runs

        customerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            subscriberAttributesManager,
            identityManager,
            offlineEntitlementsManager,
            appConfig = appConfig,
            diagnosticsTracker = diagnosticsTracker,
            promotionalEntitlementExpirationScheduler = promotionalEntitlementExpirationScheduler,
        )
    }


    // region updatedCustomerInfoListener

    @Test
    fun `setting listener sends cached value if it exists`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) }
    }

    @Test
    fun `setting listener doesn't send cached value if custom entitlements computation enabled`() {
        every { appConfig.customEntitlementComputation } returns true
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 0) { listenerMock.onReceived(mockInfo) }
    }

    @Test
    fun `setting listener sends offline customer info cached value if it exists over cached value`() {
        val mockCustomerInfo2 = mockk<CustomerInfo>()
        every { offlineEntitlementsManager.offlineCustomerInfo } returns mockCustomerInfo2
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { listenerMock.onReceived(mockCustomerInfo2) }
    }

    @Test
    fun `setting listener does not send cached value if it does not exist`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        every { deviceCache.getCachedCustomerInfo(any()) } returns null
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 0) { listenerMock.onReceived(mockInfo) }
    }

    @Test
    fun `setting listener tracks customer info verification result`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) }
    }

    // endregion

    // region cacheAndNotifyListeners

    @Test
    fun `caching and notifying listeners caches customer info with correct parameters`() {
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { deviceCache.cacheCustomerInfo(appUserId, mockInfo) }
    }

    @Test
    fun `caching and notifying listeners merges the customer info's subscriber attributes after caching it`() {
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo, appUserId)

        verifyOrder {
            deviceCache.cacheCustomerInfo(appUserId, mockInfo)
            subscriberAttributesManager.mergeAttributes(mockInfo, appUserId)
        }
    }

    @Test
    fun `caching and notifying listeners for a previous user merges attributes under that user`() {
        every { deviceCache.cacheCustomerInfo("previous-user", mockInfo) } just Runs

        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo, "previous-user")

        verify(exactly = 1) { subscriberAttributesManager.mergeAttributes(mockInfo, "previous-user") }
        verify(exactly = 0) { subscriberAttributesManager.mergeAttributes(any(), appUserId) }
    }

    @Test
    fun `caching and notifying listeners does not notify listeners if same than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

    @Test
    fun `caching and notifying listeners notifies listeners if different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs
        customerInfoUpdateHandler.cacheAndNotifyListeners(newCustomerInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Test
    fun `caching and notifying listeners for a previous user caches under that user but does not notify`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val previousUserId = "previous-app-user-id"
        val previousUserInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(previousUserId, previousUserInfo) } just Runs
        customerInfoUpdateHandler.cacheAndNotifyListeners(previousUserInfo, previousUserId)

        verify(exactly = 1) { deviceCache.cacheCustomerInfo(previousUserId, previousUserInfo) }
        verify(exactly = 0) { deviceCache.cacheCustomerInfo(appUserId, previousUserInfo) }
        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 0) { listenerMock.onReceived(previousUserInfo) }
        verify(exactly = 0) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(previousUserInfo) }
    }

    // endregion

    // region notifyListeners

    @Test
    fun `does not update listener if customer info belongs to a previous user`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val previousUserInfo = mockk<CustomerInfo>()
        customerInfoUpdateHandler.notifyListeners(previousUserInfo, "previous-app-user-id")

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 0) { listenerMock.onReceived(previousUserInfo) }
    }

    @Test
    fun `ignoring a previous user's customer info keeps the last sent one for deduplication`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val previousUserInfo = mockk<CustomerInfo>()
        customerInfoUpdateHandler.notifyListeners(previousUserInfo, "previous-app-user-id")
        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener, not sent again
        verify(exactly = 0) { listenerMock.onReceived(previousUserInfo) }
    }

    @Test
    fun `does not update listener if customer info same as previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

    @Test
    fun `updates listener if customer info different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
    }

    @Test
    fun `does not update listener if customer info same one in several calls`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo, appUserId)
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo, appUserId)
        customerInfoUpdateHandler.notifyListeners(newCustomerInfo, appUserId)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(newCustomerInfo) }
        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(mockInfo) } // From setting the listener
    }

    @Test
    fun `tracks customer info verification result if customer info different than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        val newCustomerInfo = mockk<CustomerInfo>()
        every { deviceCache.cacheCustomerInfo(appUserId, newCustomerInfo) } just Runs

        customerInfoUpdateHandler.notifyListeners(newCustomerInfo, appUserId)

        verify(exactly = 1) { diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(newCustomerInfo) }
    }

    // endregion

    // region promotional entitlement expirations

    @Test
    fun `notifying listeners tracks the promotional entitlement expirations of the customer info`() {
        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { promotionalEntitlementExpirationScheduler.track(mockInfo, any()) }
    }

    @Test
    fun `caching and notifying listeners tracks the promotional entitlement expirations of the customer info`() {
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo, appUserId)

        verify(exactly = 1) { promotionalEntitlementExpirationScheduler.track(mockInfo, any()) }
    }

    @Test
    fun `setting listener tracks the promotional entitlement expirations of the cached customer info`() {
        customerInfoUpdateHandler.updatedCustomerInfoListener = mockk(relaxed = true)

        verify(exactly = 1) { promotionalEntitlementExpirationScheduler.track(mockInfo, any()) }
    }

    @Test
    fun `does not track promotional entitlement expirations of a previous user's customer info`() {
        customerInfoUpdateHandler.notifyListeners(mockk(), "previous-app-user-id")

        verify(exactly = 0) { promotionalEntitlementExpirationScheduler.track(any(), any()) }
    }

    @Test
    fun `does not track promotional entitlement expirations if custom entitlements computation enabled`() {
        every { appConfig.customEntitlementComputation } returns true

        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        verify(exactly = 0) { promotionalEntitlementExpirationScheduler.track(any(), any()) }
    }

    @Test
    fun `does not track promotional entitlement expirations in UI preview mode`() {
        every { appConfig.uiPreviewMode } returns true

        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        verify(exactly = 0) { promotionalEntitlementExpirationScheduler.track(any(), any()) }
    }

    @Test
    fun `checking promotional entitlement expirations reschedules the scheduler`() {
        customerInfoUpdateHandler.checkPromotionalEntitlementExpirations()

        verify(exactly = 1) { promotionalEntitlementExpirationScheduler.reschedule() }
    }

    @Test
    fun `closing closes the scheduler`() {
        customerInfoUpdateHandler.close()

        verify(exactly = 1) { promotionalEntitlementExpirationScheduler.close() }
    }

    @Test
    fun `sends the cached customer info to the listener when a promotional entitlement expires`() {
        val expiredInfo = customerInfoWithoutEntitlements()
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val advanceToExpiration = setUpWithPromotionalEntitlementExpiringInAnHour()
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        every { deviceCache.getCachedCustomerInfo(appUserId) } returns expiredInfo
        advanceToExpiration()

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
        verify(exactly = 1) { listenerMock.onReceived(expiredInfo) }
    }

    @Test
    fun `does not send the cached customer info when a promotional entitlement expires if a newer one arrived`() {
        val expiredInfo = customerInfoWithoutEntitlements()
        val newerInfo = customerInfoWithoutEntitlements()
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        val advanceToExpiration = setUpWithPromotionalEntitlementExpiringInAnHour()
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        every { deviceCache.getCachedCustomerInfo(appUserId) } answers {
            // A newer CustomerInfo lands while the expired one is being read from the cache.
            customerInfoUpdateHandler.notifyListeners(newerInfo, appUserId)
            expiredInfo
        }
        advanceToExpiration()

        verify(exactly = 1) { listenerMock.onReceived(newerInfo) }
        verify(exactly = 0) { listenerMock.onReceived(expiredInfo) }
    }

    @Test
    fun `does not read the cached customer info when a promotional entitlement expires without a listener`() {
        val advanceToExpiration = setUpWithPromotionalEntitlementExpiringInAnHour()
        customerInfoUpdateHandler.notifyListeners(mockInfo, appUserId)

        every { deviceCache.getCachedCustomerInfo(appUserId) } returns customerInfoWithoutEntitlements()
        advanceToExpiration()

        verify(exactly = 0) { deviceCache.getCachedCustomerInfo(any()) }
    }

    /**
     * Rebuilds [customerInfoUpdateHandler] with a real scheduler and makes [mockInfo] hold a promotional
     * entitlement that expires in an hour.
     * @return A function that advances the clock and the main looper to that expiration.
     */
    private fun setUpWithPromotionalEntitlementExpiringInAnHour(): () -> Unit {
        val startDate = Date(1_700_000_000_000)
        val expirationDate = Date(startDate.time + 1.hours.inWholeMilliseconds)
        var now = startDate
        val handler = Handler(Looper.getMainLooper())
        val dateProvider = object : DateProvider {
            override val now: Date get() = now
        }
        customerInfoUpdateHandler = CustomerInfoUpdateHandler(
            deviceCache,
            subscriberAttributesManager,
            identityManager,
            offlineEntitlementsManager,
            appConfig = appConfig,
            diagnosticsTracker = diagnosticsTracker,
            handler = handler,
            promotionalEntitlementExpirationScheduler = PromotionalEntitlementExpirationScheduler(
                handler,
                dateProvider,
            ),
        )
        val activeEntitlement = mockk<EntitlementInfo>()
        every { activeEntitlement.isActive } returns true
        every { activeEntitlement.store } returns Store.PROMOTIONAL
        every { activeEntitlement.expirationDate } returns expirationDate
        every { mockInfo.entitlements } returns EntitlementInfos(
            mapOf("pro" to activeEntitlement),
            VerificationResult.NOT_REQUESTED,
        )
        return {
            now = expirationDate
            shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.HOURS)
        }
    }

    private fun customerInfoWithoutEntitlements(): CustomerInfo = mockk<CustomerInfo>().also {
        every { it.entitlements } returns EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED)
    }

    // endregion
}
