@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.interfaces.GetVirtualCurrenciesCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesSpendVirtualCurrenciesTest : BasePurchasesTest() {

    private val google = Identity.google("google-token".toByteArray())
    private val amounts = mapOf("COIN" to 2)
    private val balances = VirtualCurrencies(all = emptyMap())
    private var explicitSuccess: ((CustomerInfo, String) -> Unit)? = null

    override val shouldConfigureOnSetUp: Boolean
        get() = false

    @Test
    fun `spending with IAM disabled fails as unsupported without reaching the manager`() {
        configure(iamEnabled = false)
        val callback = mockk<GetVirtualCurrenciesCallback>(relaxed = true)

        purchases.spendVirtualCurrencies(amounts, callback = callback)

        verify { callback.onError(match { it.code == PurchasesErrorCode.UnsupportedError }) }
        verify(exactly = 0) { mockVirtualCurrencyManager.spendVirtualCurrencies(any(), any(), any()) }
    }

    @Test
    fun `spending with IAM enabled passes the amounts and reference to the manager`() {
        configure()
        val callback = mockk<GetVirtualCurrenciesCallback>(relaxed = true)

        purchases.spendVirtualCurrencies(amounts, "ref", callback)

        verify { mockVirtualCurrencyManager.spendVirtualCurrencies(amounts, "ref", callback) }
    }

    @Test
    fun `a spend during an explicit IAM login runs after it`() {
        configure()
        purchases.purchasesOrchestrator.logIn(google, mockk<ReceiveCustomerInfoCallback>(relaxed = true))

        purchases.spendVirtualCurrencies(amounts, callback = mockk(relaxed = true))
        verify(exactly = 0) { mockVirtualCurrencyManager.spendVirtualCurrencies(any(), any(), any()) }

        explicitSuccess!!.invoke(mockInfo, "google-user")
        verify(exactly = 1) { mockVirtualCurrencyManager.spendVirtualCurrencies(amounts, null, any()) }
    }

    @Test
    fun `awaitSpendVirtualCurrencies returns the new balances`() = runTest {
        configure()
        every { mockVirtualCurrencyManager.spendVirtualCurrencies(any(), any(), any()) } answers {
            thirdArg<GetVirtualCurrenciesCallback>().onReceived(balances)
        }

        assertThat(purchases.awaitSpendVirtualCurrencies(amounts)).isEqualTo(balances)
    }

    @Test
    fun `awaitSpendVirtualCurrencies throws when IAM is disabled`() = runTest {
        configure(iamEnabled = false)

        val error = try {
            purchases.awaitSpendVirtualCurrencies(amounts)
            null
        } catch (e: PurchasesException) {
            e.error
        }

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
    }

    @Test
    fun `spending one currency passes it to the manager as a single-entry map`() {
        configure()
        val callback = mockk<GetVirtualCurrenciesCallback>(relaxed = true)

        purchases.spendVirtualCurrency("COIN", 3, "ref", callback)

        verify { mockVirtualCurrencyManager.spendVirtualCurrencies(mapOf("COIN" to 3), "ref", callback) }
    }

    @Test
    fun `awaitSpendVirtualCurrency returns the new balances`() = runTest {
        configure()
        every { mockVirtualCurrencyManager.spendVirtualCurrencies(mapOf("COIN" to 3), null, any()) } answers {
            thirdArg<GetVirtualCurrenciesCallback>().onReceived(balances)
        }

        assertThat(purchases.awaitSpendVirtualCurrency("COIN", 3)).isEqualTo(balances)
    }

    private fun configure(iamEnabled: Boolean = true) {
        anonymousSetup(anonymous = false)
        every { mockTokenManager.enabled } returns iamEnabled
        every { mockIdentityManager.checkIAMLoginNeeded(any()) } answers { firstArg<(Boolean) -> Unit>()(false) }
        every { mockIdentityManager.logIn(google, any<(CustomerInfo, String) -> Unit>(), any()) } answers {
            explicitSuccess = secondArg()
        }
        every { mockVirtualCurrencyManager.spendVirtualCurrencies(any(), any(), any()) } just Runs
        mockOfferingsManagerAppForeground()
        every { mockOfferingsManager.fetchAndCacheOfferings(any(), any(), any(), any()) } just Runs
        every { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) } just Runs
    }
}
