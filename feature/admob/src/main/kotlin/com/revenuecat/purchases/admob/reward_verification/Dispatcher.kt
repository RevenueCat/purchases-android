package com.revenuecat.purchases.admob.reward_verification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
private val scope = CoroutineScope(Dispatchers.IO)

internal object Dispatcher {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    fun dispatch(
        clientTransactionId: String,
        completionDelivered: AtomicBoolean,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        scope.launch {
            val result = Poller.poll(clientTransactionId)
            deliverResultOnce(completionDelivered, rewardVerificationResult, result)
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun deliverResultOnce(
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

    fun deliverOnMainIfPresent(block: (() -> Unit)?) {
        if (block == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}
