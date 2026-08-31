package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class PurchasesLifecycleWiringTest {
    private lateinit var originalServiceDispatcher: PurchasesServiceDispatcher

    @Before
    fun setUp() {
        originalServiceDispatcher = Purchases.serviceDispatcher
    }

    @After
    fun tearDownMocks() {
        Purchases.serviceDispatcher = originalServiceDispatcher
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `close notifies purchases service dispatcher`() {
        val orchestrator = mockk<PurchasesOrchestrator>(relaxed = true)
        val purchases = Purchases(orchestrator)
        val serviceDispatcher = mockk<PurchasesServiceDispatcher>(relaxed = true)
        Purchases.backingFieldSharedInstance = purchases
        Purchases.serviceDispatcher = serviceDispatcher

        purchases.close()

        verify(exactly = 1) {
            serviceDispatcher.close(purchases)
            orchestrator.close()
        }
    }
}
