package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.Purchases

@Suppress("unused")
private class DeprecatedPurchasesAPI {
    fun check(
        purchases: Purchases
    ) {
        purchases.allowSharingPlayStoreAccount = true
    }
}
