package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow

/**
 * What a checkpoint resolves to. Exposed to the RevenueCat UI module, which owns the checkpoints API and
 * presents the resolved workflow; never part of the SDK's public surface.
 */
@InternalRevenueCatAPI
public sealed class CheckpointResolution {

    /**
     * The checkpoint selected [offering] without requiring RevenueCat-managed UI.
     *
     * [checkpointRuleId] identifies the rule that was served, for the hit event to attribute. It is null when
     * the rules topic did not carry an id for that rule.
     */
    public data class MatchedOffering(
        val offering: Offering,
        val checkpointRuleId: String?,
    ) : CheckpointResolution()

    /**
     * The checkpoint matched [workflow], which should be presented against [offering].
     *
     * [checkpointRuleId] identifies the rule that was served, for the hit event to attribute. It is null when
     * the rules topic did not carry an id for that rule.
     */
    public data class MatchedWorkflow(
        val workflow: PublishedWorkflow,
        val uiConfig: UiConfig,
        val offering: Offering,
        val checkpointRuleId: String?,
    ) : CheckpointResolution()

    /** Nothing should be served for this checkpoint; the user continues uninterrupted. */
    public data class NoAction(val reason: Reason) : CheckpointResolution() {

        public enum class Reason {
            /** The checkpoint is configured, but no rule matched. */
            NO_MATCH,

            /** The configuration needed to serve the checkpoint could not be read. */
            CONFIGURATION_UNAVAILABLE,

            /** The identifier is not configured in the RevenueCat dashboard. */
            UNKNOWN_CHECKPOINT,
        }
    }
}
