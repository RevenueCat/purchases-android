package com.revenuecat.purchases.common.verification

import android.util.Base64
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.verification.SignatureVerificationResult.FailureReason
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
        const val POST_PARAMS_SEPARATOR = 0x00.toByte()
    }

    private data class Parameters(
        val salt: ByteArray,
        val apiKey: String,
        val nonce: String?,
        val urlPath: String,
        val postParamsHashHeader: String?,
        val requestTime: String,
        val eTag: String?,
        val body: ByteArray?,
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
            if (body != null) {
                if (other.body == null) return false
                if (!body.contentEquals(other.body)) return false
            } else if (other.body != null) {
                return false
            }

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
            result = 31 * result + (body?.contentHashCode() ?: 0)
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
                (body ?: byteArrayOf())
        }
    }

    fun shouldVerifyEndpoint(endpoint: Endpoint): Boolean {
        return endpoint.supportsSignatureVerification && signatureVerificationMode.shouldVerify
    }

    fun createRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES_SIZE)
        SecureRandom().nextBytes(bytes)
        return String(Base64.encode(bytes, Base64.DEFAULT)).trim()
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
                    sha256Digest.update(POST_PARAMS_SEPARATOR)
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

    /**
     * Verifies a response signature. [bodyBytes] is the signed payload: the UTF-8 bytes of a textual
     * (JSON) body, or an empty array for a `204 No Content` response.
     */
    @Suppress("LongParameterList")
    fun verifyResponse(
        urlPath: String,
        signatureString: String?,
        nonce: String?,
        bodyBytes: ByteArray?,
        requestTime: String?,
        eTag: String?,
        postFieldsToSignHeader: String?,
    ): SignatureVerificationResult = verifySignedResponse(
        urlPath = urlPath,
        signatureString = signatureString,
        nonce = nonce,
        requestTime = requestTime,
        eTag = eTag,
        postFieldsToSignHeader = postFieldsToSignHeader,
    ) { Result.Success(bodyBytes) }

    /**
     * Verifies an RC Container Format response. The backend signs the leading config element's (element 0)
     * **uncompressed** bytes — the config part / `main_body` — so the signature is verified over
     * [RCContainer.config], which is the element already decoded by the container. Per-element compression is
     * transparent to the signature (as it is to the element checksum), so a codec change never invalidates a
     * signed config. The per-element container checksums are untrusted lookup hints, not a trust anchor: inline
     * blob elements are not signed and are instead authenticated transitively by hashing against the `blob_ref`
     * in the signed config. These endpoints are not ETag-cached and send no post params, but the signature does
     * cover the request [nonce]. The signature headers are checked before [containerBytes] is parsed, so a
     * response that is missing them reports that rather than an invalid payload.
     */
    @Suppress("LongParameterList")
    fun verifyRCFormatResponse(
        urlPath: String,
        signatureString: String?,
        nonce: String?,
        containerBytes: ByteArray,
        requestTime: String?,
        eTag: String?,
    ): SignatureVerificationResult = verifySignedResponse(
        urlPath = urlPath,
        signatureString = signatureString,
        nonce = nonce,
        requestTime = requestTime,
        eTag = eTag,
        postFieldsToSignHeader = null,
    ) {
        try {
            Result.Success(RCContainer.parse(containerBytes).config)
        } catch (e: RCContainerFormatException) {
            errorLog(e) { NetworkStrings.VERIFICATION_ERROR.format(urlPath) }
            Result.Error(FailureReason.INVALID_RESPONSE_PAYLOAD)
        }
    }

    @Suppress("LongParameterList", "ReturnCount", "CyclomaticComplexMethod", "LongMethod")
    private fun verifySignedResponse(
        urlPath: String,
        signatureString: String?,
        nonce: String?,
        requestTime: String?,
        eTag: String?,
        postFieldsToSignHeader: String?,
        signedPayload: () -> Result<ByteArray?, FailureReason>,
    ): SignatureVerificationResult {
        if (appConfig.forceSigningErrors) {
            warnLog { "Forcing signing error for request with path: $urlPath" }
            return SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
        }
        val intermediateSignatureHelper = signatureVerificationMode.intermediateSignatureHelper
            ?: return SignatureVerificationResult.NotRequested
        if (!intermediateSignatureHelper.canVerify()) return SignatureVerificationResult.NotRequested

        if (signatureString == null) {
            errorLog { NetworkStrings.VERIFICATION_MISSING_SIGNATURE.format(urlPath) }
            return SignatureVerificationResult.Failed(FailureReason.MISSING_SIGNATURE)
        }
        if (requestTime == null) {
            errorLog { NetworkStrings.VERIFICATION_MISSING_REQUEST_TIME.format(urlPath) }
            return SignatureVerificationResult.Failed(FailureReason.MISSING_REQUEST_TIME)
        }
        val bodyBytes = when (val payload = signedPayload()) {
            is Result.Success -> payload.value
            is Result.Error -> return SignatureVerificationResult.Failed(payload.value)
        }
        if (bodyBytes == null && eTag == null) {
            errorLog { NetworkStrings.VERIFICATION_MISSING_BODY_OR_ETAG.format(urlPath) }
            return SignatureVerificationResult.Failed(FailureReason.MISSING_SIGNED_PAYLOAD)
        }

        val signature: Signature
        try {
            signature = Signature.fromString(signatureString)
        } catch (e: InvalidSignatureSizeException) {
            errorLog { NetworkStrings.VERIFICATION_INVALID_SIZE.format(urlPath, e.message) }
            return SignatureVerificationResult.Failed(FailureReason.INVALID_SIGNATURE_FORMAT)
        } catch (e: IllegalArgumentException) {
            errorLog { NetworkStrings.VERIFICATION_INVALID_SIGNATURE_FORMAT.format(urlPath, e.message) }
            return SignatureVerificationResult.Failed(FailureReason.INVALID_SIGNATURE_FORMAT)
        }

        when (val result = intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(signature)) {
            is Result.Error -> {
                errorLog { NetworkStrings.VERIFICATION_INTERMEDIATE_KEY_FAILED.format(urlPath, result.value.name) }
                return SignatureVerificationResult.Failed(result.value)
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
                    bodyBytes,
                )
                val verificationResult = intermediateKeyVerifier.verify(
                    signature.payload,
                    signatureParameters.toSignatureToVerify(),
                )

                return if (verificationResult) {
                    verboseLog { NetworkStrings.VERIFICATION_SUCCESS.format(urlPath) }
                    SignatureVerificationResult.Verified
                } else {
                    errorLog { NetworkStrings.VERIFICATION_ERROR.format(urlPath) }
                    SignatureVerificationResult.Failed(FailureReason.PAYLOAD_SIGNATURE_MISMATCH)
                }
            }
        }
    }
}
