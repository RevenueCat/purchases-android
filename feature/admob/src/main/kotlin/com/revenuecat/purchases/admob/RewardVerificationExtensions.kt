package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.admob.rewardverification.Dispatcher
import com.revenuecat.purchases.admob.rewardverification.Setup
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
    Setup.install(this)
}

/**
 * Enables RevenueCat reward verification for this ad.
 *
 * Call after the ad has loaded and before showing it when using reward-verification APIs.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@JvmSynthetic
public fun RewardedInterstitialAd.enableRewardVerification() {
    Setup.install(this)
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
    val state = Setup.verificationState(this)
    Setup.warnAndAssertIfMissingState(state)
    val fallbackCompletionDelivered = AtomicBoolean(false)
    val consumeCompletionDeliveredToken = state?.let {
        { it.consumeCompletionDeliveredToken() }
    } ?: {
        fallbackCompletionDelivered.compareAndSet(false, true)
    }

    this.show(activity, placement) {
        Dispatcher.dispatchStarted(rewardVerificationStarted)
        if (state == null) {
            Dispatcher.dispatchResult(
                consumeCompletionDeliveredToken = consumeCompletionDeliveredToken,
                rewardVerificationResult = rewardVerificationResult,
                result = RewardVerificationResult.failed,
            )
        } else {
            Dispatcher.dispatch(
                clientTransactionId = state.clientTransactionId,
                consumeCompletionDeliveredToken = consumeCompletionDeliveredToken,
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
    val state = Setup.verificationState(this)
    Setup.warnAndAssertIfMissingState(state)
    val fallbackCompletionDelivered = AtomicBoolean(false)
    val consumeCompletionDeliveredToken = state?.let {
        { it.consumeCompletionDeliveredToken() }
    } ?: {
        fallbackCompletionDelivered.compareAndSet(false, true)
    }

    this.show(activity, placement) {
        Dispatcher.dispatchStarted(rewardVerificationStarted)
        if (state == null) {
            Dispatcher.dispatchResult(
                consumeCompletionDeliveredToken = consumeCompletionDeliveredToken,
                rewardVerificationResult = rewardVerificationResult,
                result = RewardVerificationResult.failed,
            )
        } else {
            Dispatcher.dispatch(
                clientTransactionId = state.clientTransactionId,
                consumeCompletionDeliveredToken = consumeCompletionDeliveredToken,
                rewardVerificationResult = rewardVerificationResult,
            )
        }
    }
}
