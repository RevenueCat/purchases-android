package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.PaymentMode

@Suppress("unused", "UNUSED_VARIABLE")
private class PaymentModeAPI {
    fun check(paymentMode: PaymentMode) {
        when (paymentMode) {
            PaymentMode.PAY_AS_YOU_GO,
            PaymentMode.PAY_UP_FRONT,
            PaymentMode.FREE_TRIAL -> {
            }
        }.exhaustive
    }
}
