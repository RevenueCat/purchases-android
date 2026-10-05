package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigFetchContext
import com.revenuecat.purchases.identity.Identity
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesIAMBootstrapTest : BasePurchasesTest() {

    private val serverID = "server-assigned-user"
    private val whenNeeded = slot<() -> Unit>()

    override val shouldConfigureOnSetUp: Boolean
        get() = false

    @Test
    fun `configure registers the bootstrap login once`() {
        configure()

        verify(exactly = 1) { mockIdentityManager.whenIAMLoginNeeded(any()) }
        verify(exactly = 0) { mockIdentityManager.logIn(any<Identity>(), any(), any()) }
    }

    @Test
    fun `a successful bootstrap login refreshes everything for the server-assigned user`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        every { mockIdentityManager.logIn(Identity.anonymous, captureLambda(), any()) } answers {
            lambda<(CustomerInfo, String) -> Unit>().captured.invoke(mockInfo, serverID)
        }

        whenNeeded.captured.invoke()

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
        every { mockIdentityManager.logIn(Identity.anonymous, any(), captureLambda()) } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.NetworkError))
        }

        whenNeeded.captured.invoke()

        verify(exactly = 0) { mockCustomerInfoUpdateHandler.notifyListeners(any(), any()) }
        verify(exactly = 0) {
            mockRemoteConfigManager.refreshRemoteConfig(any(), any(), RemoteConfigFetchContext.IdentityChange)
        }
        verify(exactly = 0) { mockOfferingsManager.fetchAndCacheOfferings(any(), any(), any(), any()) }
        verify(exactly = 0) { mockBackupManager.dataChanged() }
    }

    private fun configure() {
        every { mockIdentityManager.whenIAMLoginNeeded(capture(whenNeeded)) } answers { }
        anonymousSetup(anonymous = true)
    }
}
