package com.revenuecat.purchases.virtualcurrencies

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.utils.Responses
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VirtualCurrencyManagerSpendTest {

    private val appUserID = "appUserID"
    private val balances = VirtualCurrenciesFactory.buildVirtualCurrencies(
        JSONObject(Responses.validFullVirtualCurrenciesResponse),
    )
    private val deviceCache = mockk<DeviceCache>().apply {
        every { cacheVirtualCurrencies(any(), any()) } just Runs
        every { isVirtualCurrenciesCacheStale(any(), any()) } returns false
        every { getCachedVirtualCurrencies(any()) } returns balances
    }
    private val identityManager = mockk<IdentityManager>().apply {
        every { currentAppUserID } returns appUserID
    }
    private val appConfig = mockk<AppConfig>().apply { every { isAppBackgrounded } returns false }
    private val backend = mockk<Backend>()
    private val manager = VirtualCurrencyManager(identityManager, deviceCache, backend, appConfig)

    private var received: VirtualCurrencies? = null
    private var receivedError: PurchasesError? = null
    private val callback = object : GetVirtualCurrenciesCallback {
        override fun onReceived(virtualCurrencies: VirtualCurrencies) {
            received = virtualCurrencies
        }

        override fun onError(error: PurchasesError) {
            receivedError = error
        }
    }

    @Test
    fun `a successful spend caches and returns the new balances for the spending user`() {
        stubSpend(succeed = true)

        manager.spendVirtualCurrencies(mapOf("COIN" to 2), "ref", callback)

        verify { backend.spendVirtualCurrencies(appUserID, mapOf("COIN" to 2), "ref", any(), any()) }
        verify { deviceCache.cacheVirtualCurrencies(appUserID, balances) }
        assertThat(received).isEqualTo(balances)
    }

    @Test
    fun `a failed spend reports the error and caches nothing`() {
        stubSpend(succeed = false)

        manager.spendVirtualCurrencies(mapOf("COIN" to 2), null, callback)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
        verify(exactly = 0) { deviceCache.cacheVirtualCurrencies(any(), any()) }
    }

    @Test
    fun `non-positive amounts and oversized codes are dropped`() {
        stubSpend(succeed = true)

        val amounts = mapOf("COIN" to 2, "GEM" to 0, "DUST" to -1, "X".repeat(101) to 5)
        manager.spendVirtualCurrencies(amounts, null, callback)

        verify { backend.spendVirtualCurrencies(appUserID, mapOf("COIN" to 2), null, any(), any()) }
    }

    @Test
    fun `a spend with nothing valid returns the current balances without spending`() {
        manager.spendVirtualCurrencies(mapOf("COIN" to 0), null, callback)

        verify(exactly = 0) { backend.spendVirtualCurrencies(any(), any(), any(), any(), any()) }
        assertThat(received).isEqualTo(balances)
    }

    @Test
    fun `an empty spend returns the current balances without spending`() {
        manager.spendVirtualCurrencies(emptyMap(), null, callback)

        verify(exactly = 0) { backend.spendVirtualCurrencies(any(), any(), any(), any(), any()) }
        assertThat(received).isEqualTo(balances)
    }

    private fun stubSpend(succeed: Boolean) {
        every { backend.spendVirtualCurrencies(any(), any(), any(), any(), any()) } answers {
            if (succeed) {
                arg<(VirtualCurrencies) -> Unit>(3)(balances)
            } else {
                arg<(PurchasesError) -> Unit>(4)(PurchasesError(PurchasesErrorCode.NetworkError))
            }
        }
    }
}
