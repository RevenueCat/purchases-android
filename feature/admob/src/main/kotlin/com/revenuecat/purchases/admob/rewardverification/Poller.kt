package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.RewardVerificationException
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

    private const val BACKOFF_SCHEDULING_FAILED_DESCRIPTION: String =
        "Failed to schedule the next reward verification poll attempt."

    // The retry variants track why a read asked to retry, so an exhausted poll can report the most actionable reason.
    private sealed interface Step {
        class Terminal(val outcome: Outcome) : Step
        object RetryPending : Step
        object RetryTransientError : Step
        object RetryUnknown : Step
    }

    // Jittered ~1s per-attempt backoff so concurrent verifications don't align their polls.
    private val defaultJitterSeconds: () -> Double = {
        Random.nextDouble(
            from = DEFAULT_JITTER_LOWER_BOUND_SECONDS,
            until = DEFAULT_JITTER_UPPER_BOUND_SECONDS,
        )
    }

    // sleepSeconds/jitterSeconds/maxAttempts/logFailure are injectable test seams; all default for production callers.
    @Suppress("LongParameterList")
    suspend fun poll(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher = RewardVerificationFetcher.default,
        sleepSeconds: suspend (Double) -> Unit = { delay((it * MILLIS_PER_SECOND).toLong()) },
        jitterSeconds: () -> Double = defaultJitterSeconds,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        logFailure: (message: String, isError: Boolean) -> Unit = ::logFailureToLogcat,
    ): RewardVerificationResult {
        val outcome = pollOutcome(
            clientTransactionId = clientTransactionId,
            fetcher = fetcher,
            sleepSeconds = sleepSeconds,
            jitterSeconds = jitterSeconds,
            maxAttempts = maxAttempts,
        )
        if (outcome is Outcome.Failed) {
            logFailure(outcome.logMessage, outcome.isUnexpected)
        }
        return outcome.toResult()
    }

    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
        sleepSeconds: suspend (Double) -> Unit,
        jitterSeconds: () -> Double,
        maxAttempts: Int,
    ): Outcome {
        Logger.d("Reward verification poll start transactionId=$clientTransactionId maxAttempts=$maxAttempts")
        var sawUnknownStatus = false
        var lastRetryWasTransientError = false
        var terminal: Outcome? = null
        var attempt = 0
        while (terminal == null && attempt < maxAttempts) {
            Logger.v(
                "Reward verification poll attempt ${attempt + 1}/$maxAttempts transactionId=$clientTransactionId",
            )
            if (attempt > 0 && !awaitBackoff(sleepSeconds, jitterSeconds, clientTransactionId)) {
                terminal = Outcome.Failed.TerminalError(BACKOFF_SCHEDULING_FAILED_DESCRIPTION)
            } else {
                when (val step = fetchStep(clientTransactionId, fetcher)) {
                    is Step.Terminal -> terminal = step.outcome
                    Step.RetryPending -> lastRetryWasTransientError = false
                    Step.RetryTransientError -> lastRetryWasTransientError = true
                    Step.RetryUnknown -> {
                        sawUnknownStatus = true
                        lastRetryWasTransientError = false
                    }
                }
            }
            attempt++
        }
        if (terminal != null) {
            return terminal
        }
        // Exhausted without a terminal status. An unknown status seen along the way is the most
        // actionable signal (likely version skew), so it takes precedence over the timeout buckets.
        // Diagnostic only; the user-facing exhaustion message is logged once by poll().
        Logger.v("Reward verification poll exhausted $maxAttempts attempts transactionId=$clientTransactionId")
        return when {
            sawUnknownStatus -> Outcome.Failed.UnexpectedResponse
            lastRetryWasTransientError -> Outcome.Failed.ExhaustedWhileTransientErroring
            else -> Outcome.Failed.ExhaustedWhilePending
        }
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
            // Diagnostic only; the user-facing terminal-error message is logged once by poll().
            Logger.v("Reward verification poll backoff scheduling failed transactionId=$clientTransactionId")
            false
        }
    }

    // The broad Exception catch is the deliberate backstop that turns any unexpected failure into a terminal error.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchStep(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
    ): Step {
        return try {
            val result = fetcher.fetch(clientTransactionId)
            Logger.v("Reward verification poll result=${result.logDescription()} transactionId=$clientTransactionId")
            when (result) {
                is CoreRewardVerificationResult.Verified ->
                    Step.Terminal(Outcome.Verified(result.reward.toAdMobReward()))
                is CoreRewardVerificationResult.Failed ->
                    Step.Terminal(Outcome.Failed.BackendRejected(result.message))
                CoreRewardVerificationResult.PENDING -> Step.RetryPending
                CoreRewardVerificationResult.UNKNOWN -> Step.RetryUnknown
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            if (e.isTransientPollingError()) {
                Logger.v(
                    "Reward verification poll transient error, retrying: ${e.code} " +
                        "transactionId=$clientTransactionId",
                )
                Step.RetryTransientError
            } else {
                Logger.v("Reward verification poll terminal error: ${e.code} transactionId=$clientTransactionId")
                Step.Terminal(Outcome.Failed.TerminalError(e.describeForLog()))
            }
        } catch (e: Exception) {
            Logger.v("Reward verification poll unexpected error transactionId=$clientTransactionId")
            Step.Terminal(Outcome.Failed.TerminalError(e.describeForLog()))
        }
    }

    // Retry only transient failures: transport NetworkError and HTTP 5xx server errors (a retry may
    // reach a healthy backend instance). The decision is keyed on the transport error and the 5xx
    // flag, NOT on the backend error code — so an infra 5xx that maps to UnknownBackendError is
    // retried, while any non-5xx failure (4xx, or a non-5xx UnknownBackendError) is deterministic
    // and fails fast.
    private fun PurchasesException.isTransientPollingError(): Boolean {
        return code == PurchasesErrorCode.NetworkError ||
            (this is RewardVerificationException && isServerError)
    }

    private fun PurchasesException.describeForLog(): String {
        val underlying = underlyingErrorMessage?.takeIf { it.isNotBlank() }
        return if (underlying != null) "$message ($underlying)" else message
    }

    private fun Exception.describeForLog(): String {
        return message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
    }

    private fun logFailureToLogcat(message: String, isError: Boolean) {
        if (isError) Logger.e(message) else Logger.w(message)
    }

    // Readable log form: object statuses log their name; Verified inlines the reward payload.
    private fun CoreRewardVerificationResult.logDescription(): String {
        return when (this) {
            is CoreRewardVerificationResult.Verified -> "verified(reward=$reward)"
            CoreRewardVerificationResult.PENDING -> "pending"
            is CoreRewardVerificationResult.Failed -> "failed"
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
