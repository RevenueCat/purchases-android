package com.revenuecat.purchases.admob.rewardverification

import android.os.Handler
import android.os.Looper
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.admob.RewardVerificationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
private val scope = CoroutineScope(Dispatchers.IO)

internal object Dispatcher {

    fun dispatchStarted(block: (() -> Unit)?) {
        deliverOnMainIfPresent(block)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun dispatchResult(
        consumeCompletionDeliveredToken: () -> Boolean,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
        result: RewardVerificationResult,
    ) {
        deliverResultOnce(consumeCompletionDeliveredToken, rewardVerificationResult, result)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, InternalRevenueCatAPI::class)
    fun dispatch(
        clientTransactionId: String,
        consumeCompletionDeliveredToken: () -> Boolean,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
    ) {
        scope.launch {
            val result = Poller.poll(clientTransactionId)
            deliverResultOnce(consumeCompletionDeliveredToken, rewardVerificationResult, result)
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    internal fun deliverResultOnce(
        consumeCompletionDeliveredToken: () -> Boolean,
        rewardVerificationResult: (RewardVerificationResult) -> Unit,
        result: RewardVerificationResult,
        deliver: ((() -> Unit) -> Unit)? = null,
    ) {
        if (!consumeCompletionDeliveredToken()) {
            return
        }

        val resolvedDeliver = deliver ?: { block -> deliverOnMainIfPresent(block) }
        resolvedDeliver {
            rewardVerificationResult(result)
        }
    }

    private fun deliverOnMainIfPresent(block: (() -> Unit)?) {
        if (block == null) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }
}
