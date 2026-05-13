package com.revenuecat.purchases.admob.reward_verification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward
import com.revenuecat.purchases.awaitGetRewardVerificationStatus
import kotlinx.coroutines.CancellationException

internal object Poller {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    suspend fun poll(
        clientTransactionId: String,
        fetchStatus: suspend (String) -> RewardVerificationStatus = {
            Purchases.sharedInstance.awaitGetRewardVerificationStatus(clientTransactionId = it)
        },
    ): RewardVerificationResult {
        return pollOutcome(clientTransactionId, fetchStatus).toResult()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    private suspend fun pollOutcome(
        clientTransactionId: String,
        fetchStatus: suspend (String) -> RewardVerificationStatus,
    ): Outcome {
        return try {
            mapStatusToOutcome(fetchStatus(clientTransactionId))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Outcome.Failed
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    internal fun mapStatusToOutcome(status: RewardVerificationStatus): Outcome {
        return when (status) {
            RewardVerificationStatus.VERIFIED -> Outcome.Verified(VerifiedReward.NoReward)
            // Polling/retry orchestration lands in a follow-up PR; one-shot non-terminal/unknown statuses fail for now.
            RewardVerificationStatus.PENDING,
            RewardVerificationStatus.UNKNOWN,
            RewardVerificationStatus.FAILED,
            -> Outcome.Failed
        }
    }
}
