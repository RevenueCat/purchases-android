@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackIfConfiguredTest {

    @After
    fun tearDown() {
        PurchasesTestHelper.setSharedInstance(null)
    }

    @Test
    fun `executes block when Purchases is configured`() {
        PurchasesTestHelper.setSharedInstance(mockk(relaxed = true))

        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertTrue(blockExecuted)
    }

    @Test
    fun `skips block when Purchases is not configured`() {
        PurchasesTestHelper.setSharedInstance(null)

        var blockExecuted = false
        trackIfConfigured { blockExecuted = true }

        assertFalse(blockExecuted)
    }

    @Test
    fun `block receives Purchases sharedInstance as receiver`() {
        val purchases = mockk<Purchases>(relaxed = true)
        PurchasesTestHelper.setSharedInstance(purchases)

        var receivedInstance: Purchases? = null
        trackIfConfigured { receivedInstance = this }

        assertEquals(purchases, receivedInstance)
    }
}
