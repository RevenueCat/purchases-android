package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.strings.NetworkStrings
import java.security.SecureRandom

class SigningManager(
    private val signatureVerifier: SignatureVerifier
) {
    private companion object {
        const val NONCE_BYTES_SIZE = 12
        const val SALT_BYTES_SIZE = 16
    }

    fun createRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES_SIZE)
        SecureRandom().nextBytes(bytes)
        return String(Base64.encode(bytes, Base64.DEFAULT))
    }

    @Suppress("LongParameterList", "ReturnCount")
    fun verifyResponse(
        requestPath: String,
        signature: String?,
        nonce: String,
        body: String?,
        requestTime: String?,
        eTag: String?
    ): HTTPResult.VerificationStatus {
        if (signature == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_SIGNATURE.format(requestPath))
            return HTTPResult.VerificationStatus.ERROR
        }
        if (requestTime == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_REQUEST_TIME.format(requestPath))
            return HTTPResult.VerificationStatus.ERROR
        }

        // No body means NOT_MODIFIED response. We verify the etag instead.
        val signatureMessage = body ?: eTag
        if (signatureMessage == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_BODY_OR_ETAG.format(requestPath))
            return HTTPResult.VerificationStatus.ERROR
        }

        val decodedNonce = Base64.decode(nonce, Base64.DEFAULT)
        val decodedSignature = Base64.decode(signature, Base64.DEFAULT)
        val saltBytes = decodedSignature.copyOfRange(0, SALT_BYTES_SIZE)
        val signatureToVerify = decodedSignature.copyOfRange(SALT_BYTES_SIZE, decodedSignature.size)
        val messageToVerify = saltBytes + decodedNonce + requestTime.toByteArray() + signatureMessage.toByteArray()
        val verificationResult = signatureVerifier.verify(signatureToVerify, messageToVerify)
        return if (verificationResult) {
            HTTPResult.VerificationStatus.SUCCESS
        } else {
            HTTPResult.VerificationStatus.ERROR
        }
    }
}
