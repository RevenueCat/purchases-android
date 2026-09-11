package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.Purchases
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackIfConfiguredTest {
    private val purchases = mockk<Purchases>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(Purchases)
        every { Purchases.isConfigured } returns true
        every { Purchases.sharedInstance } returns purchases
    }

    @After
    fun tearDown() {
        unmockkObject(Purchases)
    }

    @Test
    fun `executes block when Purchases is configured`() {
        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertTrue(blockExecuted)
    }

    @Test
    fun `skips block when Purchases is not configured`() {
        every { Purchases.isConfigured } returns false

        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertFalse(blockExecuted)
    }

    @Test
    fun `block receives Purchases sharedInstance as receiver`() {
        var receivedInstance: Purchases? = null
        trackIfConfigured { receivedInstance = this }

        assertEquals(purchases, receivedInstance)
    }

    @Test
    fun `swallows a failure raised by the block`() {
        trackIfConfigured { throw IllegalStateException("boom") }
    }
}
