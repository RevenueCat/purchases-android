package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode

sealed class SignatureVerificationMode {
    companion object {
        fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
            signatureVerifier: SignatureVerifier? = null
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    Informational(signatureVerifier ?: DefaultSignatureVerifier())
                EntitlementVerificationMode.ENFORCED ->
                    Enforced(signatureVerifier ?: DefaultSignatureVerifier())
            }
        }
    }
    object Disabled : SignatureVerificationMode()
    data class Informational(val signatureVerifier: SignatureVerifier) : SignatureVerificationMode()
    data class Enforced(val signatureVerifier: SignatureVerifier) : SignatureVerificationMode()

    val shouldVerify: Boolean
        get() = when (this) {
            Disabled ->
                false
            is Informational,
            is Enforced ->
                true
        }
}
