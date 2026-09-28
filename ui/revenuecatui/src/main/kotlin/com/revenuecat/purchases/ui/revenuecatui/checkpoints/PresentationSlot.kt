package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import kotlinx.coroutines.CompletableDeferred

/**
 * The one-presentation-at-a-time slot: holds the single pending checkpoint call and is itself the gate that
 * enforces that rule, so there is no second piece of state to fall out of sync with. Every accessor matches on
 * the call id, which is what makes a late report from an abandoned or process-restored presentation a no-op
 * instead of something that disturbs the call that replaced it.
 */
internal class PresentationSlot {

    class PendingCall(
        val callId: String,
        // Both null for a call whose flow a PaywallPresenter shows: that UI, and what happens in it, is the
        // presenter's, and the manager only learns how it ended.
        val workflow: CheckpointResolution.MatchedWorkflow?,
        val activeEntitlementsBefore: Set<String>?,
        val customVariables: Map<String, CustomVariableValue>,
        val flowFinished: CompletableDeferred<CheckpointRun>,
    ) {
        // Null until the paywall reports something; kept on the call rather than the presented window so
        // losing the window (configuration change) doesn't reset it.
        var outcome: CheckpointFlowOutcome? = null

        // Only used to take an orphaned workflow window down when its call is abandoned.
        var presenter: CheckpointWorkflowPresenter? = null
    }

    private var pendingCall: PendingCall? = null

    /** Claims the slot for [call]; false while any presentation, SDK- or app-owned, is live. */
    fun claim(call: PendingCall): Boolean =
        synchronized(this) { (pendingCall == null).also { if (it) pendingCall = call } }

    /** Releases the slot and returns its call, but only if [callId] still identifies it. */
    fun take(callId: String): PendingCall? = with(callId) {
        pendingCall = null
        it
    }

    /** Runs [block] under the lock on the pending call, but only if [callId] still identifies it. */
    fun <T> with(callId: String, block: (PendingCall) -> T): T? =
        synchronized(this) { pendingCall?.takeIf { it.callId == callId }?.let(block) }
}
