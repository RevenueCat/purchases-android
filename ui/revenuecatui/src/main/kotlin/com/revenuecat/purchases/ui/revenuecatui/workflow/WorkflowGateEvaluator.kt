@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStepGating

/** Whether the gate can block the checkpoint. An [WorkflowStepGating.Invalid] gate fails closed and never passes. */
internal val WorkflowStepGating?.isEnforced: Boolean
    get() = this is WorkflowStepGating.Hard || this == WorkflowStepGating.Invalid

internal fun WorkflowStepGating?.isSatisfiedBy(activeEntitlementIds: Set<String>): Boolean = when (this) {
    is WorkflowStepGating.Hard -> entitlementIdentifiers.any { it in activeEntitlementIds }
    WorkflowStepGating.Invalid -> false
    WorkflowStepGating.Soft, null -> true
}
