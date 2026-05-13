package com.revenuecat.purchases.admob

import android.app.Activity
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.awaitGetRewardVerificationStatus
import kotlinx.coroutines.CancellationException
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
    val clientTransactionId = rewardVerificationClientTransactionId(this)
    warnAndAssertIfMissingVerificationState(clientTransactionId)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMainIfPresent(rewardVerificationStarted)
        if (clientTransactionId == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            fetchOneShotVerificationResult(
                clientTransactionId = clientTransactionId,
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
    val clientTransactionId = rewardVerificationClientTransactionId(this)
    warnAndAssertIfMissingVerificationState(clientTransactionId)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMainIfPresent(rewardVerificationStarted)
        if (clientTransactionId == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            fetchOneShotVerificationResult(
                clientTransactionId = clientTransactionId,
                completionDelivered = completionDelivered,
                rewardVerificationResult = rewardVerificationResult,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal suspend fun performOneShotVerification(
    clientTransactionId: String,
    fetchStatus: suspend (String) -> RewardVerificationStatus = {
        Purchases.sharedInstance.awaitGetRewardVerificationStatus(clientTransactionId = it)
    },
): RewardVerificationResult {
    return try {
        mapStatusToResult(fetchStatus(clientTransactionId))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        RewardVerificationResult.failed
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal fun mapStatusToResult(status: RewardVerificationStatus): RewardVerificationResult {
    return when (status) {
        RewardVerificationStatus.VERIFIED -> RewardVerificationResult.verified(VerifiedReward.NoReward)
        // Polling/retry orchestration lands in a follow-up PR; one-shot non-terminal/unknown statuses fail for now.
        RewardVerificationStatus.PENDING,
        RewardVerificationStatus.UNKNOWN,
        RewardVerificationStatus.FAILED,
        -> RewardVerificationResult.failed
    }
}

