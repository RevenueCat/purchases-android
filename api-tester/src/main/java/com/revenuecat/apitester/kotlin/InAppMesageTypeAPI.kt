package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.InAppMessageType

@Suppress("unused", "UNUSED_VARIABLE")
private class InAppMesageTypeAPI {
    fun check(inAppMessageType: InAppMessageType) {
        when (inAppMessageType) {
            InAppMessageType.BILLING_ISSUES,
            -> {
            }
        }.exhaustive
    }
}
