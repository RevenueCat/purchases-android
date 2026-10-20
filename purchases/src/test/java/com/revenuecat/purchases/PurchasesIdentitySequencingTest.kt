@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.identity.Identity
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Explicit identity changes take turns with the IAM bootstrap and hold user-scoped writes (Step 31a). */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesIdentitySequencingTest : BasePurchasesTest() {

    private val serverID = "server-assigned-user"
    private val google = Identity.google("google-token".toByteArray())

    private val needChecks = mutableListOf<(Boolean) -> Unit>()
    private var bootstrapSuccess: ((CustomerInfo, String) -> Unit)? = null
    private var explicitSuccess: ((CustomerInfo, String) -> Unit)? = null
    private var explicitError: ((PurchasesError) -> Unit)? = null
    private var logOutCompletion: ((PurchasesError?) -> Unit)? = null

    override val shouldConfigureOnSetUp: Boolean
        get() = false

    @Test
    fun `a purchase completing during an explicit IAM login posts after it`() {
        configure()
        purchases.purchasesOrchestrator.logIn(google, callback())

        purchaseCompletes()
        verify(exactly = 0) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }

        explicitSuccess!!.invoke(mockInfo, serverID)
        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `a restore during an explicit logout runs after it, even when the logout fails`() {
        configure()
        every { mockBillingAbstract.queryAllPurchases(any(), any(), any()) } just Runs
        purchases.purchasesOrchestrator.logOut(callback())

        purchases.purchasesOrchestrator.restorePurchases(callback())
        needChecks.last().invoke(false)
        verify(exactly = 0) { mockBillingAbstract.queryAllPurchases(any(), any(), any()) }

        logOutCompletion!!.invoke(PurchasesError(PurchasesErrorCode.NetworkError))
        verify(exactly = 1) { mockBillingAbstract.queryAllPurchases(any(), any(), any()) }
    }

    @Test
    fun `an explicit login during the bootstrap waits for it instead of racing it`() {
        configure()
        foreground()
        needChecks.single().invoke(true)

        purchases.purchasesOrchestrator.logIn(google, callback())
        verify(exactly = 0) { mockIdentityManager.logIn(google, any(), any()) }

        bootstrapSuccess!!.invoke(mockInfo, serverID)
        verify(exactly = 1) { mockIdentityManager.logIn(google, any(), any()) }
    }

    @Test
    fun `waiting work runs in arrival order around a queued login`() {
        configure()
        foreground()
        needChecks.single().invoke(true)
        purchaseCompletes() // before the login: released by the bootstrap
        purchases.purchasesOrchestrator.logIn(google, callback())
        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs
        purchases.purchasesOrchestrator.syncPurchases() // after the login: waits for it
        needChecks.last().invoke(false)

        bootstrapSuccess!!.invoke(mockInfo, serverID)
        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
        verify(exactly = 0) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }

        explicitSuccess!!.invoke(mockInfo, "google-user")
        verifyOrder {
            mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any())
            mockIdentityManager.logIn(google, any(), any())
            mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any())
        }
    }

    @Test
    fun `a login by app user ID also holds writes until it finishes`() {
        configure()
        every { mockIdentityManager.logIn("new-user", any(), any()) } just Runs
        purchases.purchasesOrchestrator.logIn("new-user", mockk<LogInCallback>(relaxed = true))

        purchaseCompletes()

        verify(exactly = 0) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `with IAM disabled, explicit identity changes hold nothing`() {
        configure(iamEnabled = false)
        purchases.purchasesOrchestrator.logIn(google, callback())

        purchaseCompletes()

        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    @Test
    fun `a failed explicit login still releases the waiting work`() {
        configure()
        purchases.purchasesOrchestrator.logIn(google, callback())
        purchaseCompletes()

        explicitError!!.invoke(PurchasesError(PurchasesErrorCode.InvalidCredentialsError))

        verify(exactly = 1) { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) }
    }

    private fun purchaseCompletes() {
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            listOf(getMockedStoreTransaction("product", "token", ProductType.SUBS)),
        )
        needChecks.last().invoke(false)
    }

    private fun configure(iamEnabled: Boolean = true) {
        anonymousSetup(anonymous = false)
        every { mockTokenManager.enabled } returns iamEnabled
        every { mockIdentityManager.checkIAMLoginNeeded(capture(needChecks)) } just Runs
        every { mockIdentityManager.needsIAMLogin() } returns true
        every { mockIdentityManager.logIn(Identity.anonymous, any<(CustomerInfo, String) -> Unit>(), any()) } answers {
            bootstrapSuccess = secondArg()
        }
        every { mockIdentityManager.logIn(google, any<(CustomerInfo, String) -> Unit>(), any()) } answers {
            explicitSuccess = secondArg()
            explicitError = thirdArg()
        }
        every { mockIdentityManager.logOut(any()) } answers { logOutCompletion = firstArg() }
        mockOfferingsManagerAppForeground()
        every { mockOfferingsManager.fetchAndCacheOfferings(any(), any(), any(), any()) } just Runs
        every { mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any(), any(), any()) } just Runs
        every { mockBillingAbstract.queryProductDetailsAsync(any(), any(), any(), any()) } just Runs
    }

    private fun foreground() {
        purchases.purchasesOrchestrator.onAppForegrounded()
    }

    private fun callback() = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
}
