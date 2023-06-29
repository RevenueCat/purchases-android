package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode

sealed class SignatureVerificationMode {
    companion object {
        fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
            signatureVerifier: SignatureVerifier? = null,
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    Informational(signatureVerifier ?: DefaultSignatureVerifier())
                // Hidden ENFORCED mode during feature beta
                // EntitlementVerificationMode.ENFORCED ->
                //     Enforced(signatureVerifier ?: DefaultSignatureVerifier())
            }
        }
    }
    object Disabled : SignatureVerificationMode()
    data class Informational(
        val signatureVerifier: SignatureVerifier,
        override val intermediateSignatureHelper: IntermediateSignatureHelper = IntermediateSignatureHelper(
            signatureVerifier,
        ),
    ) : SignatureVerificationMode()
    data class Enforced(
        val signatureVerifier: SignatureVerifier,
        override val intermediateSignatureHelper: IntermediateSignatureHelper = IntermediateSignatureHelper(
            signatureVerifier,
        ),
    ) : SignatureVerificationMode()

    val shouldVerify: Boolean
        get() = when (this) {
            Disabled ->
                false
            is Informational,
            is Enforced,
            ->
                true
        }

    open val intermediateSignatureHelper: IntermediateSignatureHelper?
        get() = when (this) {
            is Disabled -> null
            is Informational -> intermediateSignatureHelper
            is Enforced -> intermediateSignatureHelper
        }
}
