package com.revenuecat.purchases.ads.rewardverification

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bridges the suspend reward-verification poll to the callback-based public API, so coroutine usage stays
 * out of [com.revenuecat.purchases.Purchases]. The poll loop only suspends (never blocks), so running it on
 * [Dispatchers.Main] keeps the callback on the main thread like the SDK's other callback APIs.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class RewardVerificationPollLauncher(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    fun launch(
        poll: suspend () -> RewardVerificationResult,
        onCompleted: (RewardVerificationResult) -> Unit,
    ) {
        scope.launch { onCompleted(poll()) }
    }
}
