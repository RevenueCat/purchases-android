@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ads.events.types.AdRewardFailureReason

// Internal poll result. The public [RewardVerificationResult] stays binary (verified/failed); these subtypes
// capture *why* polling ended so the poller can log an actionable reason without adding public cases.
internal sealed interface Outcome {
    class Verified(val reward: VerifiedReward, val moreRewards: List<VerifiedReward>) : Outcome

    sealed interface Failed : Outcome {
        val logMessage: String

        val isUnexpected: Boolean

        // The reward-tracking equivalent of this failure, forwarded verbatim into AdRewardFailedToVerifyData.
        val trackingFailureReason: AdRewardFailureReason

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
            override val trackingFailureReason: AdRewardFailureReason
                get() = AdRewardFailureReason.BackendError(failureReason)
        }

        object ExhaustedWhilePending : Failed {
            override val logMessage: String
                get() = "Reward verification timed out: the AdMob server-side verification (SSV) callback was " +
                    "not received in time. Possible causes: SSV is not enabled/configured for this ad unit in " +
                    "the AdMob Dashboard, the SSV callback URL is misconfigured in the AdMob Dashboard, AdMob " +
                    "delayed delivering the callback, or RevenueCat failed to process the SSV webhook."
            override val isUnexpected: Boolean get() = false
            override val trackingFailureReason: AdRewardFailureReason get() = AdRewardFailureReason.Timeout
        }

        object ExhaustedWhileTransientErroring : Failed {
            override val logMessage: String
                get() = "Reward verification timed out after repeated transient errors while polling — " +
                    "typically unstable device network connectivity. The reward couldn't be verified."
            override val isUnexpected: Boolean get() = false
            override val trackingFailureReason: AdRewardFailureReason get() = AdRewardFailureReason.NetworkError
        }

        object UnexpectedResponse : Failed {
            override val logMessage: String
                get() = "Reward verification stopped after the server returned a status this SDK version " +
                    "doesn't recognize. Update to the latest SDK version; if you're already on the latest, " +
                    "contact RevenueCat support."
            override val isUnexpected: Boolean get() = true
            override val trackingFailureReason: AdRewardFailureReason get() = AdRewardFailureReason.Unknown
        }

        class TerminalError(private val error: String) : Failed {
            override val logMessage: String
                get() = "Reward verification stopped after an unrecoverable error: $error. This is " +
                    "unexpected; if it persists, contact RevenueCat support with the error above."
            override val isUnexpected: Boolean get() = true
            override val trackingFailureReason: AdRewardFailureReason get() = AdRewardFailureReason.Unknown
        }

        // The poll was cancelled before reaching a terminal status — e.g. the ad was dismissed or the SDK
        // closed while polling was in flight. Never returned by Poller itself (cancellation propagates as a
        // thrown CancellationException); callers that catch it synthesize this Outcome to track the attempt.
        object Cancelled : Failed {
            override val logMessage: String
                get() = "Reward verification was cancelled before it could complete."
            override val isUnexpected: Boolean get() = false
            override val trackingFailureReason: AdRewardFailureReason get() = AdRewardFailureReason.Cancelled
        }
    }
}

internal fun Outcome.toResult(): RewardVerificationResult {
    return when (this) {
        is Outcome.Verified -> RewardVerificationResult.verified(reward, moreRewards)
        is Outcome.Failed -> RewardVerificationResult.failed
    }
}
