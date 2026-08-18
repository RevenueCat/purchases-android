package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridges the suspend reward-verification poll to the callback-based public API, so coroutine usage stays
 * out of [com.revenuecat.purchases.Purchases]. Polling runs off the main thread and the result is delivered
 * back on the main thread, like the SDK's other callback APIs.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class RewardVerificationPollLauncher(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    fun launch(
        poll: suspend () -> RewardVerificationResult,
        onCompleted: (RewardVerificationResult) -> Unit,
    ) {
        scope.launch {
            val result = poll()
            withContext(Dispatchers.Main) { onCompleted(result) }
        }
    }

    /**
     * Cancels any in-flight poll. Called when the owning [com.revenuecat.purchases.Purchases] instance is
     * closed so a poll started on it cannot survive close/reconfigure and run against a different SDK.
     */
    fun close() {
        scope.cancel()
    }
}
