package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesLoginTests : BasePurchasesTest() {

    // region Switch user
    @Test
    fun `switchUser switches user`() {
        val newAppUserID = "newUser"
        every {
            mockIdentityManager.switchUser(newAppUserID)
        } just Runs
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.switchUser(newAppUserID)

        verify { mockIdentityManager.switchUser(newAppUserID) }
    }

    @Test
    fun `switchUser refreshes offerings cache`() {
        val newAppUserID = "newUser"
        every {
            mockIdentityManager.switchUser(newAppUserID)
        } just Runs
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.switchUser(newAppUserID)

        verify { mockOfferingsManager.fetchAndCacheOfferings(newAppUserID, false, any(), any()) }
    }

    @Test
    fun `switchUser no ops if new app user ID is same as current`() {
        val newAppUserID = appUserId
        every {
            mockIdentityManager.switchUser(newAppUserID)
        } just Runs
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.switchUser(newAppUserID)

        verify(exactly = 0) { mockIdentityManager.switchUser(newAppUserID) }
        verify(exactly = 0) { mockOfferingsManager.fetchAndCacheOfferings(newAppUserID, false, any(), any()) }
    }
    // endregion
}
