package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class PurchasesLifecycleWiringTest {
    private lateinit var originalLifecycleListener: PurchasesLifecycleListener

    @Before
    fun setUp() {
        originalLifecycleListener = Purchases.lifecycleListener
    }

    @After
    fun tearDownMocks() {
        Purchases.lifecycleListener = originalLifecycleListener
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `close notifies purchases lifecycle bus`() {
        val orchestrator = mockk<PurchasesOrchestrator>(relaxed = true)
        val purchases = Purchases(orchestrator)
        val lifecycleListener = mockk<PurchasesLifecycleListener>(relaxed = true)
        Purchases.backingFieldSharedInstance = purchases
        Purchases.lifecycleListener = lifecycleListener

        purchases.close()

        verify(exactly = 1) {
            lifecycleListener.onPurchasesClosed(purchases)
            orchestrator.close()
        }
    }
}
