package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrencyManagerTest {

    private val virtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
        JSONObject(Responses.validFullVirtualCurrenciesResponse)
    )

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

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockk()
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

        val virtualCurrencyManager = VirtualCurrencyManager(
            identityManager = mockIdentityManager,
            deviceCache = mockDeviceCache,
            backend = mockk(),
            appConfig = mockk()
        )

        assertThat(virtualCurrencyManager.cachedVirtualCurrencies()).isNull()
    }
}