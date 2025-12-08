package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.samsung.SamsungBillingMode

@Suppress("unused", "UNUSED_VARIABLE")
private class SamsungBillingModeAPI {
    fun check(samsungBillingMode: SamsungBillingMode) {
        when (samsungBillingMode) {
            SamsungBillingMode.PRODUCTION,
            SamsungBillingMode.TEST,
            SamsungBillingMode.ALWAYS_FAIL -> {
            }
        }.exhaustive
    }
}