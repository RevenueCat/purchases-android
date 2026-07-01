package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.RewardVerificationException
import com.revenuecat.purchases.RewardVerificationPollStatus
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
class PollerTest {

    private val recordedSleeps = mutableListOf<Double>()

    // No-op sleep so retry-driven tests do not wait on the real clock; record calls for assertions.
    private val noSleep: suspend (Double) -> Unit = { recordedSleeps.add(it) }
    private val fixedJitter: () -> Double = { 1.0 }

    // Captures the user-facing failure message (and its severity) logged for a failed poll.
    private val recordedFailures = mutableListOf<RecordedFailure>()
    private val captureFailure: (String, Boolean) -> Unit = { message, isError ->
        recordedFailures.add(RecordedFailure(message, isError))
    }

    private data class RecordedFailure(val message: String, val isError: Boolean)

    @Test
    fun `poll calls core status fetch with client transaction id`() = runBlocking {
        var receivedClientTransactionId: String? = null

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = { clientTransactionId ->
                receivedClientTransactionId = clientTransactionId
                RewardVerificationPollStatus.Verified(CoreVerifiedReward.NoReward)
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
                RewardVerificationPollStatus.Verified(CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 10))
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
        val backendMessage = "AdMob server-side reward verification is not enabled for this app."

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                RewardVerificationPollStatus.Failed(failureReason = "ssv_not_enabled", message = backendMessage)
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            logFailure = captureFailure,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        // Backend-rejected: the backend message is logged verbatim, at warning level.
        assertEquals(listOf(RecordedFailure(backendMessage, isError = false)), recordedFailures)
    }

    @Test
    fun `poll logs a fallback message when the backend rejects without a message`() = runBlocking {
        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = { RewardVerificationPollStatus.Failed() },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            logFailure = captureFailure,
        )

        assertTrue(result.failed)
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.BACKEND_REJECTED_WITHOUT_MESSAGE, isError = false)),
            recordedFailures,
        )
    }

    @Test
    fun `poll falls back to the failure reason when the backend rejects without a message`() = runBlocking {
        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = { RewardVerificationPollStatus.Failed(failureReason = "no_reward_rule") },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            logFailure = captureFailure,
        )

        assertTrue(result.failed)
        // No human-readable message, but the machine-readable reason is still surfaced in the log.
        assertEquals(
            listOf(
                RecordedFailure(
                    RewardVerificationStrings.backendRejectedWithReason("no_reward_rule"),
                    isError = false,
                ),
            ),
            recordedFailures,
        )
    }

    @Test
    fun `poll retries on pending until a terminal verified status is reached`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 3) {
                    RewardVerificationPollStatus.PENDING
                } else {
                    RewardVerificationPollStatus.Verified(CoreVerifiedReward.VirtualCurrency(code = "gems", amount = 5))
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
                RewardVerificationPollStatus.PENDING
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 4,
            logFailure = captureFailure,
        )

        assertEquals(4, attempts)
        assertEquals(3, recordedSleeps.size)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        // Exhausted while every read was still pending.
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.EXHAUSTED_WHILE_PENDING, isError = false)),
            recordedFailures,
        )
    }

    @Test
    fun `poll retries on unknown status until max attempts then fails`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                RewardVerificationPollStatus.UNKNOWN
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 3,
            logFailure = captureFailure,
        )

        assertEquals(3, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        // An unknown status seen along the way is reported as unexpected_response (error level), not as a
        // plain pending timeout.
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.UNEXPECTED_RESPONSE, isError = true)),
            recordedFailures,
        )
    }

    @Test
    fun `poll prefers unexpected response over transient exhaustion when an unknown status was seen`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                // Unknown first, then transient errors until exhaustion: the unknown status wins.
                if (attempts == 1) {
                    RewardVerificationPollStatus.UNKNOWN
                } else {
                    throw RewardVerificationException(
                        PurchasesError(PurchasesErrorCode.NetworkError),
                        isServerError = false,
                    )
                }
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 3,
            logFailure = captureFailure,
        )

        assertTrue(result.failed)
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.UNEXPECTED_RESPONSE, isError = true)),
            recordedFailures,
        )
    }

    @Test
    fun `poll reports transient exhaustion when polls keep failing with transient errors`() = runBlocking {
        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                throw RewardVerificationException(
                    PurchasesError(PurchasesErrorCode.NetworkError),
                    isServerError = false,
                )
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            maxAttempts = 3,
            logFailure = captureFailure,
        )

        assertTrue(result.failed)
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.EXHAUSTED_WHILE_TRANSIENT_ERRORING, isError = false)),
            recordedFailures,
        )
    }

    @Test
    fun `poll retries transient network errors then succeeds`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 2) {
                    throw RewardVerificationException(
                        PurchasesError(PurchasesErrorCode.NetworkError),
                        isServerError = false,
                    )
                }
                RewardVerificationPollStatus.Verified(CoreVerifiedReward.NoReward)
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
                RewardVerificationPollStatus.Verified(CoreVerifiedReward.NoReward)
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
    fun `poll does not retry non-server unknown backend errors`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                // A non-5xx unrecognized backend code is deterministic; retrying is pointless.
                throw RewardVerificationException(
                    PurchasesError(PurchasesErrorCode.UnknownBackendError),
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
    fun `poll retries server errors even when mapped to an unknown backend code`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                if (attempts < 2) {
                    // An infra 5xx with no recognized code surfaces as UnknownBackendError; retry is
                    // keyed on isServerError (the 5xx), not on the code.
                    throw RewardVerificationException(
                        PurchasesError(PurchasesErrorCode.UnknownBackendError),
                        isServerError = true,
                    )
                }
                RewardVerificationPollStatus.Verified(CoreVerifiedReward.NoReward)
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
        )

        assertEquals(2, attempts)
        assertFalse(result.failed)
    }

    @Test
    fun `poll stops on non-transient errors`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                throw RewardVerificationException(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                    isServerError = false,
                )
            },
            sleepSeconds = noSleep,
            jitterSeconds = fixedJitter,
            logFailure = captureFailure,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        // Terminal error: the failing error description is threaded into the terminal-error message at error level.
        assertEquals(
            listOf(
                RecordedFailure(
                    RewardVerificationStrings.terminalError(PurchasesErrorCode.InvalidCredentialsError.description),
                    isError = true,
                ),
            ),
            recordedFailures,
        )
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
            logFailure = captureFailure,
        )

        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        assertEquals(
            listOf(RecordedFailure(RewardVerificationStrings.terminalError("backend exploded"), isError = true)),
            recordedFailures,
        )
    }

    @Test
    fun `poll fails deterministically when scheduling the backoff throws`() = runBlocking {
        var attempts = 0

        val result = Poller.poll(
            clientTransactionId = "ct_1",
            fetcher = {
                attempts++
                RewardVerificationPollStatus.PENDING
            },
            sleepSeconds = { throw IllegalStateException("scheduler down") },
            jitterSeconds = fixedJitter,
            logFailure = captureFailure,
        )

        // First read returns pending, scheduling the backoff fails before the second read.
        assertEquals(1, attempts)
        assertTrue(result.failed)
        assertNull(result.verifiedReward)
        // A backoff scheduling failure ends the poll as a terminal error at error level.
        val failure = recordedFailures.single()
        assertTrue(failure.isError)
        assertTrue(failure.message.contains("unrecoverable error"))
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
                fetcher = { RewardVerificationPollStatus.PENDING },
                sleepSeconds = { throw CancellationException("cancelled") },
                jitterSeconds = fixedJitter,
            )
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
