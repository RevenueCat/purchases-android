package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.VerificationResult

/**
 * Result of verifying an HTTP response signature. Keeps the failure reason until it is bridged to the public
 * [VerificationResult] at an API boundary.
 */
internal sealed class SignatureVerificationResult {
    object NotRequested : SignatureVerificationResult()
    object Verified : SignatureVerificationResult()
    data class Failed(val reason: FailureReason) : SignatureVerificationResult()

    enum class FailureReason {
        MISSING_SIGNATURE,
        MISSING_REQUEST_TIME,
        MISSING_SIGNED_PAYLOAD,
        INVALID_SIGNATURE_FORMAT,
        INVALID_INTERMEDIATE_KEY_SIGNATURE,
        INVALID_INTERMEDIATE_KEY,
        INVALID_RESPONSE_PAYLOAD,
        INTERMEDIATE_KEY_EXPIRED,
        PAYLOAD_SIGNATURE_MISMATCH,
        UNKNOWN,
    }

    val isFailed: Boolean
        get() = this is Failed

    val failureReason: FailureReason?
        get() = (this as? Failed)?.reason

    val result: VerificationResult
        get() = when (this) {
            NotRequested -> VerificationResult.NOT_REQUESTED
            Verified -> VerificationResult.VERIFIED
            is Failed -> VerificationResult.FAILED
        }
}
