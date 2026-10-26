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
 * Call-scoped gate state only; the presenter owns navigation, rendering, and completion.
 *
 * Entitlements are only trusted for the app user they were read for. When the app user changes mid-checkpoint
 * they become unknown, and the gate fails closed until [update] is given customer info read for the new user
 * (after an anonymous purchase is aliased onto a newly logged-in user, that info holds the entitlement).
 */
internal class CheckpointWorkflowGateController(private val currentAppUserId: () -> String) {
    private var context: CheckpointGateContext? = null
    private var appUserId: String? = null
    private var activeEntitlementIds: Set<String> = emptySet()
    private var reachedGate: WorkflowStepGating? = null
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
        val gate = step.gating?.takeIf { it.isEnforced }
        if (context == null || gate == null) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        return evaluateGate(gate)
    }

    /**
     * Applies [customerInfo], which was read for [forAppUserId]. Ignored when that is no longer the current app
     * user, as when the host switched users before this info was delivered.
     */
    fun update(forAppUserId: String, customerInfo: CustomerInfo): WorkflowGateDecision {
        if (context == null) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        if (forAppUserId == appUserId) activeEntitlementIds = customerInfo.entitlements.active.keys
        return currentDecision()
    }

    fun blocksForwardNavigation(step: WorkflowStep): Boolean {
        refreshIdentity()
        return context != null && step.gating.isEnforced && !step.gating.isSatisfiedBy(activeEntitlementIds)
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

    private fun currentDecision(): WorkflowGateDecision =
        reachedGate?.let(::evaluateGate) ?: WorkflowGateDecision.NOT_GATED

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
