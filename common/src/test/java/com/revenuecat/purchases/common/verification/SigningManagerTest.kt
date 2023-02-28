package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.HTTPResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SigningManagerTest {
    private companion object {
        const val TEST_SIGNATURE = "Jmax3TdnBIe0/zFeHT5KJrFNoGxWtQAOuYTjnEXDHa0z3/npDG9nRB4vrUkt/ZxVh7SU+P++O3LnObxeuz3tFAILs75bxIqXwp6LqdV7Tgo="
        const val TEST_NONCE = "NYwvC+KDXzUq9pfg"
    }

    private lateinit var verifier: SignatureVerifier

    private lateinit var signingManager: SigningManager

    @Before
    fun setUp() {
        verifier = mockk()

        signingManager = SigningManager(verifier)
    }

    // region createRandomNonce

    @Test
    fun `createRandomNonce returns 12 base64 encoded random bytes`() {
        val nonce = signingManager.createRandomNonce()
        val decodedNonce = Base64.decode(nonce, Base64.DEFAULT)
        assertThat(decodedNonce.size).isEqualTo(12)
    }

    @Test
    fun `createRandomNonce returns different nonce on each call`() {
        val nonce1 = signingManager.createRandomNonce()
        val nonce2 = signingManager.createRandomNonce()
        assertThat(nonce1).isNotEqualTo(nonce2)
    }

    // endregion

    // region verifyResponse

    @Test
    fun `verifyResponse returns error if signature is null`() {
        val verificationStatus = callVerifyResponse(signature = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if request time is null`() {
        val verificationStatus = callVerifyResponse(requestTime = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if both body and etag are null`() {
        val verificationStatus = callVerifyResponse(body = null, eTag = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse verifies if body is null`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationStatus = callVerifyResponse(body = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Test
    fun `verifyResponse returns success if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationStatus = callVerifyResponse()
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Test
    fun `verifyResponse returns error if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns false
        val verificationStatus = callVerifyResponse()
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    // endregion

    // region Helpers

    private fun callVerifyResponse(
        requestPath: String = "test-url-path",
        signature: String? = TEST_SIGNATURE,
        nonce: String = TEST_NONCE,
        body: String? = "{\"test-key\":\"test-value\"}",
        requestTime: String? = "1234567890",
        eTag: String? = "test-etag"
    ) = signingManager.verifyResponse(requestPath, signature, nonce, body, requestTime, eTag)

    // endregion
}
