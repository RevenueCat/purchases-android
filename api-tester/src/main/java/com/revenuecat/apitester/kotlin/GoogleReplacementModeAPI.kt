package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.GoogleReplacementMode

@Suppress("unused", "UNUSED_VARIABLE")
private class GoogleReplacementModeAPI {
    fun check(mode: GoogleReplacementMode) {
        when (mode) {
            GoogleReplacementMode.WITHOUT_PRORATION,
            GoogleReplacementMode.WITH_TIME_PRORATION,
            // GoogleReplacementMode.DEFERRED,
            GoogleReplacementMode.CHARGE_FULL_PRICE,
            GoogleReplacementMode.CHARGE_PRORATED_PRICE,
            -> {}
        }.exhaustive
    }
}
