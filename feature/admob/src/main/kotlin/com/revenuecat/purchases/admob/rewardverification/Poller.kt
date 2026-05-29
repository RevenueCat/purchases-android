package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import kotlinx.coroutines.CancellationException
import com.revenuecat.purchases.RewardVerificationResult as CoreRewardVerificationResult
import com.revenuecat.purchases.VerifiedReward as CoreVerifiedReward

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal object Poller {

    suspend fun poll(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher = RewardVerificationFetcher.default,
    ): RewardVerificationResult {
        return pollOutcome(clientTransactionId, fetcher).toResult()
    }

    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetcher: RewardVerificationFetcher,
    ): Outcome {
        return try {
            mapResultToOutcome(fetcher.fetch(clientTransactionId))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Outcome.Failed
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    internal fun mapResultToOutcome(result: CoreRewardVerificationResult): Outcome {
        return when (result) {
            is CoreRewardVerificationResult.Verified ->
                Outcome.Verified(result.reward.toAdMobReward())
            // Polling/retry orchestration lands in a follow-up PR; one-shot non-terminal/unknown statuses fail for now.
            CoreRewardVerificationResult.PENDING,
            CoreRewardVerificationResult.UNKNOWN,
            CoreRewardVerificationResult.FAILED,
            -> Outcome.Failed
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
