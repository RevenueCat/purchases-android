package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The entitlements active before a checkpoint runs, so grants can be told apart from what the user already
 * had. Null when there is no cached customer info to compare against; [toResult] then counts every
 * entitlement active after the flow as obtained during it.
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

/**
 * The result to deliver for this run: null when nothing was presented (nothing to serve, a presentation that
 * failed, or a flow that ended in an error), what the user obtained otherwise.
 */
internal fun CheckpointRun.toResult(activeEntitlementsBefore: Set<String>?): FlowResult? =
    when (val outcome = flowOutcome) {
        null, is CheckpointFlowOutcome.Error -> null
        is CheckpointFlowOutcome.Purchased ->
            FlowResult(obtainedEntitlements(outcome.customerInfo, activeEntitlementsBefore))
        is CheckpointFlowOutcome.Restored ->
            FlowResult(obtainedEntitlements(outcome.customerInfo, activeEntitlementsBefore))
        is CheckpointFlowOutcome.Finished ->
            FlowResult(obtainedEntitlements(outcome.customerInfo, activeEntitlementsBefore))
        // Dismissed, WebCheckoutOpened, and any future outcome without an in-app grant signal.
        else -> FlowResult(obtainedEntitlements = emptySet())
    }

private fun obtainedEntitlements(
    customerInfo: CustomerInfo,
    activeEntitlementsBefore: Set<String>?,
): Set<ObtainedEntitlement> = customerInfo.entitlements.active.values
    .filter { activeEntitlementsBefore == null || it.identifier !in activeEntitlementsBefore }
    .map { ObtainedEntitlement(it) }
    .toSet()
