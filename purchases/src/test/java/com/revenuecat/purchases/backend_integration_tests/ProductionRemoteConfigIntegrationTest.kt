package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.every
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ProductionRemoteConfigIntegrationTest : BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    @Test
    fun `can fetch remote config`() {
        setupTest(SignatureVerificationMode.Informational())

        val (error, container, verification) = fetchRemoteConfig(manifest = null)

        assertThat(error).isNull()
        assertThat(verification).isEqualTo(VerificationResult.VERIFIED)
        val rcContainer = requireNotNull(container) { "Expected a 200 container, got 204 (no content)." }
        assertThat(rcContainer.config.isChecksumValid()).isTrue()

        val config = RemoteConfiguration.parse(rcContainer.config.data)
        assertThat(config.domain).isEqualTo("app")
        assertThat(config.manifest).isNotEmpty()
        assertThat(config.activeTopics).contains("sources", "ui_config", "workflows")

        val topicsSerializer = MapSerializer(
            String.serializer(),
            MapSerializer(String.serializer(), RemoteConfiguration.ConfigItem.serializer()),
        )
        val actualTopics = JsonProvider.defaultJson.encodeToJsonElement(topicsSerializer, config.topics).jsonObject
        val expectedApiSourcesTopic = JsonProvider.defaultJson.parseToJsonElement(
            """
            {
               "sources": [
                  { "id": "primary", "url": "https://api.revenuecat.com/", "priority": 0, "weight": 100 }
                ]
            }
            """.trimIndent(),
        )
        assertThat(actualTopics).containsKey("sources")
        assertThat(actualTopics["sources"]?.jsonObject?.get("api")).isEqualTo(expectedApiSourcesTopic)
        assertThat(actualTopics).containsKey("ui_config")
        assertThat(actualTopics).containsKey("workflows")
    }

    @Test
    fun `decodes an inline workflows content blob`() {
        setupTest(SignatureVerificationMode.Informational())

        val (error, container, verification) = fetchRemoteConfig(manifest = null)

        assertThat(error).isNull()
        assertThat(verification).isEqualTo(VerificationResult.VERIFIED)
        val rcContainer = requireNotNull(container) { "Expected a 200 container, got 204 (no content)." }

        val config = RemoteConfiguration.parse(rcContainer.config.data)

        // Pick a workflows item whose blob was inlined in the container, so we can decode it directly.
        val workflows = requireNotNull(config.topics["workflows"]) { "Expected a workflows topic." }
        val ref = requireNotNull(
            workflows.values.mapNotNull { it.blobRef }.firstOrNull { it in rcContainer.elements },
        ) { "Expected a workflows item with an inlined blob_ref." }
        val element = rcContainer.elements.getValue(ref)

        // isChecksumValid() decodes (uncompresses) the element and checks the SHA-256 of the
        // uncompressed bytes against the advertised ref, so a true result proves the uncompression
        // produced exactly the content the backend addressed.
        assertThat(element.isChecksumValid()).isTrue()

        // The decoded bytes are real, structured content (a non-empty JSON object), not garbage.
        val decodedView = element.decode().duplicate().apply { rewind() }
        val decodedBytes = ByteArray(decodedView.remaining()).also { decodedView.get(it) }
        val json = JsonProvider.defaultJson.parseToJsonElement(decodedBytes.decodeToString())
        assertThat(json.jsonObject).isNotEmpty()
    }

    @Test
    fun `replaying the manifest returns no content`() {
        setupTest(SignatureVerificationMode.Informational())

        // First run: full resolve -> 200 with a fresh opaque manifest.
        val (firstError, firstContainer, firstVerification) = fetchRemoteConfig(manifest = null)
        assertThat(firstError).isNull()
        assertThat(firstVerification).isEqualTo(VerificationResult.VERIFIED)
        val rcContainer = requireNotNull(firstContainer) { "Expected a 200 container on the first run." }
        val manifest = RemoteConfiguration.parse(rcContainer.config.data).manifest

        // Replaying that manifest with nothing changed server-side -> 204 (success, but no container to parse).
        val (secondError, secondContainer, secondVerification) = fetchRemoteConfig(manifest = manifest)
        assertThat(secondError).isNull()
        assertThat(secondContainer).isNull()
        assertThat(secondVerification).isEqualTo(VerificationResult.VERIFIED)
    }

    @Test
    fun `verifies the signed response when verification is enforced`() {
        setupTest(SignatureVerificationMode.Enforced())

        val (error, container, verification) = fetchRemoteConfig(manifest = null)

        // With enforcement on, a failed verification surfaces as an error, so a null error already implies the
        // signature verified; assert the result explicitly too.
        assertThat(error).isNull()
        assertThat(container).isNotNull
        assertThat(verification).isEqualTo(VerificationResult.VERIFIED)
        assertSigningPerformed()
    }

    @Test
    fun `verifies the no content response when verification is enforced`() {
        setupTest(SignatureVerificationMode.Enforced())

        // Get a fresh manifest from a full resolve, then replay it to force a signed 204.
        val (firstError, firstContainer, _) = fetchRemoteConfig(manifest = null)
        assertThat(firstError).isNull()
        val rcContainer = requireNotNull(firstContainer) { "Expected a 200 container on the first run." }
        val manifest = RemoteConfiguration.parse(rcContainer.config.data).manifest

        // The 204 carries no body, but its signature covers the request context. Under enforcement a verification
        // failure would surface as an error, so a null error with a VERIFIED result confirms the 204 was verified.
        val (secondError, secondContainer, secondVerification) = fetchRemoteConfig(manifest = manifest)
        assertThat(secondError).isNull()
        assertThat(secondContainer).isNull()
        assertThat(secondVerification).isEqualTo(VerificationResult.VERIFIED)
    }

    /**
     * Performs a single `/v1/config` request and blocks until it completes, returning the error (or `null`), the
     * container (`null` on a `204`), and the verification result (`null` on error).
     */
    private fun fetchRemoteConfig(
        manifest: String?,
        prefetchedBlobs: List<String> = emptyList(),
    ): Triple<PurchasesError?, RCContainer?, VerificationResult?> {
        every { appConfig.isDebugBuild } returns false

        var error: PurchasesError? = null
        var container: RCContainer? = null
        var verification: VerificationResult? = null
        ensureBlockFinishes { latch ->
            backend.getRemoteConfig(
                appInBackground = false,
                appUserID = "integrationTestRemoteConfigUser",
                domain = "app",
                manifest = manifest,
                prefetchedBlobs = prefetchedBlobs,
                onSuccess = { rcContainer, verificationResult ->
                    container = rcContainer
                    verification = verificationResult
                    latch.countDown()
                },
                onError = { purchasesError ->
                    error = purchasesError
                    latch.countDown()
                },
            )
        }
        return Triple(error, container, verification)
    }
}
