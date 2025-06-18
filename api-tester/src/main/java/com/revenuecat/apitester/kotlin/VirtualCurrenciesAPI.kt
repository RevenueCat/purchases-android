package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency

@Suppress("unused", "UNUSED_VARIABLE")
private class VirtualCurrenciesAPI {
    fun check(virtualCurrencies: VirtualCurrencies) {
        val all: Map<String, VirtualCurrency> = virtualCurrencies.all
        val withZeroBalance: Map<String, VirtualCurrency> = virtualCurrencies.withZeroBalance
        val withNonZeroBalance: Map<String, VirtualCurrency> = virtualCurrencies.withNonZeroBalance
        val subscriptTest: VirtualCurrency? = virtualCurrencies["COIN"]
        val getTest: VirtualCurrency? = virtualCurrencies.get(code = "COIN")
    }
}