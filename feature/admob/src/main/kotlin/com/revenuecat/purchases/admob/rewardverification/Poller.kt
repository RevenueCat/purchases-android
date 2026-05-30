package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.revenuecat.purchases.RewardVerificationResult as CoreRewardVerificationResult
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object Poller {

    internal const val DEFAULT_MAX_ATTEMPTS: Int = 10
    private const val DEFAULT_JITTER_LOWER_BOUND_SECONDS: Double = 0.75
    private const val DEFAULT_JITTER_UPPER_BOUND_SECONDS: Double = 1.25
    private const val MILLIS_PER_SECOND: Double = 1_000.0

    /**
     * Per-attempt backoff in seconds, jittered around a 1s interval so that concurrent verifications
     * do not align their polls. The defaults give an ~10s overall verification window.
     */
    internal val defaultJitterSeconds: () -> Double = {
        Random.nextDouble(
            from = DEFAULT_JITTER_LOWER_BOUND_SECONDS,
            until = DEFAULT_JITTER_UPPER_BOUND_SECONDS,
        )
    }

    /**
     * Polls the backend for the verification status of [clientTransactionId] until it reaches a
     * terminal state (verified or failed), [maxAttempts] is exhausted, or the coroutine is cancelled.
     *
     * Pending/unknown statuses and transient network errors are retried after a [jitterSeconds]
     * backoff; any other error and an exhausted attempt budget resolve to a failed result.
     */
    suspend fun poll(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher = RewardVerificationFetcher.default,
        sleepSeconds: suspend (Double) -> Unit = { delay((it * MILLIS_PER_SECOND).toLong()) },
        jitterSeconds: () -> Double = defaultJitterSeconds,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    ): RewardVerificationResult {
        return pollOutcome(
            clientTransactionId = clientTransactionId,
            fetcher = fetcher,
            sleepSeconds = sleepSeconds,
            jitterSeconds = jitterSeconds,
            maxAttempts = maxAttempts,
        ).toResult()
    }

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
        sleepSeconds: suspend (Double) -> Unit,
        jitterSeconds: () -> Double,
        maxAttempts: Int,
    ): Outcome {
        for (attempt in 0 until maxAttempts) {
            if (attempt > 0) {
                try {
                    sleepSeconds(jitterSeconds())
                } catch (e: CancellationException) {
                    // Preserve cooperative cancellation for coroutine callers.
                    throw e
                } catch (_: Exception) {
                    // If scheduling the backoff fails, finish with a deterministic failure instead
                    // of spinning in a tight retry loop.
                    return Outcome.Failed
                }
            }

            try {
                terminalOutcomeOrNull(fetcher.fetch(clientTransactionId))?.let { return it }
            } catch (e: CancellationException) {
                // Preserve cooperative cancellation for coroutine callers.
                throw e
            } catch (e: Exception) {
                if (e.isTransientPollingError()) {
                    continue
                }
                return Outcome.Failed
            }
        }

        // Exhausted every attempt without reaching a terminal verified/failed status.
        return Outcome.Failed
    }

    /**
     * Maps a single status read to a terminal [Outcome], or `null` when the status is non-terminal
     * (pending / unknown) and polling should keep retrying.
     */
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    internal fun terminalOutcomeOrNull(result: CoreRewardVerificationResult): Outcome? {
        return when (result) {
            is CoreRewardVerificationResult.Verified -> Outcome.Verified(result.reward.toAdMobReward())
            CoreRewardVerificationResult.FAILED -> Outcome.Failed
            CoreRewardVerificationResult.PENDING,
            CoreRewardVerificationResult.UNKNOWN,
            -> null
        }
    }

    private fun Throwable.isTransientPollingError(): Boolean {
        val purchasesException = this as? PurchasesException ?: return false
        return when (purchasesException.code) {
            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.UnknownBackendError,
            -> true
            else -> false
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    private fun CoreVerifiedReward.toAdMobReward(): VerifiedReward {
        return when (this) {
            is CoreVerifiedReward.VirtualCurrency ->
                VerifiedReward.VirtualCurrency(code = code, amount = amount)
            CoreVerifiedReward.NoReward -> VerifiedReward.NoReward
            CoreVerifiedReward.UnsupportedReward -> VerifiedReward.UnsupportedReward
        }
    }
}
