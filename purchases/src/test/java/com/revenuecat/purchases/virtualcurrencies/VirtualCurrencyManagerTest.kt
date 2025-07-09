package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrencyManagerTest {

    private val virtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
        JSONObject(Responses.validFullVirtualCurrenciesResponse)
    )

    // region virtualCurrencies
    @Test
    fun `virtualCurrencies returns cached VirtualCurrencies when cache is not stale`() {
        val appUserID = "appUserID"

        val mockDeviceCache = mockk<DeviceCache>()
        every {
            mockDeviceCache.getCachedVirtualCurrencies(any())
        } returns this.virtualCurrencies
        every {
            mockDeviceCache.isVirtualCurrenciesCacheStale(any(), any())
        } returns false

        val mockIdentityManager = mockk<IdentityManager>()
        every {
            mockIdentityManager.currentAppUserID
        } returns appUserID

        val mockAppConfig = mockk<AppConfig>()
        every {
            mockAppConfig.isAppBackgrounded
        } returns false

        val mockCallback = mockk<GetVirtualCurrenciesCallback>()
        every {
            mockCallback.onReceived(any())
        } returns Unit

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockAppConfig
        )

        virtualCurrencyManager.virtualCurrencies(mockCallback)

        verify {
            mockDeviceCache.getCachedVirtualCurrencies(appUserID = appUserID)
            mockDeviceCache.isVirtualCurrenciesCacheStale(appUserID = appUserID, appInBackground = false)
            mockCallback.onReceived(this@VirtualCurrencyManagerTest.virtualCurrencies)
        }
    }

    @Suppress("ForbiddenComment")
    @Test
    fun `virtualCurrencies fetches VirtualCurrencies from network when cache is stale`() {
        val appUserID = "appUserID"

        val mockDeviceCache = mockk<DeviceCache>()
        every {
            mockDeviceCache.getCachedVirtualCurrencies(any())
        } returns this.virtualCurrencies
        every {
            mockDeviceCache.isVirtualCurrenciesCacheStale(any(), any())
        } returns true
        every {
            mockDeviceCache.cacheVirtualCurrencies(any(), any())
        } returns Unit

        val mockIdentityManager = mockk<IdentityManager>()
        every {
            mockIdentityManager.currentAppUserID
        } returns appUserID

        val mockAppConfig = mockk<AppConfig>()
        every {
            mockAppConfig.isAppBackgrounded
        } returns false

        val mockCallback = mockk<GetVirtualCurrenciesCallback>()
        every {
            mockCallback.onReceived(any())
        } returns Unit

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockAppConfig
        )

        virtualCurrencyManager.virtualCurrencies(mockCallback)

        verify(exactly = 1) {
            mockDeviceCache.isVirtualCurrenciesCacheStale(appUserID = appUserID, appInBackground = false)

            // TODO: Ensure that the VCs we cache come from the mocked backend
//            mockDeviceCache.cacheVirtualCurrencies(
//                appUserID = appUserID,
//                virtualCurrencies = this@VirtualCurrencyManagerTest.virtualCurrencies
//            )

            // TODO: Ensure that we send the VCs from the network call to the callback
            mockCallback.onReceived(VirtualCurrencies(all = emptyMap()))
        }
        
        verify(exactly = 0) {
            mockDeviceCache.getCachedVirtualCurrencies(any())
        }
    }

    // endregion

    // region invalidateVirtualCurrenciesCache
    @Test
    fun `invalidateVirtualCurrenciesCache clears the virtual currencies cache`() {
        val appUserID = "appUserID"

        val mockDeviceCache = mockk<DeviceCache>()
        every {
            mockDeviceCache.clearVirtualCurrenciesCache(any())
        } returns Unit

        val mockIdentityManager = mockk<IdentityManager>()
        every {
            mockIdentityManager.currentAppUserID
        } returns appUserID

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockk()
        )

        virtualCurrencyManager.invalidateVirtualCurrenciesCache()

        verify(exactly = 1) {
            mockDeviceCache.clearVirtualCurrenciesCache(appUserID = appUserID)
        }
    }
    // endregion

    // region cachedVirtualCurrencies
    @Test
    fun `cachedVirtualCurrencies returns cached VirtualCurrencies when present`() {
        val appUserID = "appUserID"

        val mockDeviceCache = mockk<DeviceCache>()
        every {
            mockDeviceCache.getCachedVirtualCurrencies(any())
        } returns this.virtualCurrencies

        val mockIdentityManager = mockk<IdentityManager>()
        every {
            mockIdentityManager.currentAppUserID
        } returns appUserID

        val mockAppConfig = mockk<AppConfig>()
        every {
            mockAppConfig.isAppBackgrounded
        } returns false

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockAppConfig
        )

        assertThat(virtualCurrencyManager.cachedVirtualCurrencies()).isEqualTo(virtualCurrencies)
    }

    @Test
    fun `cachedVirtualCurrencies returns null when virtual currencies cache is empty`() {
        val appUserID = "appUserID"

        val mockDeviceCache = mockk<DeviceCache>()
        every {
            mockDeviceCache.getCachedVirtualCurrencies(any())
        } returns null

        val mockIdentityManager = mockk<IdentityManager>()
        every {
            mockIdentityManager.currentAppUserID
        } returns appUserID

        val mockAppConfig = mockk<AppConfig>()
        every {
            mockAppConfig.isAppBackgrounded
        } returns false

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockAppConfig
        )

        assertThat(virtualCurrencyManager.cachedVirtualCurrencies()).isNull()
    }
    // endregion
}