package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.paywalls.events.PaywallEvent

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

    fun track(event: PaywallEvent)

    val purchasesAreCompletedBy: PurchasesAreCompletedBy

    fun syncPurchases()
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

    override fun track(event: PaywallEvent) {
        purchases.track(event)
    }

    override fun syncPurchases() {
        purchases.syncPurchases()
    }

    override val purchasesAreCompletedBy: PurchasesAreCompletedBy
        get() = purchases.purchasesAreCompletedBy
}
