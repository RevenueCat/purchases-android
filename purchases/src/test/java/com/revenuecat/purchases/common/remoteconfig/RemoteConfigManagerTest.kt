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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var topicStore: RemoteConfigTopicStore
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
    private var capturedFetchContext: RemoteConfigFetchContext? = null
    private var capturedPrefetchedBlobs: List<String>? = null
    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit
    private lateinit var onError: (PurchasesError, GetRemoteConfigErrorHandlingBehavior) -> Unit

    private lateinit var onFallbackSuccess: (RemoteConfiguration, VerificationResult) -> Unit
    private lateinit var onFallbackError: (PurchasesError, GetRemoteConfigErrorHandlingBehavior) -> Unit

    @Before
    fun setup() {
        backend = mockk()
        diskCache = mockk(relaxed = true)
        blobStore = mockk(relaxed = true)
        topicStore = RemoteConfigTopicStore { diskCache.read()?.topics?.get(it.wireName) }
        sourceProvider = mockk(relaxed = true)
        blobFetcher = mockk(relaxed = true)
        manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = dateProvider,
            scope = testScope,
            topicStore = topicStore,
            sourceProvider = sourceProvider,
            blobFetcher = blobFetcher,
        )

        // Persist succeeds by default; the blob store is only touched once the configuration is persisted.
        every { diskCache.write(any()) } returns true

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            capturedAppUserID = arg(1)
            capturedFetchContext = arg(2)
            capturedDomain = arg(3)
            capturedManifest = arg(4)
            capturedPrefetchedBlobs = arg(5)
            onSuccess = arg(6)
            onError = arg(7)
        }

        every {
            backend.getRemoteConfigFallback(any(), any(), any(), any())
        } answers {
            onFallbackSuccess = arg(2)
            onFallbackError = arg(3)
        }
    }

    @Test
    fun `first run sends the app domain with no manifest`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        assertThat(capturedAppUserID).isEqualTo(TEST_APP_USER_ID)
        assertThat(capturedDomain).isEqualTo("app")
        assertThat(capturedManifest).isNull()
        assertThat(capturedPrefetchedBlobs).isEmpty()
    }

    @Test
    fun `the first request is forced to the app_start fetch context regardless of the requested context`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )

        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
    }

    @Test
    fun `the first stale request is forced to the app_start fetch context regardless of the requested context`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfigIfStale(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.Foreground,
        )

        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
    }

    @Test
    fun `only the first request is forced to the app_start fetch context`() {
        every { diskCache.read() } returns null

        // First committed request is forced to AppStart, even though IdentityChange was requested.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // The next committed request reports its own context.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.IdentityChange)
    }

    @Test
    fun `requests keep being forced to app_start until one succeeds`() {
        every { diskCache.read() } returns null

        // A failed first request must not consume the forced AppStart, so the next attempt is forced too.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)

        // Once a request succeeds, the forcing stops and later requests report their own context.
        onSuccess.invoke(null, VerificationResult.VERIFIED)
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.IdentityChange)
    }

    @Test
    fun `forcing stops once a 200 config is persisted`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag",
              "active_topics": [],
              "topics": {}
            }
        """.trimIndent()

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.IdentityChange)
    }

    @Test
    fun `a 200 that fails to parse keeps forcing app_start`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
        // A 200 whose body fails to parse commits nothing, so the initial config is still not committed.
        onSuccess.invoke(containerWithConfig("{ not valid json"), VerificationResult.VERIFIED)

        // The next request must still be forced to AppStart, since no config landed yet.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)
    }

    @Test
    fun `forcing stops once the fallback commits its config`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.AppStart)

        // The main request fails on a cold cache, routing to the fallback, which commits its config.
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackSuccess.invoke(
            remoteConfiguration(
                """
                {
                  "domain": "app",
                  "manifest": "v1.fallback.sources:etag",
                  "active_topics": [],
                  "topics": {}
                }
                """.trimIndent(),
            ),
            VerificationResult.VERIFIED,
        )

        // The fallback commit counts as the initial config, so later requests report their own context.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.IdentityChange,
        )
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.IdentityChange)
    }

    @Test
    fun `refreshRemoteConfigIfStale refreshes on the first call in a process`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `refreshRemoteConfigIfStale skips within the staleness window after a successful sync`() {
        every { diskCache.read() } returns null

        // First refresh completes (204), stamping the in-memory last-sync time.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // Same clock: still within the foreground window, so no new request.
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `refreshRemoteConfigIfStale refreshes again once the window elapses`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        currentTimeMillis = FIXED_MILLIS + STALE_FOREGROUND_AGE_MILLIS

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache makes refreshRemoteConfigIfStale refresh again within the window`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // Identity change wipes the cache and the in-memory marker, so the next user refreshes immediately.
        manager.clearCache(TEST_APP_USER_ID)
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache clears the refresh attempt cooldown`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        manager.clearCache(TEST_APP_USER_ID)
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failed refresh does not stamp the refresh time, but stale-gated retries are throttled`() {
        // A warm cache: a retryable error settles the refresh directly (no cold-start fallback), so this
        // exercises the "failure doesn't stamp the time" bookkeeping in isolation.
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        // Same clock: the failure left lastRefreshedAt unset, but the recent attempt keeps stale-gated refreshes
        // from retrying on every caller.
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }

        currentTimeMillis += REFRESH_ATTEMPT_COOLDOWN_MILLIS + 1
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a skipped stale-gated refresh while already in flight does not start the cooldown`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `explicit refresh retries immediately after a retryable failure`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `replays the persisted opaque manifest on subsequent runs`() {
        every { diskCache.read() } returns persisted(manifest = "v1.123.sources:etag1", domain = "app")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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
        val validData = byteArrayOf(1, 2, 3)

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(
            containerWithConfig(
                response,
                blobs = listOf(
                    inlineElement(REF_VALID, validData),
                    // A tampered blob fails verification: its decode() throws, so it is skipped.
                    inlineElement(REF_TAMPERED, decoded = null),
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
        val wantedData = byteArrayOf(1, 2, 3)

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(
            containerWithConfig(
                response,
                blobs = listOf(
                    inlineElement(REF_VALID, wantedData),
                    // Valid bytes, but not in prefetch_blobs nor referenced by any active topic (never decoded).
                    inlineElement(REF_UNWANTED, byteArrayOf(7)),
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(
            containerWithConfig(
                response,
                blobs = listOf(inlineElement(REF_VALID, byteArrayOf(1, 2, 3))),
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        // Re-arm the blob sources before fetching, but only if a prior cycle exhausted them.
        verify(exactly = 1) { sourceProvider.restartIfExhausted(RemoteConfigSourceHandle.Purpose.BLOB) }
        // The server prefetch set and any item flagged prefetch=true are warmed; non-flagged items are not.
        val prefetched = slot<List<String>>()
        verify(exactly = 1) { blobFetcher.prefetch(capture(prefetched)) }
        assertThat(prefetched.captured).containsExactlyInAnyOrder(REF_VALID, REF_TAMPERED)
    }

    @Test
    fun `prefetch preserves the server prefetch_blobs order, then item prefetch refs, deduped`() {
        every { diskCache.read() } returns null
        // prefetch_blobs deliberately not sorted: the SDK must warm them in the exact order the server sent.
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.workflows:etag2",
              "active_topics": ["workflows"],
              "prefetch_blobs": ["$REF_TAMPERED", "$REF_VALID", "$REF_UNWANTED"],
              "topics": {
                "workflows": {
                  "wf1": { "blob_ref": "$REF_VALID", "prefetch": true },
                  "wf2": { "blob_ref": "$REF_EXTRA", "prefetch": true }
                }
              }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val prefetched = slot<List<String>>()
        verify(exactly = 1) { blobFetcher.prefetch(capture(prefetched)) }
        // Server order first (as-is), then the only new item ref; REF_VALID is deduped to its first position.
        assertThat(prefetched.captured).containsExactly(REF_TAMPERED, REF_VALID, REF_UNWANTED, REF_EXTRA)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        verify(exactly = 0) { sourceProvider.restartIfExhausted(any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 204 response does not re-arm the sources nor prefetch`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { sourceProvider.restartIfExhausted(any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 204 response leaves the cache untouched and does no blob work`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { blobStore.retainOnly(any()) }
    }

    @Test
    fun `an error leaves the cache untouched`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a 4xx disables the endpoint for the session and blocks further refreshes and blob work`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.isDisabled).isTrue()

        // No further config request and no blob fetch happen for the rest of the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { blobFetcher.prefetch(any()) }
    }

    @Test
    fun `a 200 persist advances the generation and notifies listeners with it`() {
        every { diskCache.read() } returns null
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)
        val response = """
            {
              "domain": "app",
              "manifest": "v1",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "b" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(manager.configGeneration).isEqualTo(1)
        assertThat(recorder.committed).containsExactly(1)
        assertThat(recorder.invalidated).isEmpty()
    }

    @Test
    fun `clearCache advances the generation and invalidates listeners`() {
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)

        manager.clearCache(TEST_APP_USER_ID)

        assertThat(manager.configGeneration).isEqualTo(1)
        assertThat(recorder.invalidated).containsExactly(1)
        assertThat(recorder.committed).isEmpty()
    }

    @Test
    fun `a 4xx disable invalidates listeners once`() {
        every { diskCache.read() } returns null
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)

        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.isDisabled).isTrue()
        assertThat(recorder.invalidated).containsExactly(1)
    }

    @Test
    fun `a 4xx disable signals onRemoteConfigDisabled exactly once`() {
        every { diskCache.read() } returns null
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Further refreshes are no-ops (already disabled), so the disable signal must not fire again.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        assertThat(recorder.disabled).containsExactly(1)
    }

    @Test
    fun `clearCache does not signal onRemoteConfigDisabled`() {
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)

        manager.clearCache(TEST_APP_USER_ID)

        assertThat(recorder.disabled).isEmpty()
    }

    @Test
    fun `a normal commit does not signal onRemoteConfigDisabled`() {
        every { diskCache.read() } returns null
        val recorder = RecordingCommitListener()
        manager.registerListener(recorder)
        val response = """
            {
              "domain": "app",
              "manifest": "v1",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "b" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        assertThat(recorder.disabled).isEmpty()
    }

    @Test
    fun `committedTopicOrNull returns committed data without triggering a sync`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager()

        val topic = manager.committedTopicOrNull(RemoteConfigTopic.Workflows)

        assertThat(topic).isNotNull
        assertThat(topic!!["wf1"]!!.blobRef).isEqualTo(REF_VALID)
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `committedTopicOrNull returns null without a sync when nothing is committed`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        val topic = manager.committedTopicOrNull(RemoteConfigTopic.Workflows)

        assertThat(topic).isNull()
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Even though the topic is cached, a disabled endpoint yields no reads.
        assertThat(manager.topic(RemoteConfigTopic.Workflows)).isNull()
    }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady waits on prefetch-marked item blobs and returns the topic`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf(
                        "wf-prefetch" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID, prefetch = true),
                        // On-demand item: not prefetch, so its blob is not awaited here.
                        "wf-ondemand" to RemoteConfiguration.ConfigItem(blobRef = REF_UNWANTED),
                    ),
                ),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(any<List<String>>()) } returns true

        val topic = readManager().awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)

        assertThat(topic).isNotNull
        assertThat(topic!!.keys).containsExactlyInAnyOrder("wf-prefetch", "wf-ondemand")
        // Only the prefetch-marked item's blob is awaited; the on-demand item's is left for a lazy blobData read.
        coVerify(exactly = 1) { blobFetcher.ensureDownloaded(listOf(REF_VALID)) }
    }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady returns the topic without touching the fetcher when nothing is prefetch`() =
        runTest {
            every { diskCache.read() } returns persisted(
                manifest = "m",
                activeTopics = listOf("workflows"),
                topics = mapOf(
                    "workflows" to ConfigTopic(
                        mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID)),
                    ),
                ),
            )

            val topic = readManager().awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)

            assertThat(topic).isNotNull
            coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<List<String>>()) }
        }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady returns null and skips the fetcher once the endpoint is disabled`() =
        runTest {
            every { diskCache.read() } returns persisted(
                manifest = "m",
                activeTopics = listOf("workflows"),
                topics = mapOf(
                    "workflows" to ConfigTopic(
                        mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID, prefetch = true)),
                    ),
                ),
            )
            val manager = readManager()
            manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
            onError.invoke(
                PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
                GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
            )

            assertThat(manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)).isNull()
            coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<List<String>>()) }
        }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady returns null when an identity change wipes the cache mid-wait`() = runTest {
        val topicA = ConfigTopic(
            mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID, prefetch = true)),
        )
        var current: PersistedRemoteConfigurationState? = persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf("workflows" to topicA),
        )
        every { diskCache.read() } answers { current }
        // clearCache() wipes the disk cache while we're blocked on the prefetch download.
        coEvery { blobFetcher.ensureDownloaded(listOf(REF_VALID)) } answers {
            current = null
            true
        }

        // No app user is known, so the post-wait re-read does not self-prime a new sync: the cache stays wiped,
        // so the method reports null instead of the stale pre-clear snapshot it first waited on.
        val topic = readManager().awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)

        assertThat(topic).isNull()
    }

    @Test
    fun `awaitTopicAndPrefetchBlobsReady re-awaits when a newer sync commits a different topic mid-wait`() = runTest {
        val topicA = ConfigTopic(
            mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID, prefetch = true)),
        )
        val topicB = ConfigTopic(
            mapOf("wf2" to RemoteConfiguration.ConfigItem(blobRef = REF_UNWANTED, prefetch = true)),
        )
        var current = persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf("workflows" to topicA),
        )
        every { diskCache.read() } answers { current }
        // While we wait on topic A's blob, a new identity's sync commits topic B underneath us.
        coEvery { blobFetcher.ensureDownloaded(listOf(REF_VALID)) } answers {
            current = persisted(
                manifest = "m2",
                activeTopics = listOf("workflows"),
                topics = mapOf("workflows" to topicB),
            )
            true
        }
        coEvery { blobFetcher.ensureDownloaded(listOf(REF_UNWANTED)) } returns true

        val topic = readManager().awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.Workflows)

        // Returns the freshly committed topic after waiting on ITS prefetch blob, not the stale snapshot.
        assertThat(topic).isEqualTo(topicB)
        coVerifyOrder {
            blobFetcher.ensureDownloaded(listOf(REF_VALID))
            blobFetcher.ensureDownloaded(listOf(REF_UNWANTED))
        }
    }

    @Test
    fun `a retryable error does not disable the endpoint`() {
        // Warm cache: a retryable error settles directly (no cold-start fallback), so a subsequent refresh fires.
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        assertThat(manager.isDisabled).isFalse()

        // The endpoint is still usable, so a subsequent refresh fires.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // region Fallback endpoint

    @Test
    fun `a cold-start retryable error fetches from the fallback endpoint and commits its config`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        // No cached config: the retryable main error routes to the fallback endpoint for the same domain.
        verify(exactly = 1) { backend.getRemoteConfigFallback(any(), "app", any(), any()) }

        // The fallback returns a plain-JSON RemoteConfiguration, which the manager persists as the commit.
        onFallbackSuccess.invoke(
            remoteConfiguration(
                """
                {
                  "domain": "app",
                  "manifest": "v1.fallback.sources:etag",
                  "active_topics": ["sources"],
                  "prefetch_blobs": ["newBlob"],
                  "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
                }
                """.trimIndent(),
            ),
            VerificationResult.VERIFIED,
        )

        val written = slot<PersistedRemoteConfigurationState>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.manifest).isEqualTo("v1.fallback.sources:etag")
        assertThat(written.captured.activeTopics).containsExactly("sources")
        assertThat(written.captured.topics["sources"]!!["default"]!!.blobRef).isEqualTo("newBlob")
    }

    @Test
    fun `a fallback commit prefetches wanted blobs over the network with no inline extraction`() {
        every { diskCache.read() } returns null
        every { blobStore.contains(any()) } returns false

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackSuccess.invoke(
            remoteConfiguration(
                """
                {
                  "domain": "app",
                  "manifest": "v1.fallback.sources:etag",
                  "active_topics": ["sources"],
                  "prefetch_blobs": ["newBlob"],
                  "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
                }
                """.trimIndent(),
            ),
            VerificationResult.VERIFIED,
        )

        // The fallback body carries no inlined elements, so no inline blob is written; the wanted blob is
        // instead fetched over the network.
        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 1) { blobFetcher.prefetch(match { it.contains("newBlob") }) }
    }

    @Test
    fun `the fallback is not attempted when a retryable error occurs with cached data`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        // Cached data wins: no fallback request, and the cache is left untouched.
        verify(exactly = 0) { backend.getRemoteConfigFallback(any(), any(), any(), any()) }
        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a 4xx does not attempt the fallback`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        verify(exactly = 0) { backend.getRemoteConfigFallback(any(), any(), any(), any()) }
        assertThat(manager.isDisabled).isTrue()
    }

    @Test
    fun `a failing fallback releases the guard so a later refresh retries`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "still down"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        // The fallback settled the sync, so a later refresh is allowed to fire again.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failing fallback is throttled for stale-gated retries`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "still down"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )

        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }

        currentTimeMillis += REFRESH_ATTEMPT_COOLDOWN_MILLIS + 1
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a 4xx from the fallback disables the endpoint`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownBackendError, "server error"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        onFallbackError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.isDisabled).isTrue()
    }

    // endregion Fallback endpoint

    @Test
    fun `clearCache does not re-enable an endpoint disabled by a 4xx`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Identity change: the disable survives (it is an endpoint/app-level fact, not per-user).
        manager.clearCache(TEST_APP_USER_ID)

        assertThat(manager.isDisabled).isTrue()
        manager.refreshRemoteConfigIfStale(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a refresh while one is already in flight is skipped`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        // The first refresh has not settled yet (the stub captures callbacks without invoking them).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a new refresh is allowed after a success settles the in-flight one`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(null, VerificationResult.VERIFIED)
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a new refresh is allowed after an error settles the in-flight one`() {
        // Warm cache: the error settles the in-flight refresh directly (no cold-start fallback continuation).
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a malformed config payload leaves the cache untouched`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithConfig(config), VerificationResult.VERIFIED)

        // The parse failure is caught: nothing is persisted and the cache is left intact.
        verify(exactly = 0) { diskCache.write(any()) }

        // The guard was released in the finally block, so a subsequent refresh is allowed to start.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a config element that fails to decode leaves the cache untouched and releases the guard`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onSuccess.invoke(containerWithUndecodableConfig(), VerificationResult.VERIFIED)

        // The decode failure is caught: nothing is persisted and the cache is left intact.
        verify(exactly = 0) { diskCache.write(any()) }

        // The guard was released in the finally block, so a subsequent refresh is allowed to start.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `clearCache wipes the disk cache, blob store and resolved sources`() {
        manager.clearCache(TEST_APP_USER_ID)

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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        // Identity change wipes the cache before the in-flight request settles.
        manager.clearCache(TEST_APP_USER_ID)
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

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        // Run the 200 path on its own thread: it enters the lock and parks inside diskCache.write.
        val persistThread = thread { onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED) }
        check(writeEntered.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "persist did not reach diskCache.write" }

        // Identity change mid-persist: clearCache must block on the lock persist is holding.
        val clearThread = thread { manager.clearCache(TEST_APP_USER_ID) }

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

        manager.clearCache(TEST_APP_USER_ID)
        // A brand-new sync (e.g. for the new user) proceeds normally: the guard was released by clearCache.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `topic triggers a sync for an uncached topic when none is in flight and returns the committed index`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        // The first committed request is forced to AppStart, so prime it before asserting the on-demand read's Read.
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = RemoteConfigFetchContext.AppStart,
        )
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        // Nothing is in flight and nothing is cached: the read triggers its own sync and waits for it.
        var result: ConfigTopic? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        // The on-demand sync is issued as foreground for the current user with a read fetch context.
        assertThat(capturedAppUserID).isEqualTo(TEST_APP_USER_ID)
        assertThat(capturedFetchContext).isEqualTo(RemoteConfigFetchContext.Read)
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
    fun `topic-triggered syncs are throttled after a retryable failure`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("sources"),
            topics = mapOf(
                "sources" to ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        var firstResult: ConfigTopic? = ConfigTopic(emptyMap())
        val firstRead = launch(UnconfinedTestDispatcher(testScheduler)) {
            firstResult = manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        assertThat(firstRead.isCompleted).isTrue()
        assertThat(firstResult).isNull()

        assertThat(manager.topic(RemoteConfigTopic.Workflows)).isNull()
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }

        currentTimeMillis += REFRESH_ATTEMPT_COOLDOWN_MILLIS + 1
        val retryRead = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 2) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        onError.invoke(
            PurchasesError(PurchasesErrorCode.NetworkError),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        assertThat(retryRead.isCompleted).isTrue()
    }

    @Test
    fun `an on-demand sync after clearCache uses the newly bound user, not the lagging provider`() = runTest {
        // The device-cache-backed provider can still return the previous user for a window after an identity
        // change (the caller caches the new ID only after wiping remote config), but clearCache() binds the new
        // user atomically with the epoch bump. The on-demand sync this cold read triggers must therefore fetch
        // for the new user — otherwise it would repopulate the freshly wiped cache with the previous user's
        // config and bleed it across identities.
        every { diskCache.read() } returns null
        val manager = readManager(appUserIDProvider = { "old-user" })

        manager.clearCache("new-user")

        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(capturedAppUserID).isEqualTo("new-user")

        // Settle the triggered sync so the parked read completes cleanly.
        onSuccess.invoke(null, VerificationResult.VERIFIED)
        assertThat(read.isCompleted).isTrue()
    }

    @Test
    fun `on-demand sync uses the bound user even if identity changes between resolving it and capturing the epoch`() =
        runTest {
            // The tightest form of the race: the identity change lands *after* the read resolves its (stale)
            // user but *before* refreshRemoteConfig captures the epoch. Simulated by a provider that clears the
            // cache for the new user and then returns the old one. Because refreshRemoteConfig snapshots the user
            // together with the epoch under the lock, the request must carry the newly bound user, not the stale
            // provider value — otherwise the old user's response would persist under the post-clear epoch.
            every { diskCache.read() } returns null

            lateinit var manager: RemoteConfigManager
            manager = readManager(
                appUserIDProvider = {
                    manager.clearCache("new-user")
                    "old-user"
                },
            )

            val read = launch(UnconfinedTestDispatcher(testScheduler)) {
                manager.topic(RemoteConfigTopic.Workflows)
            }
            verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
            assertThat(capturedAppUserID).isEqualTo("new-user")

            onSuccess.invoke(null, VerificationResult.VERIFIED)
            assertThat(read.isCompleted).isTrue()
        }

    @Test
    fun `an on-demand sync before any identity change syncs for the bootstrap provider user`() = runTest {
        // No clearCache() yet, so no identity is bound: the cold read falls back to the provider (the device
        // cache), which is accurate in this pre-first-change window since no transition is racing.
        every { diskCache.read() } returns null
        val manager = readManager(appUserIDProvider = { "bootstrap-user" })

        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.topic(RemoteConfigTopic.Workflows)
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertThat(capturedAppUserID).isEqualTo("bootstrap-user")

        onSuccess.invoke(null, VerificationResult.VERIFIED)
        assertThat(read.isCompleted).isTrue()
    }

    @Test
    fun `topic waits for an in-flight refresh and returns the freshly committed index`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        val manager = readManager()

        // A refresh is in flight: the backend stub captures the callbacks without settling them yet.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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
    fun `useIfCurrent executes exactly once for a current blob snapshot`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        val manager = readManager()
        val snapshot = manager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }
        var invocationCount = 0

        val used = manager.useIfCurrent(snapshot!!) {
            invocationCount++
            assertThat(it).isEqualTo(byteArrayOf(4, 2))
        }

        assertThat(used).isTrue
        assertThat(invocationCount).isEqualTo(1)
    }

    @Test
    fun `useIfCurrent rejects a blob snapshot after identity invalidation`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        val manager = readManager()
        val snapshot = manager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }
        manager.clearCache("new-user")
        var invoked = false

        assertThat(manager.useIfCurrent(snapshot!!) { invoked = true }).isFalse()
        assertThat(invoked).isFalse()
    }

    @Test
    fun `useIfCurrent rejects a blob snapshot after a newer config commit`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        val manager = readManager()
        val snapshot = manager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        val response = """
            {
              "domain": "app",
              "manifest": "v2",
              "active_topics": ["workflows"],
              "topics": { "workflows": { "wf1": { "blob_ref": "$REF_TAMPERED" } } }
            }
        """.trimIndent()
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)
        var invoked = false

        assertThat(manager.useIfCurrent(snapshot!!) { invoked = true }).isFalse()
        assertThat(invoked).isFalse()
    }

    @Test
    fun `useIfCurrent rejects a blob snapshot after disabling or closing the manager`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)

        val disabledManager = readManager()
        val disabledSnapshot = disabledManager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }
        disabledManager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )
        assertThat(disabledManager.useIfCurrent(disabledSnapshot!!) {}).isFalse()

        val closedManager = readManager()
        val closedSnapshot = closedManager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }
        closedManager.close()
        assertThat(closedManager.useIfCurrent(closedSnapshot!!) {}).isFalse()
    }

    @Test
    fun `blobData retains legacy behavior when identity changes during blob resolution`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } answers {
            manager.clearCache("new-user")
            true
        }
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)

        val result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }

        assertThat(result).isEqualTo(byteArrayOf(4, 2))
    }

    @Test
    fun `blobData retains legacy behavior when the item ref changes during blob resolution`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } answers {
            val response = """
                {
                  "domain": "app",
                  "manifest": "v2",
                  "active_topics": ["workflows"],
                  "topics": { "workflows": { "wf1": { "blob_ref": "$REF_TAMPERED" } } }
                }
            """.trimIndent()
            onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)
            true
        }

        val result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }

        assertThat(result).isEqualTo(byteArrayOf(4, 2))
    }

    @Test
    fun `blobDataSnapshot discards bytes when the item ref changes during blob resolution`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID))),
            ),
        )
        every { blobStore.read(REF_VALID) } returns byteArrayOf(4, 2)
        manager.refreshRemoteConfig(
            appInBackground = false,
            appUserID = TEST_APP_USER_ID,
            fetchContext = DEFAULT_FETCH_CONTEXT,
        )
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } answers {
            val response = """
                {
                  "domain": "app",
                  "manifest": "v2",
                  "active_topics": ["workflows"],
                  "topics": { "workflows": { "wf1": { "blob_ref": "$REF_TAMPERED" } } }
                }
            """.trimIndent()
            onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)
            true
        }

        val result = manager.blobDataSnapshot(RemoteConfigTopic.Workflows, "wf1") { it }

        assertThat(result).isNull()
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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
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
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `blobData does not trigger a sync for an uncached item once the endpoint is disabled`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })
        // A 4xx disables the endpoint for the session (this is the only config request that should ever fire).
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        assertThat(manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isNull()
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        assertThat(manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }).isEqualTo(byteArrayOf(9))
    }

    @Test
    fun `mergeItemsBlobData nests each item's blob JSON under its item key and decodes into a single object`() =
        runTest {
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
            // Merged into {"wf1": {"value":"x"}, "wf2": {"value":"y"}}, keyed by item key.
            every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()
            every { blobStore.read(REF_TAMPERED) } returns """{"value":"y"}""".toByteArray()

            val result = readManager()
                .mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

            assertThat(result).isEqualTo(MergedBlob(wf1 = Section("x"), wf2 = Section("y")))
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
        every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()

        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData returns null when a resolved blob is not valid JSON`() = runTest {
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
        every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()
        // wf2's blob is not parseable JSON, so the whole merge fails.
        every { blobStore.read(REF_TAMPERED) } returns "not json".toByteArray()

        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData returns null when the merged object does not deserialize into T`() = runTest {
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
        // Both blobs are valid JSON, but wf2 is missing Section's required "value" field, so the merged
        // {"wf1": {"value":"x"}, "wf2": {"nope":true}} can't decode into MergedBlob.
        every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()
        every { blobStore.read(REF_TAMPERED) } returns """{"nope":true}""".toByteArray()

        val result = readManager().mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData returns null for an empty key list without touching the network`() = runTest {
        every { diskCache.read() } returns persisted(
            manifest = "m",
            activeTopics = listOf("workflows"),
            topics = mapOf(
                "workflows" to ConfigTopic(
                    mapOf("wf1" to RemoteConfiguration.ConfigItem(blobRef = REF_VALID)),
                ),
            ),
        )

        // Even though OptionalBlob would decode from an empty object, an empty key list is a no-op → null.
        val result = readManager().mergeItemsBlobData<OptionalBlob>(RemoteConfigTopic.Workflows, emptyList())

        assertThat(result).isNull()
        coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<String>()) }
        verify(exactly = 0) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
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
        every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()

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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        val result = manager.mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))

        assertThat(result).isNull()
        coVerify(exactly = 0) { blobFetcher.ensureDownloaded(any<String>()) }
    }

    @Test
    fun `mergeItemsBlobData returns null for an empty key list once the endpoint is disabled`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        // A 4xx disables the endpoint for the session.
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
        onError.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "bad request"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE,
        )

        // Without the disabled short-circuit an empty key list would build an empty object and decode to a
        // non-null OptionalBlob (all fields optional); the kill-switch must take precedence and return null.
        val result = manager.mergeItemsBlobData<OptionalBlob>(RemoteConfigTopic.Workflows, emptyList())

        assertThat(result).isNull()
    }

    @Test
    fun `mergeItemsBlobData triggers a single shared sync for multiple uncached items then decodes`() = runTest {
        var state: PersistedRemoteConfigurationState? = null
        every { diskCache.read() } answers { state }
        every { diskCache.write(any()) } answers { state = firstArg(); true }
        coEvery { blobFetcher.ensureDownloaded(REF_VALID) } returns true
        coEvery { blobFetcher.ensureDownloaded(REF_TAMPERED) } returns true
        every { blobStore.read(REF_VALID) } returns """{"value":"x"}""".toByteArray()
        every { blobStore.read(REF_TAMPERED) } returns """{"value":"y"}""".toByteArray()
        val manager = readManager(appUserIDProvider = { TEST_APP_USER_ID })

        // Nothing cached and nothing in flight: the read fans out, but the concurrent per-item waits
        // collapse onto a single shared on-demand sync.
        var result: MergedBlob? = null
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.mergeItemsBlobData<MergedBlob>(RemoteConfigTopic.Workflows, listOf("wf1", "wf2"))
        }
        verify(exactly = 1) { backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any()) }
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
        assertThat(result).isEqualTo(MergedBlob(wf1 = Section("x"), wf2 = Section("y")))
    }

    @Test
    fun `clearCache unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        // Identity change unblocks the waiter, which re-reads the now-wiped cache and gives up.
        manager.clearCache(TEST_APP_USER_ID)

        assertThat(read.isCompleted).isTrue()
        assertThat(result).isNull()
    }

    @Test
    fun `close unblocks a body waiting on an in-flight refresh`() = runTest {
        every { diskCache.read() } returns null
        val manager = readManager()
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

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
        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)

        var result: ByteArray? = byteArrayOf(1)
        val read = launch(UnconfinedTestDispatcher(testScheduler)) {
            result = manager.blobData(RemoteConfigTopic.Workflows, "wf1") { it }
        }
        assertThat(read.isActive).isTrue()

        // Cold start (empty cache): the retryable main error continues into the fallback, so the read stays
        // parked until the fallback also settles. A failing fallback then releases the guard and unblocks it.
        onError.invoke(
            PurchasesError(PurchasesErrorCode.UnknownError, "boom"),
            GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY,
        )
        assertThat(read.isActive).isTrue()
        onFallbackError.invoke(
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
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            arg<(RCContainer?, VerificationResult) -> Unit>(6)
                .invoke(containerWithConfig(stressResponse), VerificationResult.VERIFIED)
        }
        val manager = RemoteConfigManager(
            backend,
            diskCache,
            blobStore,
            dateProvider = dateProvider,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            ioDispatcher = Dispatchers.IO,
            topicStore = topicStore,
            sourceProvider = sourceProvider,
            blobFetcher = blobFetcher,
            appUserIDProvider = { TEST_APP_USER_ID },
        )

        val clearer = thread { repeat(STRESS_ITERATIONS) { manager.clearCache(TEST_APP_USER_ID) } }
        val refresher = thread {
            repeat(STRESS_ITERATIONS) {
                manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID, fetchContext = DEFAULT_FETCH_CONTEXT)
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
            topicStore = topicStore,
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

    /**
     * A fake inline blob element for a container mock: [ref] is what the manager reads to decide whether it
     * wants the blob; [decoded] is the bytes decode() yields, or null to model a bad blob whose decode() throws.
     */
    private fun inlineElement(ref: String, decoded: ByteArray?): RCElement {
        val element = mockk<RCElement>()
        every { element.checksumBase64() } returns ref
        if (decoded == null) {
            every { element.decode() } throws RCContainerFormatException("checksum verification failed")
        } else {
            every { element.decode() } returns decoded
        }
        return element
    }

    /** Parses a plain-JSON [RemoteConfiguration] the way the fallback endpoint delivers it (no RC container). */
    private fun remoteConfiguration(json: String): RemoteConfiguration =
        RemoteConfiguration.parse(json.toByteArray())

    private fun containerWithConfig(json: String, blobs: List<RCElement> = emptyList()): RCContainer {
        val container = mockk<RCContainer>()
        every { container.config } returns json.toByteArray()
        every { container.contentElements } returns blobs
        return container
    }

    private fun containerWithUndecodableConfig(): RCContainer {
        val container = mockk<RCContainer>()
        every { container.config } throws RCContainerFormatException("Unsupported content encoding id 2.")
        return container
    }

    private class RecordingCommitListener : RemoteConfigCommitListener {
        val committed = mutableListOf<Int>()
        val invalidated = mutableListOf<Int>()
        val disabled = mutableListOf<Int>()

        override fun onConfigCommitted(generation: Int) {
            committed += generation
        }

        override fun onConfigInvalidated(generation: Int) {
            invalidated += generation
        }

        override fun onRemoteConfigDisabled(generation: Int) {
            disabled += generation
        }
    }

    @Serializable
    private data class TestBlob(val id: String)

    // A single object assembled from several items in a topic, keyed by item key: each item's blob JSON is
    // nested under a field named after its item key.
    @Serializable
    private data class Section(val value: String)

    @Serializable
    private data class MergedBlob(val wf1: Section, val wf2: Section)

    // All fields optional, so it decodes from an empty JSON object to a non-null instance — used to prove the
    // disabled kill-switch short-circuits before an empty key list would build and decode an empty object.
    @Serializable
    private data class OptionalBlob(val a: String? = null)

    private companion object {
        private const val TEST_APP_USER_ID = "test-app-user-id"
        private val DEFAULT_FETCH_CONTEXT = RemoteConfigFetchContext.AppStart
        private const val FIXED_MILLIS = 1_710_000_000_000L

        // Older than the 5-minute foreground staleness window (see Date?.isCacheStale).
        private const val STALE_FOREGROUND_AGE_MILLIS = 6 * 60 * 1000L
        private const val REFRESH_ATTEMPT_COOLDOWN_MILLIS = 60_000L
        private const val WAIT_SECONDS = 5L
        private const val BLOCKED_MILLIS = 200L
        private const val STRESS_ITERATIONS = 50
        private const val STRESS_READERS = 8
        private const val STRESS_TIMEOUT_SECONDS = 30L
        private const val REF_VALID = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
        private const val REF_TAMPERED = "IIIIJJJJKKKKLLLLMMMMNNNNOOOOPPPP"
        private const val REF_UNWANTED = "QQQQRRRRSSSSTTTTUUUUVVVVWWWWXXXX"
        private const val REF_EXTRA = "11112222333344445555666677778888"
    }
}
