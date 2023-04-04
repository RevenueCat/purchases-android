package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.models.StoreTransaction
import java.util.Date
import java.util.concurrent.TimeUnit

class PurchasedProductsFetcher(
    private val deviceCache: DeviceCache,
    private val billing: BillingAbstract,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {

    fun queryPurchasedProducts(
        appUserID: String,
        onSuccess: (List<PurchasedProduct>) -> Unit,
        onError: (PurchasesError) -> Unit
    ) {
        val productEntitlementMapping = deviceCache.getProductEntitlementMapping()

        billing.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = { allPurchases ->
                billing.queryPurchases(
                    appUserID,
                    onSuccess = { activePurchasesByHashedToken ->
                        val purchasedProductIdentifiers = allPurchases.map { it.skus[0] }.toSet()
                        val activeProducts = activePurchasesByHashedToken.values.map { it.skus[0] }.toSet()
                        val purchasedProducts = purchasedProductIdentifiers.map { productIdentifier ->
                            createPurchasedProduct(
                                allPurchases,
                                productIdentifier,
                                activeProducts,
                                productEntitlementMapping
                            )
                        }
                        onSuccess(purchasedProducts)
                    },
                    onError
                )
            },
            onError
        )
    }

    private fun createPurchasedProduct(
        allPurchases: List<StoreTransaction>,
        productIdentifier: String,
        activeProducts: Set<String>,
        productEntitlementMapping: ProductEntitlementMapping?
    ): PurchasedProduct {
        val purchaseAssociatedToProduct = allPurchases.first { it.skus[0] == productIdentifier }
        val isActive = activeProducts.contains(productIdentifier)
        val expirationDate = getExpirationDate(isActive, purchaseAssociatedToProduct)
        return PurchasedProduct(
            productIdentifier,
            purchaseAssociatedToProduct,
            isActive,
            productEntitlementMapping?.toMap()?.get(productIdentifier) ?: emptyList(),
            expirationDate
        )
    }

    private fun getExpirationDate(
        isActive: Boolean,
        purchaseAssociatedToProduct: StoreTransaction
    ): Date? {
        return if (purchaseAssociatedToProduct.type == ProductType.SUBS) {
            if (isActive) {
                Date(dateProvider.now.time + TimeUnit.DAYS.toMillis(1))
            } else {
                Date(purchaseAssociatedToProduct.purchaseTime)
            }
        } else {
            null
        }
    }
}
