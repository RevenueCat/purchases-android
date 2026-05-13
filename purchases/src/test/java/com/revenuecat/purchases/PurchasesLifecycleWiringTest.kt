package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Test

internal class PurchasesLifecycleWiringTest {

    @After
    fun tearDownMocks() {
        unmockkObject(PurchasesLifecycleEventBus)
        Purchases.backingFieldSharedInstance = null
    }

    @Test
    fun `close notifies purchases lifecycle bus`() {
        val orchestrator = mockk<PurchasesOrchestrator>(relaxed = true)
        val purchases = Purchases(orchestrator)
        Purchases.backingFieldSharedInstance = purchases
        mockkObject(PurchasesLifecycleEventBus)
        every { PurchasesLifecycleEventBus.onClosed(any()) } just Runs

        purchases.close()

        verify(exactly = 1) {
            PurchasesLifecycleEventBus.onClosed(purchases)
            orchestrator.close()
        }
        assertThat(Purchases.backingFieldSharedInstance).isNull()
    }
}
