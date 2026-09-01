package com.revenuecat.purchases.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * What a checkpoint resolved to, as reported in the `result` field of a checkpoint hit.
 */
@Serializable
internal enum class CheckpointHitResult {
    @SerialName("workflow")
    WORKFLOW,

    @SerialName("offering")
    OFFERING,

    @SerialName("no_match")
    NO_MATCH,

    @SerialName("configuration_unavailable")
    CONFIGURATION_UNAVAILABLE,

    @SerialName("unknown_checkpoint")
    UNKNOWN_CHECKPOINT,
}

/**
 * Whether a checkpoint is one RevenueCat defines or one the app declares.
 *
 * Every checkpoint is [CUSTOM] today. [STANDARD] is declared so the backend vocabulary and this one stay in
 * step, and so producing it later is a one-line change rather than a new type.
 */
@Serializable
internal enum class CheckpointType {
    @SerialName("standard")
    STANDARD,

    @SerialName("custom")
    CUSTOM,
}

/**
 * Records that a checkpoint was hit and what it resolved to. This is persisted and sent through the shared
 * analytics events pipeline.
 *
 * [timestamp] is when the user reached the checkpoint, not when the event was created, so hit volume over time
 * stays comparable with events recorded before the outcome was attached.
 */
@OptIn(InternalRevenueCatAPI::class)
internal data class CheckpointEvent(
    val identifier: String,
    val checkpointType: CheckpointType,
    val result: CheckpointHitResult,
    val workflowId: String? = null,
    val offeringId: String? = null,
    val checkpointRuleId: String? = null,
    val id: UUID = UUID.randomUUID(),
    val timestamp: Date = Date(),
) : FeatureEvent

/**
 * Builds the hit event for a resolved checkpoint. [timestamp] is the moment the checkpoint was reached, captured
 * before resolution started.
 */
@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun CheckpointResolution.toCheckpointEvent(identifier: String, timestamp: Date): CheckpointEvent =
    when (this) {
        is CheckpointResolution.MatchedWorkflow -> CheckpointEvent(
            identifier = identifier,
            checkpointType = CheckpointType.CUSTOM,
            result = CheckpointHitResult.WORKFLOW,
            workflowId = workflow.id,
            offeringId = offering.identifier,
            checkpointRuleId = checkpointRuleId,
            timestamp = timestamp,
        )
        is CheckpointResolution.MatchedOffering -> CheckpointEvent(
            identifier = identifier,
            checkpointType = CheckpointType.CUSTOM,
            result = CheckpointHitResult.OFFERING,
            offeringId = offering.identifier,
            checkpointRuleId = checkpointRuleId,
            timestamp = timestamp,
        )
        // A NoAction carries no rule id by construction: CONFIGURATION_UNAVAILABLE is about this SDK's state
        // rather than about the rule that matched.
        is CheckpointResolution.NoAction -> CheckpointEvent(
            identifier = identifier,
            checkpointType = CheckpointType.CUSTOM,
            result = reason.toCheckpointHitResult(),
            timestamp = timestamp,
        )
    }

@OptIn(InternalRevenueCatAPI::class)
private fun CheckpointResolution.NoAction.Reason.toCheckpointHitResult(): CheckpointHitResult =
    when (this) {
        CheckpointResolution.NoAction.Reason.NO_MATCH -> CheckpointHitResult.NO_MATCH
        CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE ->
            CheckpointHitResult.CONFIGURATION_UNAVAILABLE
        CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT -> CheckpointHitResult.UNKNOWN_CHECKPOINT
    }
