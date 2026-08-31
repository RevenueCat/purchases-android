package com.revenuecat.purchases

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric so the dispatcher's errorLog (android.util.Log) works when a service throws.
@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `a service throwing during initialize or close neither propagates nor wedges the dispatcher`() {
        RecordingPurchasesService.failOnInitialize = true
        RecordingPurchasesService.failOnClose = true
        val first = mockk<Purchases>(relaxed = true)
        val second = mockk<Purchases>(relaxed = true)
        val dispatcher = PurchasesServices.default()

        // None of these should throw, even though the service throws on every initialize/close.
        dispatcher.initialize(first)
        dispatcher.initialize(second)
        dispatcher.close(second)

        // Every lifecycle call was still attempted: the throwing close during reconfigure didn't prevent
        // the second initialize, and first was closed with its own instance.
        assertThat(RecordingPurchasesService.initialized).containsExactly(first, second)
        assertThat(RecordingPurchasesService.closed).containsExactly(first, second)
    }
}
