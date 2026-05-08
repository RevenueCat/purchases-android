package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var topicFetcher: TopicFetcher

    @Before
    fun setUp() {
        backend = mockk()
        topicFetcher = mockk()
    }

    @Test
    fun `single topic with single source delegates to fetcher and completes with null`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val src = source("primary")
        val entry = topicEntry("blob-default")
        val response = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to entry)),
        )
        mockBackendSuccess(response)
        val capturedTopic = slot<Topic>()
        val capturedVariant = slot<String>()
        val capturedEntry = slot<TopicEntry>()
        val capturedSource = slot<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(
                topic = capture(capturedTopic),
                variant = capture(capturedVariant),
                topicEntry = capture(capturedEntry),
                source = capture(capturedSource),
            )
        } returns null

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        assertThat(capturedTopic.captured).isEqualTo(Topic.PRODUCT_ENTITLEMENT_MAPPING)
        assertThat(capturedVariant.captured).isEqualTo("default")
        assertThat(capturedEntry.captured).isEqualTo(entry)
        assertThat(capturedSource.captured).isEqualTo(src)
    }

    @Test
    fun `empty sources skips fetcher and completes with null`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = emptyList(),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)

        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        var completionInvoked = false
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        coVerify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any()) }
    }

    @Test
    fun `empty topics map skips fetcher and completes with null`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = listOf(source("primary")),
            topics = emptyMap(),
        )
        mockBackendSuccess(response)

        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        coVerify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any()) }
    }

    @Test
    fun `topic without default variant is skipped`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("EXPERIMENT_A" to topicEntry("blob")),
            ),
        )
        mockBackendSuccess(response)

        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        coVerify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any()) }
    }

    @Test
    fun `selects the first source when multiple are available`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val first = source("first")
        val second = source("second")
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val capturedSource = slot<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                variant = any(),
                topicEntry = any(),
                source = capture(capturedSource),
            )
        } returns null

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(capturedSource.captured).isEqualTo(first)
    }

    @Test
    fun `forwards fetcher error to completion`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val fetcherError = PurchasesError(PurchasesErrorCode.NetworkError, "fetcher failed")
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns fetcherError

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isSameAs(fetcherError)
    }

    @Test
    fun `backend error short-circuits and never invokes fetcher`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val backendError = PurchasesError(PurchasesErrorCode.NetworkError, "backend down")
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke(backendError) }

        var completionInvoked = false
        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isSameAs(backendError)
        coVerify(exactly = 0) { topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any()) }
    }

    @Test
    fun `forwards background flag to backend`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns null

        manager.updateRemoteConfigIfNeeded(appInBackground = true) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            backend.getRemoteConfig(
                appInBackground = true,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `null completion does not crash on success`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns null

        manager.updateRemoteConfigIfNeeded(appInBackground = false)
        testScheduler.advanceUntilIdle()
        // Reaching here means no exception was thrown.
    }

    @Test
    fun `refresh runs even when completion is null`() = runTest {
        val manager = RemoteConfigManager(
            backend = backend,
            topicFetcher = topicFetcher,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            downloadDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        val src = source("primary")
        val entry = topicEntry("blob-default")
        val response = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to entry)),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns null

        manager.updateRemoteConfigIfNeeded(appInBackground = false, completion = null)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
        coVerify(exactly = 1) {
            topicFetcher.fetchTopicIfNeeded(
                topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
                variant = "default",
                topicEntry = entry,
                source = src,
            )
        }
    }

    private fun mockBackendSuccess(response: RemoteConfigResponse) {
        val onSuccess = slot<(RemoteConfigResponse) -> Unit>()
        every {
            backend.getRemoteConfig(any(), capture(onSuccess), any())
        } answers { onSuccess.captured.invoke(response) }
    }

    private fun response(
        blobSources: List<BlobSource>,
        topics: Map<Topic, Map<String, TopicEntry>>,
    ): RemoteConfigResponse = RemoteConfigResponse(
        blobSources = blobSources,
        manifest = Manifest(topics = topics),
    )

    private fun source(id: String) = BlobSource(
        id = id,
        urlFormat = "https://assets.example/{blob_ref}",
        priority = 0,
        weight = 100,
    )

    private fun topicEntry(blobRef: String) = TopicEntry(blobRef = blobRef)
}
