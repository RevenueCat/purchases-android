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
            noWorkflowReason = reason.toNoWorkflowReason(),
            error = null,
        )
        is CheckpointResult.PaywallPresented -> paywallOutcome.toGateResult(activeEntitlementsBefore)
        else -> errorGateResult(
            PurchasesError(PurchasesErrorCode.UnknownError, "Unknown checkpoint result: $this"),
        )
    }

/**
 * Each reason is mapped to its declared counterpart explicitly, never by assuming the two constant sets share
 * string values. The compiler cannot prove exhaustiveness over a value-based constant class, so
 * CheckpointGateResultMappingTest reflects over the declared [CheckpointResult.NoAction.Reason] constants and
 * fails when one of them falls into the pass-through branch, which only exists for reason values a newer
 * producer sends at runtime.
 */
private fun CheckpointResult.NoAction.Reason.toNoWorkflowReason(): CheckpointGateResult.NoWorkflowReason =
    when (this) {
        CheckpointResult.NoAction.Reason.NO_MATCH -> CheckpointGateResult.NoWorkflowReason.NO_MATCH
        CheckpointResult.NoAction.Reason.HOLDOUT -> CheckpointGateResult.NoWorkflowReason.HOLDOUT
        CheckpointResult.NoAction.Reason.FREQUENCY_CAPPED ->
            CheckpointGateResult.NoWorkflowReason.FREQUENCY_CAPPED
        CheckpointResult.NoAction.Reason.CONFIGURATION_UNAVAILABLE ->
            CheckpointGateResult.NoWorkflowReason.CONFIGURATION_UNAVAILABLE
        CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT ->
            CheckpointGateResult.NoWorkflowReason.UNKNOWN_CHECKPOINT
        CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER ->
            CheckpointGateResult.NoWorkflowReason.INVALID_CHECKPOINT_IDENTIFIER
        else -> CheckpointGateResult.NoWorkflowReason(value)
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
