package com.revenuecat.purchases.ui.revenuecatui.checkpoints

/** Whether the workflow's gate permits the pending checkpoint to pass. */
internal enum class CheckpointGateOutcome {
    NOT_APPLICABLE,
    PASSED,
    NOT_PASSED,
}

/** The checkpoint's cached entitlement baseline and call-scoped gate reporting channel. */
internal class CheckpointGateContext(
    val initialAppUserId: String?,
    val initialActiveEntitlementIds: Set<String>?,
    val onOutcomeChanged: (CheckpointGateOutcome) -> Unit,
)
