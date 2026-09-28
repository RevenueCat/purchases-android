package com.revenuecat.purchases.common.verification

import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.verification.SignatureVerificationResult.FailureReason
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SignatureVerificationResultTest {

    @Test
    fun `not requested bridges to the public not requested result`() {
        val result = SignatureVerificationResult.NotRequested
        assertThat(result.result).isEqualTo(VerificationResult.NOT_REQUESTED)
        assertThat(result.isFailed).isFalse
    }

    @Test
    fun `verified bridges to the public verified result`() {
        val result = SignatureVerificationResult.Verified
        assertThat(result.result).isEqualTo(VerificationResult.VERIFIED)
        assertThat(result.isFailed).isFalse
    }

    @Test
    fun `every failure reason bridges to the public failed result`() {
        FailureReason.values().forEach { reason ->
            val result = SignatureVerificationResult.Failed(reason)
            assertThat(result.result).isEqualTo(VerificationResult.FAILED)
            assertThat(result.isFailed).isTrue
            assertThat(result.reason).isEqualTo(reason)
        }
    }

    @Test
    fun `failure reason is only exposed for failed results`() {
        assertThat(SignatureVerificationResult.NotRequested.failureReason).isNull()
        assertThat(SignatureVerificationResult.Verified.failureReason).isNull()
        FailureReason.values().forEach { reason ->
            assertThat(SignatureVerificationResult.Failed(reason).failureReason).isEqualTo(reason)
        }
    }

    @Test
    fun `failure reason names match the diagnostics wire values shared with iOS`() {
        assertThat(FailureReason.values().map { it.name }).containsExactly(
            "MISSING_SIGNATURE",
            "MISSING_REQUEST_TIME",
            "MISSING_SIGNED_PAYLOAD",
            "INVALID_SIGNATURE_FORMAT",
            "INVALID_INTERMEDIATE_KEY_SIGNATURE",
            "INVALID_INTERMEDIATE_KEY",
            "INVALID_RESPONSE_PAYLOAD",
            "INTERMEDIATE_KEY_EXPIRED",
            "PAYLOAD_SIGNATURE_MISMATCH",
            "UNKNOWN",
        )
    }
}
