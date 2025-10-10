package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import java.util.Locale

/**
 * Mock implementation of [PurchasesType] for previews and test data
 * NOTE: This is only used for UI previews and test data, not for actual testing
 */
internal class MockPurchasesType(
    override val preferredUILocaleOverride: String? = null,
    override val purchasesAreCompletedBy: PurchasesAreCompletedBy = PurchasesAreCompletedBy.REVENUECAT,
    override val storefrontCountryCode: String? = null,
    override val customerCenterListener: CustomerCenterListener? = null,
) : PurchasesType {
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitRestore(): CustomerInfo {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitOfferings(): Offerings {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitCustomerInfo(fetchPolicy: CacheFetchPolicy): CustomerInfo {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitCustomerCenterConfigData(): CustomerCenterConfigData {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitGetProduct(productId: String, basePlan: String?): StoreProduct? {
        throw NotImplementedError("Mock implementation for previews only")
    }
    override suspend fun awaitGetVirtualCurrencies(): VirtualCurrencies {
        throw NotImplementedError("Mock implementation for previews only")
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

    override fun currencyLocaleForStorefrontCountryCode(storefrontCountryCode: String?, locale: Locale): Locale {
        // Just return the current locale for mock
        return locale
    }
}
