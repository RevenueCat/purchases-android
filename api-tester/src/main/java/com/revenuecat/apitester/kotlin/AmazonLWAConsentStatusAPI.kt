package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.AmazonLWAConsentStatus

@Suppress("unused", "UNUSED_VARIABLE")
private class AmazonLWAConsentStatusAPI {
    fun check(status: AmazonLWAConsentStatus) {
        when (status) {
            AmazonLWAConsentStatus.CONSENTED,
            AmazonLWAConsentStatus.UNAVAILABLE
            -> {
            }
        }.exhaustive
    }
}
