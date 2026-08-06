@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesException

/**
 * Resolves a checkpoint to the workflow that should run for it, or to the reason none should. The production
 * implementation will read a checkpoints topic from the config endpoint; the PoC stands it in with
 * [RandomWorkflowCheckpointResolver].
 */
internal interface CheckpointWorkflowResolver {

    /**
     * @throws PurchasesException when the checkpoint should be served but can't be resolved.
     */
    suspend fun resolve(identifier: String, customProperties: Map<String, Any>): CheckpointResolution
}
