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
internal data class VerificationMessage(
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
        val loading = VerificationMessage("⏳ Loading ad...", Severity.INFO, isLoading = true)
        val readyWithoutVerification = VerificationMessage("🔓 Ready", Severity.INFO)
        val readyWithVerification = VerificationMessage("🔐 Ready", Severity.INFO)
        val verifyingReward = VerificationMessage("⏳ Verifying reward...", Severity.INFO, isLoading = true)
        val loadFailed = VerificationMessage("❌ Load failed", Severity.ERROR)
        val verificationFailed = VerificationMessage("❌ Verification failed", Severity.ERROR)

        /** Status shown for the non-verified reward path (AdMob reward only, no server verification). */
        fun rewardEarned(amount: Int, type: String) = VerificationMessage(
            "✅ Reward earned\n🎁 $amount $type",
            Severity.SUCCESS,
        )

        /** Maps a server-verification outcome to a user-facing message, mirroring iOS. */
        @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
        fun forVerificationResult(result: RewardVerificationResult): VerificationMessage {
            return when (val reward = result.verifiedReward) {
                is VerifiedReward.VirtualCurrency ->
                    VerificationMessage(
                        "✅ Verified\n🎁 Reward granted: ${reward.amount} ${reward.code}",
                        Severity.SUCCESS,
                    )
                VerifiedReward.NoReward ->
                    VerificationMessage("✅ Verified\nℹ️ No reward", Severity.SUCCESS)
                VerifiedReward.UnsupportedReward ->
                    VerificationMessage("✅ Verified\n⚠️ Unsupported reward", Severity.WARNING)
                null -> verificationFailed
                else -> VerificationMessage("✅ Verified\n⚠️ Unhandled reward type", Severity.WARNING)
            }
        }
    }
}
