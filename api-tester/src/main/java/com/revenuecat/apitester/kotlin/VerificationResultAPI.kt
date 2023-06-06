package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.VerificationResult

@Suppress("unused", "UNUSED_VARIABLE")
private class VerificationResultAPI {

    fun check(verificationResult: VerificationResult) {
        when (verificationResult) {
            VerificationResult.VERIFIED_ON_DEVICE,
            VerificationResult.NOT_REQUESTED,
            VerificationResult.VERIFIED,
            VerificationResult.FAILED,
            -> {}
        }.exhaustive
    }
}
