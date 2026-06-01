package com.revenuecat.sample.admob.ui.ads.verification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import com.revenuecat.purchases.admob.VerifiedReward

/**
 * Status message surfaced in the sample UI while loading, showing and verifying a rewarded ad.
 *
 * Mirrors the iOS `AdMobIntegrationSample` `Message` model so both samples present the same
 * reward-verification states.
 */
internal data class RewardedVerificationMessage(
    val text: String,
    val severity: Severity,
    val isLoading: Boolean = false,
) {
    enum class Severity {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
    }

    companion object {
        val loading = RewardedVerificationMessage("⏳ Loading ad...", Severity.INFO, isLoading = true)
        val readyWithoutVerification = RewardedVerificationMessage("🔓 Ready", Severity.INFO)
        val readyWithVerification = RewardedVerificationMessage("🔐 Ready", Severity.INFO)
        val verifyingReward = RewardedVerificationMessage("⏳ Verifying reward...", Severity.INFO, isLoading = true)
        val loadFailed = RewardedVerificationMessage("❌ Load failed", Severity.ERROR)
        val verificationFailed = RewardedVerificationMessage("❌ Verification failed", Severity.ERROR)

        /** Status shown for the non-verified reward path (AdMob reward only, no server verification). */
        fun rewardEarned(amount: Int, type: String) = RewardedVerificationMessage(
            "✅ Reward earned\n🎁 $amount $type",
            Severity.SUCCESS,
        )

        /** Maps a server-verification outcome to a user-facing message, mirroring iOS. */
        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        fun forVerificationResult(result: RewardVerificationResult): RewardedVerificationMessage {
            return when (val reward = result.verifiedReward) {
                is VerifiedReward.VirtualCurrency ->
                    RewardedVerificationMessage(
                        "✅ Verified\n🎁 Reward granted: ${reward.amount} ${reward.code}",
                        Severity.SUCCESS,
                    )
                VerifiedReward.NoReward ->
                    RewardedVerificationMessage("✅ Verified\nℹ️ No reward", Severity.SUCCESS)
                VerifiedReward.UnsupportedReward ->
                    RewardedVerificationMessage("✅ Verified\n⚠️ Unsupported reward", Severity.WARNING)
                null -> verificationFailed
                else -> RewardedVerificationMessage("✅ Verified\n⚠️ Unhandled reward type", Severity.WARNING)
            }
        }
    }
}
