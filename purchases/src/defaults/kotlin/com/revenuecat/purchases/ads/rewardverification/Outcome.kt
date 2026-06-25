package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI

// Internal poll result. The public [RewardVerificationResult] stays binary (verified/failed); these subtypes
// capture *why* polling ended so the poller can log an actionable reason without adding public cases.
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal sealed interface Outcome {
    class Verified(val reward: VerifiedReward, val moreRewards: List<VerifiedReward>) : Outcome

    sealed interface Failed : Outcome {
        val logMessage: String

        val isUnexpected: Boolean

        // Logs the backend-provided message verbatim, falling back to the machine-readable failure
        // reason when no message is present so the actionable signal isn't lost.
        class BackendRejected(
            private val backendMessage: String?,
            private val failureReason: String? = null,
        ) : Failed {
            override val logMessage: String
                get() = backendMessage?.takeIf { it.isNotBlank() }
                    ?: failureReason?.takeIf { it.isNotBlank() }
                        ?.let { RewardVerificationStrings.backendRejectedWithReason(it) }
                    ?: RewardVerificationStrings.BACKEND_REJECTED_WITHOUT_MESSAGE
            override val isUnexpected: Boolean get() = false
        }

        object ExhaustedWhilePending : Failed {
            override val logMessage: String get() = RewardVerificationStrings.EXHAUSTED_WHILE_PENDING
            override val isUnexpected: Boolean get() = false
        }

        object ExhaustedWhileTransientErroring : Failed {
            override val logMessage: String get() = RewardVerificationStrings.EXHAUSTED_WHILE_TRANSIENT_ERRORING
            override val isUnexpected: Boolean get() = false
        }

        object UnexpectedResponse : Failed {
            override val logMessage: String get() = RewardVerificationStrings.UNEXPECTED_RESPONSE
            override val isUnexpected: Boolean get() = true
        }

        class TerminalError(private val error: String) : Failed {
            override val logMessage: String get() = RewardVerificationStrings.terminalError(error)
            override val isUnexpected: Boolean get() = true
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal fun Outcome.toResult(): RewardVerificationResult {
    return when (this) {
        is Outcome.Verified -> RewardVerificationResult.verified(reward, moreRewards)
        is Outcome.Failed -> RewardVerificationResult.failed
    }
}
