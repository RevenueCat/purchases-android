package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.rewardverification.RewardVerificationManager
import kotlin.jvm.JvmSynthetic

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedAd.enableRewardVerification() {
    RewardVerificationManager.install(this)
}

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.enableRewardVerification() {
    RewardVerificationManager.install(this)
}

/**
 * Shows a rewarded ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationCompleted] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    val ad = this

    ad.show(activity, placement) {
        RewardVerificationManager.handleRewardEarned(
            onAd = ad,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}

/**
 * Shows a rewarded interstitial ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationCompleted] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    val ad = this

    ad.show(activity, placement) {
        RewardVerificationManager.handleRewardEarned(
            onAd = ad,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}
