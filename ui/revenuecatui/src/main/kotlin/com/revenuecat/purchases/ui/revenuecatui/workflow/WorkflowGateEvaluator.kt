@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.WorkflowStepGating

internal fun WorkflowStepGating?.isSatisfiedBy(activeEntitlementIds: Set<String>): Boolean = when (this) {
    is WorkflowStepGating.Hard -> entitlementIdentifiers.any { it in activeEntitlementIds }
    WorkflowStepGating.Soft, WorkflowStepGating.Invalid, null -> true
}
