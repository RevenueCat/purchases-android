package com.revenuecat.purchases.backend_integration_tests

import android.content.Context
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDiskCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class ProductionRemoteConfigIntegrationTest : BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey

    private val testFolder = "temp_production_remote_config_integration_test_folder"

    private lateinit var remoteConfigBlobStore: RemoteConfigBlobStore

    @After
    fun tearDownRemoteConfigStorage() {
        File(testFolder).deleteRecursively()
    }

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
    fun `fetches, persists and reads an inlined workflows blob back through the manager facade`() {
        setupTest(SignatureVerificationMode.Informational())
        every { appConfig.isDebugBuild } returns false
        val manager = buildRemoteConfigManager()

        // A cold read finds nothing cached, so through the facade it triggers a real /v1/config fetch, persists
        // the whole config to disk (extracting inline blobs into the blob store), and only then resolves. The
        // sources "api" item has no blob_ref, so it resolves to null, but the read still drives the end-to-end
        // fetch -> persist, leaving every active topic committed on disk for the reads below.
        runBlocking {
            withTimeout(FETCH_TIMEOUT) { manager.blobData(RemoteConfigTopic.Sources, "api") { it } }
        }

        // topic(): the workflows item index is now served from the real on-disk cache through the facade.
        val workflows = requireNotNull(runBlocking { manager.topic(RemoteConfigTopic.Workflows) }) {
            "Expected a workflows topic after the sync."
        }

        // Pick a workflows item whose blob the backend inlined and the manager extracted to disk during persist.
        val (itemKey, ref) = requireNotNull(
            workflows.entries.firstNotNullOfOrNull { (key, item) ->
                item.blobRef?.takeIf { remoteConfigBlobStore.contains(it) }?.let { key to it }
            },
        ) { "Expected a workflows item with an inlined blob stored on disk." }

        // blobData(): resolves the blob-backed item off disk through the facade (no network: it is already cached).
        val body = runBlocking { manager.blobData(RemoteConfigTopic.Workflows, itemKey) { it } }
        assertThat(body).isNotNull
        assertThat(body).isEqualTo(remoteConfigBlobStore.read(ref))
        // The bytes are real, structured content (a non-empty JSON object), not garbage.
        val json = JsonProvider.defaultJson.parseToJsonElement(body!!.decodeToString()).jsonObject
        assertThat(json).isNotEmpty()
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
                onError = { purchasesError, _ ->
                    error = purchasesError
                    latch.countDown()
                },
            )
        }
        return Triple(error, container, verification)
    }

    /**
     * Builds a [RemoteConfigManager] wired to the real [backend] and backed by the real [RemoteConfigDiskCache] +
     * [RemoteConfigBlobStore] on a temp folder, so a read exercises the full fetch -> persist -> disk -> read path.
     *
     * Only the blob fetcher's network layer is kept out: it is unit-tested elsewhere, and these reads only touch a
     * blob the real backend inlined and the manager extracted to disk, so [RemoteConfigBlobFetcher.ensureDownloaded]
     * resolves from the store (its real short-circuit for an already-cached ref) with no CDN call, and prefetch is
     * a no-op.
     */
    private fun buildRemoteConfigManager(): RemoteConfigManager {
        val context = mockk<Context>()
        every { context.noBackupFilesDir } returns File(testFolder).apply { mkdirs() }
        remoteConfigBlobStore = RemoteConfigBlobStore(context)
        val blobFetcher = mockk<RemoteConfigBlobFetcher>(relaxed = true)
        coEvery { blobFetcher.ensureDownloaded(any<String>()) } answers { remoteConfigBlobStore.contains(firstArg()) }
        return RemoteConfigManager(
            backend = backend,
            diskCache = RemoteConfigDiskCache(context),
            blobStore = remoteConfigBlobStore,
            blobFetcher = blobFetcher,
            appUserIDProvider = { REMOTE_CONFIG_USER },
        )
    }

    private companion object {
        private const val REMOTE_CONFIG_USER = "integrationTestRemoteConfigUser"
        private val FETCH_TIMEOUT = 15.seconds
    }
}
