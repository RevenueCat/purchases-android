package com.revenuecat.purchases

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class PurchasesServicesTest {

    @Before
    fun setUp() {
        RecordingPurchasesService.reset()
    }

    @After
    fun tearDown() {
        RecordingPurchasesService.reset()
    }

    @Test
    fun `default dispatcher discovers ServiceLoader providers and forwards lifecycle`() {
        val purchases = mockk<Purchases>(relaxed = true)
        val dispatcher = PurchasesServices.default()

        dispatcher.initialize(purchases)
        dispatcher.close(purchases)

        assertThat(RecordingPurchasesService.initialized).containsExactly(purchases)
        assertThat(RecordingPurchasesService.closed).containsExactly(purchases)
    }

    @Test
    fun `reinitializing without an explicit close still closes the previously loaded services`() {
        val first = mockk<Purchases>(relaxed = true)
        val second = mockk<Purchases>(relaxed = true)
        val dispatcher = PurchasesServices.default()

        dispatcher.initialize(first)
        dispatcher.initialize(second)

        // The second initialize must tear down the first configuration's services before reloading, and
        // close them with the instance they were initialized with (first) — not the new one (second).
        assertThat(RecordingPurchasesService.initialized).containsExactly(first, second)
        assertThat(RecordingPurchasesService.closed).containsExactly(first)
    }
}
