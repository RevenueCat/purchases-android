package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import com.revenuecat.purchases.awaitGetRewardVerificationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random

internal object Poller {

    internal const val DEFAULT_MAX_ATTEMPTS: Int = 10
    private const val DEFAULT_JITTER_LOWER_BOUND_SECONDS: Double = 0.75
    private const val DEFAULT_JITTER_UPPER_BOUND_SECONDS: Double = 1.25

    internal val defaultJitterSeconds: () -> Double = {
        Random.nextDouble(
            from = DEFAULT_JITTER_LOWER_BOUND_SECONDS,
            until = DEFAULT_JITTER_UPPER_BOUND_SECONDS,
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    suspend fun poll(
        clientTransactionId: String,
        fetchStatus: suspend (String) -> RewardVerificationStatus = {
            Purchases.sharedInstance.awaitGetRewardVerificationStatus(clientTransactionId = it)
        },
        sleepSeconds: suspend (Double) -> Unit = { delay((it * 1000).toLong()) },
        jitterSeconds: () -> Double = defaultJitterSeconds,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    ): RewardVerificationResult {
        return pollOutcome(
            clientTransactionId = clientTransactionId,
            fetchStatus = fetchStatus,
            sleepSeconds = sleepSeconds,
            jitterSeconds = jitterSeconds,
            maxAttempts = maxAttempts,
        ).toResult()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetchStatus: suspend (String) -> RewardVerificationStatus,
        sleepSeconds: suspend (Double) -> Unit,
        jitterSeconds: () -> Double,
        maxAttempts: Int,
    ): Outcome {
        for (attempt in 0 until maxAttempts) {
            if (attempt > 0) {
                try {
                    sleepSeconds(jitterSeconds())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // If scheduling backoff fails, finish with a deterministic failure instead of
                    // proceeding with a tight retry loop.
                    return Outcome.Failed
                }
            }

            try {
                when (fetchStatus(clientTransactionId)) {
                    RewardVerificationStatus.VERIFIED -> return Outcome.Verified(VerifiedReward.NoReward)
                    RewardVerificationStatus.FAILED -> return Outcome.Failed
                    RewardVerificationStatus.PENDING,
                    RewardVerificationStatus.UNKNOWN,
                    -> continue
                }
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

        return Outcome.Failed
    }

    private fun Throwable.isTransientPollingError(): Boolean {
        val purchasesException = this as? PurchasesException ?: return false
        return when (purchasesException.error.code) {
            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.UnknownBackendError,
            -> true
            else -> false
        }
    }
}
