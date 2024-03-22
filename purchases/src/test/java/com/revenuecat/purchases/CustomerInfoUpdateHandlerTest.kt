package com.revenuecat.purchases

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
        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo)

        verify(exactly = 1) { deviceCache.cacheCustomerInfo(appUserId, mockInfo) }
    }

    @Test
    fun `caching and notifying listeners does not notify listeners if same than previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.cacheAndNotifyListeners(mockInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

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

    @Test
    fun `does not update listener if customer info same as previous one`() {
        val listenerMock = mockk<UpdatedCustomerInfoListener>(relaxed = true)
        customerInfoUpdateHandler.updatedCustomerInfoListener = listenerMock

        customerInfoUpdateHandler.notifyListeners(mockInfo)

        verify(exactly = 1) { listenerMock.onReceived(mockInfo) } // From setting the listener
    }

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
}
