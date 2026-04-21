package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CreateSupportTicketResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCreateSupportTicket
import com.revenuecat.purchases.awaitCustomerCenterConfigData
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetProducts
import com.revenuecat.purchases.awaitGetVirtualCurrencies
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

/**
 * Abstraction over [Purchases] that can be mocked.
 */
@Suppress("TooManyFunctions")
internal interface PurchasesType {
    suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult

    suspend fun awaitRestore(): CustomerInfo

    suspend fun awaitOfferings(): Offerings

    suspend fun awaitCustomerInfo(
        fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.default(),
    ): CustomerInfo

    suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData

    suspend fun awaitGetProduct(productId: String, basePlan: String?): StoreProduct?

    @Throws(PurchasesException::class)
    suspend fun awaitGetVirtualCurrencies(): VirtualCurrencies

    fun invalidateVirtualCurrenciesCache()

    fun track(event: FeatureEvent)

    val purchasesAreCompletedBy: PurchasesAreCompletedBy

    suspend fun awaitSyncPurchases(): CustomerInfo

    val storefrontCountryCode: String?

    val customerCenterListener: CustomerCenterListener?

    val preferredUILocaleOverride: String?

    @Throws(PurchasesException::class)
    suspend fun awaitCreateSupportTicket(email: String, description: String): CreateSupportTicketResult
}

@Suppress("TooManyFunctions")
internal class PurchasesImpl(private val purchases: Purchases = Purchases.sharedInstance) : PurchasesType {
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult {
        return purchases.awaitPurchase(purchaseParams.build())
    }

    override suspend fun awaitRestore(): CustomerInfo {
        return purchases.awaitRestore()
    }

    override suspend fun awaitOfferings(): Offerings {
        return purchases.awaitOfferings()
    }

    override suspend fun awaitCustomerInfo(fetchPolicy: CacheFetchPolicy): CustomerInfo {
        return purchases.awaitCustomerInfo(fetchPolicy)
    }

    override suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData {
        return purchases.awaitCustomerCenterConfigData()
    }

    override suspend fun awaitGetProduct(productId: String, basePlan: String?): StoreProduct? {
        val products = purchases.awaitGetProducts(listOf(productId))
        // Get products returns one product per base plan ID if it is a subscription product.
        // If the product is an in-app product, it will return a single product.
        return if (basePlan == null) {
            products.firstOrNull()
        } else {
            products.firstOrNull { it.googleProduct?.basePlanId == basePlan }
        }
    }

    @Throws(PurchasesException::class)
    override suspend fun awaitGetVirtualCurrencies(): VirtualCurrencies {
        return purchases.awaitGetVirtualCurrencies()
    }

    override fun invalidateVirtualCurrenciesCache() {
        purchases.invalidateVirtualCurrenciesCache()
    }

    override fun track(event: FeatureEvent) {
        purchases.track(event)
    }

    override suspend fun awaitSyncPurchases(): CustomerInfo {
        return purchases.awaitSyncPurchases()
    }

    override val purchasesAreCompletedBy: PurchasesAreCompletedBy
        get() = purchases.purchasesAreCompletedBy

    override val storefrontCountryCode: String?
        get() = purchases.storefrontCountryCode

    override val customerCenterListener: CustomerCenterListener?
        get() = purchases.customerCenterListener

    override val preferredUILocaleOverride: String?
        get() = purchases.preferredUILocaleOverride

    @Throws(PurchasesException::class)
    override suspend fun awaitCreateSupportTicket(email: String, description: String): CreateSupportTicketResult {
        return purchases.awaitCreateSupportTicket(email, description)
    }
}
