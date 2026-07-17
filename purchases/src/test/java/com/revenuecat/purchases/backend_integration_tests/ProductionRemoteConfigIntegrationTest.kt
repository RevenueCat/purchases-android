package com.revenuecat.purchases.backend_integration_tests

import android.content.Context
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.remoteconfig.DefaultRemoteConfigSourceProvider
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDiskCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigFetchContext
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopicStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.io.File

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

        val config = RemoteConfiguration.parse(rcContainer.config)
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

        val config = RemoteConfiguration.parse(rcContainer.config)

        // The refs of workflows items whose bodies the container may have inlined.
        val workflows = requireNotNull(config.topics["workflows"]) { "Expected a workflows topic." }
        val wantedRefs = workflows.values.mapNotNull { it.blobRef }.toSet()

        // Find an inlined workflows item and capture its decoded bytes. decode() uncompresses and checks the
        // SHA-256 against the advertised ref, so the bytes are exactly the content the backend addressed there.
        val element = requireNotNull(rcContainer.contentElements.firstOrNull { it.checksumBase64() in wantedRefs }) {
            "Expected a workflows item with an inlined blob_ref."
        }

        // The decoded bytes are real, structured content (a non-empty JSON object), not garbage.
        val json = JsonProvider.defaultJson.parseToJsonElement(element.decode().decodeToString())
        assertThat(json.jsonObject).isNotEmpty()
    }

    @Test
    fun `fetches, persists and reads an inlined workflows blob back through the manager facade`() {
        setupTest(SignatureVerificationMode.Informational())
        every { appConfig.isDebugBuild } returns false
        val manager = buildRemoteConfigManager()

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
    fun `reads and deserializes a workflows blob into a typed value through the facade`() {
        setupTest(SignatureVerificationMode.Informational())
        every { appConfig.isDebugBuild } returns false
        val manager = buildRemoteConfigManager()

        val workflows = requireNotNull(runBlocking { manager.topic(RemoteConfigTopic.Workflows) }) {
            "Expected a workflows topic after the sync."
        }

        // Pick a workflows item whose blob the backend inlined and the manager extracted to disk during persist.
        val itemKey = requireNotNull(
            workflows.entries.firstNotNullOfOrNull { (key, item) ->
                key.takeIf { item.blobRef?.let(remoteConfigBlobStore::contains) == true }
            },
        ) { "Expected a workflows item with an inlined blob stored on disk." }

        // The reified facade resolves the blob off disk and deserializes it into a concrete @Serializable type.
        // We use a minimal test-only type (not the full PublishedWorkflow) because the shared defaultJson used by
        // the reified overload can't decode the full workflow's polymorphic component tree, and these lazy paywall
        // workflows carry no ui_config; a small subset over the stable top-level fields exercises the typed path.
        val workflow = runBlocking { manager.blobData<TestWorkflowBlob>(RemoteConfigTopic.Workflows, itemKey) }

        assertThat(workflow).isNotNull
        assertThat(workflow!!.id).isNotEmpty()
        assertThat(workflow.displayName).isNotEmpty()
        assertThat(workflow.initialStepId).isNotEmpty()
    }

    // A minimal projection of a workflows blob's stable top-level fields, used only to exercise the reified
    // blobData<T> deserialization path. Extra keys (screens, steps, metadata, ...) are ignored by defaultJson.
    @Serializable
    private data class TestWorkflowBlob(
        val id: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("initial_step_id") val initialStepId: String,
    )

    @Test
    fun `replaying the manifest returns no content`() {
        setupTest(SignatureVerificationMode.Informational())

        // First run: full resolve -> 200 with a fresh opaque manifest.
        val (firstError, firstContainer, firstVerification) = fetchRemoteConfig(manifest = null)
        assertThat(firstError).isNull()
        assertThat(firstVerification).isEqualTo(VerificationResult.VERIFIED)
        val rcContainer = requireNotNull(firstContainer) { "Expected a 200 container on the first run." }
        val manifest = RemoteConfiguration.parse(rcContainer.config).manifest

        // Replaying that manifest with nothing changed server-side -> 204 (success, but no container to parse).
        // A non-`app_start` context is required: the backend always fully resolves an `app_start` request, so it
        // never returns 204 for one.
        val (secondError, secondContainer, secondVerification) =
            fetchRemoteConfig(manifest = manifest, fetchContext = RemoteConfigFetchContext.Foreground)
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
        val manifest = RemoteConfiguration.parse(rcContainer.config).manifest

        // The 204 carries no body, but its signature covers the request context. Under enforcement a verification
        // failure would surface as an error, so a null error with a VERIFIED result confirms the 204 was verified.
        // A non-`app_start` context is required: an `app_start` request is always fully resolved (never 204).
        val (secondError, secondContainer, secondVerification) =
            fetchRemoteConfig(manifest = manifest, fetchContext = RemoteConfigFetchContext.Foreground)
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
        fetchContext: RemoteConfigFetchContext = RemoteConfigFetchContext.AppStart,
    ): Triple<PurchasesError?, RCContainer?, VerificationResult?> {
        every { appConfig.isDebugBuild } returns false

        var error: PurchasesError? = null
        var container: RCContainer? = null
        var verification: VerificationResult? = null
        ensureBlockFinishes { latch ->
            backend.getRemoteConfig(
                appInBackground = false,
                appUserID = "integrationTestRemoteConfigUser",
                fetchContext = fetchContext,
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
        val diskCache = RemoteConfigDiskCache(context)
        val topicStore = RemoteConfigTopicStore { diskCache.read()?.topics?.get(it.wireName) }
        return RemoteConfigManager(
            backend = backend,
            diskCache = diskCache,
            blobStore = remoteConfigBlobStore,
            topicStore = topicStore,
            sourceProvider = DefaultRemoteConfigSourceProvider(topicStore),
            blobFetcher = blobFetcher,
            appUserIDProvider = { REMOTE_CONFIG_USER },
        )
    }

    private companion object {
        private const val REMOTE_CONFIG_USER = "integrationTestRemoteConfigUser"
    }
}
