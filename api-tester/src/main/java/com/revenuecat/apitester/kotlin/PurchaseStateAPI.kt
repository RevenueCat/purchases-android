package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.PurchaseState

@Suppress("unused", "UNUSED_VARIABLE")
private class PurchaseStateAPI {
    fun check(state: PurchaseState) {
        when (state) {
            PurchaseState.UNSPECIFIED_STATE,
            PurchaseState.PURCHASED,
            PurchaseState.PENDING,
            -> {}
        }.exhaustive
    }
}
