package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
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

    private lateinit var backendSuccessSlot: CapturingSlot<(ProductEntitlementMappings) -> Unit>
    private lateinit var backendErrorSlot: CapturingSlot<(PurchasesError) -> Unit>

    private lateinit var backend: Backend
    private lateinit var deviceCache: DeviceCache

    private lateinit var offlineEntitlementsManager: OfflineEntitlementsManager

    @Before
    fun setUp() {
        backendSuccessSlot = slot()
        backendErrorSlot = slot()

        backend = mockk()
        deviceCache = mockk()

        every {
            backend.getProductEntitlementMappings(capture(backendSuccessSlot), capture(backendErrorSlot))
        } just Runs

        offlineEntitlementsManager = OfflineEntitlementsManager(
            backend,
            deviceCache
        )
    }

    @Test
    fun `updateProductEntitlementMappingsCacheIfStale does nothing if cache not stale`() {
        every { deviceCache.isProductEntitlementMappingsCacheStale() } returns false
        offlineEntitlementsManager.updateProductEntitlementMappingsCacheIfStale()
        verify(exactly = 0) { backend.getProductEntitlementMappings(any(), any()) }
    }

    @Test
    fun `updateProductEntitlementMappingsCacheIfStale does nothing if backend request errors`() {
        every { deviceCache.isProductEntitlementMappingsCacheStale() } returns true
        offlineEntitlementsManager.updateProductEntitlementMappingsCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMappings(any(), any()) }
        backendErrorSlot.captured(PurchasesError(PurchasesErrorCode.NetworkError))
        verify(exactly = 0) { deviceCache.cacheProductEntitlementMappings(any()) }
    }

    @Test
    fun `updateProductEntitlementMappingsCacheIfStale caches mapping from backend if request successful`() {
        every { deviceCache.isProductEntitlementMappingsCacheStale() } returns true
        every { deviceCache.cacheProductEntitlementMappings(any()) } just Runs
        offlineEntitlementsManager.updateProductEntitlementMappingsCacheIfStale()
        verify(exactly = 1) { backend.getProductEntitlementMappings(any(), any()) }
        val expectedMappings = createProductEntitlementMapping()
        backendSuccessSlot.captured(expectedMappings)
        verify(exactly = 1) { deviceCache.cacheProductEntitlementMappings(expectedMappings) }
    }
}
