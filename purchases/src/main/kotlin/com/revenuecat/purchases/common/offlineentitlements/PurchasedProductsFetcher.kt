package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import java.util.Date
import java.util.concurrent.TimeUnit

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
                val activePurchases = activePurchasesByHashedToken.values
                val purchasedProducts = activePurchases.flatMap {
                    createPurchasedProducts(it, productEntitlementMapping)
                }
                onSuccess(purchasedProducts)
            },
            onError,
        )
    }

    private fun createPurchasedProducts(
        transaction: StoreTransaction,
        productEntitlementMapping: ProductEntitlementMapping,
    ): List<PurchasedProduct> {
        val expirationDate = getExpirationDate(transaction)
        return transaction.productIds.map { productIdentifier ->
            // Build an entry per product in the transaction so multi-line purchases expose all subscriptions.
            val mapping = findMappingForProduct(
                productIdentifier,
                transaction,
                productEntitlementMapping,
            )
            PurchasedProduct(
                productIdentifier,
                mapping?.basePlanId,
                transaction,
                mapping?.entitlements ?: emptyList(),
                expirationDate,
            )
        }
    }

    private fun getExpirationDate(
        purchaseAssociatedToProduct: StoreTransaction,
    ): Date? {
        return when (purchaseAssociatedToProduct.type) {
            ProductType.SUBS -> Date(dateProvider.now.time + TimeUnit.DAYS.toMillis(1))
            else -> null
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun findMappingForProduct(
        productIdentifier: String,
        transaction: StoreTransaction,
        productEntitlementMapping: ProductEntitlementMapping,
    ): ProductEntitlementMapping.Mapping? {
        val possibleBasePlanIds = buildList {
            // TODO: I don't think we'll ever have the option IDs here, we can probably remove this. TODO: Confirm this.
            // Prefer the per-product base plan captured when the purchase was made.
            transaction.subscriptionOptionIdForProductIDs
                ?.get(productIdentifier)
                ?.substringBefore(':')
                ?.let { add(it) }
            // Fall back to the top-level subscription option if the per-product mapping is missing.
            transaction.subscriptionOptionId
                ?.substringBefore(':')
                ?.let { add(it) }
        }.distinct()

        possibleBasePlanIds.forEach { basePlanId ->
            productEntitlementMapping.mappings["$productIdentifier:$basePlanId"]?.let { return it }
        }

        return productEntitlementMapping.mappings[productIdentifier]
            ?: productEntitlementMapping.mappings.values.firstOrNull { mapping ->
                mapping.productIdentifier == productIdentifier &&
                    (possibleBasePlanIds.isEmpty() || possibleBasePlanIds.contains(mapping.basePlanId))
            }
            ?: productEntitlementMapping.mappings.values.firstOrNull { mapping ->
                mapping.productIdentifier == productIdentifier
            }
    }
}
