@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowStepGating
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateOutcome

internal enum class WorkflowGateDecision { NOT_GATED, PASSED, BLOCKED }

/**
 * Call-scoped gate state only; the presenter owns navigation, rendering, completion, and reading the customer's
 * entitlements when a gate is reached.
 */
internal class CheckpointWorkflowGateController {
    private var context: CheckpointGateContext? = null
    private var activeEntitlementIds: Set<String> = emptySet()
    private var reachedGate: WorkflowStepGating? = null
    private var outcome = CheckpointGateOutcome.NOT_APPLICABLE

    val hasUnresolvedGate: Boolean
        get() = outcome == CheckpointGateOutcome.NOT_PASSED

    fun reset(context: CheckpointGateContext?) {
        this.context = context
        activeEntitlementIds = emptySet()
        reachedGate = null
        outcome = CheckpointGateOutcome.NOT_APPLICABLE
        context?.onOutcomeChanged?.invoke(outcome)
    }

    /** Whether entering [step] needs the customer's current entitlements first. */
    fun isGated(step: WorkflowStep): Boolean = context != null && step.gating.isEnforced

    /** Evaluates [step]'s gate against [activeEntitlementIds], read for the customer as the gate was reached. */
    fun evaluate(step: WorkflowStep, activeEntitlementIds: Set<String>): WorkflowGateDecision {
        val gate = step.gating?.takeIf { it.isEnforced }
        if (context == null || gate == null) return WorkflowGateDecision.NOT_GATED
        this.activeEntitlementIds = activeEntitlementIds
        return evaluateGate(gate)
    }

    /** Re-evaluates the reached gate, if any, against [customerInfo] from a purchase or restore. */
    fun update(customerInfo: CustomerInfo): WorkflowGateDecision {
        if (context == null) return WorkflowGateDecision.NOT_GATED
        activeEntitlementIds = customerInfo.entitlements.active.keys
        return reachedGate?.let(::evaluateGate) ?: WorkflowGateDecision.NOT_GATED
    }

    fun blocksForwardNavigation(step: WorkflowStep): Boolean =
        context != null && step.gating.isEnforced && !step.gating.isSatisfiedBy(activeEntitlementIds)

    private fun evaluateGate(gate: WorkflowStepGating): WorkflowGateDecision {
        val passed = gate.isSatisfiedBy(activeEntitlementIds)
        reachedGate = gate
        val newOutcome = if (passed) CheckpointGateOutcome.PASSED else CheckpointGateOutcome.NOT_PASSED
        if (newOutcome != outcome) {
            outcome = newOutcome
            context?.onOutcomeChanged?.invoke(newOutcome)
        }
        return if (passed) WorkflowGateDecision.PASSED else WorkflowGateDecision.BLOCKED
    }
}
