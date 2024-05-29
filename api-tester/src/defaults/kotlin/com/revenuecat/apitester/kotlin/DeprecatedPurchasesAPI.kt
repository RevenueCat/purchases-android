package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getNonSubscriptionSkusWith
import com.revenuecat.purchases.getSubscriptionSkusWith
import com.revenuecat.purchases.models.StoreProduct

@Suppress("unused")
private class DeprecatedPurchasesAPI {
    @SuppressWarnings("LongParameterList", "LongMethod", "EmptyFunctionBlock")
    fun check(
        purchases: Purchases,
    ) {
        purchases.allowSharingPlayStoreAccount = true
        purchases.getSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> },
        )
        purchases.getNonSubscriptionSkusWith(
            ArrayList<String>(),
            onError = { _: PurchasesError -> },
            onReceiveSkus = { _: List<StoreProduct> -> },
        )

        Purchases.debugLogsEnabled = false
        val debugLogs: Boolean = Purchases.debugLogsEnabled
    }
}
