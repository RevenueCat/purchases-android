@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError

/**
 * Resolves a checkpoint to the workflow that should run for it, or to the reason none should. The production
 * implementation will read a checkpoints topic from the config endpoint; the PoC stands it in with
 * [RandomWorkflowCheckpointResolver].
 */
internal interface CheckpointWorkflowResolver {
    suspend fun resolve(checkpoint: CheckpointInfo): CheckpointWorkflowResolution
}

internal sealed class CheckpointWorkflowResolution {
    data class Matched(val presentation: CheckpointWorkflowPresentation) : CheckpointWorkflowResolution()
    data class NoMatch(val reason: CheckpointResult.NoAction.Reason) : CheckpointWorkflowResolution()
    data class Failed(val error: PurchasesError) : CheckpointWorkflowResolution()
}
