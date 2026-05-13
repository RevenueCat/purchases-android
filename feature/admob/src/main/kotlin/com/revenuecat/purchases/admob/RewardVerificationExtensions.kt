package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.reward_verification.dispatchOneShotVerificationResult
import com.revenuecat.purchases.admob.reward_verification.deliverOnMainIfPresent
import com.revenuecat.purchases.admob.reward_verification.deliverResultOnce
import com.revenuecat.purchases.admob.reward_verification.enableRewardVerificationInternal
import com.revenuecat.purchases.admob.reward_verification.verificationStateForAd
import com.revenuecat.purchases.admob.reward_verification.warnAndAssertIfMissingState
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.JvmSynthetic

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedAd.enableRewardVerification() {
    enableRewardVerificationInternal(this)
}

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.enableRewardVerification() {
    enableRewardVerificationInternal(this)
}

/**
 * Shows a rewarded ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationResult] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Suppress("UnusedParameter")
@JvmSynthetic
public fun RewardedAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    val state = verificationStateForAd(this)
    warnAndAssertIfMissingState(state)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMainIfPresent(rewardVerificationStarted)
        if (state == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            dispatchOneShotVerificationResult(
                clientTransactionId = state.clientTransactionId,
                completionDelivered = completionDelivered,
                rewardVerificationResult = rewardVerificationResult,
            )
        }
    }
}

/**
 * Shows a rewarded interstitial ad with reward-verification callbacks.
 *
 * [rewardVerificationStarted] is optional and [rewardVerificationResult] is required.
 * [enableRewardVerification] must be called before showing the ad with this overload.
 * Callback invocations are delivered on the main thread.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Suppress("UnusedParameter")
@JvmSynthetic
public fun RewardedInterstitialAd.show(
    activity: Activity,
    placement: String? = null,
    rewardVerificationStarted: (() -> Unit)? = null,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    val state = verificationStateForAd(this)
    warnAndAssertIfMissingState(state)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMainIfPresent(rewardVerificationStarted)
        if (state == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            dispatchOneShotVerificationResult(
                clientTransactionId = state.clientTransactionId,
                completionDelivered = completionDelivered,
                rewardVerificationResult = rewardVerificationResult,
            )
        }
    }
}

