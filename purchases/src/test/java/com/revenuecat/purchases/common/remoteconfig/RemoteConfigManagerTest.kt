package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import com.revenuecat.purchases.common.networking.RCElement
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var sourceProvider: RemoteConfigSourceProvider
    private lateinit var blobFetcher: RemoteConfigBlobFetcher
    private lateinit var manager: RemoteConfigManager

    // Unconfined so the launched 200-path coroutine runs eagerly: invoke(onSuccess) then verify still works.
    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    private var capturedAppUserID: String? = null
    private var capturedDomain: String? = null
    private var capturedManifest: String? = null
    private var capturedPrefetchedBlobs: List<String>? = null
    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit
    private lateinit var onError: (PurchasesError) -> Unit

    @Before
    fun setup() {
        backend = mockk()
        diskCache = mockk(relaxed = true)
        blobStore = mockk(relaxed = true)
        sourceProvider = mockk(relaxed = true)
        blobFetcher = mockk(relaxed = true)
        manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = FixedDateProvider,
            scope = testScope,
            sourceProvider = sourceProvider,
            blobFetcher = blobFetcher,
        )

        // Persist succeeds by default; the blob store is only touched once the configuration is persisted.
        every { diskCache.write(any()) } returns true

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any())
        } answers {
            capturedAppUserID = arg(1)
            capturedDomain = arg(2)
            capturedManifest = arg(3)
            capturedPrefetchedBlobs = arg(4)
            onSuccess = arg(5)
            onError = arg(6)
        }
    }

    @Test
    fun `first run sends the app domain with no manifest`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        assertThat(capturedAppUserID).isEqualTo(TEST_APP_USER_ID)
        assertThat(capturedDomain).isEqualTo("app")
        assertThat(capturedManifest).isNull()
        assertThat(capturedPrefetchedBlobs).isEmpty()
    }

    @Test
    fun `replays the persisted opaque manifest on subsequent runs`() {
        every { diskCache.read() } returns persisted(manifest = "v1.123.sources:etag1", domain = "app")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        assertThat(capturedDomain).isEqualTo("app")
        assertThat(capturedManifest).isEqualTo("v1.123.sources:etag1")
    }

    @Test
    fun `reports only the prefetch blobs actually held on the request`() {
        every { diskCache.read() } returns persisted(
            manifest = "v1.1.sources:etag1",
            prefetchBlobs = listOf(REF_VALID, REF_TAMPERED),
        )
        every { blobStore.cachedRefs() } returns setOf(REF_VALID)

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        assertThat(capturedPrefetchedBlobs).containsExactly(REF_VALID)
    }

    @Test
    fun `a 200 response persists the server manifest, active topics and changed topic blob refs`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["newBlob"],
              "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfigurationState>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.manifest).isEqualTo("v1.200.sources:etag2")
        assertThat(written.captured.activeTopics).containsExactly("sources")
        assertThat(written.captured.prefetchBlobs).containsExactly("newBlob")
        assertThat(written.captured.topics).containsOnlyKeys("sources")
        assertThat(written.captured.topics["sources"]!!["default"]!!.blobRef).isEqualTo("newBlob")
        assertThat(written.captured.lastRefreshAt).isEqualTo(FIXED_MILLIS)
    }

    @Test
    fun `a 200 response overwrites changed topics, carries unchanged active topics forward, and prunes inactive ones`() {
        every { diskCache.read() } returns persisted(
            manifest = "v1.1.sources:etag1,workflows:wfEtag1,product_entitlement_mapping:pemEtag1",
            topics = mapOf(
                "sources" to ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "oldSources"))),
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = "wfBlob"))),
                "product_entitlement_mapping" to ConfigTopic(
                    mapOf("pem" to RemoteConfiguration.ConfigItem(blobRef = "pemBlob")),
                ),
            ),
        )
        // sources changed; workflows still active but unchanged (omitted by the server); PEM no longer active.
        val response = """
            {
              "domain": "app",
              "manifest": "v1.2.sources:etag2",
              "active_topics": ["sources", "workflows"],
              "topics": { "sources": { "default": { "blob_ref": "newSources" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfigurationState>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.topics).containsOnlyKeys("sources", "workflows")
        // Changed topic overwritten with the new index.
        assertThat(written.captured.topics["sources"]!!["default"]!!.blobRef).isEqualTo("newSources")
        // Unchanged-but-active topic carried forward verbatim (the server omitted its body).
        assertThat(written.captured.topics["workflows"]!!["wf1"]!!.blobRef).isEqualTo("wfBlob")
    }

    @Test
    fun `a 200 response caches valid inline blobs and skips tampered ones`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID", "$REF_TAMPERED"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        val validData = ByteBuffer.wrap(byteArrayOf(1, 2, 3))

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(
            containerWithConfig(
                response,
                elements = mapOf(
                    REF_VALID to inlineElement(validData, checksumValid = true),
                    REF_TAMPERED to inlineElement(ByteBuffer.wrap(byteArrayOf(9)), checksumValid = false),
                ),
            ),
            VerificationResult.VERIFIED,
        )

        verify(exactly = 1) { blobStore.write(REF_VALID, validData) }
        verify(exactly = 0) { blobStore.write(REF_TAMPERED, any()) }
    }

    @Test
    fun `a 200 response does not cache a valid inline blob the config does not reference`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        val wantedData = ByteBuffer.wrap(byteArrayOf(1, 2, 3))

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(
            containerWithConfig(
                response,
                elements = mapOf(
                    REF_VALID to inlineElement(wantedData, checksumValid = true),
                    // Valid bytes, but not in prefetch_blobs nor referenced by any active topic.
                    REF_UNWANTED to inlineElement(ByteBuffer.wrap(byteArrayOf(7)), checksumValid = true),
                ),
            ),
            VerificationResult.VERIFIED,
        )

        verify(exactly = 1) { blobStore.write(REF_VALID, wantedData) }
        verify(exactly = 0) { blobStore.write(REF_UNWANTED, any()) }
    }

    @Test
    fun `a failed persist leaves the blob store untouched`() {
        every { diskCache.read() } returns null
        every { diskCache.write(any()) } returns false
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(
            containerWithConfig(
                response,
                elements = mapOf(REF_VALID to inlineElement(ByteBuffer.wrap(byteArrayOf(1, 2, 3)), checksumValid = true)),
            ),
            VerificationResult.VERIFIED,
        )

        // Both blob-store mutations are gated on a successful persist.
        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { blobStore.retainOnly(any()) }
    }

    @Test
    fun `a 200 response evicts blobs no longer referenced by the config`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_TAMPERED" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val retained = slot<Set<String>>()
        verify(exactly = 1) { blobStore.retainOnly(capture(retained)) }
        // Prefetch blobs plus topic blob refs are retained; everything else is evicted.
        assertThat(retained.captured).containsExactlyInAnyOrder(REF_VALID, REF_TAMPERED)
    }

    @Test
    fun `a 200 response records no blob refs for an inline-only topic and does no blob work`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "topics": {
                "sources": {
                  "api": { "id": "primary", "url": "https://api.revenuecat.com", "priority": 100, "weight": 100 }
                }
              }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfigurationState>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        // An inline-only topic is persisted with its inline content and no blob_ref, and triggers no blob write.
        assertThat(written.captured.topics).containsOnlyKeys("sources")
        assertThat(written.captured.topics["sources"]!!["api"]!!.blobRef).isNull()
        verify(exactly = 0) { blobStore.write(any(), any()) }
    }

    @Test
    fun `a 200 response prefetches wanted blobs not already cached, after re-arming the blob sources`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag2",
              "active_topics": ["workflows"],
              "prefetch_blobs": ["$REF_VALID"],
              "topics": {
                "workflows": {
                  "wf1": { "blob_ref": "$REF_TAMPERED", "prefetch": true },
                  "wf2": { "blob_ref": "$REF_UNWANTED", "prefetch": false }
                }
              }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        // Re-arm the (possibly exhausted) blob sources before fetching.
        verify(exactly = 1) { sourceProvider.restart(RemoteConfigSourceHandle.Purpose.BLOB) }
        // The server prefetch set and any item flagged prefetch=true are warmed; non-flagged items are not.
        val prefetched = slot<List<String>>()
        verify(exactly = 1) { blobFetcher.prefetch(capture(prefetched)) }
        assertThat(prefetched.captured).containsExactlyInAnyOrder(REF_VALID, REF_TAMPERED)
    }

    @Test
    fun `a 200 response does not prefetch blobs already cached`() {
        every { diskCache.read() } returns null
        every { blobStore.contains(REF_VALID) } returns true
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID", "$REF_TAMPERED"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_TAMPERED" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val prefetched = slot<List<String>>()
        verify(exactly = 1) { blobFetcher.prefetch(capture(prefetched)) }
        assertThat(prefetched.captured).containsExactly(REF_TAMPERED)
    }

    @Test
    fun `a failed persist neither re-arms the sources nor prefetches`() {
        every { diskCache.read() } returns null
        every { diskCache.write(any()) } returns false
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "prefetch_blobs": ["$REF_VALID"],
              "topics": { "sources": { "default": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        verify(exactly = 0) { sourceProvider.restart(any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 204 response does not re-arm the sources nor prefetch`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { sourceProvider.restart(any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 204 response leaves the cache untouched and does no blob work`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { blobStore.retainOnly(any()) }
    }

    @Test
    fun `an error leaves the cache untouched`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(PurchasesError(PurchasesErrorCode.UnknownError, "boom"))

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a refresh while one is already in flight is skipped`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        // The first refresh has not settled yet (the stub captures callbacks without invoking them).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a new refresh is allowed after a success settles the in-flight one`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a new refresh is allowed after an error settles the in-flight one`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(PurchasesError(PurchasesErrorCode.UnknownError, "boom"))
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a malformed config payload leaves the cache untouched`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig("{ not valid json"), VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a config element that fails to decode leaves the cache untouched and releases the guard`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithUndecodableConfig(), VerificationResult.VERIFIED)

        // The decode failure is caught: nothing is persisted and the cache is left intact.
        verify(exactly = 0) { diskCache.write(any()) }

        // The guard was released in the finally block, so a subsequent refresh is allowed to start.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache wipes the disk cache, blob store and resolved sources`() {
        manager.clearCache()

        verify(exactly = 1) { diskCache.clear() }
        verify(exactly = 1) { blobStore.clear() }
        verify(exactly = 1) { sourceProvider.clear() }
    }

    @Test
    fun `a response that arrives after clearCache does not persist`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        // Identity change wipes the cache before the in-flight request settles.
        manager.clearCache()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        // The stale response is dropped (epoch guard): nothing is written over the wiped cache.
        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `clearCache during an in-flight persist wipes after the write, never leaving stale config`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
            }
        """.trimIndent()

        val writeEntered = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        val clearEntered = CountDownLatch(1)
        // persist() holds cacheLock while it writes; block inside the write so a concurrent clearCache races it.
        every { diskCache.write(any()) } answers {
            writeEntered.countDown()
            check(releaseWrite.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "write was not released in time" }
            true
        }
        every { diskCache.clear() } answers { clearEntered.countDown() }

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        // Run the 200 path on its own thread: it enters the lock and parks inside diskCache.write.
        val persistThread = thread { onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED) }
        check(writeEntered.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "persist did not reach diskCache.write" }

        // Identity change mid-persist: clearCache must block on the lock persist is holding.
        val clearThread = thread { manager.clearCache() }

        // While persist holds the lock, the wipe cannot run — the stale config can't be cleared out from under it.
        assertThat(clearEntered.await(BLOCKED_MILLIS, TimeUnit.MILLISECONDS)).isFalse()

        // Release the write: persist finishes and frees the lock, and only then does the wipe run.
        releaseWrite.countDown()
        persistThread.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS))
        clearThread.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS))

        assertThat(clearEntered.count).isZero()
        verify(exactly = 1) { diskCache.write(any()) }
        verify(exactly = 1) { diskCache.clear() }
    }

    @Test
    fun `the manager can sync again after clearCache`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
            }
        """.trimIndent()

        manager.clearCache()
        // A brand-new sync (e.g. for the new user) proceeds normally: the guard was released by clearCache.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        verify(exactly = 1) { diskCache.write(any()) }
    }

    private fun persisted(
        manifest: String,
        domain: String = "app",
        activeTopics: List<String> = emptyList(),
        prefetchBlobs: List<String> = emptyList(),
        topics: Map<String, ConfigTopic> = emptyMap(),
    ) = PersistedRemoteConfigurationState(
        domain = domain,
        manifest = manifest,
        activeTopics = activeTopics,
        prefetchBlobs = prefetchBlobs,
        topics = topics,
    )

    private fun inlineElement(data: ByteBuffer, checksumValid: Boolean): RCElement {
        val element = mockk<RCElement>()
        every { element.data } returns data
        // The manager decodes (codec-aware) then verifies the decoded bytes against the checksum.
        every { element.decode() } returns data
        every { element.matchesChecksum(any()) } returns checksumValid
        return element
    }

    private fun containerWithConfig(json: String, elements: Map<String, RCElement> = emptyMap()): RCContainer {
        val element = mockk<RCElement>()
        every { element.decode() } returns ByteBuffer.wrap(json.toByteArray())
        val container = mockk<RCContainer>()
        every { container.config } returns element
        every { container.elements } returns elements
        return container
    }

    private fun containerWithUndecodableConfig(): RCContainer {
        val element = mockk<RCElement>()
        every { element.decode() } throws RCContainerFormatException("Unsupported content encoding id 2.")
        val container = mockk<RCContainer>()
        every { container.config } returns element
        every { container.elements } returns emptyMap()
        return container
    }

    private companion object {
        private const val TEST_APP_USER_ID = "test-app-user-id"
        private const val FIXED_MILLIS = 1_710_000_000_000L
        private const val WAIT_SECONDS = 5L
        private const val BLOCKED_MILLIS = 200L
        private const val REF_VALID = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
        private const val REF_TAMPERED = "IIIIJJJJKKKKLLLLMMMMNNNNOOOOPPPP"
        private const val REF_UNWANTED = "QQQQRRRRSSSSTTTTUUUUVVVVWWWWXXXX"
        private val FixedDateProvider = object : DateProvider {
            override val now: Date get() = Date(FIXED_MILLIS)
        }
    }
}
