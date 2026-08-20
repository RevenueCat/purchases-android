@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.localrules.RulesDimensionValue

/**
 * Resolves a checkpoint to the workflow that should run for it, or to the reason none should.
 * [CheckpointWorkflowResolverImpl] does this from the `checkpoint_rules` remote-config topic.
 */
internal interface CheckpointWorkflowResolver {

    /**
     * @throws PurchasesException when the checkpoint should be served but can't be resolved.
     */
    suspend fun resolve(identifier: String, customVariables: Map<String, RulesDimensionValue>): CheckpointResolution
}
