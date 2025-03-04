package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.VirtualCurrencyInfo

@Suppress("unused", "UNUSED_VARIABLE")
private class VirtualCurrencyInfoAPI {
    fun check(virtualCurrencyInfo: VirtualCurrencyInfo) {
        with(virtualCurrencyInfo) {
            val balance: Long = virtualCurrencyInfo.balance
        }
    }
}
