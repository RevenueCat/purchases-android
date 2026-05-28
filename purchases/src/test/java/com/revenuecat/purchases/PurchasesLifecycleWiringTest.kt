package com.revenuecat.purchases

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasesLifecycleWiringTest {
    private lateinit var originalServiceForwarder: PurchasesService
    private val registeredServices = mutableListOf<PurchasesService>()

    @Before
    fun setUp() {
        originalServiceForwarder = Purchases.serviceForwarder
    }

    @After
    fun tearDownMocks() {
        registeredServices.forEach { Purchases.unregisterService(it) }
        registeredServices.clear()
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

    @Test
    fun `registerService receives lifecycle events from the default service forwarder`() {
        val service = mockk<PurchasesService>(relaxed = true)
        val purchases = mockk<Purchases>(relaxed = true)

        Purchases.registerService(service)
        registeredServices += service

        originalServiceForwarder.initialize(purchases)
        originalServiceForwarder.close(purchases)

        verify(exactly = 1) {
            service.initialize(purchases)
            service.close(purchases)
        }
    }

    @Test
    fun `unregisterService stops further lifecycle delivery`() {
        val service = mockk<PurchasesService>(relaxed = true)
        val purchases = mockk<Purchases>(relaxed = true)

        Purchases.registerService(service)
        Purchases.unregisterService(service)

        originalServiceForwarder.initialize(purchases)
        originalServiceForwarder.close(purchases)

        verify(exactly = 0) {
            service.initialize(purchases)
            service.close(purchases)
        }
    }
}
