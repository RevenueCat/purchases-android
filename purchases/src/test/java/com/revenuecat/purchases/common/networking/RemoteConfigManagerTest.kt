package com.revenuecat.purchases.common.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var topicFetcher: TopicFetcher
    private lateinit var manager: RemoteConfigManager

    @Before
    fun setUp() {
        backend = mockk()
        topicFetcher = mockk()
        manager = RemoteConfigManager(backend, topicFetcher)
    }

    @Test
    fun `single topic with single asset source delegates to fetcher and completes with null`() {
        val source = assetSource("primary")
        val entry = topicEntry("blob-default")
        val response = response(
            assetSources = listOf(source),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to entry)),
        )
        mockBackendSuccess(response)
        val capturedTopic = slot<Topic>()
        val capturedVariant = slot<String>()
        val capturedEntry = slot<TopicEntry>()
        val capturedAssetSource = slot<AssetSource>()
        val capturedCompletion = slot<(PurchasesError?) -> Unit>()
        every {
            topicFetcher.fetchTopicIfNeeded(
                topic = capture(capturedTopic),
                variant = capture(capturedVariant),
                topicEntry = capture(capturedEntry),
                assetSource = capture(capturedAssetSource),
                completion = capture(capturedCompletion),
            )
        } answers { capturedCompletion.captured.invoke(null) }

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        assertThat(capturedTopic.captured).isEqualTo(Topic.PRODUCT_ENTITLEMENT_MAPPING)
        assertThat(capturedVariant.captured).isEqualTo("DEFAULT")
        assertThat(capturedEntry.captured).isEqualTo(entry)
        assertThat(capturedAssetSource.captured).isEqualTo(source)
    }

    @Test
    fun `empty asset_sources skips fetcher and completes with null`() {
        val response = response(
            assetSources = emptyList(),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)

        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        var completionInvoked = false
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        verify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `empty topics map skips fetcher and completes with null`() {
        val response = response(
            assetSources = listOf(assetSource("primary")),
            topics = emptyMap(),
        )
        mockBackendSuccess(response)

        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        verify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `topic without DEFAULT variant is skipped`() {
        val response = response(
            assetSources = listOf(assetSource("primary")),
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("EXPERIMENT_A" to topicEntry("blob")),
            ),
        )
        mockBackendSuccess(response)

        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        verify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `selects the first asset source when multiple are available`() {
        val first = assetSource("first")
        val second = assetSource("second")
        val response = response(
            assetSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val capturedAssetSource = slot<AssetSource>()
        val capturedCompletion = slot<(PurchasesError?) -> Unit>()
        every {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                variant = any(),
                topicEntry = any(),
                assetSource = capture(capturedAssetSource),
                completion = capture(capturedCompletion),
            )
        } answers { capturedCompletion.captured.invoke(null) }

        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) {}

        assertThat(capturedAssetSource.captured).isEqualTo(first)
    }

    @Test
    fun `forwards fetcher error to completion`() {
        val response = response(
            assetSources = listOf(assetSource("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val fetcherError = PurchasesError(PurchasesErrorCode.NetworkError, "fetcher failed")
        val capturedCompletion = slot<(PurchasesError?) -> Unit>()
        every {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                variant = any(),
                topicEntry = any(),
                assetSource = any(),
                completion = capture(capturedCompletion),
            )
        } answers { capturedCompletion.captured.invoke(fetcherError) }

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isSameAs(fetcherError)
    }

    @Test
    fun `backend error short-circuits and never invokes fetcher`() {
        val backendError = PurchasesError(PurchasesErrorCode.NetworkError, "backend down")
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke(backendError) }

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isSameAs(backendError)
        verify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `forwards background flag to backend`() {
        val response = response(
            assetSources = listOf(assetSource("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        every {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any(), any())
        } just Runs

        manager.updateRemoteConfigIfNeeded(appUserID = "user-1", appInBackground = true) {}

        verify(exactly = 1) {
            backend.getRemoteConfig(
                appUserID = "user-1",
                appInBackground = true,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `null completion does not crash on success`() {
        val response = response(
            assetSources = listOf(assetSource("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("DEFAULT" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val capturedCompletion = slot<(PurchasesError?) -> Unit>()
        every {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                variant = any(),
                topicEntry = any(),
                assetSource = any(),
                completion = capture(capturedCompletion),
            )
        } answers { capturedCompletion.captured.invoke(null) }

        manager.updateRemoteConfigIfNeeded(appUserID = "user", appInBackground = false)
        // Reaching here means no exception was thrown.
    }

    private fun mockBackendSuccess(response: RemoteConfigResponse) {
        val onSuccess = slot<(RemoteConfigResponse) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onSuccess), any())
        } answers { onSuccess.captured.invoke(response) }
    }

    private fun response(
        assetSources: List<AssetSource>,
        topics: Map<Topic, Map<String, TopicEntry>>,
    ): RemoteConfigResponse = RemoteConfigResponse(
        configVersion = "v1",
        apiSources = emptyList(),
        assetSources = assetSources,
        manifest = Manifest(topics = topics),
    )

    private fun assetSource(id: String) = AssetSource(
        id = id,
        urlFormat = "https://assets.example/{blob_ref}",
        priority = 0,
        weight = 100,
        blacklistTimeSeconds = 600L,
        testUrl = null,
    )

    private fun topicEntry(blobRef: String) =
        TopicEntry(assetBlobRef = blobRef, contentType = "application/json", prefetch = true)
}
