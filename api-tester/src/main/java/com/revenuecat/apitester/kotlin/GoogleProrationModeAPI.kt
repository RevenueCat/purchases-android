package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GoogleProrationMode

@Suppress("unused", "UNUSED_VARIABLE")
private class GoogleProrationModeAPI {
    fun check(mode: GoogleProrationMode) {
        when (mode) {
            GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION,
            GoogleProrationMode.IMMEDIATE_WITH_TIME_PRORATION,
            GoogleProrationMode.DEFERRED,
            GoogleProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE,
            GoogleProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE -> {}
        }.exhaustive
    }
}
