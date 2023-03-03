package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
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

    private lateinit var disabledSigningManager: SigningManager
    private lateinit var informationalSigningManager: SigningManager
    private lateinit var enforcedSigningManager: SigningManager

    @Before
    fun setUp() {
        verifier = mockk()

        disabledSigningManager = SigningManager(SignatureVerificationMode.Disabled)
        informationalSigningManager = SigningManager(SignatureVerificationMode.Informational(verifier))
        enforcedSigningManager = SigningManager(SignatureVerificationMode.Enforced(verifier))
    }

    // region shouldVerifyEndpoint

    @Test
    fun `shouldVerifyEndpoint returns false if endpoint supports validation but verification mode disabled`() {
        assertThat(disabledSigningManager.shouldVerifyEndpoint(Endpoint.PostReceipt)).isFalse
    }

    @Test
    fun `shouldVerifyEndpoint returns true if endpoint supports validation and verification mode informational`() {
        assertThat(informationalSigningManager.shouldVerifyEndpoint(Endpoint.PostReceipt)).isTrue
    }

    @Test
    fun `shouldVerifyEndpoint returns true if endpoint supports validation and verification mode enforced`() {
        assertThat(enforcedSigningManager.shouldVerifyEndpoint(Endpoint.PostReceipt)).isTrue
    }

    @Test
    fun `shouldVerifyEndpoint returns false if endpoint does not support validation and mode informational`() {
        assertThat(informationalSigningManager.shouldVerifyEndpoint(Endpoint.PostDiagnostics)).isFalse
    }

    @Test
    fun `shouldVerifyEndpoint returns false if endpoint does not support validation and mode enforced`() {
        assertThat(enforcedSigningManager.shouldVerifyEndpoint(Endpoint.PostDiagnostics)).isFalse
    }

    // endpoint

    // region createRandomNonce

    @Test
    fun `createRandomNonce returns 12 base64 encoded random bytes`() {
        val nonce = disabledSigningManager.createRandomNonce()
        val decodedNonce = Base64.decode(nonce, Base64.DEFAULT)
        assertThat(decodedNonce.size).isEqualTo(12)
    }

    @Test
    fun `createRandomNonce returns different nonce on each call`() {
        val nonce1 = disabledSigningManager.createRandomNonce()
        val nonce2 = disabledSigningManager.createRandomNonce()
        assertThat(nonce1).isNotEqualTo(nonce2)
    }

    // endregion

    // region verifyResponse

    @Test
    fun `verifyResponse returns NOT_VERIFIED if verification mode disabled`() {
        val verificationStatus = callVerifyResponse(disabledSigningManager)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.NOT_VERIFIED)
    }

    @Test
    fun `verifyResponse returns error if signature is null`() {
        val verificationStatus = callVerifyResponse(informationalSigningManager, signature = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if request time is null`() {
        val verificationStatus = callVerifyResponse(informationalSigningManager, requestTime = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if both body and etag are null`() {
        val verificationStatus = callVerifyResponse(informationalSigningManager, body = null, eTag = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if status code success and body is empty`() {
        val verificationStatus = callVerifyResponse(informationalSigningManager, body = null)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns error if status code not modified and etag is empty`() {
        val verificationStatus = callVerifyResponse(
            informationalSigningManager,
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            eTag = null
        )
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns success if verifier returns success for not modified `() {
        every { verifier.verify(any(), any()) } returns true
        val verificationStatus = callVerifyResponse(
            informationalSigningManager,
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            body = null,
            eTag = "test-etag"
        )
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Test
    fun `verifyResponse returns success if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationStatus = callVerifyResponse(informationalSigningManager)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Test
    fun `verifyResponse returns error if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns false
        val verificationStatus = callVerifyResponse(informationalSigningManager)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse with real data verifies correctly`() {
        val signingManager = SigningManager(SignatureVerificationMode.Informational(DefaultSignatureVerifier()))
        val verificationStatus = callVerifyResponse(signingManager)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `verifyResponse with slightly different data does not verify correctly`() {
        val signingManager = SigningManager(SignatureVerificationMode.Informational(DefaultSignatureVerifier()))
        assertThat(
            callVerifyResponse(signingManager, requestTime = "1677005916011") // Wrong request time
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(signingManager, signature = "2bm3QppRywK5ULyCRLS5JJy9sq+84IkMk0Ue4LsywEp87t0tDObpzPlu30l4Desq9X65UFuosqwCLMizruDHbKvPqQLce1hrIuZpgic+cQ8=") // Wrong signature
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(signingManager, nonce = "MTIzNDU2Nzg5MGFj") // Wrong nonce
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
        assertThat(
            callVerifyResponse(signingManager, body = "{\"request_date\":\"2023-02-21T18:58:37Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n") // Wrong body
        ).isEqualTo(HTTPResult.VerificationStatus.ERROR)
    }

    @Test
    fun `verifyResponse returns success for enforced mode if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationStatus = callVerifyResponse(enforcedSigningManager)
        assertThat(verificationStatus).isEqualTo(HTTPResult.VerificationStatus.SUCCESS)
    }

    // endregion

    // region Helpers

    private fun callVerifyResponse(
        signingManager: SigningManager,
        requestPath: String = "test-url-path",
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        signature: String? = "2bm3QppRywK5ULyCRLS5JJy9sq+84IkMk0Ue4LsywEp87t0tDObpzPlu30l4Desq9X65UFuosqwCLMizruDHbKvPqQLce0hrIuZpgic+cQ8=",
        nonce: String = "MTIzNDU2Nzg5MGFi",
        body: String? = "{\"request_date\":\"2023-02-21T18:58:36Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n",
        requestTime: String? = "1677005916012",
        eTag: String? = null
    ) = signingManager.verifyResponse(requestPath, responseCode, signature, nonce, body, requestTime, eTag)

    // endregion
}
