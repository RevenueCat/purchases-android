package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.strings.NetworkStrings
import java.security.SecureRandom

class SigningManager(
    val signatureVerificationMode: SignatureVerificationMode
) {
    private companion object {
        const val NONCE_BYTES_SIZE = 12
        const val SALT_BYTES_SIZE = 16
    }

    fun shouldVerifyEndpoint(endpoint: Endpoint): Boolean {
        return endpoint.supportsSignatureValidation && signatureVerificationMode.shouldVerify
    }

    fun createRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES_SIZE)
        SecureRandom().nextBytes(bytes)
        return String(Base64.encode(bytes, Base64.DEFAULT))
    }

    @Suppress("LongParameterList", "ReturnCount")
    fun verifyResponse(
        urlPath: String,
        responseCode: Int,
        signature: String?,
        nonce: String,
        body: String?,
        requestTime: String?,
        eTag: String?
    ): HTTPResult.VerificationStatus {
        val signatureVerifier = getVerifier() ?: return HTTPResult.VerificationStatus.NOT_VERIFIED

        if (signature == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_SIGNATURE.format(urlPath))
            return HTTPResult.VerificationStatus.ERROR
        }
        if (requestTime == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_REQUEST_TIME.format(urlPath))
            return HTTPResult.VerificationStatus.ERROR
        }

        val signatureMessage = getSignatureMessage(responseCode, body, eTag)
        if (signatureMessage == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_BODY_OR_ETAG.format(urlPath))
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
            errorLog(NetworkStrings.VERIFICATION_ERROR)
            HTTPResult.VerificationStatus.ERROR
        }
    }

    private fun getVerifier(): SignatureVerifier? {
        return when (signatureVerificationMode) {
            is SignatureVerificationMode.Disabled -> null
            is SignatureVerificationMode.Informational -> signatureVerificationMode.signatureVerifier
            is SignatureVerificationMode.Enforced -> signatureVerificationMode.signatureVerifier
        }
    }

    private fun getSignatureMessage(responseCode: Int, body: String?, eTag: String?): String? {
        return if (responseCode == RCHTTPStatusCodes.NOT_MODIFIED) eTag else body
    }
}
