package com.revenuecat.purchases.common.verification

import android.util.Base64

interface SignatureVerifier {
    fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray): Boolean
}

class DefaultSignatureVerifier(publicKey: String = DEFAULT_PUBLIC_KEY) : SignatureVerifier {
    companion object {
        private const val DEFAULT_PUBLIC_KEY = "" // WIP: Add B64 encoded public key here
    }

    @Suppress("UnusedPrivateMember") // WIP: Remove suppress
    private val publicKeyBytes = Base64.decode(publicKey, Base64.DEFAULT)

    override fun verify(signatureToVerify: ByteArray, messageToVerify: ByteArray): Boolean {
        // WIP: Add library
        return true
    }
}
