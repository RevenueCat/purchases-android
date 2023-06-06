package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.OfferPaymentMode

@Suppress("unused", "UNUSED_VARIABLE")
private class OfferPaymentModeAPI {
    fun check(offerPaymentMode: OfferPaymentMode) {
        when (offerPaymentMode) {
            OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT,
            OfferPaymentMode.SINGLE_PAYMENT,
            OfferPaymentMode.FREE_TRIAL,
            -> {
            }
        }.exhaustive
    }
}
