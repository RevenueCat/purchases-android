package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The entitlements active before a checkpoint runs, so grants can be told apart from what the user already
 * had. Null when there is no cached customer info to compare against; [toGateResult] then counts every
 * entitlement active after the workflow as obtained during it.
 */
internal suspend fun cachedActiveEntitlementIds(purchases: Purchases): Set<String>? =
    suspendCancellableCoroutine { continuation ->
        purchases.getCustomerInfo(
            CacheFetchPolicy.CACHE_ONLY,
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    continuation.resume(customerInfo.entitlements.active.keys)
                }

                override fun onError(error: PurchasesError) {
                    continuation.resume(null)
                }
            },
        )
    }

internal fun CheckpointResult.toGateResult(activeEntitlementsBefore: Set<String>?): CheckpointGateResult =
    when (this) {
        is CheckpointResult.NoAction -> CheckpointGateResult(
            entitlements = emptyList(),
            noWorkflowReason = CheckpointGateResult.NoWorkflowReason(reason.value),
            error = null,
        )
        is CheckpointResult.PaywallPresented -> paywallOutcome.toGateResult(activeEntitlementsBefore)
        else -> errorGateResult(
            PurchasesError(PurchasesErrorCode.UnknownError, "Unknown checkpoint result: $this"),
        )
    }

private fun CheckpointPaywallOutcome.toGateResult(activeEntitlementsBefore: Set<String>?): CheckpointGateResult =
    when (this) {
        is CheckpointPaywallOutcome.Purchased ->
            workflowGateResult(entitlementGrants(customerInfo, activeEntitlementsBefore))
        is CheckpointPaywallOutcome.Restored ->
            workflowGateResult(entitlementGrants(customerInfo, activeEntitlementsBefore))
        is CheckpointPaywallOutcome.Error -> workflowGateResult(error = error)
        // Dismissed, WebCheckoutOpened, and any future outcome without an in-app grant signal.
        else -> workflowGateResult()
    }

private fun entitlementGrants(
    customerInfo: CustomerInfo,
    activeEntitlementsBefore: Set<String>?,
): List<EntitlementGrant> = customerInfo.entitlements.active.keys
    .filter { activeEntitlementsBefore == null || it !in activeEntitlementsBefore }
    .sorted()
    .map { EntitlementGrant(it) }

private fun workflowGateResult(
    entitlements: List<EntitlementGrant> = emptyList(),
    error: PurchasesError? = null,
): CheckpointGateResult = CheckpointGateResult(
    entitlements = entitlements,
    noWorkflowReason = null,
    error = error,
)

internal fun errorGateResult(error: PurchasesError): CheckpointGateResult = CheckpointGateResult(
    entitlements = emptyList(),
    noWorkflowReason = CheckpointGateResult.NoWorkflowReason.ERROR,
    error = error,
)
