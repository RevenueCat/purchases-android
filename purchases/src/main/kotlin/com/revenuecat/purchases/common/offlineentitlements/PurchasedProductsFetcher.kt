package com.revenuecat.purchases.common.offlineentitlements

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.Constants
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.LocalTransactionMetadataStore
import com.revenuecat.purchases.common.warnLog
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
        val receiptInfo = localTransactionMetadataStore
            .getLocalTransactionMetadata(transaction.purchaseToken)
            ?.receiptInfo
        val periodType = getPeriodType(transaction, receiptInfo)

        return transaction.productIds
            .map { productIdentifier ->
                val mapping = findMapping(
                    productEntitlementMapping,
                    productIdentifier,
                    purchasedBasePlanId = receiptInfo?.basePlanIdFor(productIdentifier),
                )
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
     * Play Billing's `Purchase` carries no base plan, so a queried purchase of a subscription with several base
     * plans cannot be attributed to the one that was bought. Resolving the product identifier alone would grant the
     * entitlements of the backwards compatible base plan, which may be a plan the user never bought. When the base
     * plan is unknown and the product's base plans are not equally entitled, only the entitlements every base plan
     * grants are safe to unlock.
     */
    private fun findMapping(
        productEntitlementMapping: ProductEntitlementMapping,
        productIdentifier: String,
        purchasedBasePlanId: String?,
    ): ProductEntitlementMapping.Mapping? {
        val mappings = productEntitlementMapping.mappings
        val purchasedBasePlanMapping = purchasedBasePlanId?.let {
            mappings["$productIdentifier${Constants.SUBS_ID_BASE_PLAN_ID_SEPARATOR}$it"]
        }
        if (purchasedBasePlanMapping != null) return purchasedBasePlanMapping

        val mapping = mappings[productIdentifier]
        val entitlementsByBasePlan = mappings.values
            .filter { it.productIdentifier == productIdentifier }
            .map { it.entitlements.toSet() }
        return if (mapping == null || entitlementsByBasePlan.distinct().size <= 1) {
            mapping
        } else {
            val sharedEntitlements = entitlementsByBasePlan
                .reduce { shared, entitlements -> shared intersect entitlements }
                .toList()
            warnLog {
                OfflineEntitlementsStrings.UNKNOWN_BASE_PLAN_FOR_PURCHASE.format(productIdentifier, sharedEntitlements)
            }
            mapping.copy(basePlanId = null, entitlements = sharedEntitlements)
        }
    }

    /**
     * Play Billing's `Purchase` carries no offer or pricing phase. The purchase flow's phases are only stored until
     * the receipt is posted, and describe a single line.
     */
    private fun getPeriodType(transaction: StoreTransaction, receiptInfo: ReceiptInfo?): PeriodType {
        if (transaction.productIds.size != 1) return PeriodType.NORMAL
        val pricingPhases = receiptInfo?.pricingPhases
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
