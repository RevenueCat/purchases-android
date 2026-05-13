package com.revenuecat.purchases.admob.rewardverification

import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward

internal sealed interface Outcome {
    data class Verified(val reward: VerifiedReward) : Outcome

    data object Failed : Outcome
}

internal fun Outcome.toResult(): RewardVerificationResult {
    return when (this) {
        is Outcome.Verified -> RewardVerificationResult.verified(reward)
        Outcome.Failed -> RewardVerificationResult.failed
    }
}
