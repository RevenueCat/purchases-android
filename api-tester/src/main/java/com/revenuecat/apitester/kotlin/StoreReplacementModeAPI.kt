package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.StoreReplacementMode

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreReplacementModeAPI {
    fun check(mode: StoreReplacementMode) {
        when (mode) {
            StoreReplacementMode.WITHOUT_PRORATION,
            StoreReplacementMode.WITH_TIME_PRORATION,
            StoreReplacementMode.CHARGE_FULL_PRICE,
            StoreReplacementMode.CHARGE_PRORATED_PRICE,
            StoreReplacementMode.DEFERRED,
            -> {}
        }.exhaustive
    }
}
