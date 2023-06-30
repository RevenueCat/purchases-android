package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.NetworkStrings
import java.security.SecureRandom

internal class SigningManager(
    val signatureVerificationMode: SignatureVerificationMode,
    val appConfig: AppConfig,
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
        eTag: String?,
    ): VerificationResult {
        if (appConfig.forceSigningErrors) {
            warnLog("Forcing signing error for request with path: $urlPath")
            return VerificationResult.FAILED
        }
        val signatureVerifier = signatureVerificationMode.verifier ?: return VerificationResult.NOT_REQUESTED

        if (signature == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_SIGNATURE.format(urlPath))
            return VerificationResult.FAILED
        }
        if (requestTime == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_REQUEST_TIME.format(urlPath))
            return VerificationResult.FAILED
        }

        val signatureMessage = getSignatureMessage(responseCode, body, eTag)
        if (signatureMessage == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_BODY_OR_ETAG.format(urlPath))
            return VerificationResult.FAILED
        }

        val decodedNonce = Base64.decode(nonce, Base64.DEFAULT)
        val decodedSignature = Base64.decode(signature, Base64.DEFAULT)
        val saltBytes = decodedSignature.copyOfRange(0, SALT_BYTES_SIZE)
        val signatureToVerify = decodedSignature.copyOfRange(SALT_BYTES_SIZE, decodedSignature.size)
        val messageToVerify = saltBytes + decodedNonce + requestTime.toByteArray() + signatureMessage.toByteArray()
        val verificationResult = signatureVerifier.verify(signatureToVerify, messageToVerify)

        return if (verificationResult) {
            VerificationResult.VERIFIED
        } else {
            errorLog(NetworkStrings.VERIFICATION_ERROR.format(urlPath))
            VerificationResult.FAILED
        }
    }

    private fun getSignatureMessage(responseCode: Int, body: String?, eTag: String?): String? {
        return if (responseCode == RCHTTPStatusCodes.NOT_MODIFIED) eTag else body
    }
}
