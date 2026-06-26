package com.revenuecat.purchases.common.workflows

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PurchasesError
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
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * End-to-end spike: drives a fake `/v1/config` sync through the **real** [RemoteConfigManager] (now the single
 * read front door via `topic()`/`body()`) + [RemoteConfigBlobStore] + [WorkflowsTopicHandler] +
 * [RemoteConfigBlobFetcher] + [WorkflowsConfigProvider]. Only the backend transport and the blob HTTP download
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
    private lateinit var onError: (PurchasesError) -> Unit

    /** Stateful stand-in for the persisted config file: write stashes, read returns the latest. */
    private var persistedState: PersistedRemoteConfigurationState? = null

    /** Stand-in for the (Phase 5) downloader: ref -> bytes the fake fetcher writes into the store. */
    private val downloads = mutableMapOf<String, ByteArray>()
    private var downloadCount = 0

    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        backend = mockk()
        diskCache = mockk()
        every { diskCache.read() } answers { persistedState }
        every { diskCache.write(any()) } answers { persistedState = firstArg(); true }
        blobStore = RemoteConfigBlobStore(context)
        blobStore.retainOnly(emptySet()) // start from a clean blob dir between runs
        // Fake fetcher standing in for the real (Phase 5) one: already-cached refs short-circuit (no download),
        // otherwise it writes the arranged bytes into the store.
        val fetcher = RemoteConfigBlobFetcher { ref ->
            if (blobStore.contains(ref)) {
                true
            } else {
                downloadCount++
                val bytes = downloads[ref]
                if (bytes != null) {
                    blobStore.write(ref, ByteBuffer.wrap(bytes))
                    true
                } else {
                    false
                }
            }
        }
        manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            topicHandlers = listOf(WorkflowsTopicHandler(fetcher)),
            blobFetcher = fetcher,
            scope = testScope,
        )
        provider = WorkflowsConfigProvider(manager)

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any())
        } answers {
            onSuccess = arg(5)
            onError = arg(6)
        }
    }

    @Test
    fun `a prefetched inline workflow is served without any download`() = runTest {
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
        assertThat(result!!.workflow.id).isEqualTo("wf-1")
        // The body arrived inline, so neither prefetch nor the read touches the downloader.
        assertThat(downloadCount).isZero()
    }

    @Test
    fun `a prefetch-marked workflow is downloaded during the sync, before any read`() = runTest {
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

        sync(config) // no inline blob; the handler prefetches it during the sync

        assertThat(blobStore.contains(ref)).isTrue()
        assertThat(downloadCount).isEqualTo(1)
        // The read is then served from disk — the shared fetcher does not download again.
        assertThat(provider.getWorkflow("wf-1")).isNotNull
        assertThat(downloadCount).isEqualTo(1)
    }

    @Test
    fun `a non-prefetched workflow commits metadata and downloads its body on demand`() = runTest {
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
        assertThat(manager.topic("workflows")?.containsKey("wf-1")).isTrue()

        // First read misses the blob store and pulls the body on demand.
        val result = provider.getWorkflow("wf-1")
        assertThat(result).isNotNull
        assertThat(result!!.workflow.id).isEqualTo("wf-1")
        assertThat(downloadCount).isEqualTo(1)

        // A second read is served from the store — no further download.
        assertThat(provider.getWorkflow("wf-1")).isNotNull
        assertThat(downloadCount).isEqualTo(1)
    }

    @Test
    fun `WorkflowManager kept as the seam serves through the config path by offering id`() = runTest {
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
        val workflowManager = WorkflowManager(workflowsConfigProvider = provider, scope = testScope)

        var delivered: WorkflowDataResult? = null
        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "premium_annual", // by offering id; resolved via config metadata, not a backend call
            onSuccess = { delivered = it },
            onError = { error = it },
        )

        assertThat(error).isNull()
        assertThat(delivered?.workflow?.id).isEqualTo("wf-1")
        assertThat(downloadCount).isEqualTo(1) // body pulled on demand through the manager
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
        uiConfig = UiConfig(),
    )

    private fun containerWith(configJson: String, vararg blobs: Pair<String, String>): RCContainer {
        val configElement = mockk<RCElement>()
        every { configElement.data } returns ByteBuffer.wrap(configJson.toByteArray())
        val elements = blobs.associate { (ref, json) ->
            val element = mockk<RCElement>()
            every { element.data } returns ByteBuffer.wrap(json.toByteArray())
            every { element.isChecksumValid() } returns true
            ref to element
        }
        val container = mockk<RCContainer>()
        every { container.config } returns configElement
        every { container.elements } returns elements
        return container
    }

    private companion object {
        // A valid content-address ref for the inline path (the store checks shape, not hash, on write).
        private const val INLINE_REF = "abcdefghijklmnopqrstuvwxyz012345"

        /** `base64url-nopad(sha256(bytes)[0 until 24])` — the ref the workflow body hashes to. */
        private fun refOf(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).copyOf(24)
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}
