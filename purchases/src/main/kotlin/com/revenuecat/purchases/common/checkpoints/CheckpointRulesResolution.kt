package com.revenuecat.purchases.common.checkpoints

/**
 * Outcome of reading one checkpoint's rules from the `checkpoint_rules` topic. It lets the caller tell apart
 * three situations that a bare `CheckpointResponse?` conflates, each of which the checkpoints API reports
 * differently to the app:
 *
 * - [Found]: the checkpoint is configured and its rules were read.
 * - [NotConfigured]: the topic is committed and carries no item for this identifier, so the checkpoint does not
 *   exist in the dashboard. A project with no checkpoints at all still gets the topic committed (empty), so this
 *   requires a committed topic — an absent one is [Unavailable], not an answer.
 * - [Disabled]: the `/v1/config` endpoint is disabled for the session by a 4xx kill switch, so nothing about
 *   this checkpoint is knowable.
 * - [Unavailable]: the rules could not be read — either the topic is not committed (a failed or not-yet-run
 *   sync), or the checkpoint is published but its rules could not be resolved or did not parse.
 */
internal sealed class CheckpointRulesResolution {
    data class Found(
        val checkpoint: CheckpointResponse,
        val configGeneration: Int,
    ) : CheckpointRulesResolution()

    object NotConfigured : CheckpointRulesResolution()

    object Disabled : CheckpointRulesResolution()

    object Unavailable : CheckpointRulesResolution()
}
