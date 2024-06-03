package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.PurchasesAreCompletedBy

@Suppress("unused")
private class PurchasesAreCompletedByAPI {
    fun check(mode: PurchasesAreCompletedBy) {
        when (mode) {
            PurchasesAreCompletedBy.REVENUECAT,
            PurchasesAreCompletedBy.MY_APP,
            -> {
            }
        }.exhaustive
    }
}
