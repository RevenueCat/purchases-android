package com.revenuecat.purchases.admob.reward_verification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
class PollerTest {

    @Test
    fun `poll calls core status fetch with client transaction id`() = runBlocking {
        var receivedClientTransactionId: String? = null

        val result = Poller.poll(clientTransactionId = "ct_1") { clientTransactionId ->
            receivedClientTransactionId = clientTransactionId
            RewardVerificationStatus.VERIFIED
        }

        assertEquals("ct_1", receivedClientTransactionId)
        assertNotNull(result.verifiedReward)
        assertFalse(result.failed)
    }

    @Test
    fun `poll maps failed status to failed result`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            RewardVerificationStatus.FAILED
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps pending status to failed result`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            RewardVerificationStatus.PENDING
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps unknown status to failed result`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            RewardVerificationStatus.UNKNOWN
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps backend errors to failed result`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            error("backend exploded")
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll rethrows coroutine cancellation`() = runBlocking {
        try {
            Poller.poll(clientTransactionId = "ct_1") {
                throw CancellationException("cancelled")
            }
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
