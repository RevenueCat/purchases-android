package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.GeneralSecurityException

interface SignatureVerifier {
    fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray): Boolean
}

class DefaultSignatureVerifier(publicKey: String = DEFAULT_PUBLIC_KEY) : SignatureVerifier {
    companion object {
        private const val DEFAULT_PUBLIC_KEY = "UC1upXWg5QVmyOSwozp755xLqquBKjjU+di6U8QhMlM="
    }

    private val publicKeyBytes = Base64.decode(publicKey, Base64.DEFAULT)

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
