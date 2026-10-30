package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataStore
import com.revenuecat.purchases.models.OfferPaymentMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(InternalRevenueCatAPI::class)
internal class PurchasedProductsFetcher(
    private val deviceCache: DeviceCache,
    private val billing: BillingAbstract,
    private val localTransactionMetadataStore: LocalTransactionMetadataStore,
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
        val periodType = getPeriodType(transaction)

        return transaction.productIds
            .map { productIdentifier ->
                val mapping = productEntitlementMapping.mappings[productIdentifier]
                PurchasedProduct(
                    productIdentifier,
                    mapping?.basePlanId,
                    transaction,
                    mapping?.entitlements ?: emptyList(),
                    expirationDate,
                    isSynced,
                    periodType,
                )
            }
    }

    /**
     * Play Billing's `Purchase` carries no offer or pricing phase. The purchase flow's phases are only stored until
     * the receipt is posted, and describe a single line.
     */
    private fun getPeriodType(transaction: StoreTransaction): PeriodType {
        if (transaction.productIds.size != 1) return PeriodType.NORMAL
        val pricingPhases = localTransactionMetadataStore
            .getLocalTransactionMetadata(transaction.purchaseToken)
            ?.receiptInfo
            ?.pricingPhases
        return when (pricingPhases?.firstOrNull()?.offerPaymentMode) {
            OfferPaymentMode.FREE_TRIAL -> PeriodType.TRIAL
            OfferPaymentMode.SINGLE_PAYMENT,
            OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT,
            -> PeriodType.INTRO
            null -> PeriodType.NORMAL
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
}
