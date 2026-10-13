@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.identity.AuthenticationDelegate
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesIAMBootstrapTest : BasePurchasesTest() {

    private val serverID = "server-assigned-user"

    // Pending checkIAMLoginNeeded calls; a test answers them to stand in for the token cache load.
    private val needChecks = mutableListOf<(Boolean) -> Unit>()
    private var bootstrapSuccess: ((CustomerInfo, String) -> Unit)? = null
    private var bootstrapError: ((PurchasesError) -> Unit)? = null

    // What IdentityManager.needsIAMLogin reports when the orchestrator re-checks a "needed" answer.
    private var loginStillNeeded = true

    override val shouldConfigureOnSetUp: Boolean
        get() = false

    @Test
    fun `configure alone does not start the bootstrap login`() {
        configure()

        verify(exactly = 0) { mockIdentityManager.logIn(any<Identity>(), any(), any()) }
    }

    @Test
    fun `a foreground does no per-user work until the token cache has loaded`() {
        configure()

        foreground()

        assertNoForegroundWork()
    }

    @Test
    fun `a foreground that needs no bootstrap login does its usual work`() {
        configure()

        foreground()
        needChecks.single().invoke(false)

        verify(exactly = 0) { mockIdentityManager.logIn(any<Identity>(), any(), any()) }
        assertForegroundWork(refreshedCustomerInfo = true)
    }

    @Test
    fun `a foreground that needs a bootstrap login waits for it`() {
        configure()

        foreground()
        needChecks.single().invoke(true)

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        assertNoForegroundWork()
    }

    @Test
    fun `after a successful bootstrap login, the foreground work runs without refetching CustomerInfo`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        foreground()
        needChecks.single().invoke(true)

        bootstrapSuccess!!.invoke(mockInfo, serverID)

        verify(exactly = 1) { mockCustomerInfoUpdateHandler.notifyListeners(mockInfo, serverID) }
        verify(exactly = 1) { mockOfferingsManager.fetchAndCacheOfferings(serverID, false, any(), any()) }
        assertForegroundWork(refreshedCustomerInfo = false)
    }

    @Test
    fun `after a failed bootstrap login, the foreground work runs as usual`() {
        configure()
        foreground()
        needChecks.single().invoke(true)

        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))

        verify(exactly = 0) { mockCustomerInfoUpdateHandler.notifyListeners(any(), any()) }
        assertForegroundWork(refreshedCustomerInfo = true)
    }

    @Test
    fun `repeated foregrounds start one bootstrap login and run the foreground work once`() {
        configure()
        foreground()
        foreground()
        needChecks.forEach { it(true) }

        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        assertForegroundWork(refreshedCustomerInfo = true)
    }

    @Test
    fun `a later foreground retries after a failed bootstrap login`() {
        configure()
        foreground()
        needChecks.last().invoke(true)
        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))

        foreground()
        needChecks.last().invoke(true)

        verify(exactly = 2) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
    }

    @Test
    fun `billing connecting does not sync pending purchases for a user the bootstrap will replace`() {
        configure()
        mockGetStorefront()

        capturedBillingWrapperStateListener.captured.onConnected()
        needChecks.single().invoke(true)

        verify(exactly = 0) { mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any()) }
    }

    @Test
    fun `billing connecting syncs pending purchases when no bootstrap login is needed`() {
        configure()
        mockGetStorefront()

        capturedBillingWrapperStateListener.captured.onConnected()
        needChecks.single().invoke(false)

        verify(exactly = 1) { mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any()) }
    }

    @Test
    fun `a purchase that completes while the bootstrap is in flight posts for the user it switches to`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        foreground()
        needChecks.single().invoke(true)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(listOf(purchase()))
        needChecks.last().invoke(true)
        verify(exactly = 0) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }

        bootstrapSuccess!!.invoke(mockInfo, serverID)

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `a purchase starts the bootstrap when one is needed and none is in flight`() {
        configure()

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(listOf(purchase()))
        needChecks.single().invoke(true)

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        verify(exactly = 0) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `restorePurchases waits for the bootstrap, and still runs if it fails`() {
        configure()
        every { mockBillingAbstract.queryAllPurchases(any(), any(), any()) } just Runs

        purchases.purchasesOrchestrator.restorePurchases(mockk(relaxed = true))
        needChecks.single().invoke(true)
        verify(exactly = 0) { mockBillingAbstract.queryAllPurchases(any(), any(), any()) }

        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))

        verify(exactly = 1) { mockBillingAbstract.queryAllPurchases(appUserId, any(), any()) }
    }

    @Test
    fun `syncPurchases waits for the bootstrap`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs

        purchases.purchasesOrchestrator.syncPurchases()
        needChecks.single().invoke(true)
        verify(exactly = 0) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }

        bootstrapSuccess!!.invoke(mockInfo, serverID)

        verify(exactly = 1) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }
    }

    @Test
    fun `syncPurchases runs right away when no bootstrap is needed`() {
        configure()
        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs

        purchases.purchasesOrchestrator.syncPurchases()
        needChecks.single().invoke(false)

        verify(exactly = 1) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }
        verify(exactly = 0) { mockIdentityManager.logIn(any<Identity>(), any(), any()) }
    }

    @Test
    fun `a stale needed answer after the bootstrap finished does not start a second one`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        foreground()
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(listOf(purchase()))
        needChecks.first().invoke(true)
        bootstrapSuccess!!.invoke(mockInfo, serverID)
        loginStillNeeded = false

        // Evaluated before the bootstrap finished, this check still says "needed".
        needChecks.last().invoke(true)

        verify(exactly = 1) { mockIdentityManager.logIn(Identity.anonymous, any(), any()) }
        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `a failed bootstrap login reaches the authentication delegate`() {
        configure()
        val delegate = RecordingDelegate()
        purchases.purchasesOrchestrator.authentication.delegate = delegate
        foreground()
        needChecks.single().invoke(true)

        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))

        assertThat(delegate.errors.map { it.code }).containsExactly(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `a bootstrap result dropped because the identity changed is not reported`() {
        configure()
        val delegate = RecordingDelegate()
        purchases.purchasesOrchestrator.authentication.delegate = delegate
        foreground()
        needChecks.single().invoke(true)

        bootstrapError!!.invoke(PurchasesError(PurchasesErrorCode.OperationAlreadyInProgressError))

        assertThat(delegate.errors).isEmpty()
    }

    @Test
    fun `logIn with an identity runs the identity-change follow-up`() {
        configure()
        mockOfferingsManagerFetchOfferings(serverID)
        val identity = Identity.google("google-token".toByteArray())
        every { mockIdentityManager.logIn(identity, captureLambda(), any()) } answers {
            lambda<(CustomerInfo, String) -> Unit>().captured.invoke(mockInfo, serverID)
        }
        var received: CustomerInfo? = null

        purchases.purchasesOrchestrator.authentication.logIn(
            identity,
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    received = customerInfo
                }

                override fun onError(error: PurchasesError) = Unit
            },
        )

        assertThat(received).isEqualTo(mockInfo)
        verify(exactly = 1) { mockCustomerInfoUpdateHandler.notifyListeners(mockInfo, serverID) }
        verify(exactly = 1) { mockOfferingsManager.fetchAndCacheOfferings(serverID, false, any(), any()) }
    }

    private class RecordingDelegate : AuthenticationDelegate {
        val errors = mutableListOf<PurchasesError>()

        override fun authenticatorDidEncounterError(error: PurchasesError) {
            errors.add(error)
        }
    }

    private fun purchase() = getMockedStoreTransaction("product", "token", ProductType.SUBS)

    private fun configure() {
        // Whether a login is needed is IdentityManager's call; the base fixtures' user stubs the foreground work.
        anonymousSetup(anonymous = false)
        every { mockIdentityManager.checkIAMLoginNeeded(capture(needChecks)) } just Runs
        every { mockIdentityManager.needsIAMLogin() } answers { loginStillNeeded }
        every { mockIdentityManager.logIn(Identity.anonymous, any<(CustomerInfo, String) -> Unit>(), any()) } answers {
            bootstrapSuccess = secondArg()
            bootstrapError = thirdArg()
        }
        mockOfferingsManagerAppForeground()
        every { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) } just Runs
        every { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) } just Runs
    }

    private fun foreground() {
        purchases.purchasesOrchestrator.onAppForegrounded()
    }

    private fun assertNoForegroundWork() {
        verify(exactly = 0) { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { mockOfferingsManager.onAppForeground(any()) }
        verify(exactly = 0) { mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any()) }
    }

    private fun assertForegroundWork(refreshedCustomerInfo: Boolean) {
        verify(exactly = if (refreshedCustomerInfo) 1 else 0) {
            mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 1) { mockOfferingsManager.onAppForeground(any()) }
        verify(exactly = 1) { mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any()) }
    }

    private fun mockGetStorefront() {
        every { mockBillingAbstract.getStorefront(onSuccess = any(), onError = any()) } just Runs
    }
}
