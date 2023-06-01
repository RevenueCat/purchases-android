package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.EntitlementVerificationMode

@Suppress("unused", "UNUSED_VARIABLE")
private class EntitlementVerificationModeAPI {
    fun check(verificationMode: EntitlementVerificationMode) {
        when (verificationMode) {
            EntitlementVerificationMode.DISABLED,
            EntitlementVerificationMode.INFORMATIONAL,
            // Hidden ENFORCED mode during feature beta
            // EntitlementVerificationMode.ENFORCED
            -> {}
        }.exhaustive
    }
}
