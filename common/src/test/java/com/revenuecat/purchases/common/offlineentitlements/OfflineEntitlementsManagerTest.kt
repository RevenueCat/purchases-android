package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class OfflineEntitlementsManagerTest {

    private lateinit var backendSuccessSlot: CapturingSlot<(ProductEntitlementMapping) -> Unit>
    private lateinit var backendErrorSlot: CapturingSlot<(PurchasesError) -> Unit>

    private lateinit var appConfig: AppConfig
    private lateinit var backend: Backend
    private lateinit var deviceCache: DeviceCache
    private lateinit var offlineEntitlementsCalculator: OfflineCustomerInfoCalculator

    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager

    @Before
    fun setUp() {
        backendSuccessSlot = slot()
        backendErrorSlot = slot()

        appConfig = mockk()
        backend = mockk()
        deviceCache = mockk()
        offlineEntitlementsCalculator = mockk()

        every {
            backend.getProductEntitlementMapping(capture(backendSuccessSlot), capture(backendErrorSlot))
        } just Runs
        every {
            appConfig.areOfflineEntitlementsEnabled
        } returns true

        offlineEntitlementsManager = OfflineEntitlementsManager(
            appConfig,
            backend,
            offlineEntitlementsCalculator,
            deviceCache
        )
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale does nothing if cache not stale`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns false
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 0) { backend.getProductEntitlementMapping(any(), any()) }
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale does nothing if backend request errors`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns true
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMapping(any(), any()) }
        backendErrorSlot.captured(PurchasesError(PurchasesErrorCode.NetworkError))
        verify(exactly = 0) { deviceCache.cacheProductEntitlementMapping(any()) }
    }

    @Test
    fun `updateProductEntitlementMappingCacheIfStale caches mapping from backend if request successful`() {
        every { deviceCache.isProductEntitlementMappingCacheStale() } returns true
        every { deviceCache.cacheProductEntitlementMapping(any()) } just Runs
        offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMapping(any(), any()) }
        val expectedMappings = createProductEntitlementMapping()
        backendSuccessSlot.captured(expectedMappings)
        verify(exactly = 1) { deviceCache.cacheProductEntitlementMapping(expectedMappings) }
    }
}
