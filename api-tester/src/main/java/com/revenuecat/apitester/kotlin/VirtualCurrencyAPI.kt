package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.virtualcurrencies.VirtualCurrency

@Suppress("unused", "UNUSED_VARIABLE")
private class VirtualCurrencyAPI {
    fun check(virtualCurrency: VirtualCurrency) {
        val balance: Int = virtualCurrency.balance
        val name: String = virtualCurrency.name
        val code: String = virtualCurrency.code
        val serverDescription: String? = virtualCurrency.serverDescription
    }
}
