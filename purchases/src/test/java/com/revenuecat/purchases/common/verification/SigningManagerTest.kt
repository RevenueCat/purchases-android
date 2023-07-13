package com.revenuecat.purchases.common.verification

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.crypto.tink.subtle.Ed25519Sign
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.utils.Result
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SigningManagerTest {

    private val apiKey = "test-api-key"

    private lateinit var intermediateKeyVerifier: SignatureVerifier
    private lateinit var appConfig: AppConfig
    private lateinit var intermediateSignatureHelper: IntermediateSignatureHelper

    private lateinit var disabledSigningManager: SigningManager
    private lateinit var informationalSigningManager: SigningManager
    private lateinit var enforcedSigningManager: SigningManager

    private val testFieldsToSign = listOf(
        "test-field-1" to "test-value-1",
        "test-field-2" to "test-value-2",
    )

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { forceSigningErrors } returns false
        }
        intermediateKeyVerifier = mockk()
        intermediateSignatureHelper = mockk<IntermediateSignatureHelper>().apply {
            every { createIntermediateKeyVerifierIfVerified(any()) } returns Result.Success(intermediateKeyVerifier)
        }

        disabledSigningManager = SigningManager(SignatureVerificationMode.Disabled, appConfig, apiKey)
        informationalSigningManager = SigningManager(
            SignatureVerificationMode.Informational(intermediateSignatureHelper), appConfig, apiKey,
        )
        enforcedSigningManager = SigningManager(
            SignatureVerificationMode.Enforced(intermediateSignatureHelper), appConfig, apiKey,
        )
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
    fun `createRandomNonce does not end with new line`() {
        val nonce = disabledSigningManager.createRandomNonce()
        assertThat(nonce.endsWith('\n')).isFalse
    }

    @Test
    fun `createRandomNonce returns different nonce on each call`() {
        val nonce1 = disabledSigningManager.createRandomNonce()
        val nonce2 = disabledSigningManager.createRandomNonce()
        assertThat(nonce1).isNotEqualTo(nonce2)
    }

    // endregion

    // region getPostParamsForSigningHeaderIfNeeded

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns null if null fields passed`() {
        val header = informationalSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.LogIn, null)
        assertThat(header).isNull()
    }

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns null if empty fields passed`() {
        val header = informationalSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.LogIn, emptyList())
        assertThat(header).isNull()
    }

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns null if endpoint does not support signing`() {
        val header = informationalSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.PostDiagnostics, testFieldsToSign)
        assertThat(header).isNull()
    }

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns null if signing disabled`() {
        val header = disabledSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.LogIn, testFieldsToSign)
        assertThat(header).isNull()
    }

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns header with correct value in informational`() {
        val header = informationalSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.LogIn, testFieldsToSign)
        assertThat(header).isEqualTo("test-field-1,test-field-2:sha256:0a1d806f908079361fc0d18073dfcfcacb730afe4ace3d0478602479892e81d3")
    }

    @Test
    fun `getPostParamsForSigningHeaderIfNeeded returns header with correct value in enforced`() {
        val header = enforcedSigningManager.getPostParamsForSigningHeaderIfNeeded(Endpoint.LogIn, testFieldsToSign)
        assertThat(header).isEqualTo("test-field-1,test-field-2:sha256:0a1d806f908079361fc0d18073dfcfcacb730afe4ace3d0478602479892e81d3")
    }

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
            body = null,
            eTag = null
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns error if failed to verify intermediate key`() {
        every {
            intermediateSignatureHelper.createIntermediateKeyVerifierIfVerified(any())
        } returns Result.Error(PurchasesError(PurchasesErrorCode.SignatureVerificationError))
        val verificationResult = callVerifyResponse(informationalSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse returns success if intermediate key verifier returns success for not modified `() {
        every { intermediateKeyVerifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(
            informationalSigningManager,
            body = null,
            eTag = "test-etag"
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse returns success if intermediate key verifier returns success for given parameters`() {
        every { intermediateKeyVerifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(informationalSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse returns error if intermediate key verifier returns error for given parameters`() {
        every { intermediateKeyVerifier.verify(any(), any()) } returns false
        val verificationResult = callVerifyResponse(informationalSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.FAILED)
    }

    @Test
    fun `verifyResponse with real data verifies correctly`() {
        val rootVerifier = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(IntermediateSignatureHelper(rootVerifier)),
            appConfig,
            apiKey,
        )
        val verificationResult = callVerifyResponse(signingManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse with encoded url verifies correctly`() {
        val rootVerifier = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(IntermediateSignatureHelper(rootVerifier)),
            appConfig,
            apiKey,
        )
        val verificationResult = callVerifyResponse(
            signingManager,
            requestPath = "/v1/subscribers/\$RCAnonymousID%3A1af512a3b9c848899fe427f39dd69f2b",
            signature = "xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsAAMNQ1HiPDL34Vx0Uy74KPV5mztuk3DHBpucT/rSYVlkxIa3ModYmPfYZ20lnlbSB1UiP6oJHwAA2pXlS6AQ5eLSuAmm2UIYPrDGEEC8Lgj1sAn2fGMRMx2eaPNzDPBGTxxZROfjkI1wtsyJC0w0I7d8TkLeXjUTlWNafmc4GVMleE/tQZZGIoNrnar0HqICUnB8B",
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse with both payload and etag verifies correctly`() {
        val rootVerifier = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(IntermediateSignatureHelper(rootVerifier)),
            appConfig,
            apiKey,
        )
        val verificationResult = callVerifyResponse(
            signingManager,
            signature = "xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsAAMNQ1HiPDL34Vx0Uy74KPV5mztuk3DHBpucT/rSYVlkxIa3ModYmPfYZ20lnlbSB1UiP6oJHwAA2pXlS6AQ5eLSuAp8tBA6mZLAMEFIzY6iOQ/+seoyIwJ3WX+Ucu6/V3h7cEFYZGfiS2WYiFfM1H3cre+7wzwQsAXe6nm8FidwZTsDrbr6krvO7X5IXUkInvsoE",
            eTag = "test-etag",
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `verifyResponse with slightly different data does not verify correctly`() {
        val rootVerifier = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(IntermediateSignatureHelper(rootVerifier)),
            appConfig,
            apiKey,
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
        every { intermediateKeyVerifier.verify(any(), any()) } returns true
        val verificationResult = callVerifyResponse(enforcedSigningManager)
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifyResponse with post fields to sign verifies correctly`() {
        val rootVerifier = DefaultSignatureVerifier("yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=")
        val signingManager = SigningManager(
            SignatureVerificationMode.Informational(IntermediateSignatureHelper(rootVerifier)),
            appConfig,
            apiKey,
        )
        val postParamsHeader = "test-field-1,test-field-2:sha256:0a1d806f908079361fc0d18073dfcfcacb730afe4ace3d0478602479892e81d3"
        val verificationResult = callVerifyResponse(
            signingManager,
            signature = "xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsAAMNQ1HiPDL34Vx0Uy74KPV5mztuk3DHBpucT/rSYVlkxIa3ModYmPfYZ20lnlbSB1UiP6oJHwAA2pXlS6AQ5eLSuAr6y+BuXlXXhZybxIJRFiToh3gs+X7IVmtCZwtkNu0rM0CGZ6FbQFAutF+Y5dGM8Q4HUAdy8pb639Zv9Lf7eZZ/Td9XuWqd+TA98M2MfL6ED",
            postParamsHeader = postParamsHeader,
        )
        assertThat(verificationResult).isEqualTo(VerificationResult.VERIFIED)
    }

    // endregion

    // region Helpers

    private fun callVerifyResponse(
        signingManager: SigningManager,
        requestPath: String = "test-url-path",
        // Generated signature using test keys:
        // Test root private key: YMHMQMpepBKamtSzO8KCN2M8Z3AUW5R1JXIFtxUWFUI
        // Test root public key: yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=
        // Test intermediate private key: fPBoIjQ7DecE89ATW6PZsqLVQNyEs5fiX3sUyS3U4YI
        // Test intermediate public key: xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fs=
        signature: String? = "xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fsAAMNQ1HiPDL34Vx0Uy74KPV5mztuk3DHBpucT/rSYVlkxIa3ModYmPfYZ20lnlbSB1UiP6oJHwAA2pXlS6AQ5eLSuAt3A7Zq9dAGTs3BUhkDsuHE0AqCL5aklRXCGA59RoXPpLkcXDvNGGoC0u0elbxac/Y3kECzXLhawDOD9NEtSDXFw5Eh92U32YV6VHirQKwAO",
        nonce: String = "MTIzNDU2Nzg5MGFi",
        body: String? = "{\"request_date\":\"2023-02-21T18:58:36Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n",
        requestTime: String? = "1677005916012",
        eTag: String? = null,
        postParamsHeader: String? = null,
    ) = signingManager.verifyResponse(requestPath, signature, nonce, body, requestTime, eTag, postParamsHeader)

    // We can use this function to create new signatures if we need to change the test data
    @Suppress("Unused")
    private fun createFakeSignature(
        rootPrivateKeyEncoded: String = "YMHMQMpepBKamtSzO8KCN2M8Z3AUW5R1JXIFtxUWFUI",
        rootPublicKeyEncoded: String = "yg2wZGAr8Af+Unt9RImQDbL7qA81txk+ga0I+ylmcyo=",
        intermediatePrivateKeyEncoded: String = "fPBoIjQ7DecE89ATW6PZsqLVQNyEs5fiX3sUyS3U4YI",
        intermediatePublicKeyEncoded: String = "xoDYyUeHnIlSIAeOOzmvdNPOlbNSKK+xE0fE/ufS1fs=",
        intermediateKeyExpirationDaysBytes: ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(50_000).order(ByteOrder.LITTLE_ENDIAN).array(),
        requestPath: String = "test-url-path",
        postParamsHeader: String = "test-field-1,test-field-2:sha256:0a1d806f908079361fc0d18073dfcfcacb730afe4ace3d0478602479892e81d3",
        apiKey: String = this.apiKey,
        body: String? = "{\"request_date\":\"2023-02-21T18:58:36Z\",\"request_date_ms\":1677005916011,\"subscriber\":{\"entitlements\":{},\"first_seen\":\"2023-02-21T18:58:35Z\",\"last_seen\":\"2023-02-21T18:58:35Z\",\"management_url\":null,\"non_subscriptions\":{},\"original_app_user_id\":\"login\",\"original_application_version\":null,\"original_purchase_date\":null,\"other_purchases\":{},\"subscriptions\":{}}}\n",
        requestTime: String? = "1677005916012",
        eTag: String? = null,
        nonce: String? = "MTIzNDU2Nzg5MGFi",
        salt: ByteArray = ByteArray(16).apply { SecureRandom().nextBytes(this) }
    ): String {
        val rootSigner = Ed25519Sign(Base64.decode(rootPrivateKeyEncoded, Base64.DEFAULT))
        val intermediatePublicKey = Base64.decode(intermediatePublicKeyEncoded, Base64.DEFAULT)
        val intermediatePublicKeySignature = rootSigner.sign(intermediateKeyExpirationDaysBytes + intermediatePublicKey)
        val intermediateSigner = Ed25519Sign(Base64.decode(intermediatePrivateKeyEncoded, Base64.DEFAULT))
        val payloadToSign = salt +
            apiKey.toByteArray() +
            (nonce?.let { Base64.decode(it, Base64.DEFAULT) } ?: byteArrayOf()) +
            requestPath.toByteArray() +
            postParamsHeader.toByteArray() +
            (requestTime?.toByteArray() ?: byteArrayOf()) +
            (eTag?.toByteArray() ?: byteArrayOf()) +
            (body?.toByteArray() ?: byteArrayOf())
        val signature =  intermediatePublicKey +
            intermediateKeyExpirationDaysBytes +
            intermediatePublicKeySignature +
            salt +
            intermediateSigner.sign(payloadToSign)
        return Base64.encodeToString(signature, Base64.DEFAULT)
    }

    // endregion
}
