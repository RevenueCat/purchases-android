package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.models.PurchaseState

@Suppress("unused")
private class PurchaseStateAPI {
    fun check(state: PurchaseState) {
        when (state) {
            PurchaseState.UNSPECIFIED_STATE,
            PurchaseState.PURCHASED,
            PurchaseState.PENDING
            -> {}
        }
    }
}
