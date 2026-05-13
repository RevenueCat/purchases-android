package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.RewardVerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
class PollerTest {

    @Test
    fun `poll retries pending until verified`() = runBlocking {
        val statuses = ArrayDeque(
            listOf(
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.VERIFIED,
            ),
        )
        val sleepDurations = mutableListOf<Double>()
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                statuses.removeFirst()
            },
            sleepSeconds = { sleepDurations += it },
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(3, callCount)
        assertEquals(listOf(1.0, 1.0), sleepDurations)
        assertNotNull(result.verifiedReward)
        assertFalse(result.failed)
    }

    @Test
    fun `poll retries pending until failed`() = runBlocking {
        val statuses = ArrayDeque(
            listOf(
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.FAILED,
            ),
        )
        val sleepDurations = mutableListOf<Double>()
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                statuses.removeFirst()
            },
            sleepSeconds = { sleepDurations += it },
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(3, callCount)
        assertEquals(listOf(1.0, 1.0), sleepDurations)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll times out after max attempts for pending status`() = runBlocking {
        val sleepDurations = mutableListOf<Double>()
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                RewardVerificationStatus.PENDING
            },
            sleepSeconds = { sleepDurations += it },
            jitterSeconds = { 1.0 },
            maxAttempts = 3,
        )

        assertEquals(3, callCount)
        assertEquals(listOf(1.0, 1.0), sleepDurations)
        assertTrue(result.failed)
    }

    @Test
    fun `poll retries unknown status like pending`() = runBlocking {
        val statuses = ArrayDeque(
            listOf(
                RewardVerificationStatus.UNKNOWN,
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.VERIFIED,
            ),
        )
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                statuses.removeFirst()
            },
            sleepSeconds = {},
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(3, callCount)
        assertFalse(result.failed)
    }

    @Test
    fun `poll retries transient endpoint errors`() = runBlocking {
        val steps = ArrayDeque(
            listOf<Any>(
                PurchasesErrorCode.NetworkError,
                RewardVerificationStatus.PENDING,
                RewardVerificationStatus.VERIFIED,
            ),
        )
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                when (val step = steps.removeFirst()) {
                    is RewardVerificationStatus -> step
                    is PurchasesErrorCode -> throw PurchasesException(PurchasesError(step))
                    else -> error("Unexpected step: $step")
                }
            },
            sleepSeconds = {},
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(3, callCount)
        assertFalse(result.failed)
    }

    @Test
    fun `poll fails fast on non transient endpoint errors`() = runBlocking {
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                throw PurchasesException(PurchasesError(PurchasesErrorCode.SignatureVerificationError))
            },
            sleepSeconds = {},
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(1, callCount)
        assertTrue(result.failed)
    }

    @Test
    fun `poll fails fast on unexpected errors`() = runBlocking {
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                error("backend exploded")
            },
            sleepSeconds = {},
            jitterSeconds = { 1.0 },
            maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
        )

        assertEquals(1, callCount)
        assertTrue(result.failed)
    }

    @Test
    fun `poll rethrows cancellation from fetch status`() = runBlocking {
        try {
            Poller.poll(
                clientTransactionId = "ct_1",
                fetchStatus = { throw CancellationException("cancelled") },
                sleepSeconds = {},
                jitterSeconds = { 1.0 },
                maxAttempts = Poller.DEFAULT_MAX_ATTEMPTS,
            )
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `poll rethrows cancellation from inter-attempt sleep`() = runBlocking {
        try {
            Poller.poll(
                clientTransactionId = "ct_1",
                fetchStatus = { RewardVerificationStatus.PENDING },
                sleepSeconds = { throw CancellationException("cancelled") },
                jitterSeconds = { 1.0 },
                maxAttempts = 2,
            )
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `poll returns failed if backoff scheduling throws non cancellation error`() = runBlocking {
        var callCount = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetchStatus = {
                callCount += 1
                RewardVerificationStatus.PENDING
            },
            sleepSeconds = { error("scheduler failed") },
            jitterSeconds = { 1.0 },
            maxAttempts = 3,
        )

        assertEquals(
            "Polling should stop when sleep scheduling fails to avoid immediate retry storms",
            1,
            callCount,
        )
        assertTrue(result.failed)
    }
}
