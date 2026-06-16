package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCElement
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var manager: RemoteConfigManager

    private val manifestSlot = slot<RemoteConfiguration.Manifest>()
    private val onSuccessSlot = slot<(RCContainer?, VerificationResult) -> Unit>()
    private val onErrorSlot = slot<(PurchasesError) -> Unit>()

    @Before
    fun setup() {
        backend = mockk()
        diskCache = mockk(relaxed = true)
        manager = RemoteConfigManager(backend, diskCache)

        every {
            backend.getRemoteConfig(
                appInBackground = any(),
                manifest = capture(manifestSlot),
                onSuccess = capture(onSuccessSlot),
                onError = capture(onErrorSlot),
            )
        } just Runs
    }

    @Test
    fun `first run sends an empty app-domain manifest`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false)

        assertThat(manifestSlot.captured).isEqualTo(RemoteConfiguration.Manifest(domain = "app"))
    }

    @Test
    fun `replays the persisted manifest on subsequent runs`() {
        val persistedManifest = RemoteConfiguration.Manifest(domain = "app", topics = mapOf("sources" to "etag1"))
        every { diskCache.read() } returns PersistedRemoteConfig(persistedManifest, emptyMap())

        manager.refreshRemoteConfig(appInBackground = false)

        assertThat(manifestSlot.captured).isEqualTo(persistedManifest)
    }

    @Test
    fun `a 200 response persists the server manifest and changed topic bodies`() {
        every { diskCache.read() } returns null
        val response = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag2" } },
              "topics": { "sources": { "default": { "blob_ref": "newBlob" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccessSlot.captured.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val manifestWritten = slot<RemoteConfiguration.Manifest>()
        val topicsWritten = slot<Map<String, ConfigTopic>>()
        verify(exactly = 1) { diskCache.write(capture(manifestWritten), capture(topicsWritten)) }
        assertThat(manifestWritten.captured.topics).containsExactlyEntriesOf(mapOf("sources" to "etag2"))
        assertThat(topicsWritten.captured.getValue("sources").getValue("default").blobRef).isEqualTo("newBlob")
    }

    @Test
    fun `a 200 response merges unchanged topics and prunes topics dropped from the manifest`() {
        val previous = PersistedRemoteConfig(
            manifest = RemoteConfiguration.Manifest(
                domain = "app",
                topics = mapOf("sources" to "etag1", "product_entitlement_mapping" to "pemEtag1"),
            ),
            topics = mapOf(
                "sources" to mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "oldSources")),
                "product_entitlement_mapping" to mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "pemBlob")),
            ),
        )
        every { diskCache.read() } returns previous
        // sources changed; product_entitlement_mapping dropped from the manifest entirely.
        val response = """
            {
              "domain": "app",
              "manifest": { "domain": "app", "topics": { "sources": "etag2" } },
              "topics": { "sources": { "default": { "blob_ref": "newSources" } } }
            }
        """.trimIndent()

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccessSlot.captured.invoke(containerWithConfig(response), VerificationResult.VERIFIED)

        val topicsWritten = slot<Map<String, ConfigTopic>>()
        verify(exactly = 1) { diskCache.write(any(), capture(topicsWritten)) }
        assertThat(topicsWritten.captured.keys).containsExactly("sources")
        assertThat(topicsWritten.captured.getValue("sources").getValue("default").blobRef).isEqualTo("newSources")
    }

    @Test
    fun `a 204 response leaves the cache untouched`() {
        every { diskCache.read() } returns PersistedRemoteConfig(RemoteConfiguration.Manifest(domain = "app"), emptyMap())

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccessSlot.captured.invoke(null, VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any(), any()) }
    }

    @Test
    fun `an error leaves the cache untouched`() {
        every { diskCache.read() } returns PersistedRemoteConfig(RemoteConfiguration.Manifest(domain = "app"), emptyMap())

        manager.refreshRemoteConfig(appInBackground = false)
        onErrorSlot.captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError, "boom"))

        verify(exactly = 0) { diskCache.write(any(), any()) }
    }

    @Test
    fun `a malformed config payload leaves the cache untouched`() {
        every { diskCache.read() } returns null

        manager.refreshRemoteConfig(appInBackground = false)
        onSuccessSlot.captured.invoke(containerWithConfig("{ not valid json"), VerificationResult.VERIFIED)

        verify(exactly = 0) { diskCache.write(any(), any()) }
    }

    private fun containerWithConfig(json: String): RCContainer {
        val element = mockk<RCElement>()
        every { element.data } returns ByteBuffer.wrap(json.toByteArray())
        val container = mockk<RCContainer>()
        every { container.config } returns element
        return container
    }
}
