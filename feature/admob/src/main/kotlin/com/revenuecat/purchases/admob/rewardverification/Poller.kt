package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.admob.Logger
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.revenuecat.purchases.RewardVerificationResult as CoreRewardVerificationResult
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object Poller {

    private const val DEFAULT_MAX_ATTEMPTS: Int = 10
    private const val DEFAULT_JITTER_LOWER_BOUND_SECONDS: Double = 0.75
    private const val DEFAULT_JITTER_UPPER_BOUND_SECONDS: Double = 1.25
    private const val MILLIS_PER_SECOND: Double = 1_000.0

    // Jittered ~1s per-attempt backoff so concurrent verifications don't align their polls.
    private val defaultJitterSeconds: () -> Double = {
        Random.nextDouble(
            from = DEFAULT_JITTER_LOWER_BOUND_SECONDS,
            until = DEFAULT_JITTER_UPPER_BOUND_SECONDS,
        )
    }

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

    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
        sleepSeconds: suspend (Double) -> Unit,
        jitterSeconds: () -> Double,
        maxAttempts: Int,
    ): Outcome {
        Logger.d("Reward verification poll start transactionId=$clientTransactionId maxAttempts=$maxAttempts")
        var outcome: Outcome? = null
        var attempt = 0
        while (outcome == null && attempt < maxAttempts) {
            Logger.v(
                "Reward verification poll attempt ${attempt + 1}/$maxAttempts transactionId=$clientTransactionId",
            )
            outcome = if (attempt > 0 && !awaitBackoff(sleepSeconds, jitterSeconds, clientTransactionId)) {
                Outcome.Failed
            } else {
                fetchOutcomeOrRetry(clientTransactionId, fetcher)
            }
            attempt++
        }
        // Null outcome => every attempt exhausted without a terminal status.
        if (outcome == null) {
            Logger.w(
                "Reward verification poll exhausted $maxAttempts attempts transactionId=$clientTransactionId",
            )
        }
        return outcome ?: Outcome.Failed
    }

    // Returns false (fail deterministically) if scheduling the backoff fails, rather than spinning in a tight retry.
    private suspend fun awaitBackoff(
        sleepSeconds: suspend (Double) -> Unit,
        jitterSeconds: () -> Double,
        clientTransactionId: String,
    ): Boolean {
        return try {
            sleepSeconds(jitterSeconds())
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Logger.e("Reward verification poll backoff scheduling failed transactionId=$clientTransactionId")
            false
        }
    }

    // Returns a terminal Outcome, or null when polling should retry (pending/unknown or a transient error).
    private suspend fun fetchOutcomeOrRetry(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
    ): Outcome? {
        return try {
            val result = fetcher.fetch(clientTransactionId)
            Logger.d("Reward verification poll result=${result.logDescription()} transactionId=$clientTransactionId")
            when (result) {
                is CoreRewardVerificationResult.Verified -> Outcome.Verified(result.reward.toAdMobReward())
                CoreRewardVerificationResult.FAILED -> Outcome.Failed
                CoreRewardVerificationResult.PENDING,
                CoreRewardVerificationResult.UNKNOWN,
                -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            if (e.isTransientPollingError()) {
                Logger.d(
                    "Reward verification poll transient error, retrying: ${e.code} " +
                        "transactionId=$clientTransactionId",
                )
                null
            } else {
                Logger.e("Reward verification poll terminal error: ${e.code} transactionId=$clientTransactionId")
                Outcome.Failed
            }
        } catch (_: Exception) {
            Logger.e("Reward verification poll unexpected error transactionId=$clientTransactionId")
            Outcome.Failed
        }
    }

    private fun PurchasesException.isTransientPollingError(): Boolean {
        return when (code) {
            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.UnknownBackendError,
            -> true
            else -> false
        }
    }

    // Readable log form: object statuses log their name; Verified inlines the reward payload.
    private fun CoreRewardVerificationResult.logDescription(): String {
        return when (this) {
            is CoreRewardVerificationResult.Verified -> "verified(reward=$reward)"
            CoreRewardVerificationResult.PENDING -> "pending"
            CoreRewardVerificationResult.FAILED -> "failed"
            CoreRewardVerificationResult.UNKNOWN -> "unknown"
        }
    }

    private fun CoreVerifiedReward.toAdMobReward(): VerifiedReward {
        return when (this) {
            is CoreVerifiedReward.VirtualCurrency ->
                VerifiedReward.VirtualCurrency(code = code, amount = amount)
            CoreVerifiedReward.NoReward -> VerifiedReward.NoReward
            CoreVerifiedReward.UnsupportedReward -> VerifiedReward.UnsupportedReward
        }
    }
}
