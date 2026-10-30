package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
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
            obtainedProducts = customer.obtainedProducts(
                customerInfoBefore,
                presentedOfferings,
                purchase = (flowOutcome as? CheckpointFlowOutcome.Purchased)?.storeTransaction,
            ),
        )
    }

/**
 * The offerings a resolved checkpoint can present: the matched one, or the ones a workflow's steps reference, on
 * the step itself or on its screen. Exit offers are not presented for checkpoints.
 */
internal val CheckpointResolution.presentedOfferings: List<Offering>
    get() = when (this) {
        is CheckpointResolution.MatchedOffering -> listOf(offering)
        is CheckpointResolution.MatchedWorkflow ->
            workflow.steps.values
                .mapNotNull { step ->
                    step.offeringIdentifier ?: step.screenId?.let { workflow.screens[it]?.offeringIdentifier }
                }
                .distinct()
                .mapNotNull { offerings[it] }
        is CheckpointResolution.NoAction -> emptyList()
    }

/**
 * One complete, nonempty entitlement set per offered product, per the SDK's product-entitlement [mapping]: none
 * when it has not been cached yet. Keyed like [purchaseDate]: the store product identifier with its base plan first,
 * the plain one otherwise.
 */
internal fun List<Offering>.entitlementsGrantedBy(mapping: ProductEntitlementMapping?): List<Set<String>> {
    if (mapping == null) {
        Logger.d(
            "No product-entitlement mapping is cached: a restore during the flow " +
                "cannot be tied to the presented products.",
        )
        return emptyList()
    }
    return flatMap { offering -> offering.availablePackages.map { it.product } }
        .mapNotNull { product ->
            val productMapping = mapping.mappings[product.id] ?: mapping.mappings[product.purchasingData.productId]
            productMapping?.entitlements?.toSet()?.takeIf { it.isNotEmpty() }
        }
}

/**
 * The products among those [presentedOfferings] sell purchased since [customerInfoBefore]: those whose purchase
 * date is now known and differs from before (a renewal or a repeat consumable counts). Matched per base plan,
 * since that is how purchases are keyed, then one per store product identifier: a customer holds at most one
 * base plan of a subscription. Each is delivered with the presented offering context of the offering it was
 * bought from when [purchase] says which, of its only presenting offering otherwise, and with none when several
 * offerings presented it and the purchase is not the SDK's own.
 */
internal fun CustomerInfo.obtainedProducts(
    customerInfoBefore: CustomerInfo,
    presentedOfferings: List<Offering>,
    purchase: StoreTransaction?,
): Set<ObtainedProduct> = presentedOfferings
    .flatMap { offering -> offering.availablePackages.map { it.product } }
    .filter { product ->
        val purchaseDate = purchaseDate(product)
        purchaseDate != null && purchaseDate != customerInfoBefore.purchaseDate(product)
    }
    .groupBy { it.purchasingData.productId }
    .values
    .map { instances -> ObtainedProduct(instances.withPresentationContext(purchase)) }
    .toSet()

private fun List<StoreProduct>.withPresentationContext(purchase: StoreTransaction?): StoreProduct {
    val product = first()
    val purchasedFrom = purchase?.presentedOfferingContext?.takeIf {
        product.purchasingData.productId in purchase.productIds || product.id in purchase.productIds
    }
    return when {
        purchasedFrom != null -> product.copyWithPresentedOfferingContext(purchasedFrom)
        distinctBy { it.presentedOfferingContext?.offeringIdentifier }.size == 1 -> product
        else -> product.copyWithPresentedOfferingContext(null)
    }
}

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

/** Whether a complete offered product's entitlement set is active, with at least one entitlement newly active. */
internal fun CustomerInfo.grantsPresentedEntitlement(
    activeEntitlementsBefore: Set<String>?,
    presentedEntitlements: List<Set<String>>,
): Boolean {
    val active = activeEntitlementIds
    val newlyActive = active - activeEntitlementsBefore.orEmpty()
    return presentedEntitlements.any { entitlements ->
        active.containsAll(entitlements) && entitlements.any { it in newlyActive }
    }
}

/** The entitlements active now that were not in [activeEntitlementsBefore]; all of them when that is unknown. */
internal fun CustomerInfo.obtainedEntitlements(
    activeEntitlementsBefore: Set<String>?,
): Set<ObtainedEntitlement> = entitlements.active.values
    .filter { activeEntitlementsBefore == null || it.identifier !in activeEntitlementsBefore }
    .map { ObtainedEntitlement(it) }
    .toSet()
