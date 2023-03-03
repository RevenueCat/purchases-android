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

    @Test
    fun `verifyResponse with real data verifies correctly`() {
        signingManager = SigningManager(DefaultSignatureVerifier())
        val verificationStatus = callVerifyResponse()
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `verifyResponse with slightly different data does not verify correctly`() {
        signingManager = SigningManager(DefaultSignatureVerifier())
        assertThat(
            callVerifyResponse(requestTime = "1677005916011") // Wrong request time
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(signature = "2bm3QppRywK5ULyCRLS5JJy9sq+84IkMk0Ue4LsywEp87t0tDObpzPlu30l4Desq9X65UFuosqwCLMizruDHbKvPqQLce1hrIuZpgic+cQ8=") // Wrong signature
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(nonce = "MTIzNDU2Nzg5MGFj") // Wrong nonce
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(body = "{\"request_date\":\"2023-02-21T18:58:37Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n") // Wrong body
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    // endregion

    // region Helpers

    private fun callVerifyResponse(
        requestPath: String = "test-url-path",
        signature: String? = "2bm3QppRywK5ULyCRLS5JJy9sq+84IkMk0Ue4LsywEp87t0tDObpzPlu30l4Desq9X65UFuosqwCLMizruDHbKvPqQLce0hrIuZpgic+cQ8=",
        nonce: String = "MTIzNDU2Nzg5MGFi",
        body: String? = "{\"request_date\":\"2023-02-21T18:58:36Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n",
        requestTime: String? = "1677005916012",
        eTag: String? = "test-etag"
    ) = signingManager.verifyResponse(requestPath, signature, nonce, body, requestTime, eTag)

    // endregion
}
