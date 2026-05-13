package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Test

internal class PurchasesLifecycleEventBusTest {

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `register notifies configured immediately when purchases is already configured`() {
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesLifecycleListener>(relaxed = true)
        Purchases.backingFieldSharedInstance = purchases

        PurchasesLifecycleEventBus.register(listener)

        verify(exactly = 1) {
            listener.onPurchasesConfigured(purchases)
        }

        PurchasesLifecycleEventBus.unregister(listener)
    }

    @Test
    fun `onConfigured and onClosed are broadcast to registered listeners`() {
        val purchases = mockk<Purchases>(relaxed = true)
        val listenerOne = mockk<PurchasesLifecycleListener>(relaxed = true)
        val listenerTwo = mockk<PurchasesLifecycleListener>(relaxed = true)

        PurchasesLifecycleEventBus.register(listenerOne)
        PurchasesLifecycleEventBus.register(listenerTwo)

        PurchasesLifecycleEventBus.onConfigured(purchases)
        PurchasesLifecycleEventBus.onClosed(purchases)

        verify(exactly = 1) {
            listenerOne.onPurchasesConfigured(purchases)
            listenerOne.onPurchasesClosed(purchases)
            listenerTwo.onPurchasesConfigured(purchases)
            listenerTwo.onPurchasesClosed(purchases)
        }

        PurchasesLifecycleEventBus.unregister(listenerOne)
        PurchasesLifecycleEventBus.unregister(listenerTwo)
    }

    @Test
    fun `unregister prevents further lifecycle callbacks`() {
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesLifecycleListener>(relaxed = true)

        PurchasesLifecycleEventBus.register(listener)
        PurchasesLifecycleEventBus.unregister(listener)
        PurchasesLifecycleEventBus.onConfigured(purchases)
        PurchasesLifecycleEventBus.onClosed(purchases)

        verify(exactly = 0) {
            listener.onPurchasesConfigured(purchases)
            listener.onPurchasesClosed(purchases)
        }
    }
}
