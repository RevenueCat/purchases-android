@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.tracking

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackIfConfiguredTest {

    @After
    fun tearDown() {
        PaidEventTrackingTest.setPurchasesSingleton(null)
    }

    @Test
    fun `executes block when Purchases is configured`() {
        val mockPurchases = mockk<Purchases>(relaxed = true)
        PaidEventTrackingTest.setPurchasesSingleton(mockPurchases)

        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertTrue(blockExecuted)
    }

    @Test
    fun `skips block when Purchases is not configured`() {
        PaidEventTrackingTest.setPurchasesSingleton(null)

        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertFalse(blockExecuted)
    }

    @Test
    fun `block receives Purchases sharedInstance as receiver`() {
        val mockPurchases = mockk<Purchases>(relaxed = true)
        PaidEventTrackingTest.setPurchasesSingleton(mockPurchases)

        var receivedInstance: Purchases? = null
        trackIfConfigured { receivedInstance = this }

        assertEquals(mockPurchases, receivedInstance)
    }

    @Test
    fun `multiple tracking calls when configured all execute`() {
        val mockPurchases = mockk<Purchases>(relaxed = true)
        every { mockPurchases.adTracker } returns mockk(relaxed = true)
        PaidEventTrackingTest.setPurchasesSingleton(mockPurchases)

        var callCount = 0
        trackIfConfigured { callCount++ }
        trackIfConfigured { callCount++ }
        trackIfConfigured { callCount++ }

        assertEquals(3, callCount)
    }
}
