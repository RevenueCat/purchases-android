package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasedProductsFetcher(
    private val deviceCache: DeviceCache,
    private val billing: BillingAbstract,
    private val dateProvider: DateProvider = DefaultDateProvider(),
) {

    fun queryActiveProducts(
        appUserID: String,
        onSuccess: (List<PurchasedProduct>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val productEntitlementMapping = deviceCache.getProductEntitlementMapping() ?: run {
            onError(
                PurchasesError(
                    PurchasesErrorCode.CustomerInfoError,
                    OfflineEntitlementsStrings.PRODUCT_ENTITLEMENT_MAPPING_REQUIRED,
                ),
            )
            return
        }

        billing.queryPurchases(
            appUserID,
            onSuccess = { activePurchasesByHashedToken ->
                val unsyncedPurchases = deviceCache.getActivePurchasesNotInCache(activePurchasesByHashedToken)
                val activePurchases = activePurchasesByHashedToken.values
                    .filter { it.purchaseState != PurchaseState.PENDING }
                val purchasedProducts = activePurchases.flatMap {
                    createPurchasedProducts(it, productEntitlementMapping, isSynced = it !in unsyncedPurchases)
                }
                onSuccess(purchasedProducts)
            },
            onError,
        )
    }

    private fun createPurchasedProducts(
        transaction: StoreTransaction,
        productEntitlementMapping: ProductEntitlementMapping,
        isSynced: Boolean,
    ): List<PurchasedProduct> {
        val expirationDate = getExpirationDate(transaction)

        return transaction.productIds
            .map { productIdentifier ->
                val mapping = findMapping(productIdentifier, productEntitlementMapping)
                PurchasedProduct(
                    productIdentifier,
                    mapping?.basePlanId,
                    transaction,
                    mapping?.entitlements ?: emptyList(),
                    expirationDate,
                    isSynced,
                )
            }
    }

    /**
     * Queried transactions only contain the subscription identifier, not the base plan that was purchased, and
     * the mapping for the subscription identifier is the one of the backwards compatible base plan. So when the
     * base plans of a subscription unlock different entitlements, we can't know which ones to unlock. In that
     * case we only unlock the entitlements that all of its base plans unlock, and we don't report a base plan,
     * instead of unlocking the backwards compatible base plan's entitlements.
     */
    private fun findMapping(
        productIdentifier: String,
        productEntitlementMapping: ProductEntitlementMapping,
    ): ProductEntitlementMapping.Mapping? {
        val mapping = productEntitlementMapping.mappings[productIdentifier]
        val entitlementsPerBasePlan = productEntitlementMapping.mappings
            .filterKeys { it != productIdentifier }
            .values
            .filter { it.productIdentifier == productIdentifier }
            .map { it.entitlements.toSet() }
        val mappingEntitlements = mapping?.entitlements?.toSet() ?: emptySet()
        if (entitlementsPerBasePlan.all { it == mappingEntitlements }) {
            return mapping
        }

        val entitlementsUnlockedByAllBasePlans = mapping?.entitlements
            ?.filter { entitlement -> entitlementsPerBasePlan.all { entitlement in it } }
            ?: emptyList()
        warnLog {
            OfflineEntitlementsStrings.UNKNOWN_BASE_PLAN_FOR_PRODUCT.format(
                productIdentifier,
                entitlementsUnlockedByAllBasePlans,
            )
        }
        return ProductEntitlementMapping.Mapping(
            productIdentifier = productIdentifier,
            basePlanId = null,
            entitlements = entitlementsUnlockedByAllBasePlans,
        )
    }

    private fun getExpirationDate(
        purchaseAssociatedToProduct: StoreTransaction,
    ): Date? {
        return when (purchaseAssociatedToProduct.type) {
            ProductType.SUBS -> Date(dateProvider.now.time + TimeUnit.DAYS.toMillis(1))
            else -> null
        }
    }
}
