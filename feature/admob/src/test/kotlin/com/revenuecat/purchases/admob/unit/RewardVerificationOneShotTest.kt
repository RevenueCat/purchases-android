package com.revenuecat.purchases.admob

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
class RewardVerificationOneShotTest {

    @Test
    fun `performOneShotVerification calls core status fetch with client transaction id`() = runBlocking {
        var receivedClientTransactionId: String? = null

        val result = performOneShotVerification(clientTransactionId = "ct_1") { clientTransactionId ->
            receivedClientTransactionId = clientTransactionId
            RewardVerificationStatus.VERIFIED
        }

        assertEquals("ct_1", receivedClientTransactionId)
        assertNotNull(result.verifiedReward)
        assertFalse(result.failed)
    }

    @Test
    fun `performOneShotVerification maps failed status to failed result`() = runBlocking {
        val result = performOneShotVerification(clientTransactionId = "ct_1") {
            RewardVerificationStatus.FAILED
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `performOneShotVerification maps pending status to failed result`() = runBlocking {
        val result = performOneShotVerification(clientTransactionId = "ct_1") {
            RewardVerificationStatus.PENDING
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `performOneShotVerification maps unknown status to failed result`() = runBlocking {
        val result = performOneShotVerification(clientTransactionId = "ct_1") {
            RewardVerificationStatus.UNKNOWN
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `performOneShotVerification maps backend errors to failed result`() = runBlocking {
        val result = performOneShotVerification(clientTransactionId = "ct_1") {
            error("backend exploded")
        }

        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `performOneShotVerification rethrows coroutine cancellation`() = runBlocking {
        try {
            performOneShotVerification(clientTransactionId = "ct_1") {
                throw CancellationException("cancelled")
            }
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
