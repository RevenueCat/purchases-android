package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.rewardverification.RewardVerificationManager
import kotlin.jvm.JvmSynthetic

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun rewardVerificationListener(
    onAd: Any,
    rewardVerificationStarted: (() -> Unit)?,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
): OnUserEarnedRewardListener {
    return OnUserEarnedRewardListener {
        RewardVerificationManager.handleRewardEarned(
            onAd = onAd,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        )
    }
}

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
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    show(
        activity,
        rewardVerificationListener(
            onAd = this,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        ),
    )
}

/**
 * Shows a rewarded ad with reward-verification callbacks and an explicit RevenueCat analytics placement override.
 *
 * [placement] takes precedence over any placement provided at load time.
 * [rewardVerificationStarted] is optional and [rewardVerificationCompleted] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String?,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    show(
        activity,
        placement,
        rewardVerificationListener(
            onAd = this,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        ),
    )
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
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    show(
        activity,
        rewardVerificationListener(
            onAd = this,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        ),
    )
}

/**
 * Shows a rewarded interstitial ad with reward-verification callbacks and an explicit RevenueCat analytics placement
 * override.
 *
 * [placement] takes precedence over any placement provided at load time.
 * [rewardVerificationStarted] is optional and [rewardVerificationCompleted] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String?,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationCompleted: (RewardVerificationResult) -> Unit,
) {
    show(
        activity,
        placement,
        rewardVerificationListener(
            onAd = this,
            rewardVerificationStarted = rewardVerificationStarted,
            rewardVerificationCompleted = rewardVerificationCompleted,
        ),
    )
}
