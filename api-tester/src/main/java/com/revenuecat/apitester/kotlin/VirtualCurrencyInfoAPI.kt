package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.VirtualCurrencyInfo

@Suppress("unused", "UNUSED_VARIABLE")
private class VirtualCurrencyInfoAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(virtualCurrencyInfo: VirtualCurrencyInfo) {
        with(virtualCurrencyInfo) {
            val balance: Int = virtualCurrencyInfo.balance
        }
    }
}
