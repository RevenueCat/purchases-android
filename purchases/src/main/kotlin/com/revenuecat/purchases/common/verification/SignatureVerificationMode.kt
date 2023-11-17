package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode

internal sealed class SignatureVerificationMode {
    companion object {
        fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
            rootVerifier: SignatureVerifier? = null,
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    Informational(IntermediateSignatureHelper(rootVerifier ?: DefaultSignatureVerifier()))
                 EntitlementVerificationMode.ENFORCED ->
                     Enforced(IntermediateSignatureHelper(rootVerifier ?: DefaultSignatureVerifier()))
            }
        }

        private fun createIntermediateSignatureHelper(): IntermediateSignatureHelper {
            return IntermediateSignatureHelper(DefaultSignatureVerifier())
        }
    }
    object Disabled : SignatureVerificationMode()
    data class Informational(
        override val intermediateSignatureHelper: IntermediateSignatureHelper = createIntermediateSignatureHelper(),
    ) : SignatureVerificationMode()
    data class Enforced(
        override val intermediateSignatureHelper: IntermediateSignatureHelper = createIntermediateSignatureHelper(),
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
