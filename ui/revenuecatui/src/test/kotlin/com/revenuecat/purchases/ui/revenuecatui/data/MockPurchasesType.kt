package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CreateSupportTicketResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.ui.revenuecatui.CustomPaywallHandlerFactory
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies

/**
 * Mock implementation of [PurchasesType] for tests and previews
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class MockPurchasesType(
    override val preferredUILocaleOverride: String? = null,
    override val purchasesAreCompletedBy: PurchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
    override val storefrontCountryCode: String? = null,
    override val customerCenterListener: CustomerCenterListener? = null,
    override val customPaywallHandlerFactory: CustomPaywallHandlerFactory? = null,
) : PurchasesType {
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitRestore(): CustomerInfo {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitOfferings(): Offerings {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitCustomerInfo(fetchPolicy: CacheFetchPolicy): CustomerInfo {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitGetProduct(productId: String, basePlan: String?): StoreProduct? {
        throw NotImplementedError("Mock implementation")
    }
    override suspend fun awaitGetVirtualCurrencies(): VirtualCurrencies {
        throw NotImplementedError("Mock implementation")
    }
    override fun invalidateVirtualCurrenciesCache() {
        // No-op for mock
    }
    override fun track(event: FeatureEvent) {
        // No-op for mock
    }
    override fun syncPurchases() {
        // No-op for mock
    }
    override suspend fun awaitCreateSupportTicket(email: String, description: String): CreateSupportTicketResult {
        // No-op for mock - return success to simulate success
        return CreateSupportTicketResult(success = true)
    }
}
