package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlin.jvm.JvmSynthetic

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@JvmSynthetic
public fun RewardedAd.enableRewardVerification() {
    throw NotImplementedError("AdMob reward verification is not implemented yet.")
}

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@JvmSynthetic
public fun RewardedInterstitialAd.enableRewardVerification() {
    throw NotImplementedError("AdMob reward verification is not implemented yet.")
}

/**
 * Shows a rewarded ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationResult] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    throw NotImplementedError("AdMob reward verification is not implemented yet.")
}

/**
 * Shows a rewarded interstitial ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationResult] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    throw NotImplementedError("AdMob reward verification is not implemented yet.")
}
