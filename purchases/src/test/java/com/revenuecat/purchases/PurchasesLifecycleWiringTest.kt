package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasesLifecycleWiringTest {
    private lateinit var originalServiceForwarder: PurchasesService

    @Before
    fun setUp() {
        originalServiceForwarder = Purchases.serviceForwarder
    }

    @After
    fun tearDownMocks() {
        Purchases.serviceForwarder = originalServiceForwarder
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `close notifies purchases service forwarder`() {
        val orchestrator = mockk<PurchasesOrchestrator>(relaxed = true)
        val purchases = Purchases(orchestrator)
        val serviceForwarder = mockk<PurchasesService>(relaxed = true)
        Purchases.backingFieldSharedInstance = purchases
        Purchases.serviceForwarder = serviceForwarder

        purchases.close()

        verify(exactly = 1) {
            serviceForwarder.close(purchases)
            orchestrator.close()
        }
    }
}
