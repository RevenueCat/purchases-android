package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigFetchContext
import com.revenuecat.purchases.identity.Identity
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesIAMBootstrapTest : BasePurchasesTest() {

    private val serverID = "server-assigned-user"
    private val whenNeeded = mutableListOf<() -> Unit>()

    override val shouldConfigureOnSetUp: Boolean
        get() = false

    @Test
    fun `configure alone does not start the bootstrap login`() {
        configure()

        verify(exactly = 0) { mockIdentityManager.whenIAMLoginNeeded(any()) }
        verify(exactly = 0) { mockIdentityManager.logIn(any<Identity>(), any(), any()) }
    }

    @Test
    fun `foregrounding registers the bootstrap login`() {
        configure()

        foreground()

        verify(exactly = 1) { mockIdentityManager.whenIAMLoginNeeded(any()) }
    }

    @Test
    fun `a successful bootstrap login refreshes everything for the server-assigned user`() {
        configure()
        foreground()
        mockOfferingsManagerFetchOfferings(serverID)
        every { mockIdentityManager.logIn(Identity.anonymous, captureLambda(), any()) } answers {
            lambda<(CustomerInfo, String) -> Unit>().captured.invoke(mockInfo, serverID)
        }

        whenNeeded.single().invoke()

        verify(exactly = 1) { mockCustomerInfoUpdateHandler.notifyListeners(mockInfo, serverID) }
        verify(exactly = 1) {
            mockRemoteConfigManager.refreshRemoteConfig(false, serverID, RemoteConfigFetchContext.IdentityChange)
        }
        verify(exactly = 1) { mockOfferingsManager.fetchAndCacheOfferings(serverID, false, any(), any()) }
        verify(exactly = 1) { mockBackupManager.dataChanged() }
    }

    @Test
    fun `a failed bootstrap login refreshes nothing`() {
        configure()
        foreground()
        failBootstrapLogIn()

        whenNeeded.single().invoke()

        verify(exactly = 0) { mockCustomerInfoUpdateHandler.notifyListeners(any(), any()) }
        verify(exactly = 0) {
            mockRemoteConfigManager.refreshRemoteConfig(any(), any(), RemoteConfigFetchContext.IdentityChange)
        }
        verify(exactly = 0) { mockOfferingsManager.fetchAndCacheOfferings(any(), any(), any(), any()) }
        verify(exactly = 0) { mockBackupManager.dataChanged() }
    }

    @Test
    fun `repeated foregrounds start one bootstrap login while it is in flight`() {
        configure()
        every {
            mockIdentityManager.logIn(Identity.anonymous, any<(CustomerInfo, String) -> Unit>(), any())
        } just Runs

        foreground()
        foreground()
        whenNeeded.forEach { it() }

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
    }

    @Test
    fun `a later foreground retries after a failed bootstrap login`() {
        configure()
        failBootstrapLogIn()

        foreground()
        whenNeeded.last().invoke()
        foreground()
        whenNeeded.last().invoke()

        verify(exactly = 2) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
    }

    @Test
    fun `a foreground that starts the bootstrap login skips the CustomerInfo refresh for the user it replaces`() {
        configure()
        every { mockIdentityManager.whenIAMLoginNeeded(any()) } answers { firstArg<() -> Unit>().invoke() }
        every {
            mockIdentityManager.logIn(Identity.anonymous, any<(CustomerInfo, String) -> Unit>(), any())
        } just Runs

        foreground()

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        verify(exactly = 0) { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a foreground that needs no bootstrap login still refreshes CustomerInfo`() {
        configure()

        foreground()

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any(),
                any(),
                any(),
            )
        }
    }

    private fun configure() {
        every { mockIdentityManager.whenIAMLoginNeeded(capture(whenNeeded)) } just Runs
        // Whether a login is needed is IdentityManager's call; the base fixtures' user stubs the foreground work.
        anonymousSetup(anonymous = false)
        mockOfferingsManagerAppForeground()
        every { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) } just Runs
    }

    private fun foreground() {
        purchases.purchasesOrchestrator.onAppForegrounded()
    }

    private fun failBootstrapLogIn() {
        every { mockIdentityManager.logIn(Identity.anonymous, any(), captureLambda()) } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.NetworkError))
        }
    }
}
