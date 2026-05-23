package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

internal class PurchasesLifecycleEventBusTest {

    @Test
    fun `register notifies configured immediately when bus is already configured`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesService>(relaxed = true)
        eventBus.onConfigured(purchases)

        eventBus.register(listener)

        verify(exactly = 1) {
            listener.initialize(purchases)
        }

        eventBus.unregister(listener)
    }

    @Test
    fun `onConfigured and onClosed are broadcast to registered listeners`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listenerOne = mockk<PurchasesService>(relaxed = true)
        val listenerTwo = mockk<PurchasesService>(relaxed = true)

        eventBus.register(listenerOne)
        eventBus.register(listenerTwo)

        eventBus.onConfigured(purchases)
        eventBus.onClosed(purchases)

        verify(exactly = 1) {
            listenerOne.initialize(purchases)
            listenerOne.close(purchases)
            listenerTwo.initialize(purchases)
            listenerTwo.close(purchases)
        }

        eventBus.unregister(listenerOne)
        eventBus.unregister(listenerTwo)
    }

    @Test
    fun `unregister prevents further lifecycle callbacks`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesService>(relaxed = true)

        eventBus.register(listener)
        eventBus.unregister(listener)
        eventBus.onConfigured(purchases)
        eventBus.onClosed(purchases)

        verify(exactly = 0) {
            listener.initialize(purchases)
            listener.close(purchases)
        }
    }

    @Test
    fun `onConfigured then register then onClosed notifies listener in order`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesService>(relaxed = true)

        eventBus.onConfigured(purchases)
        eventBus.register(listener)
        eventBus.onClosed(purchases)

        verifyOrder {
            listener.initialize(purchases)
            listener.close(purchases)
        }
        verify(exactly = 1) {
            listener.initialize(purchases)
            listener.close(purchases)
        }
    }

    @Test
    fun `onConfigured then onClosed then register does not deliver stale configured`() {
        val eventBus = PurchasesLifecycleEventBus()
        val purchases = mockk<Purchases>(relaxed = true)
        val listener = mockk<PurchasesService>(relaxed = true)

        eventBus.onConfigured(purchases)
        eventBus.onClosed(purchases)
        eventBus.register(listener)

        verify(exactly = 0) {
            listener.initialize(purchases)
            listener.close(purchases)
        }
    }
}
