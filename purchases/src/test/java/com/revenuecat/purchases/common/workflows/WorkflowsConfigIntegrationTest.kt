package com.revenuecat.purchases.common.workflows

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.emptyUiConfig
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCElement
import com.revenuecat.purchases.common.remoteconfig.PersistedRemoteConfigurationState
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobFetcher
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobStore
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDiskCache
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSource
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceProvider
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigFetchContext
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopicStore
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.uiconfig.UiConfigResolution
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.util.Date

/**
 * End-to-end: drives a fake `/v1/config` sync through the **real** [RemoteConfigManager] (the single read
 * front door via `topic()`/`body()`, and the owner of the generic best-effort prefetch) + [RemoteConfigBlobStore]
 * + [RemoteConfigBlobFetcher] + [WorkflowsConfigProvider]. Only the backend transport and the blob HTTP download
 * are faked; the disk cache is a stateful in-memory stand-in so reads see exactly what the sync committed.
 */
@OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class WorkflowsConfigIntegrationTest {

    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var provider: WorkflowsConfigProvider
    private lateinit var manager: RemoteConfigManager

    private lateinit var onSuccess: (RCContainer?, Date?, VerificationResult) -> Unit

    /** Stateful stand-in for the persisted config file: write stashes, read returns the latest. */
    private var persistedState: PersistedRemoteConfigurationState? = null

    /** Stand-in for the blob downloader: ref -> bytes the fake HTTP layer serves to the fetcher. */
    private val downloads = mutableMapOf<String, ByteArray>()
    private var downloadCount = 0

    // One scheduler for the manager's scope AND its ioDispatcher, so reads and the sync's persist all run
    // eagerly inline under runTest(testDispatcher).
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    // A known app user, so cold reads can self-prime a sync the way they do in production.
    private var appUserID: String? = "user-1"

    // Mutable clock, so a test can age the committed config past the staleness window that gates read priming.
    private var currentTimeMillis = FIXED_MILLIS
    private val dateProvider = object : DateProvider {
        override val now: Date get() = Date(currentTimeMillis)
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        backend = mockk()
        diskCache = mockk()
        every { diskCache.read() } answers { persistedState }
        every { diskCache.write(any()) } answers { persistedState = firstArg(); true }
        blobStore = RemoteConfigBlobStore(context)
        blobStore.retainOnly(emptySet()) // start from a clean blob dir between runs
        val fetcher = RemoteConfigBlobFetcher(
            blobStore = blobStore,
            sourceProvider = FakeBlobSourceProvider,
            timeoutManager = mockk(relaxed = true),
            urlConnectionFactory = fakeUrlConnectionFactory(),
            scope = testScope,
        )
        manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = dateProvider,
            blobFetcher = fetcher,
            scope = testScope,
            ioDispatcher = testDispatcher,
            topicStore = RemoteConfigTopicStore { diskCache.read()?.topics?.get(it.wireName) },
            sourceProvider = FakeBlobSourceProvider,
            appUserIDProvider = { appUserID },
        )
        // Same scheduler as the manager, so a commit-driven re-warm also runs inline under runTest.
        provider = WorkflowsConfigProvider(manager, scope = testScope)

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            onSuccess = arg(7)
        }
    }

    @Test
    fun `a prefetched inline workflow is served without any download`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "prefetch_blobs": ["$INLINE_REF"],
              "topics": {
                "workflows": {
                  "wf-1": { "blob_ref": "$INLINE_REF", "offering_identifier": "premium_annual", "prefetch": true }
                }
              }
            }
        """.trimIndent()

        sync(config, INLINE_REF to workflowJson)

        assertThat(provider.workflowIdForOfferingId("premium_annual")).isEqualTo("wf-1")
        val result = provider.getWorkflow("wf-1")
        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo("wf-1")
        // The body arrived inline, so neither prefetch nor the read touches the downloader.
        assertThat(downloadCount).isZero()
    }

    @Test
    fun `duplicate offering_identifier resolves to the last entry`() = runTest(testDispatcher) {
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": {
                  "wf-1": { "blob_ref": "$INLINE_REF", "offering_identifier": "premium_annual" },
                  "wf-2": { "blob_ref": "$INLINE_REF", "offering_identifier": "premium_annual" }
                }
              }
            }
        """.trimIndent()

        sync(config)

        // Matches the old workflows-list map: last entry wins (and a warning is logged).
        assertThat(provider.workflowIdForOfferingId("premium_annual")).isEqualTo("wf-2")
    }

    @Test
    fun `a prefetch-marked workflow is downloaded during the sync, before any read`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val ref = refOf(workflowJson.toByteArray())
        downloads[ref] = workflowJson.toByteArray()
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$ref", "prefetch": true } }
              }
            }
        """.trimIndent()

        sync(config) // no inline blob; the manager prefetches it during the sync

        assertThat(blobStore.contains(ref)).isTrue()
        assertThat(downloadCount).isEqualTo(1)
        // The read is then served from disk — the shared fetcher does not download again.
        assertThat(provider.getWorkflow("wf-1")).isNotNull
        assertThat(downloadCount).isEqualTo(1)
    }

    @Test
    fun `a non-prefetched workflow commits metadata and downloads its body on demand`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val ref = refOf(workflowJson.toByteArray())
        downloads[ref] = workflowJson.toByteArray()
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$ref", "offering_identifier": "premium_annual" } }
              }
            }
        """.trimIndent()

        sync(config) // not inlined, not prefetched: only the metadata commits during the sync
        assertThat(downloadCount).isZero()
        assertThat(manager.topic(RemoteConfigTopic.Workflows)?.containsKey("wf-1")).isTrue()

        // First read misses the blob store and pulls the body on demand.
        val result = provider.getWorkflow("wf-1")
        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo("wf-1")
        assertThat(downloadCount).isEqualTo(1)

        // A second read is served from the store — no further download.
        assertThat(provider.getWorkflow("wf-1")).isNotNull
        assertThat(downloadCount).isEqualTo(1)
    }

    @Test
    fun `awaitReady waits for a non-inlined prefetch blob to finish downloading`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val ref = refOf(workflowJson.toByteArray())
        downloads[ref] = workflowJson.toByteArray()
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$ref", "prefetch": true } }
              }
            }
        """.trimIndent()

        sync(config) // not inlined; prefetched fire-and-forget during the sync

        provider.awaitReady()

        // awaitReady joined the prefetch: the blob is cached and a later read does not download again.
        assertThat(blobStore.contains(ref)).isTrue()
        assertThat(downloadCount).isEqualTo(1)
        assertThat(provider.getWorkflow("wf-1")).isNotNull
        assertThat(downloadCount).isEqualTo(1)
    }

    @Test
    fun `awaitReady returns without downloading when the prefetch blob is inlined`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "prefetch_blobs": ["$INLINE_REF"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$INLINE_REF", "prefetch": true } }
              }
            }
        """.trimIndent()

        sync(config, INLINE_REF to workflowJson)

        provider.awaitReady()

        assertThat(blobStore.contains(INLINE_REF)).isTrue()
        assertThat(downloadCount).isZero()
    }

    @Test
    fun `awaitReady does not download bodies of non-prefetched items`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val ref = refOf(workflowJson.toByteArray())
        downloads[ref] = workflowJson.toByteArray()
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$ref" } }
              }
            }
        """.trimIndent()

        sync(config) // not inlined, not prefetched

        provider.awaitReady()

        // Nothing prefetched, so the on-demand body is left untouched until an explicit read.
        assertThat(downloadCount).isZero()
        assertThat(blobStore.contains(ref)).isFalse()
    }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady returns the committed topic`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "prefetch_blobs": ["$INLINE_REF"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$INLINE_REF", "prefetch": true } }
              }
            }
        """.trimIndent()

        sync(config, INLINE_REF to workflowJson)

        val topic = manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)
        assertThat(topic).isNotNull
        assertThat(topic!!.containsKey("wf-1")).isTrue()
    }
    @Test
    fun `WorkflowManager kept as the seam serves through the config path by offering id`() = runTest(testDispatcher) {
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val ref = refOf(workflowJson.toByteArray())
        downloads[ref] = workflowJson.toByteArray()
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$ref", "offering_identifier": "premium_annual" } }
              }
            }
        """.trimIndent()
        sync(config)

        // WorkflowManager is kept as the consumer-facing seam; only its data source moved to the config layer.
        val workflowManager = workflowManagerWith(provider)

        // By offering id; resolved via config metadata, not a backend call.
        val delivered = workflowManager.getWorkflow("premium_annual")

        assertThat(delivered.id).isEqualTo("wf-1")
        assertThat(downloadCount).isEqualTo(1) // body pulled on demand through the manager
    }

    @Test
    fun `onPaywallConfigReady completes once the topic is committed, without forcing a second sync`() =
        runTest(testDispatcher) {
            val config = """
                {
                  "domain": "app",
                  "manifest": "v1.workflows:etag1",
                  "active_topics": ["workflows"],
                  "topics": {
                    "workflows": { "wf-1": { "blob_ref": "$INLINE_REF", "offering_identifier": "premium_annual" } }
                  }
                }
            """.trimIndent()
            sync(config)

            val workflowManager = workflowManagerWith(provider)
            var completed = false
            workflowManager.onPaywallConfigReady { completed = true }

            assertThat(completed).isTrue()
            // The topic was already committed by the sync() above — onPaywallConfigReady must not trigger
            // another one; this is what keeps OfferingsManager's gate cheap on a warm cache.
            verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `cold onPaywallConfigReady completes an empty config fetch without blob warnings or downloads`() =
        runTest(testDispatcher) {
            // A project with no paywalls configured still gets both paywall topics, committed with no items, so
            // readiness should self-prime one config request, then treat the empty ui_config as not configured.
            assertThat(persistedState).isNull()
            val workflowManager = workflowManagerWithRealUiConfig()
            val warningLogs = mutableListOf<String>()
            val previousLogHandler = currentLogHandler
            try {
                currentLogHandler = object : LogHandler {
                    override fun v(tag: String, msg: String) = Unit
                    override fun d(tag: String, msg: String) = Unit
                    override fun i(tag: String, msg: String) = Unit
                    override fun w(tag: String, msg: String) {
                        warningLogs += msg
                    }
                    override fun e(tag: String, msg: String, throwable: Throwable?) = Unit
                }

                var completed = false
                workflowManager.onPaywallConfigReady { completed = true }

                assertThat(completed).isFalse()
                verify(exactly = 1) {
                    backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any(), any())
                }

                onSuccess.invoke(containerWith(NO_PAYWALLS_CONFIG), Date(), VerificationResult.VERIFIED)

                assertThat(completed).isTrue()
                repeat(2) {
                    var warmCompleted = false
                    workflowManager.onPaywallConfigReady { warmCompleted = true }
                    assertThat(warmCompleted).isTrue()
                }
            } finally {
                currentLogHandler = previousLogHandler
            }

            verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            assertThat(downloadCount).isZero()
            assertThat(warningLogs).noneMatch { it.contains("Could not resolve remote config blob(s)") }
        }

    @Test
    fun `onPaywallConfigReady awaits an in-flight refresh when the cached ui_config topic is empty`() =
        runTest(testDispatcher) {
            sync(NO_PAYWALLS_CONFIG)
            val uiConfigProvider = UiConfigProvider(manager, scope = testScope)
            val workflowManager = WorkflowManager(
                workflowsConfigProvider = provider,
                uiConfigProvider = uiConfigProvider,
                workflowAssetPrewarmer = mockk(relaxed = true),
                scope = testScope,
            )

            manager.refreshRemoteConfig(
                appInBackground = false,
                appUserID = "user-1",
                fetchContext = RemoteConfigFetchContext.Foreground,
            )
            var completed = false
            workflowManager.onPaywallConfigReady { completed = true }

            assertThat(completed).isFalse()

            val app = """{"colors":{},"fonts":{}}"""
            val localizations = "{}"
            val variableConfig = """{"variable_compatibility_map":{},"function_compatibility_map":{}}"""
            val customVariables = "{}"
            val appRef = refOf(app.toByteArray())
            val localizationsRef = refOf(localizations.toByteArray())
            val variableConfigRef = refOf(variableConfig.toByteArray())
            val customVariablesRef = refOf(customVariables.toByteArray())
            val config = """
                {
                  "domain": "app",
                  "manifest": "v1.ui_config:etag2",
                  "active_topics": ["workflows", "ui_config"],
                  "topics": {
                    "workflows": {},
                    "ui_config": {
                      "app": { "blob_ref": "$appRef" },
                      "localizations": { "blob_ref": "$localizationsRef" },
                      "variable_config": { "blob_ref": "$variableConfigRef" },
                      "custom_variables": { "blob_ref": "$customVariablesRef" }
                    }
                  }
                }
            """.trimIndent()
            onSuccess.invoke(
                containerWith(
                    config,
                    appRef to app,
                    localizationsRef to localizations,
                    variableConfigRef to variableConfig,
                    customVariablesRef to customVariables,
                ),
                Date(),
                VerificationResult.VERIFIED,
            )

            assertThat(completed).isTrue()
            assertThat(uiConfigProvider.isWarm()).isTrue()
            verify(exactly = 2) {
                backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
            assertThat(downloadCount).isZero()
        }

    @Test
    fun `a paywall published later is picked up on the next commit`() = runTest(testDispatcher) {
        // The flip side of gating read priming: a project that had no paywalls must still converge on one
        // published later. That happens on the next ordinary (stale-driven) sync, not on every getOfferings.
        val uiConfigProvider = UiConfigProvider(manager, scope = testScope)
        manager.registerListener(provider)
        manager.registerListener(uiConfigProvider)
        val workflowManager = WorkflowManager(provider, uiConfigProvider, mockk(relaxed = true), scope = testScope)

        sync(NO_PAYWALLS_CONFIG)
        workflowManager.onPaywallConfigReady { }
        assertThat(provider.resolveWorkflow("premium_annual")).isEqualTo(WorkflowResolution.NoWorkflow)

        // The developer configures a paywall; the config goes stale and the next sync commits it.
        currentTimeMillis += STALE_FOREGROUND_AGE_MILLIS
        val workflowJson = JsonTools.json.encodeToString(PublishedWorkflow.serializer(), minimalWorkflow("wf-1"))
        val config = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag2",
              "active_topics": ["workflows", "ui_config"],
              "topics": {
                "workflows": { "wf-1": { "blob_ref": "$INLINE_REF", "offering_identifier": "premium_annual" } },
                "ui_config": {}
              }
            }
        """.trimIndent()
        sync(config, INLINE_REF to workflowJson)

        assertThat(provider.resolveWorkflow("premium_annual")).isEqualTo(WorkflowResolution.Found("wf-1"))
    }

    // Asset prewarming is out of scope here (covered by WorkflowManagerTest), so the manager gets a stubbed
    // ui-config provider and a no-op prewarmer.
    private fun workflowManagerWith(provider: WorkflowsConfigProvider) = WorkflowManager(
        workflowsConfigProvider = provider,
        uiConfigProvider = mockk {
            every { isWarm() } returns false
            coEvery { getUiConfig() } returns emptyUiConfig()
            coEvery { resolveUiConfig() } returns UiConfigResolution.Found(emptyUiConfig())
        },
        workflowAssetPrewarmer = mockk(relaxed = true),
        scope = testScope,
    )

    // Unlike [workflowManagerWith], this drives the REAL ui_config read path through the manager, which is where
    // the readiness gate's cold reads (and their config-request priming) actually live.
    private fun workflowManagerWithRealUiConfig() = WorkflowManager(
        workflowsConfigProvider = provider,
        uiConfigProvider = UiConfigProvider(manager, scope = testScope),
        workflowAssetPrewarmer = mockk(relaxed = true),
        scope = testScope,
    )

    private fun sync(configJson: String, vararg blobs: Pair<String, String>) {
        manager.refreshRemoteConfig(appInBackground = false, appUserID = "user-1", fetchContext = RemoteConfigFetchContext.AppStart)
        onSuccess.invoke(containerWith(configJson, *blobs), Date(), VerificationResult.VERIFIED)
    }

    private fun minimalWorkflow(id: String) = PublishedWorkflow(
        id = id,
        displayName = "Workflow $id",
        initialStepId = "step-1",
        steps = emptyMap(),
        screens = emptyMap(),
    )

    private fun containerWith(configJson: String, vararg blobs: Pair<String, String>): RCContainer {
        val blobElements = blobs.map { (ref, json) ->
            val element = mockk<RCElement>()
            every { element.checksumBase64() } returns ref
            every { element.decode() } returns json.toByteArray()
            element
        }
        val container = mockk<RCContainer>()
        every { container.config } returns configJson.toByteArray()
        every { container.contentElements } returns blobElements
        return container
    }

    private fun fakeUrlConnectionFactory() = object : UrlConnectionFactory {
        override fun createConnection(
            url: String,
            connectTimeoutMillis: Int,
            readTimeoutMillis: Int,
            requestMethod: String,
        ): UrlConnection {
            val ref = url.substringAfterLast("/")
            val bytes = downloads[ref]
            if (bytes != null) {
                downloadCount++
            }
            return object : UrlConnection {
                override val responseCode: Int = if (bytes == null) {
                    HttpURLConnection.HTTP_NOT_FOUND
                } else {
                    HttpURLConnection.HTTP_OK
                }
                override val inputStream = ByteArrayInputStream(bytes ?: byteArrayOf())
                override fun disconnect() = Unit
            }
        }
    }

    private companion object {
        private val FakeBlobSourceProvider = object : RemoteConfigSourceProvider {
            override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle =
                RemoteConfigSourceHandle(
                    purpose = purpose,
                    source = RemoteConfigSource(url = "https://blob.test/{blob_ref}", priority = 1, weight = 1),
                    token = 0,
                )

            override fun reportUnhealthy(handle: RemoteConfigSourceHandle) = Unit

            override fun restart(purpose: RemoteConfigSourceHandle.Purpose) = Unit

            override fun restartIfExhausted(purpose: RemoteConfigSourceHandle.Purpose): Boolean = false
        }

        // A valid content-address ref for the inline path (the store checks shape, not hash, on write).
        private const val INLINE_REF = "abcdefghijklmnopqrstuvwxyz012345"

        private const val FIXED_MILLIS = 1_710_000_000_000L

        // Older than the 5-minute foreground staleness window that gates read priming (see Date?.isCacheStale).
        private const val STALE_FOREGROUND_AGE_MILLIS = 6 * 60 * 1000L

        // What a project with no paywalls configured gets: both paywall topics active and committed, but empty.
        private val NO_PAYWALLS_CONFIG = """
            {
              "domain": "app",
              "manifest": "v1.workflows:etag1",
              "active_topics": ["workflows", "ui_config"],
              "topics": { "workflows": {}, "ui_config": {} }
            }
        """.trimIndent()

        /** `base64url-nopad(sha256(bytes)[0 until 24])` — the ref the workflow body hashes to. */
        private fun refOf(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).copyOf(24)
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}
