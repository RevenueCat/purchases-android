package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.common.errorLog

internal sealed class SignatureVerificationMode {
    companion object {
        fun fromEntitlementVerificationMode(
            verificationMode: EntitlementVerificationMode,
        ): SignatureVerificationMode {
            return when (verificationMode) {
                EntitlementVerificationMode.DISABLED -> Disabled
                EntitlementVerificationMode.INFORMATIONAL ->
                    if (DefaultSignatureVerifier.isSupported()) Informational else disabledBecauseVerifierUnsupported()
                // Hidden ENFORCED mode temporarily. Will be added back in the future.
                // EntitlementVerificationMode.ENFORCED ->
                //     if (DefaultSignatureVerifier.isSupported()) Enforced else disabledBecauseVerifierUnsupported()
            }
        }

        private fun disabledBecauseVerifierUnsupported(): SignatureVerificationMode {
            errorLog {
                "Tink is restricted to FIPS mode, so the signature verifier cannot be created. " +
                    "Disabling signature verification."
            }
            return Disabled
        }
    }
    object Disabled : SignatureVerificationMode()
    object Informational : SignatureVerificationMode()
    object Enforced : SignatureVerificationMode()

    val shouldVerify: Boolean
        get() = when (this) {
            Disabled ->
                false
            Informational,
            Enforced,
            ->
                true
        }
}
