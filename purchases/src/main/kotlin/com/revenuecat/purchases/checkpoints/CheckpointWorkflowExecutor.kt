@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesException

/**
 * Runs the workflow resolved for a checkpoint. UI presentation ([UiCheckpointWorkflowExecutor]) is one execution
 * strategy; workflows made only of non-UI steps will be run by a headless executor behind this same seam.
 */
internal interface CheckpointWorkflowExecutor {

    /**
     * Runs the workflow in [presentation], suspending until it reaches its terminal state.
     *
     * @throws PurchasesException when the workflow cannot be run.
     */
    suspend fun execute(presentation: CheckpointWorkflowPresentation): CheckpointWorkflowOutcome
}

internal sealed class CheckpointWorkflowOutcome {
    data class PaywallFinished(val paywallOutcome: CheckpointPaywallOutcome) : CheckpointWorkflowOutcome()
    data class AdFinished(val adOutcome: CheckpointAdOutcome) : CheckpointWorkflowOutcome()
}

/** POC outcome for an ad checkpoint. */
@InternalRevenueCatAPI
public sealed class CheckpointAdOutcome {
    @InternalRevenueCatAPI
    public object Shown : CheckpointAdOutcome()

    @InternalRevenueCatAPI
    public data class Failed(val reason: String) : CheckpointAdOutcome()
}
