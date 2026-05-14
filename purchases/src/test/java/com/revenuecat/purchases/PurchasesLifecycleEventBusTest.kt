package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class PurchasesLifecycleEventBusTest {

    @Test
    fun `register notifies configured immediately when bus is already configured`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesLifecycleListener>(relaxed = true)
        eventBus.onConfigured(purchases)

        eventBus.register(listener)

        verify(exactly = 1) {
            listener.onPurchasesConfigured(purchases)
        }

        eventBus.unregister(listener)
    }

    @Test
    fun `onConfigured and onClosed are broadcast to registered listeners`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listenerOne = mockk<PurchasesLifecycleListener>(relaxed = true)
        val listenerTwo = mockk<PurchasesLifecycleListener>(relaxed = true)

        eventBus.register(listenerOne)
        eventBus.register(listenerTwo)

        eventBus.onConfigured(purchases)
        eventBus.onClosed(purchases)

        verify(exactly = 1) {
            listenerOne.onPurchasesConfigured(purchases)
            listenerOne.onPurchasesClosed(purchases)
            listenerTwo.onPurchasesConfigured(purchases)
            listenerTwo.onPurchasesClosed(purchases)
        }

        eventBus.unregister(listenerOne)
        eventBus.unregister(listenerTwo)
    }

    @Test
    fun `unregister prevents further lifecycle callbacks`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesLifecycleListener>(relaxed = true)

        eventBus.register(listener)
        eventBus.unregister(listener)
        eventBus.onConfigured(purchases)
        eventBus.onClosed(purchases)

        verify(exactly = 0) {
            listener.onPurchasesConfigured(purchases)
            listener.onPurchasesClosed(purchases)
        }
    }
}
