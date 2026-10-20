package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One customer info read under [policy], delivered as a value: what to do with the error is the caller's. The SDK
 * delivers a cached read inline when called on its handler's thread, so a warm read resumes without a dispatch.
 */
internal suspend fun Purchases.customerInfo(policy: CacheFetchPolicy): Result<CustomerInfo, PurchasesError> =
    suspendCancellableCoroutine { continuation ->
        getCustomerInfo(
            policy,
            object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) = continuation.resume(Result.Success(customerInfo))

                override fun onError(error: PurchasesError) = continuation.resume(Result.Error(error))
            },
        )
    }

/**
 * The entitlements active before a paywall is presented, so grants can be told apart from what the user already
 * had. Null when there is no cached customer info to compare against; every entitlement active afterwards then
 * counts as obtained. Callback form for main-thread callers that cannot suspend.
 */
internal fun Purchases.cachedActiveEntitlementIds(onResult: (Set<String>?) -> Unit) {
    getCustomerInfo(
        CacheFetchPolicy.CACHE_ONLY,
        object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) = onResult(customerInfo.activeEntitlementIds)

            override fun onError(error: PurchasesError) = onResult(null)
        },
    )
}

internal val CustomerInfo.activeEntitlementIds: Set<String>
    get() = entitlements.active.keys

/** The customer info the flow itself produced: a purchase, a granting restore, or the sync after an app-owned flow. */
internal val CheckpointFlowOutcome.customerInfo: CustomerInfo?
    get() = when (this) {
        is CheckpointFlowOutcome.Purchased -> customerInfo
        is CheckpointFlowOutcome.Restored -> customerInfo
        is CheckpointFlowOutcome.Finished -> customerInfo
        else -> null
    }

/**
 * The result to deliver for this run: null when nothing was presented because there was nothing to serve, what the
 * user obtained otherwise. A run that failed, or whose flow ended in an error, is not delivered at all (see
 * [CheckpointsManager.checkpoint]); the null branch for [CheckpointFlowOutcome.Error] only keeps the mapping total.
 * Dismissed, WebCheckoutOpened, and any future outcome without an in-app grant signal obtained nothing.
 */
internal fun CheckpointRun.toResult(activeEntitlementsBefore: Set<String>?): FlowResult? =
    when (val outcome = flowOutcome) {
        null, is CheckpointFlowOutcome.Error -> null
        else -> FlowResult(outcome.customerInfo?.obtainedEntitlements(activeEntitlementsBefore).orEmpty())
    }

/**
 * The customer info to deliver with a passed run: the one the flow produced, else the latest cached copy (a
 * restore that granted nothing, or a purchase the app's own paywall completed, refreshed it without being an
 * outcome), else [customerInfoBefore], which the cache can only have lost to an identity change meanwhile.
 */
internal suspend fun CheckpointRun.customerInfoAfter(
    purchases: Purchases,
    customerInfoBefore: CustomerInfo,
): CustomerInfo = flowOutcome?.customerInfo
    ?: purchases.customerInfo(CacheFetchPolicy.CACHE_ONLY).getOrNull()
    ?: customerInfoBefore

/** Whether the user now holds an entitlement they did not in [activeEntitlementsBefore]. */
internal fun CustomerInfo.grantsNewEntitlements(activeEntitlementsBefore: Set<String>?): Boolean =
    obtainedEntitlements(activeEntitlementsBefore).isNotEmpty()

/** The entitlements active now that were not in [activeEntitlementsBefore]; all of them when that is unknown. */
internal fun CustomerInfo.obtainedEntitlements(
    activeEntitlementsBefore: Set<String>?,
): Set<ObtainedEntitlement> = entitlements.active.values
    .filter { activeEntitlementsBefore == null || it.identifier !in activeEntitlementsBefore }
    .map { ObtainedEntitlement(it) }
    .toSet()
