package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.google.crypto.tink.config.TinkFips
import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.GeneralSecurityException

internal interface SignatureVerifier {
    fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray): Boolean
}

internal class DefaultSignatureVerifier(
    publicKeyBytes: ByteArray,
) : SignatureVerifier {
    companion object {
        private const val DEFAULT_PUBLIC_KEY = "UC1upXWg5QVmyOSwozp755xLqquBKjjU+di6U8QhMlM="

        // Ed25519 is not FIPS compatible, so Ed25519Verify cannot be created while Tink is restricted to FIPS mode.
        fun isSupported(): Boolean = !TinkFips.useOnlyFips()
    }

    constructor(publicKey: String = DEFAULT_PUBLIC_KEY) : this(Base64.decode(publicKey, Base64.DEFAULT))

    private val verifier = Ed25519Verify(publicKeyBytes)

    override fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray): Boolean {
        return try {
            verifier.verify(signatureToVerify, messageToVerify)
            true
        } catch (_: GeneralSecurityException) {
            false
        }
    }
}
