@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * POC dispatcher between [UiCheckpointWorkflowExecutor] and [AdCheckpointWorkflowExecutor], routing on
 * whether the resolved presentation carries an [CheckpointWorkflowPresentation.adUnitId].
 */
internal class CompositeCheckpointWorkflowExecutor(
    private val uiExecutor: CheckpointWorkflowExecutor,
    private val adExecutor: CheckpointWorkflowExecutor,
) : CheckpointWorkflowExecutor {

    override suspend fun execute(presentation: CheckpointWorkflowPresentation): CheckpointWorkflowOutcome =
        if (presentation.adUnitId != null) {
            adExecutor.execute(presentation)
        } else {
            uiExecutor.execute(presentation)
        }
}
