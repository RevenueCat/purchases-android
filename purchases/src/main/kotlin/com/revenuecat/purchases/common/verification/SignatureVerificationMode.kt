package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.common.errorLog

internal sealed class SignatureVerificationMode {
    companion object {
        fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
            rootVerifierProvider: () -> SignatureVerifier = { DefaultSignatureVerifier() },
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    if (DefaultSignatureVerifier.isSupported()) {
                        Informational(IntermediateSignatureHelper(rootVerifierProvider))
                    } else {
                        disabledBecauseVerifierUnsupported()
                    }
                // Hidden ENFORCED mode temporarily. Will be added back in the future.
                // EntitlementVerificationMode.ENFORCED ->
                //     if (DefaultSignatureVerifier.isSupported()) {
                //         Enforced(IntermediateSignatureHelper(rootVerifierProvider))
                //     } else {
                //         disabledBecauseVerifierUnsupported()
                //     }
            }
        }

        private fun disabledBecauseVerifierUnsupported(): SignatureVerificationMode {
            errorLog {
                "Tink is restricted to FIPS mode, so the signature verifier cannot be created. " +
                    "Disabling signature verification."
            }
            return Disabled
        }

        // Passes a provider rather than a verifier so that creating it stays off the thread that builds the
        // mode. See IntermediateSignatureHelper.rootSignatureVerifier.
        private fun createIntermediateSignatureHelper(): IntermediateSignatureHelper {
            return IntermediateSignatureHelper(rootSignatureVerifierProvider = { DefaultSignatureVerifier() })
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
