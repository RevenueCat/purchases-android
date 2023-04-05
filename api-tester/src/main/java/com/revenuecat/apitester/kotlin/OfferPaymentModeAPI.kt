package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.OfferPaymentMode

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferPaymentModeAPI {
    fun check(offerPaymentMode: OfferPaymentMode) {
        when (offerPaymentMode) {
            OfferPaymentMode.PAY_AS_YOU_GO,
            OfferPaymentMode.PAY_UP_FRONT,
            OfferPaymentMode.FREE_TRIAL -> {
            }
        }.exhaustive
    }
}
