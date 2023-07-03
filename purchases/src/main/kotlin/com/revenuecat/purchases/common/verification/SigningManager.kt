package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.Result
import java.security.SecureRandom

internal class SigningManager(
    val signatureVerificationMode: SignatureVerificationMode,
    val appConfig: AppConfig,
) {
    private companion object {
        const val NONCE_BYTES_SIZE = 12
    }

    fun shouldVerifyEndpoint(endpoint: Endpoint): Boolean {
        return endpoint.supportsSignatureValidation && signatureVerificationMode.shouldVerify
    }

    fun createRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES_SIZE)
        SecureRandom().nextBytes(bytes)
        return String(Base64.encode(bytes, Base64.DEFAULT))
    }

    @Suppress("LongParameterList", "ReturnCount", "CyclomaticComplexMethod")
    fun verifyResponse(
        urlPath: String,
        signatureString: String?,
        nonce: String?,
        body: String?,
        requestTime: String?,
        eTag: String?,
    ): VerificationResult {
        if (appConfig.forceSigningErrors) {
            warnLog("Forcing signing error for request with path: $urlPath")
            return VerificationResult.FAILED
        }
        val intermediateSignatureHelper = signatureVerificationMode.intermediateSignatureHelper
            ?: return VerificationResult.NOT_REQUESTED

        if (signatureString == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_SIGNATURE.format(urlPath))
            return VerificationResult.FAILED
        }
        if (requestTime == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_REQUEST_TIME.format(urlPath))
            return VerificationResult.FAILED
        }
        if (body == null && eTag == null) {
            errorLog(NetworkStrings.VERIFICATION_MISSING_BODY_OR_ETAG.format(urlPath))
            return VerificationResult.FAILED
        }

        val signature: Signature
        try {
            signature = Signature.fromString(signatureString)
        } catch (e: InvalidSignatureSizeException) {
            errorLog(NetworkStrings.VERIFICATION_INVALID_SIZE.format(urlPath, e.message))
            return VerificationResult.FAILED
        }

        when (val result = intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(signature)) {
            is Result.Error -> {
                errorLog(
                    NetworkStrings.VERIFICATION_INTERMEDIATE_KEY_FAILED.format(
                        urlPath,
                        result.value.underlyingErrorMessage,
                    ),
                )
                return VerificationResult.FAILED
            }
            is Result.Success -> {
                val intermediateKeyVerifier = result.value
                val decodedNonce = nonce?.let { Base64.decode(it, Base64.DEFAULT) } ?: byteArrayOf()
                val requestTimeBytes = requestTime.toByteArray()
                val eTagBytes = eTag?.toByteArray() ?: byteArrayOf()
                val payloadBytes = body?.toByteArray() ?: byteArrayOf()
                val messageToVerify = signature.salt + decodedNonce + requestTimeBytes + eTagBytes + payloadBytes
                val verificationResult = intermediateKeyVerifier.verify(signature.payload, messageToVerify)

                return if (verificationResult) {
                    VerificationResult.VERIFIED
                } else {
                    errorLog(NetworkStrings.VERIFICATION_ERROR.format(urlPath))
                    VerificationResult.FAILED
                }
            }
        }
    }
}
