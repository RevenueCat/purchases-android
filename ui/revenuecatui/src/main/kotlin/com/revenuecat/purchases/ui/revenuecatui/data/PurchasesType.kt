package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.awaitCustomerCenterConfigData
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetProducts
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct

/**
 * Abstraction over [Purchases] that can be mocked.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal interface PurchasesType {
    suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult

    suspend fun awaitRestore(): CustomerInfo

    suspend fun awaitOfferings(): Offerings

    suspend fun awaitCustomerInfo(
        fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.default(),
    ): CustomerInfo

    suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData

    suspend fun awaitGetProduct(productId: String, basePlan: String?): StoreProduct?

    fun track(event: FeatureEvent)

    val purchasesAreCompletedBy: PurchasesAreCompletedBy

    fun syncPurchases()

    val storefrontCountryCode: String?

    val customerCenterListener: CustomerCenterListener?
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
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
        return products.firstOrNull {
            it.googleProduct?.basePlanId == basePlan
        } ?: products.firstOrNull()
    }

    override fun track(event: FeatureEvent) {
        purchases.track(event)
    }

    override fun syncPurchases() {
        purchases.syncPurchases()
    }

    override val purchasesAreCompletedBy: PurchasesAreCompletedBy
        get() = purchases.purchasesAreCompletedBy

    override val storefrontCountryCode: String?
        get() = purchases.storefrontCountryCode

    override val customerCenterListener: CustomerCenterListener?
        get() = purchases.customerCenterListener
}
