@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowStepGating
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateOutcome

internal enum class WorkflowGateDecision { NOT_GATED, PASSED, BLOCKED }

/** Call-scoped gate state only; the presenter owns navigation, rendering, and completion. */
internal class CheckpointWorkflowGateController(private val currentAppUserId: () -> String) {
    private var context: CheckpointGateContext? = null
    private var appUserId: String? = null
    private var activeEntitlementIds: Set<String> = emptySet()
    private var reachedGate: WorkflowStepGating.Hard? = null
    private var outcome = CheckpointGateOutcome.NOT_APPLICABLE

    val hasUnresolvedGate: Boolean
        get() {
            refreshIdentity()
            return outcome == CheckpointGateOutcome.NOT_PASSED
        }

    fun reset(context: CheckpointGateContext?) {
        this.context = context
        appUserId = context?.initialAppUserId
        activeEntitlementIds = context?.initialActiveEntitlementIds.orEmpty()
        reachedGate = null
        outcome = CheckpointGateOutcome.NOT_APPLICABLE
        context?.onOutcomeChanged?.invoke(outcome)
    }

    fun evaluate(step: WorkflowStep): WorkflowGateDecision {
        val gate = step.gating
        if (context == null || gate !is WorkflowStepGating.Hard) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        return evaluateGate(gate)
    }

    fun update(customerInfo: CustomerInfo): WorkflowGateDecision {
        if (context == null) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        activeEntitlementIds = customerInfo.entitlements.active.keys
        return reachedGate?.let(::evaluateGate) ?: WorkflowGateDecision.NOT_GATED
    }

    fun blocksForwardNavigation(step: WorkflowStep): Boolean {
        refreshIdentity()
        return context != null && step.gating is WorkflowStepGating.Hard &&
            !step.gating.isSatisfiedBy(activeEntitlementIds)
    }

    fun isCurrentAppUserId(operationAppUserId: String): Boolean {
        refreshIdentity()
        return currentAppUserId() == operationAppUserId
    }

    private fun refreshIdentity() {
        if (context == null) return
        val currentId = currentAppUserId()
        if (appUserId != currentId) {
            appUserId = currentId
            activeEntitlementIds = emptySet()
            // Retain reached gates even after passage: host callbacks can switch identity before dismissal.
            reachedGate?.let(::evaluateGate)
        }
    }

    private fun evaluateGate(gate: WorkflowStepGating.Hard): WorkflowGateDecision {
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
