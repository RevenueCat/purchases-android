package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

internal class PurchasesServiceRegistryTest {

    @Test
    fun `register notifies configured immediately when registry is already configured`() {
        val registry = PurchasesServiceRegistry()
        val purchases = mockk<Purchases>(relaxed = true)
        val service = mockk<PurchasesService>(relaxed = true)
        registry.onConfigured(purchases)

        registry.register(service)

        verify(exactly = 1) {
            service.initialize(purchases)
        }

        registry.unregister(service)
    }

    @Test
    fun `onConfigured and onClosed are broadcast to registered services`() {
        val registry = PurchasesServiceRegistry()
        val purchases = mockk<Purchases>(relaxed = true)
        val serviceOne = mockk<PurchasesService>(relaxed = true)
        val serviceTwo = mockk<PurchasesService>(relaxed = true)

        registry.register(serviceOne)
        registry.register(serviceTwo)

        registry.onConfigured(purchases)
        registry.onClosed(purchases)

        verify(exactly = 1) {
            serviceOne.initialize(purchases)
            serviceOne.close(purchases)
            serviceTwo.initialize(purchases)
            serviceTwo.close(purchases)
        }

        registry.unregister(serviceOne)
        registry.unregister(serviceTwo)
    }

    @Test
    fun `unregister prevents further lifecycle callbacks`() {
        val registry = PurchasesServiceRegistry()
        val purchases = mockk<Purchases>(relaxed = true)
        val service = mockk<PurchasesService>(relaxed = true)

        registry.register(service)
        registry.unregister(service)
        registry.onConfigured(purchases)
        registry.onClosed(purchases)

        verify(exactly = 0) {
            service.initialize(purchases)
            service.close(purchases)
        }
    }

    @Test
    fun `onConfigured then register then onClosed notifies service in order`() {
        val registry = PurchasesServiceRegistry()
        val purchases = mockk<Purchases>(relaxed = true)
        val service = mockk<PurchasesService>(relaxed = true)

        registry.onConfigured(purchases)
        registry.register(service)
        registry.onClosed(purchases)

        verifyOrder {
            service.initialize(purchases)
            service.close(purchases)
        }
        verify(exactly = 1) {
            service.initialize(purchases)
            service.close(purchases)
        }
    }

    @Test
    fun `onConfigured then onClosed then register does not deliver stale configured`() {
        val registry = PurchasesServiceRegistry()
        val purchases = mockk<Purchases>(relaxed = true)
        val service = mockk<PurchasesService>(relaxed = true)

        registry.onConfigured(purchases)
        registry.onClosed(purchases)
        registry.register(service)

        verify(exactly = 0) {
            service.initialize(purchases)
            service.close(purchases)
        }
    }
}
