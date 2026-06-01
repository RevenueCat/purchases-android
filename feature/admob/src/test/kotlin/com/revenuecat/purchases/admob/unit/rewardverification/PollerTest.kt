package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.RewardVerificationException
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

    private val recordedSleeps = mutableListOf<Double>()

    // No-op sleep so retry-driven tests do not wait on the real clock; record calls for assertions.
    private val noSleep: suspend (Double) -> Unit = { recordedSleeps.add(it) }
    private val fixedJitter: () -> Double = { 1.0 }

    @Test
    fun `poll calls core status fetch with client transaction id`() = runBlocking {
        var receivedClientTransactionId: String? = null

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = { clientTransactionId ->
                receivedClientTransactionId = clientTransactionId
                CoreRewardVerificationResult.Verified(CoreVerifiedReward.NoReward)
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals("ct_1", receivedClientTransactionId)
        assertNotNull(result.verifiedReward)
        assertFalse(result.failed)
        // Verified on the first read: no backoff sleeps.
        assertTrue(recordedSleeps.isEmpty())
    }

    @Test
    fun `poll maps verified status to terminal verified outcome without retrying`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                CoreRewardVerificationResult.Verified(CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 10))
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(1, attempts)
        assertFalse(result.failed)
        assertEquals(VerifiedReward.VirtualCurrency(code = "gems", amount = 10), result.verifiedReward)
    }

    @Test
    fun `poll maps failed status to terminal failed outcome without retrying`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                CoreRewardVerificationResult.FAILED
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll retries on pending until a terminal verified status is reached`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 3) {
                    CoreRewardVerificationResult.PENDING
                } else {
                    CoreRewardVerificationResult.Verified(CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 5))
                }
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(3, attempts)
        // A backoff between each attempt: two sleeps for three reads.
        assertEquals(listOf(1.0, 1.0), recordedSleeps)
        assertFalse(result.failed)
        assertEquals(VerifiedReward.VirtualCurrency(code = "gems", amount = 5), result.verifiedReward)
    }

    @Test
    fun `poll exhausts max attempts on persistent pending and fails`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                CoreRewardVerificationResult.PENDING
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 4,
        )

        assertEquals(4, attempts)
        assertEquals(3, recordedSleeps.size)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll retries on unknown status until max attempts then fails`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                CoreRewardVerificationResult.UNKNOWN
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 3,
        )

        assertEquals(3, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll retries transient network errors then succeeds`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 2) {
                    throw PurchasesException(PurchasesError(PurchasesErrorCode.NetworkError))
                }
                CoreRewardVerificationResult.Verified(CoreVerifiedReward.NoReward)
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(2, attempts)
        assertFalse(result.failed)
        assertNotNull(result.verifiedReward)
    }

    @Test
    fun `poll retries server errors then succeeds`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 2) {
                    throw RewardVerificationException(
                        PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
                        isServerError = true,
                    )
                }
                CoreRewardVerificationResult.Verified(CoreVerifiedReward.NoReward)
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(2, attempts)
        assertFalse(result.failed)
    }

    @Test
    fun `poll stops on non-server reward verification errors`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                throw RewardVerificationException(
                    PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
                    isServerError = false,
                )
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll stops on unknown backend errors`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                throw PurchasesException(PurchasesError(PurchasesErrorCode.UnknownBackendError))
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        // UnknownBackendError means the backend returned an unrecognized code; retrying is pointless.
        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll stops on non-transient purchases errors`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                throw PurchasesException(PurchasesError(PurchasesErrorCode.InvalidCredentialsError))
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll maps unexpected backend errors to failed result`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                error("backend exploded")
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll fails deterministically when scheduling the backoff throws`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                CoreRewardVerificationResult.PENDING
            },
            sleepSeconds = { throw IllegalStateException("scheduler down") },
            jitterSeconds = fixedJitter,
        )

        // First read returns pending, scheduling the backoff fails before the second read.
        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
    }

    @Test
    fun `poll rethrows coroutine cancellation from fetch`() = runBlocking {
        try {
            Poller.poll(
                clientTransactionId = "ct_1",
                fetcher = { throw CancellationException("cancelled") },
                sleepSeconds = noSleep,
                jitterSeconds = fixedJitter,
            )
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `poll rethrows coroutine cancellation from backoff`() = runBlocking {
        try {
            Poller.poll(
                clientTransactionId = "ct_1",
                fetcher = { CoreRewardVerificationResult.PENDING },
                sleepSeconds = { throw CancellationException("cancelled") },
                jitterSeconds = fixedJitter,
            )
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
