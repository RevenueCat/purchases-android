package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
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
 * user obtained otherwise, read from [customer] (the information delivered with the run) against
 * [customerInfoBefore], whatever the outcome: a restore that granted nothing new, or a purchase the app's own
 * paywall completed, is not an outcome but still shows in the delivered information. A run that failed, or whose
 * flow ended in an error, is not delivered at all (see [CheckpointsManager.checkpoint]); the null branch for
 * [CheckpointFlowOutcome.Error] only keeps the mapping total.
 */
internal fun CheckpointRun.toResult(customerInfoBefore: CustomerInfo, customer: CustomerInfo): FlowResult? =
    when (flowOutcome) {
        null, is CheckpointFlowOutcome.Error -> null
        else -> FlowResult(
            obtainedEntitlements = customer.obtainedEntitlements(customerInfoBefore.activeEntitlementIds),
            obtainedProducts = customer.obtainedProducts(customerInfoBefore, presentedProducts),
        )
    }

/** The products a resolved checkpoint can sell: the matched offering's, or every offering's for a workflow. */
internal val CheckpointResolution.presentedProducts: List<StoreProduct>
    get() = when (this) {
        is CheckpointResolution.MatchedOffering -> offering.availablePackages.map { it.product }
        is CheckpointResolution.MatchedWorkflow -> offerings.all.values.flatMap { offering ->
            offering.availablePackages.map { it.product }
        }
        is CheckpointResolution.NoAction -> emptyList()
    }

/**
 * The products among [presentedProducts] purchased since [customerInfoBefore]: those whose purchase date is now
 * known and differs from before (a renewal or a repeat consumable counts). One per store product identifier: a
 * customer holds at most one base plan of a subscription, so the first package presenting it stands for all.
 */
internal fun CustomerInfo.obtainedProducts(
    customerInfoBefore: CustomerInfo,
    presentedProducts: List<StoreProduct>,
): Set<ObtainedProduct> = presentedProducts
    .distinctBy { it.purchasingData.productId }
    .filter { product ->
        val purchaseDate = purchaseDate(product)
        purchaseDate != null && purchaseDate != customerInfoBefore.purchaseDate(product)
    }
    .map { ObtainedProduct(it) }
    .toSet()

// Keyed like StoreProduct.id (base plan included for a Play subscription); the plain product identifier is the
// key a purchase recorded without one carries.
private fun CustomerInfo.purchaseDate(product: StoreProduct): Date? =
    getPurchaseDateForProductId(product.id) ?: getPurchaseDateForProductId(product.purchasingData.productId)

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
