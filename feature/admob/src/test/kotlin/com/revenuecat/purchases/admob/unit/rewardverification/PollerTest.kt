package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.RewardVerificationResult as CoreRewardVerificationResult
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward
import com.revenuecat.purchases.admob.VerifiedReward
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
            CoreRewardVerificationResult.Verified(CoreVerifiedReward.NoReward)
        }

        assertEquals("ct_1", receivedClientTransactionId)
        assertNotNull(result.verifiedReward)
        assertFalse(result.failed)
    }

    @Test
    fun `poll maps failed result to failed outcome`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            CoreRewardVerificationResult.FAILED
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps pending result to failed outcome`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            CoreRewardVerificationResult.PENDING
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps unknown result to failed outcome`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            CoreRewardVerificationResult.UNKNOWN
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps verified virtual currency reward to adapter reward`() = runBlocking {
        val result = Poller.poll(clientTransactionId = "ct_1") {
            CoreRewardVerificationResult.Verified(CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 10))
        }

        assertFalse(result.failed)
        assertEquals(VerifiedReward.VirtualCurrency(code = "gems", amount = 10), result.verifiedReward)
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
