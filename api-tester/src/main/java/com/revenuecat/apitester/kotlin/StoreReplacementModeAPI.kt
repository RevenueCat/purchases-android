package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.StoreReplacementMode

@Suppress("unused", "UNUSED_VARIABLE")
private class StoreReplacementModeAPI {
    fun check(mode: StoreReplacementMode) {
        val withoutProration: StoreReplacementMode = StoreReplacementMode.WITHOUT_PRORATION
        val withTimeProration: StoreReplacementMode = StoreReplacementMode.WITH_TIME_PRORATION
        val chargeFullPrice: StoreReplacementMode = StoreReplacementMode.CHARGE_FULL_PRICE
        val chargeProratedPrice: StoreReplacementMode = StoreReplacementMode.CHARGE_PRORATED_PRICE
        val deferred: StoreReplacementMode = StoreReplacementMode.DEFERRED
    }
}
