package com.revenuecat.purchases.common.offlineentitlements

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

class PurchasedProductsFetcher(
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
                val activePurchases = activePurchasesByHashedToken.values.toList()
                val purchasedProducts = activePurchases.map {
                    createPurchasedProduct(it, productEntitlementMapping)
                }
                onSuccess(purchasedProducts)
            },
            onError,
        )
    }

    private fun createPurchasedProduct(
        transaction: StoreTransaction,
        productEntitlementMapping: ProductEntitlementMapping,
    ): PurchasedProduct {
        val expirationDate = getExpirationDate(transaction)
        val productIdentifier = transaction.productIds.first()
        val mapping = productEntitlementMapping.mappings[productIdentifier]
        return PurchasedProduct(
            productIdentifier,
            mapping?.basePlanId,
            transaction,
            mapping?.entitlements ?: emptyList(),
            expirationDate,
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
