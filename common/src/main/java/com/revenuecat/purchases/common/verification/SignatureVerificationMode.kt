package com.revenuecat.purchases.common.verification

sealed class SignatureVerificationMode {
    companion object {
// Trusted entitlements: Commented out until ready to be made public
//        fun fromEntitlementVerificationMode(
//            verificationMode: EntitlementVerificationMode,
//            signatureVerifier: SignatureVerifier? = null
//        ): SignatureVerificationMode {
//            return when (verificationMode) {
//                EntitlementVerificationMode.DISABLED -> Disabled
//                EntitlementVerificationMode.INFORMATIONAL ->
//                    Informational(signatureVerifier ?: DefaultSignatureVerifier())
//                // Hidden ENFORCED mode during feature beta
//                // EntitlementVerificationMode.ENFORCED ->
//                //     Enforced(signatureVerifier ?: DefaultSignatureVerifier())
//            }
//        }
    }
    object Disabled : SignatureVerificationMode()
    data class Informational(val signatureVerifier: SignatureVerifier) : SignatureVerificationMode()
    data class Enforced(val signatureVerifier: SignatureVerifier) : SignatureVerificationMode()

    val shouldVerify: Boolean
        get() = when (this) {
            Disabled ->
                false
            is Informational,
            is Enforced,
            ->
                true
        }

    val verifier: SignatureVerifier?
        get() = when (this) {
            is SignatureVerificationMode.Disabled -> null
            is SignatureVerificationMode.Informational -> signatureVerifier
            is SignatureVerificationMode.Enforced -> signatureVerifier
        }
}
