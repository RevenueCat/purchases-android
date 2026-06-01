package com.revenuecat.purchases

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
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
    fun `default forwarder discovers ServiceLoader providers and forwards lifecycle`() {
        val purchases = mockk<Purchases>(relaxed = true)
        val forwarder = PurchasesServices.default()

        forwarder.initialize(purchases)
        forwarder.close(purchases)

        assertThat(RecordingPurchasesService.initialized).containsExactly(purchases)
        assertThat(RecordingPurchasesService.closed).containsExactly(purchases)
    }
}
