package com.revenuecat.purchases.common.workflows

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
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
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.every
import io.mockk.mockk
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
import java.nio.ByteBuffer
import java.security.MessageDigest

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

    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit

    /** Stateful stand-in for the persisted config file: write stashes, read returns the latest. */
    private var persistedState: PersistedRemoteConfigurationState? = null

    /** Stand-in for the blob downloader: ref -> bytes the fake HTTP layer serves to the fetcher. */
    private val downloads = mutableMapOf<String, ByteArray>()
    private var downloadCount = 0

    // One scheduler for the manager's scope AND its ioDispatcher, so reads and the sync's persist all run
    // eagerly inline under runTest(testDispatcher).
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

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
            urlConnectionFactory = fakeUrlConnectionFactory(),
            scope = testScope,
        )
        manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            blobFetcher = fetcher,
            scope = testScope,
            ioDispatcher = testDispatcher,
        )
        provider = WorkflowsConfigProvider(manager)

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any())
        } answers {
            onSuccess = arg(5)
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

    private fun sync(configJson: String, vararg blobs: Pair<String, String>) {
        manager.refreshRemoteConfig(appInBackground = false, appUserID = "user-1")
        onSuccess.invoke(containerWith(configJson, *blobs), VerificationResult.VERIFIED)
    }

    private fun minimalWorkflow(id: String) = PublishedWorkflow(
        id = id,
        displayName = "Workflow $id",
        initialStepId = "step-1",
        steps = emptyMap(),
        screens = emptyMap(),
        // Still required pre-switch; the config-endpoint bodies stop shipping it once serving moves over.
        uiConfig = UiConfig(),
    )

    private fun containerWith(configJson: String, vararg blobs: Pair<String, String>): RCContainer {
        val configElement = mockk<RCElement>()
        every { configElement.decode() } returns ByteBuffer.wrap(configJson.toByteArray())
        val elements = blobs.associate { (ref, json) ->
            val element = mockk<RCElement>()
            val bytes = ByteBuffer.wrap(json.toByteArray())
            every { element.decode() } returns bytes
            every { element.matchesChecksum(any()) } returns true
            ref to element
        }
        val container = mockk<RCContainer>()
        every { container.config } returns configElement
        every { container.elements } returns elements
        return container
    }

    private fun fakeUrlConnectionFactory() = object : UrlConnectionFactory {
        override fun createConnection(url: String, requestMethod: String): UrlConnection {
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

        /** `base64url-nopad(sha256(bytes)[0 until 24])` — the ref the workflow body hashes to. */
        private fun refOf(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).copyOf(24)
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}
