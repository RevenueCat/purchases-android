package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.VirtualCurrency

@Suppress("unused", "UNUSED_VARIABLE")
private class VirtualCurrencyInfoAPI {
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun check(virtualCurrencyInfo: VirtualCurrency) {
        with(virtualCurrencyInfo) {
            val balance: Int = virtualCurrencyInfo.balance
        }
    }
}
