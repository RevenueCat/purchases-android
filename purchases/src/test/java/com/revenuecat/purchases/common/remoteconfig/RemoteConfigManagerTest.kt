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
    private lateinit var manager: RemoteConfigManager

    private var capturedDomain: String? = null
    private var capturedManifest: String? = null
    private var capturedPrefetchedBlobs: List<String>? = null
    private lateinit var onSuccess: (RCContainer?, VerificationResult) -> Unit
    private lateinit var onError: (PurchasesError) -> Unit

    @Before
    fun setup() {
        backend = mockk()
        diskCache = mockk(relaxed = true)
        manager = RemoteConfigManager(backend, diskCache, dateProvider = FixedDateProvider)

        every {
            backend.getRemoteConfig(any(), any(), any(), any(), any(), any())
        } answers {
            capturedDomain = arg(1)
            capturedManifest = arg(2)
            capturedPrefetchedBlobs = arg(3)
            onSuccess = arg(4)
            onError = arg(5)
        }
    }

    @Test
    fun `first run sends the app domain with no manifest`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false)

        assertThat(capturedDomain).isEqualTo("app")
        assertThat(capturedManifest).isNull()
        assertThat(capturedPrefetchedBlobs).isEmpty()
    }

    @Test
    fun `replays the persisted opaque manifest on subsequent runs`() {
        every { diskCache.read() } returns persisted(manifest = "v1.123.sources:etag1", domain = "app")

        manager.refreshRemoteConfig(appInBackground = false)

        assertThat(capturedDomain).isEqualTo("app")
        assertThat(capturedManifest).isEqualTo("v1.123.sources:etag1")
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

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfig>()
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

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfig>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.topicBlobRefs)
            .containsExactlyEntriesOf(mapOf("sources" to listOf("newSources")))
    }

    @Test
    fun `a 200 response persists an empty blob ref list for inline-only topics`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": "v1.200.sources:etag2",
              "active_topics": ["sources"],
              "topics": { "sources": { "api": { "url": "https://api.revenuecat.com", "priority": 100 } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccess.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val written = slot<PersistedRemoteConfig>()
        verify(exactly = 1) { diskCache.write(capture(written)) }
        assertThat(written.captured.topicBlobRefs).containsExactlyEntriesOf(mapOf("sources" to emptyList()))
    }

    @Test
    fun `a 204 response leaves the cache untouched`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccess.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `an error leaves the cache untouched`() {
        every { diskCache.read() } returns persisted(manifest = "v1.1.sources:etag1")

        manager.refreshRemoteConfig(appInBackground = false)
        onError.invoke(PurchasesError(PurchasesErrorCode.UnknownError, "boom"))

        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `a malformed config payload leaves the cache untouched`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccess.invoke(containerWithConfig("{ not valid json"), VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any()) }
    }

    private fun persisted(
        manifest: String,
        domain: String = "app",
        topicBlobRefs: Map<String, List<String>> = emptyMap(),
    ) = PersistedRemoteConfig(
        domain = domain,
        manifest = manifest,
        topicBlobRefs = topicBlobRefs,
    )

    private fun containerWithConfig(json: String): RCContainer {
        val element = mockk<RCElement>()
        every { element.data } returns ByteBuffer.wrap(json.toByteArray())
        val container = mockk<RCContainer>()
        every { container.config } returns element
        return container
    }

    private companion object {
        private const val FIXED_MILLIS = 1_710_000_000_000L
        private val FixedDateProvider = object : DateProvider {
            override val now: Date get() = Date(FIXED_MILLIS)
        }
    }
}
