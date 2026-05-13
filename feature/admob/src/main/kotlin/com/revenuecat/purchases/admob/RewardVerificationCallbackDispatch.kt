package com.revenuecat.purchases.admob

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val rewardVerificationMainHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
private val rewardVerificationScope = CoroutineScope(Dispatchers.IO)

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
internal fun fetchOneShotVerificationResult(
    clientTransactionId: String,
    completionDelivered: AtomicBoolean,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
) {
    rewardVerificationScope.launch {
        val result = performOneShotVerification(clientTransactionId)
        deliverResultOnce(completionDelivered, rewardVerificationResult, result)
    }
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal fun deliverResultOnce(
    completionDelivered: AtomicBoolean,
    rewardVerificationResult: (RewardVerificationResult) -> Unit,
    result: RewardVerificationResult,
) {
    if (!completionDelivered.compareAndSet(false, true)) {
        return
    }

    deliverOnMainIfPresent {
        rewardVerificationResult(result)
    }
}

internal fun deliverOnMainIfPresent(block: (() -> Unit)?) {
    if (block == null) return
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        rewardVerificationMainHandler.post { block() }
    }
}
