package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.assertWarnLog
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.GetRemoteConfigErrorHandlingBehavior
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import com.revenuecat.purchases.common.networking.RCElement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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

    // Mutable clock so staleness tests can advance time past the refresh window.
    private var currentTimeMillis = FIXED_MILLIS
    private val dateProvider = object : DateProvider {
        override val now: Date get() = Date(currentTimeMillis)
    }

    private var capturedAppUserID: String? = null
    private var capturedDomain: String? = null
    private var capturedManifest: String? = null
    private var capturedPrefetchedBlobs: List<String>? = null
    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit
    private lateinit var onError: (PurchasesError, GetRemoteConfigErrorHandlingBehavior) -> Unit

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
            dateProvider = dateProvider,
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
    fun `refreshRemoteConfigIfStale refreshes on the first call in a process`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `refreshRemoteConfigIfStale skips within the staleness window after a successful sync`() {
        every { diskCache.read() } returns null

        // First refresh completes (204), stamping the in-memory last-sync time.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // Same clock: still within the foreground window, so no new request.
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `refreshRemoteConfigIfStale refreshes again once the window elapses`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        currentTimeMillis = FIXED_MILLIS + STALE_FOREGROUND_AGE_MILLIS

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache makes refreshRemoteConfigIfStale refresh again within the window`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // Identity change wipes the cache and the in-memory marker, so the next user refreshes immediately.
        manager.clearCache()
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failed refresh does not stamp the refresh time, so the next staleness check retries`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        // Same clock: the failure left lastRefreshedAt unset, so the staleness check retries immediately
        // instead of sitting out the window with no data.
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
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

        // Re-arm the blob sources before fetching, but only if a prior cycle exhausted them.
        verify(exactly = 1) { sourceProvider.restartIfExhausted(RemoteConfigSourceHandle.Purpose.BLOB) }
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

        verify(exactly = 0) { sourceProvider.restartIfExhausted(any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 204 response does not re-arm the sources nor prefetch`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { sourceProvider.restartIfExhausted(any()) }
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
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a 4xx disables the endpoint for the session and blocks further refreshes and blob work`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.isDisabled).isTrue()

        // No further config request and no blob fetch happen for the rest of the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `topic returns null once the endpoint is disabled even when data is cached`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager()
        // A 4xx disables the endpoint for the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Even though the topic is cached, a disabled endpoint yields no reads.
        assertThat(manager.topic(RemoteConfigTopic.Workflows)).isNull()
    }

    @Test
    fun `a retryable error does not disable the endpoint`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        assertThat(manager.isDisabled).isFalse()

        // The endpoint is still usable, so a subsequent refresh fires.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache does not re-enable an endpoint disabled by a 4xx`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Identity change: the disable survives (it is an endpoint/app-level fact, not per-user).
        manager.clearCache()

        assertThat(manager.isDisabled).isTrue()
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
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
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
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
    fun `a non-object topic item leaves the cache untouched and releases the guard, without crashing`() {
        every { diskCache.read() } returns null

        // Well-formed JSON whose topic item is a primitive: the custom item serializer must surface this as a
        // SerializationException the manager catches, not an uncaught error that would crash the app.
        val config = """
            {
              "domain": "app",
              "manifest": "manifest-1",
              "active_topics": ["sources"],
              "topics": { "sources": { "api": "not-an-object" } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(config), VerificationResult.VERIFIED)

        // The parse failure is caught: nothing is persisted and the cache is left intact.
        verify(exactly = 0) { diskCache.write(any()) }

        // The guard was released in the finally block, so a subsequent refresh is allowed to start.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
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

    // region Phase 6 read facade (topic / body) + wait-for-in-flight

    @Test
    fun `topic returns the committed item index and null for an unknown topic`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("sources"),
            topics = mapOf(
                "sources" to ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager()

        assertThat(manager.topic(RemoteConfigTopic.Sources)).containsKey("default")
        assertThat(manager.topic(RemoteConfigTopic.Workflows)).isNull()
    }

    @Test
    fun `topic returns null without triggering a sync when nothing is cached and no app user is known`() = runTest {
        every { diskCache.read() } returns null

        assertThat(readManager(appUserIDProvider = { null }).topic(RemoteConfigTopic.Sources)).isNull()
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `topic triggers a sync for an uncached topic when none is in flight and returns the committed index`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        // Nothing is in flight and nothing is cached: the read triggers its own sync and waits for it.
        var result: ConfigTopic? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
        // The on-demand sync is issued as foreground for the current user.
        assertThat(capturedAppUserID).isEqualTo(TEST_APP_USER_ID)
        assertThat(read.isActive).isTrue()

        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag",
              "active_topics": ["workflows"],
              "topics": { "workflows": { "wf1": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).containsKey("wf1")
    }

    @Test
    fun `topic waits for an in-flight refresh and returns the freshly committed index`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        val manager = readManager()

        // A refresh is in flight: the backend stub captures the callbacks without settling them yet.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        // The topic is not committed yet, so the read parks on the in-flight refresh.
        var result: ConfigTopic? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.topic(RemoteConfigTopic.Workflows)
        }
        assertThat(read.isActive).isTrue()

        // Settle the refresh with a 200 that commits the topic.
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag",
              "active_topics": ["workflows"],
              "topics": { "workflows": { "wf1": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).containsKey("wf1")
    }

    @Test
    fun `topic returns a committed topic without waiting even while a refresh is in flight`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager()

        // A refresh is in flight and never settles; a committed read must not block on it (else this hangs).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        assertThat(manager.topic(RemoteConfigTopic.Workflows)).containsKey("wf1")
    }

    @Test
    fun `topic maps each enum value to its wire name`() = runTest {
        // Persisted (and served) under the backend wire names; reads go through the typed enum.
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows", "ui_config", "sources"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
                "ui_config" to ConfigTopic(mapOf("ui" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED))),
                "sources" to ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = REF_UNWANTED))),
            ),
        )
        val manager = readManager()

        assertThat(manager.topic(RemoteConfigTopic.Workflows)).containsKey("wf")
        assertThat(manager.topic(RemoteConfigTopic.UiConfig)).containsKey("ui")
        assertThat(manager.topic(RemoteConfigTopic.Sources)).containsKey("default")
    }

    @Test
    fun `blobData returns null for an item without a blob ref`() = runTest {
        val metadata = buildJsonObject { put("offering_id", "offer_123") }
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(metadata = metadata))),
            ),
        )

        val result = readManager().blobData(RemoteConfigTopic.Workflows, "wf1") { it }

        // An item with no blob ref has no payload: neither the network nor the blob store is touched.
        assertThat(result).isNull()
        coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<String>()) }
        verify(exactly = 0) { blobStore.read(any()) }
    }

    @Test
    fun `blobData resolves a blob-backed item by fetching on demand then reading the blob`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)

        val result = readManager().blobData(RemoteConfigTopic.Workflows, "wf1") { it }

        assertThat(result).isEqualTo(byteArrayOf(4, 2))
        coVerify(exactly = 1) { blobFetcher.ensureDownloaded(REF_VALID) }
    }

    @Test
    fun `reified blobData fetches the blob on demand then deserializes it, like the transform overload`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns """{"id":"wf-1"}""".toByteArray()

        // The reified overload delegates to the transform overload, so it resolves the blob the same way:
        // fetch on demand (waiting for the download) and read it back, then decode the bytes as JSON.
        val result = readManager().blobData<TestBlob>(RemoteConfigTopic.Workflows, "wf1")

        assertThat(result).isEqualTo(TestBlob(id = "wf-1"))
        coVerify(exactly = 1) { blobFetcher.ensureDownloaded(REF_VALID) }
    }

    @Test
    fun `blobData returns null for a blob-backed item that cannot be fetched`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns false

        assertThat(readManager().blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isNull()
        verify(exactly = 0) { blobStore.read(any()) }
    }

    @Test
    fun `blobData skips the network for a blob-backed item once the endpoint is disabled`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager()
        // A 4xx disables the endpoint for the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isNull()
        coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<String>()) }
    }

    @Test
    fun `blobData triggers a sync for an uncached item when none is in flight and returns the committed data`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        // Nothing is in flight and nothing is cached: the read triggers its own sync and waits for it.
        var result: ByteArray? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
        // The on-demand sync is issued as foreground for the current user.
        assertThat(capturedAppUserID).isEqualTo(TEST_APP_USER_ID)
        assertThat(read.isActive).isTrue()

        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag",
              "active_topics": ["workflows"],
              "topics": { "workflows": { "wf1": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isEqualTo(byteArrayOf(4, 2))
    }

    @Test
    fun `blobData returns null without triggering a sync when no app user is known`() = runTest {
        every { diskCache.read() } returns null

        assertThat(readManager(appUserIDProvider = { null }).blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isNull()
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `blobData does not trigger a sync for an uncached item once the endpoint is disabled`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })
        // A 4xx disables the endpoint for the session (this is the only config request that should ever fire).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isNull()
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `blobData waits for an in-flight refresh and returns the freshly committed data`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        val manager = readManager()

        // A refresh is in flight: the backend stub captures the callbacks without settling them yet.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        // The item is not committed yet, so the read parks on the in-flight refresh.
        var result: ByteArray? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        // Settle the refresh with a 200 that commits the item.
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag",
              "active_topics": ["workflows"],
              "topics": { "workflows": { "wf1": { "blob_ref": "$REF_VALID" } } }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isEqualTo(byteArrayOf(4, 2))
    }

    @Test
    fun `blobData returns a committed item without waiting even while a refresh is in flight`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(9)
        val manager = readManager()

        // A refresh is in flight and never settles; a committed read must not block on it (else this hangs).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        assertThat(manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isEqualTo(byteArrayOf(9))
    }

    @Test
    fun `mergeItemsBlobData merges multiple items' JSON objects and decodes into a single object`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        "wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns true
        // Each item contributes part of the object; merged they form the full MergedBlob.
        every { blobStore.read(REF_VALID) } returns """{"a":"x"}""".toByteArray()
        every { blobStore.read(REF_TAMPERED) } returns """{"b":"y"}""".toByteArray()

        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isEqualTo(MergedBlob(a = "x", b = "y"))
    }

    @Test
    fun `mergeItemsBlobData lets a later item override an earlier item on a key collision`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        "wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns true
        every { blobStore.read(REF_VALID) } returns """{"a":"first","b":"y"}""".toByteArray()
        every { blobStore.read(REF_TAMPERED) } returns """{"a":"second"}""".toByteArray()

        // wf2 comes after wf1, so its "a" wins.
        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isEqualTo(MergedBlob(a = "second", b = "y"))
    }

    @Test
    fun `mergeItemsBlobData returns null when an item's blob cannot be fetched`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        "wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns false
        every { blobStore.read(REF_VALID) } returns """{"a":"x"}""".toByteArray()

        // All-or-nothing: one unresolved item nulls out the whole call, even though wf1 resolved.
        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData returns null for an item without a blob ref`() = runTest {
        val metadata = buildJsonObject { put("offering_id", "offer_123") }
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        // No blob ref: inline-only, no payload.
                        "wf2" to RemoteConfiguration.ConfigItem(metadata = metadata),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns """{"a":"x"}""".toByteArray()

        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData warns and returns null when one or more items cannot be resolved`() {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        "wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns false
        every { blobStore.read(REF_VALID) } returns """{"a":"x"}""".toByteArray()

        assertWarnLog(
            "Could not resolve remote config blob(s) for 1 of 2 requested item(s) in " +
                "topic 'workflows': [wf2]. Returning null.",
        ) {
            runTest {
                readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))
            }
        }
    }

    @Test
    fun `mergeItemsBlobData returns null once the endpoint is disabled without touching the network`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID),
                        "wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_TAMPERED),
                    ),
                ),
            ),
        )
        val manager = readManager()
        // A 4xx disables the endpoint for the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        val result = manager.mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
        coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<String>()) }
    }

    @Test
    fun `mergeItemsBlobData triggers a single shared sync for multiple uncached items then decodes`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns true
        every { blobStore.read(REF_VALID) } returns """{"a":"x"}""".toByteArray()
        every { blobStore.read(REF_TAMPERED) } returns """{"b":"y"}""".toByteArray()
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        // Nothing cached and nothing in flight: the read fans out, but the concurrent per-item waits
        // collapse onto a single shared on-demand sync.
        var result: MergedBlob? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any()) }
        assertThat(read.isActive).isTrue()

        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag",
              "active_topics": ["workflows"],
              "topics": {
                "workflows": {
                  "wf1": { "blob_ref": "$REF_VALID" },
                  "wf2": { "blob_ref": "$REF_TAMPERED" }
                }
              }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isEqualTo(MergedBlob(a = "x", b = "y"))
    }

    @Test
    fun `clearCache unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        // Identity change unblocks the waiter, which re-reads the now-wiped cache and gives up.
        manager.clearCache()

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `close unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        // SDK teardown unblocks the waiter, which re-reads the still-empty cache and gives up.
        manager.close()

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `a 204 unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        onSuccess.invoke(null, VerificationResult.VERIFIED)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `an error unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `concurrent reads with interleaved refreshes and cache clears all complete`() {
        // language=json
        val stressResponse = """
            {
              "domain": "app",
              "manifest": "m-stress",
              "active_topics": ["sources"],
              "topics": { "sources": { "api": { "url": "https://api.revenuecat.com" } } }
            }
        """.trimIndent()
        // A tiny thread-safe fake of the persisted state, so commits and wipes interleave like the real cache.
        val state = AtomicReference<PersistedRemoteConfigurationState?>(null)
        every { diskCache.read() } answers { state.get() }
        every { diskCache.write(any()) } answers { state.set(firstArg()); true }
        every { diskCache.clear() } answers { state.set(null) }
        every { blobStore.cachedRefs() } returns emptySet()
        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any())
        } answers {
            arg<(RCContainer?, VerificationResult) -> Unit>(5)
                .invoke(containerWithConfig(stressResponse), VerificationResult.VERIFIED)
        }
        val manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = dateProvider,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            ioDispatcher = Dispatchers.IO,
            sourceProvider = sourceProvider,
            blobFetcher = blobFetcher,
            appUserIDProvider = { TEST_APP_USER_ID },
        )

        val clearer = thread { repeat(STRESS_ITERATIONS) { manager.clearCache() } }
        val refresher = thread {
            repeat(STRESS_ITERATIONS) {
                manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
            }
        }
        // Cold readers constantly wait on (or self-trigger) refreshes; the epoch/guard/completion machinery
        // must never strand one on a hung await, even with clearCache racing every step.
        runBlocking {
            withTimeout(TimeUnit.SECONDS.toMillis(STRESS_TIMEOUT_SECONDS)) {
                List(STRESS_READERS) {
                    launch(Dispatchers.IO) {
                        repeat(STRESS_ITERATIONS) { manager.topic(RemoteConfigTopic.Sources) }
                    }
                }.joinAll()
            }
        }
        clearer.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS))
        refresher.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS))
        assertThat(clearer.isAlive).isFalse()
        assertThat(refresher.isAlive).isFalse()

        // The manager is still functional after the churn: a read self-primes and serves the committed topic.
        runBlocking {
            val topic = withTimeout(TimeUnit.SECONDS.toMillis(STRESS_TIMEOUT_SECONDS)) {
                manager.topic(RemoteConfigTopic.Sources)
            }
            assertThat(topic).isNotNull
        }
    }

    // A manager whose read methods run on this test's scheduler, so suspend reads are deterministic.
    private fun TestScope.readManager(appUserIDProvider: () -> String? = { null }): RemoteConfigManager {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = dateProvider,
            scope = CoroutineScope(dispatcher),
            ioDispatcher = dispatcher,
            sourceProvider = sourceProvider,
            blobFetcher = blobFetcher,
            appUserIDProvider = appUserIDProvider,
        )
    }

    // endregion

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

    @Serializable
    private data class TestBlob(val id: String)

    // A single object assembled from data spread across multiple items in a topic.
    @Serializable
    private data class MergedBlob(val a: String, val b: String)

    private companion object {
        private const val TEST_APP_USER_ID = "test-app-user-id"
        private const val FIXED_MILLIS = 1_710_000_000_000L

        // Older than the 5-minute foreground staleness window (see Date?.isCacheStale).
        private const val STALE_FOREGROUND_AGE_MILLIS = 6 * 60 * 1000L
        private const val WAIT_SECONDS = 5L
        private const val BLOCKED_MILLIS = 200L
        private const val STRESS_ITERATIONS = 50
        private const val STRESS_READERS = 8
        private const val STRESS_TIMEOUT_SECONDS = 30L
        private const val REF_VALID = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
        private const val REF_TAMPERED = "IIIIJJJJKKKKLLLLMMMMNNNNOOOOPPPP"
        private const val REF_UNWANTED = "QQQQRRRRSSSSTTTTUUUUVVVVWWWWXXXX"
    }
}
