package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
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
    private lateinit var appConfig: AppConfig

    private lateinit var disabledSigningManager: SigningManager
    private lateinit var informationalSigningManager: SigningManager
    private lateinit var enforcedSigningManager: SigningManager

    @Before
    fun setUp() {
        verifier = mockk()
        appConfig = mockk<AppConfig>().apply {
            every { forceSigningErrors } returns false
        }

        disabledSigningManager = SigningManager(SignatureVerificationMode.Disabled, appConfig)
        informationalSigningManager = SigningManager(SignatureVerificationMode.Informational(verifier), appConfig)
        enforcedSigningManager = SigningManager(SignatureVerificationMode.Enforced(verifier), appConfig)
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
    fun `verifyResponse returns error if forceSigningErros is true`() {
        every { appConfig.forceSigningErrors } returns true
        val verificationResult = callVerifyResponse(informationalSigningManager, signature = null)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns NOT_REQUESTED if verification mode disabled`() {
        val verificationResult = callVerifyResponse(disabledSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.NOT_REQUESTED)
    }

    @Test
    fun `verifyResponse returns error if signature is null`() {
        val verificationResult = callVerifyResponse(informationalSigningManager, signature = null)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns error if request time is null`() {
        val verificationResult = callVerifyResponse(informationalSigningManager, requestTime = null)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns error if both body and etag are null`() {
        val verificationResult = callVerifyResponse(informationalSigningManager, body = null, eTag = null)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns error if status code success and body is empty`() {
        val verificationResult = callVerifyResponse(informationalSigningManager, body = null)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns error if status code not modified and etag is empty`() {
        val verificationResult = callVerifyResponse(
            informationalSigningManager,
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            eTag = null
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns success if verifier returns success for not modified `() {
        every { verifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(
            informationalSigningManager,
            responseCode = RCHTTPStatusCodes.NOT_MODIFIED,
            body = null,
            eTag = "test-etag"
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse returns success if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(informationalSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse returns error if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns false
        val verificationResult = callVerifyResponse(informationalSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse with real data verifies correctly`() {
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(DefaultSignatureVerifier()),
            appConfig
        )
        val verificationResult = callVerifyResponse(signingManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `verifyResponse with slightly different data does not verify correctly`() {
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(DefaultSignatureVerifier()),
            appConfig,
        )
        assertThat(
            callVerifyResponse(signingManager, requestTime = "1677005916011") // Wrong request time
        ).isEqualTo(VerificationResult.FAILED)
        assertThat(
            callVerifyResponse(signingManager, signature = "2bm3QppRywK5ULyCRLS5JJy9sq+84IkMk0Ue4LsywEp87t0tDObpzPlu30l4Desq9X65UFuosqwCLMizruDHbKvPqQLce1hrIuZpgic+cQ8=") // Wrong signature
        ).isEqualTo(VerificationResult.FAILED)
        assertThat(
            callVerifyResponse(signingManager, nonce = "MTIzNDU2Nzg5MGFj") // Wrong nonce
        ).isEqualTo(VerificationResult.FAILED)
        assertThat(
            callVerifyResponse(signingManager, body = "{\"request_date\":\"2023-02-21T18:58:37Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n") // Wrong body
        ).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns success for enforced mode if verifier returns success for given parameters`() {
        every { verifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(enforcedSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
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
