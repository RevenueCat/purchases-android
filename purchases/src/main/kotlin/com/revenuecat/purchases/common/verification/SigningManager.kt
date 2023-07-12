package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.strings.NetworkStrings
import com.revenuecat.purchases.utils.Result
import java.security.MessageDigest
import java.security.SecureRandom

internal class SigningManager(
    val signatureVerificationMode: SignatureVerificationMode,
    private val appConfig: AppConfig,
    private val apiKey: String,
) {
    private companion object {
        const val NONCE_BYTES_SIZE = 12
        const val POST_PARAMS_ALGORITHM = "sha256"
    }

    private data class Parameters(
        val salt: ByteArray,
        val apiKey: String,
        val nonce: String?,
        val urlPath: String,
        val postParamsHashHeader: String?,
        val requestTime: String,
        val eTag: String?,
        val body: String?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Parameters

            if (!salt.contentEquals(other.salt)) return false
            if (apiKey != other.apiKey) return false
            if (nonce != other.nonce) return false
            if (urlPath != other.urlPath) return false
            if (postParamsHashHeader != other.postParamsHashHeader) return false
            if (requestTime != other.requestTime) return false
            if (eTag != other.eTag) return false
            if (body != other.body) return false

            return true
        }

        override fun hashCode(): Int {
            var result = salt.contentHashCode()
            result = 31 * result + apiKey.hashCode()
            result = 31 * result + (nonce?.hashCode() ?: 0)
            result = 31 * result + urlPath.hashCode()
            result = 31 * result + (postParamsHashHeader?.hashCode() ?: 0)
            result = 31 * result + requestTime.hashCode()
            result = 31 * result + (eTag?.hashCode() ?: 0)
            result = 31 * result + (body?.hashCode() ?: 0)
            return result
        }

        fun toSignatureToVerify(): ByteArray {
            return salt +
                apiKey.toByteArray() +
                (nonce?.let { Base64.decode(it, Base64.DEFAULT) } ?: byteArrayOf()) +
                urlPath.toByteArray() +
                (postParamsHashHeader?.toByteArray() ?: byteArrayOf()) +
                requestTime.toByteArray() +
                (eTag?.toByteArray() ?: byteArrayOf()) +
                (body?.toByteArray() ?: byteArrayOf())
        }
    }

    fun shouldVerifyEndpoint(endpoint: Endpoint): Boolean {
        return endpoint.supportsSignatureVerification && signatureVerificationMode.shouldVerify
    }

    fun createRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES_SIZE)
        SecureRandom().nextBytes(bytes)
        return String(Base64.encode(bytes, Base64.DEFAULT))
    }

    fun getPostParamsForSigningHeaderIfNeeded(
        endpoint: Endpoint,
        postFieldsToSign: List<Pair<String, String>>?,
    ): String? {
        return if (!postFieldsToSign.isNullOrEmpty() &&
            shouldVerifyEndpoint(endpoint)
        ) {
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            postFieldsToSign.mapIndexed { index, pair ->
                if (index > 0) {
                    sha256Digest.update(0x00.toByte())
                }
                sha256Digest.update(pair.second.toByteArray())
            }
            val hashFields = sha256Digest.digest().fold("") { str, byte -> str + "%02x".format(byte) }
            val header = listOf(
                postFieldsToSign.joinToString(",") { it.first },
                POST_PARAMS_ALGORITHM,
                hashFields,
            ).joinToString(":")
            header
        } else {
            null
        }
    }

    @Suppress("LongParameterList", "ReturnCount", "CyclomaticComplexMethod", "LongMethod")
    fun verifyResponse(
        urlPath: String,
        signatureString: String?,
        nonce: String?,
        body: String?,
        requestTime: String?,
        eTag: String?,
        postFieldsToSignHeader: String?,
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
                val signatureParameters = Parameters(
                    signature.salt,
                    apiKey,
                    nonce,
                    urlPath,
                    postFieldsToSignHeader,
                    requestTime,
                    eTag,
                    body,
                )
                val verificationResult = intermediateKeyVerifier.verify(
                    signature.payload,
                    signatureParameters.toSignatureToVerify(),
                )

                return if (verificationResult) {
                    verboseLog(NetworkStrings.VERIFICATION_SUCCESS.format(urlPath))
                    VerificationResult.VERIFIED
                } else {
                    errorLog(NetworkStrings.VERIFICATION_ERROR.format(urlPath))
                    VerificationResult.FAILED
                }
            }
        }
    }
}
