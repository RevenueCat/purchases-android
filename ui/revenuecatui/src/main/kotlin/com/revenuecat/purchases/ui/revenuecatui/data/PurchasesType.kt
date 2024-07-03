package com.revenuecat.purchases.ui.revenuecatui.data

import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.models.MyAppPurchaseLogic

/**
 * Abstraction over [Purchases] that can be mocked.
 */
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal interface PurchasesType {
    suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult

    suspend fun awaitRestore(myAppPurchaseLogic: MyAppPurchaseLogic?): CustomerInfo

    suspend fun awaitOfferings(): Offerings

    suspend fun awaitCustomerInfo(
        fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.default(),
    ): CustomerInfo

    fun track(event: PaywallEvent)
}

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
internal class PurchasesImpl(private val purchases: Purchases = Purchases.sharedInstance) : PurchasesType {
    override suspend fun awaitPurchase(purchaseParams: PurchaseParams.Builder): PurchaseResult {
        return purchases.awaitPurchase(purchaseParams.build())
    }

    override suspend fun awaitRestore(myAppPurchaseLogic: MyAppPurchaseLogic?): CustomerInfo {
        return purchases.awaitRestore(myAppPurchaseLogic)
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
}
