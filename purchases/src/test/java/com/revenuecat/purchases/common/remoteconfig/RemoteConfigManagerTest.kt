package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCElement
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var manager: RemoteConfigManager

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
        manager = RemoteConfigManager(backend, diskCache, blobStore, dateProvider = FixedDateProvider)

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
        every { blobStore.contains(REF_VALID) } returns true
        every { blobStore.contains(REF_TAMPERED) } returns false

        manager.refreshRemoteConfig(appInBackground = false)

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
        assertThat(written.captured.topicBlobRefs).containsExactlyEntriesOf(mapOf("sources" to listOf("newBlob")))
        assertThat(written.captured.lastRefreshAt).isEqualTo(FIXED_MILLIS)
    }

    @Test
    fun `a 200 response merges unchanged topics and prunes topics no longer active`() {
        every { diskCache.read() } returns persisted(
            manifest = "v1.1.sources:etag1,product_entitlement_mapping:pemEtag1",
            topicBlobRefs = mapOf(
                "sources" to listOf("oldSources"),
                "product_entitlement_mapping" to listOf("pemBlob"),
            ),
        )
        // sources changed; product_entitlement_mapping no longer active.
        val response = """
            {
              "domain": "app",
              "manifest": "v1.2.sources:etag2",
              "active_topics": ["sources"],
              "topics": { "sources": { "default": { "blob_ref": "newSources" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false, appUserID = TEST_APP_USER_ID)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfigurationState>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.topicBlobRefs)
            .containsExactlyEntriesOf(mapOf("sources" to listOf("newSources")))
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

        manager.refreshRemoteConfig(appInBackground = false)
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

        manager.refreshRemoteConfig(appInBackground = false)
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
        // An inline-only topic references no blobs, so it persists an empty list and triggers no blob write.
        assertThat(written.captured.topicBlobRefs).containsExactlyEntriesOf(mapOf("sources" to emptyList()))
        verify(exactly = 0) { blobStore.write(any(), any()) }
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

    private fun persisted(
        manifest: String,
        domain: String = "app",
        activeTopics: List<String> = emptyList(),
        prefetchBlobs: List<String> = emptyList(),
        topicBlobRefs: Map<String, List<String>> = emptyMap(),
    ) = PersistedRemoteConfigurationState(
        domain = domain,
        manifest = manifest,
        activeTopics = activeTopics,
        prefetchBlobs = prefetchBlobs,
        topicBlobRefs = topicBlobRefs,
    )

    private fun inlineElement(data: ByteBuffer, checksumValid: Boolean): RCElement {
        val element = mockk<RCElement>()
        every { element.data } returns data
        every { element.isChecksumValid() } returns checksumValid
        return element
    }

    private fun containerWithConfig(json: String, elements: Map<String, RCElement> = emptyMap()): RCContainer {
        val element = mockk<RCElement>()
        every { element.data } returns ByteBuffer.wrap(json.toByteArray())
        val container = mockk<RCContainer>()
        every { container.config } returns element
        every { container.elements } returns elements
        return container
    }

    private companion object {
        private const val TEST_APP_USER_ID = "test-app-user-id"
        private const val FIXED_MILLIS = 1_710_000_000_000L
        private const val REF_VALID = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
        private const val REF_TAMPERED = "IIIIJJJJKKKKLLLLMMMMNNNNOOOOPPPP"
        private val FixedDateProvider = object : DateProvider {
            override val now: Date get() = Date(FIXED_MILLIS)
        }
    }
}
