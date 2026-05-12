package com.revenuecat.purchases.admob

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.RewardVerificationStatus
import com.revenuecat.purchases.awaitGetRewardVerificationStatus
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.JvmSynthetic

private const val TAG = "PurchasesAdMob"
private val mainHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
private val rewardVerificationScope = CoroutineScope(Dispatchers.IO)

private data class RewardVerificationState(
    val clientTransactionId: String,
)

private object RewardVerificationStateStore {
    private val stateByAd: MutableMap<Any, RewardVerificationState> = WeakHashMap()

    @Synchronized
    fun set(ad: Any, state: RewardVerificationState) {
        stateByAd[ad] = state
    }

    @Synchronized
    fun get(ad: Any): RewardVerificationState? {
        return stateByAd[ad]
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
    val state = RewardVerificationStateStore.get(this)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMain(rewardVerificationStarted)
        if (state == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            fetchOneShotVerificationResult(
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
    val state = RewardVerificationStateStore.get(this)
    val completionDelivered = AtomicBoolean(false)

    this.show(activity, placement) {
        deliverOnMain(rewardVerificationStarted)
        if (state == null) {
            deliverResultOnce(completionDelivered, rewardVerificationResult, RewardVerificationResult.failed)
        } else {
            fetchOneShotVerificationResult(
                clientTransactionId = state.clientTransactionId,
                completionDelivered = completionDelivered,
                rewardVerificationResult = rewardVerificationResult,
            )
        }
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun enableRewardVerificationInternal(
    ad: Any,
) {
    if (!Purchases.isConfigured) {
        Log.w(
            TAG,
            "Purchases is not configured. Call Purchases.configure() before enabling reward verification.",
        )
        return
    }

    val clientTransactionId = UUID.randomUUID().toString()
    RewardVerificationStateStore.set(ad, RewardVerificationState(clientTransactionId))
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
private fun fetchOneShotVerificationResult(
    clientTransactionId: String,
    completionDelivered: AtomicBoolean,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    rewardVerificationScope.launch {
        val result = performOneShotVerification(clientTransactionId)

        deliverResultOnce(completionDelivered, rewardVerificationResult, result)
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
private fun deliverResultOnce(
    completionDelivered: AtomicBoolean,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
    result: RewardVerificationResult,
) {
    if (!completionDelivered.compareAndSet(false, true)) {
        return
    }

    deliverOnMain {
        rewardVerificationResult(result)
    }
}

private fun deliverOnMain(block: (() -> Unit)?) {
    if (block == null) return
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post { block() }
    }
}
