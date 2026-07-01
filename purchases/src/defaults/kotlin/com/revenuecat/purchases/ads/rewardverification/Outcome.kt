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
                        ?.let { "Reward verification was rejected by AdMob server-side verification (reason: $it)." }
                    ?: "Reward verification was rejected by AdMob server-side verification."
            override val isUnexpected: Boolean get() = false
        }

        object ExhaustedWhilePending : Failed {
            override val logMessage: String
                get() = "Reward verification timed out: the AdMob server-side verification (SSV) callback was " +
                    "not received in time. Possible causes: SSV is not enabled/configured for this ad unit in " +
                    "the AdMob Dashboard, the SSV callback URL is misconfigured in the AdMob Dashboard, AdMob " +
                    "delayed delivering the callback, or RevenueCat failed to process the SSV webhook."
            override val isUnexpected: Boolean get() = false
        }

        object ExhaustedWhileTransientErroring : Failed {
            override val logMessage: String
                get() = "Reward verification timed out after repeated transient errors while polling — " +
                    "typically unstable device network connectivity. The reward couldn't be verified."
            override val isUnexpected: Boolean get() = false
        }

        object UnexpectedResponse : Failed {
            override val logMessage: String
                get() = "Reward verification stopped after the server returned a status this SDK version " +
                    "doesn't recognize. Update to the latest SDK version; if you're already on the latest, " +
                    "contact RevenueCat support."
            override val isUnexpected: Boolean get() = true
        }

        class TerminalError(private val error: String) : Failed {
            override val logMessage: String
                get() = "Reward verification stopped after an unrecoverable error: $error. This is " +
                    "unexpected; if it persists, contact RevenueCat support with the error above."
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
