package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class RewardVerificationServicesTest {

    @Before
    fun setUp() {
        mockkObject(Purchases)
        every { Purchases.registerService(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    @Test
    fun `singleton exposes a locator wired to Purchases-registerService`() {
        val service = mockk<PurchasesService>()

        RewardVerificationServices.serviceRegistrar.register(service)

        verify(exactly = 1) { Purchases.registerService(service) }
    }
}
