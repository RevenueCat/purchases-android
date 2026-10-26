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
 * When the app user changes mid-checkpoint the entitlements held so far belong to the previous user and are
 * unknown for the new one: an anonymous purchase, for instance, is aliased onto a newly logged-in user. The
 * gate fails closed until [resolveIdentity] is given a customer info read for the new user, and [update] is
 * ignored meanwhile because it could carry the previous user's info.
 */
internal class CheckpointWorkflowGateController(private val currentAppUserId: () -> String) {
    private var context: CheckpointGateContext? = null
    private var appUserId: String? = null
    private var identityPending = false
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
        identityPending = false
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

    fun update(customerInfo: CustomerInfo): WorkflowGateDecision {
        if (context == null) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        if (!identityPending) activeEntitlementIds = customerInfo.entitlements.active.keys
        return currentDecision()
    }

    /** Whether the app user changed since the entitlements were known, so [resolveIdentity] is needed. */
    fun needsIdentityResolution(): Boolean {
        refreshIdentity()
        return identityPending
    }

    /**
     * Resolves the gate against [customerInfo] read for [forAppUserId], the app user after an identity change.
     * Ignored when the app user has changed again since, which leaves the resolution pending.
     */
    fun resolveIdentity(forAppUserId: String, customerInfo: CustomerInfo): WorkflowGateDecision {
        if (context == null) return WorkflowGateDecision.NOT_GATED
        refreshIdentity()
        if (forAppUserId == appUserId) {
            identityPending = false
            activeEntitlementIds = customerInfo.entitlements.active.keys
        }
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
            identityPending = true
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
