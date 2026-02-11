package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode

internal sealed class SignatureVerificationMode {
    public companion object {
        public fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
            rootVerifier: SignatureVerifier? = null,
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    Informational(IntermediateSignatureHelper(rootVerifier ?: DefaultSignatureVerifier()))
                // Hidden ENFORCED mode temporarily. Will be added back in the future.
                // EntitlementVerificationMode.ENFORCED ->
                //     Enforced(signatureVerifier ?: DefaultSignatureVerifier())
            }
        }

        private fun createIntermediateSignatureHelper(): IntermediateSignatureHelper {
            return IntermediateSignatureHelper(DefaultSignatureVerifier())
        }
    }
    public object Disabled : SignatureVerificationMode()
    public data class Informational(
        override val intermediateSignatureHelper: IntermediateSignatureHelper = createIntermediateSignatureHelper(),
    ) : SignatureVerificationMode()
    public data class Enforced(
        override val intermediateSignatureHelper: IntermediateSignatureHelper = createIntermediateSignatureHelper(),
    ) : SignatureVerificationMode()

    public val shouldVerify: Boolean
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
