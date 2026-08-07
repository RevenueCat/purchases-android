package com.revenuecat.purchases.common.checkpoints

/**
 * Outcome of reading one checkpoint's rules from the `checkpoint_rules` topic. It lets the caller tell apart
 * three situations that a bare `CheckpointResponse?` conflates, each of which the checkpoints API reports
 * differently to the app:
 *
 * - [Found]: the checkpoint is configured and its rules were read.
 * - [NotConfigured]: the topic was readable and carries no item for this identifier, so the checkpoint does not
 *   exist in the dashboard.
 * - [Disabled]: the `/v1/config` endpoint is disabled for the session by a 4xx kill switch, so nothing about
 *   this checkpoint is knowable.
 * - [Unavailable]: the checkpoint is published, but its rules could not be read or did not parse.
 */
internal sealed class CheckpointRulesResolution {
    data class Found(val checkpoint: CheckpointResponse) : CheckpointRulesResolution()

    object NotConfigured : CheckpointRulesResolution()

    object Disabled : CheckpointRulesResolution()

    object Unavailable : CheckpointRulesResolution()
}
