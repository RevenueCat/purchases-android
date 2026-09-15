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
                    Informational(createLazyIntermediateSignatureHelper(rootVerifierProvider))
                // Hidden ENFORCED mode temporarily. Will be added back in the future.
                // EntitlementVerificationMode.ENFORCED ->
                //     Enforced(createLazyIntermediateSignatureHelper(rootVerifierProvider))
            }
        }

        /**
         * Builds the root verifier at most once, on whichever thread reads it first. Constructing it
         * initializes Tink's Ed25519 constant table, which takes hundreds of milliseconds on Tink versions
         * newer than the one we declare (an app can get one transitively from another SDK), so it must not
         * happen on the thread that calls `Purchases.configure`. The first read comes either from
         * [SigningManager.warmUpVerifierAsync] or from a response verification, both off the main thread.
         */
        private fun createLazyIntermediateSignatureHelper(
            rootVerifierProvider: () -> SignatureVerifier,
        ): Lazy<IntermediateSignatureHelper?> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            try {
                IntermediateSignatureHelper(rootVerifierProvider())
            } catch (e: IllegalStateException) {
                // Tink throws when it is restricted to FIPS mode, since Ed25519 is not FIPS compatible. Fail
                // open instead of crashing: a null helper makes verification report NOT_REQUESTED. The null is
                // memoized, so the failed attempt is never repeated.
                errorLog { "Error creating signature verifier: ${e.message}. Disabling signature verification." }
                null
            }
        }
    }

    object Disabled : SignatureVerificationMode() {
        override val intermediateSignatureHelper: IntermediateSignatureHelper? = null
    }

    data class Informational(
        private val lazyIntermediateSignatureHelper: Lazy<IntermediateSignatureHelper?> =
            createLazyIntermediateSignatureHelper { DefaultSignatureVerifier() },
    ) : SignatureVerificationMode() {
        constructor(intermediateSignatureHelper: IntermediateSignatureHelper) :
            this(lazyOf(intermediateSignatureHelper))

        override val intermediateSignatureHelper: IntermediateSignatureHelper?
            get() = lazyIntermediateSignatureHelper.value
    }

    data class Enforced(
        private val lazyIntermediateSignatureHelper: Lazy<IntermediateSignatureHelper?> =
            createLazyIntermediateSignatureHelper { DefaultSignatureVerifier() },
    ) : SignatureVerificationMode() {
        constructor(intermediateSignatureHelper: IntermediateSignatureHelper) :
            this(lazyOf(intermediateSignatureHelper))

        override val intermediateSignatureHelper: IntermediateSignatureHelper?
            get() = lazyIntermediateSignatureHelper.value
    }

    val shouldVerify: Boolean
        get() = when (this) {
            Disabled ->
                false
            is Informational,
            is Enforced,
            ->
                true
        }

    /**
     * Null when verification is off, or when the root verifier could not be created. Reading this builds the
     * root verifier if nobody has yet, blocking the calling thread, so only read it off the main thread.
     */
    abstract val intermediateSignatureHelper: IntermediateSignatureHelper?

    /**
     * Creates the root verifier on the calling thread if it does not exist yet. Returns whether signature
     * verification is available at all.
     */
    fun warmUp(): Boolean = intermediateSignatureHelper != null
}
