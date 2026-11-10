package com.revenuecat.purchases.ui.revenuecatui.checkpoints

/** Whether the workflow's gate permits the pending checkpoint to pass. */
internal enum class CheckpointGateOutcome {
    NOT_APPLICABLE,
    PASSED,
    NOT_PASSED,
}

/**
 * The checkpoint's call-scoped gate reporting channel. The presented workflow reads the customer's entitlements
 * itself when it reaches a gate.
 */
internal class CheckpointGateContext(
    val onOutcomeChanged: (CheckpointGateOutcome) -> Unit,
)
