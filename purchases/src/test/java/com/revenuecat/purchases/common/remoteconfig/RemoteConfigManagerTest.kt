package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigManagerTest {

    private lateinit var backend: Backend
    private lateinit var topicFetcher: TopicFetcher
    private lateinit var diskCache: RemoteConfigDiskCache
    private lateinit var dateProvider: DateProvider
    private var fakeNow: Date = Date(0)

    @Before
    fun setUp() {
        backend = mockk()
        topicFetcher = mockk()
        coEvery { topicFetcher.cleanupUnreferencedTopics(any()) } returns Unit
        diskCache = mockk(relaxed = true)
        fakeNow = Date(0)
        dateProvider = mockk()
        every { dateProvider.now } answers { fakeNow }
    }

    @Test
    fun `single topic with single source delegates to fetcher and completes with null`() = runTest {
        val manager = newManager(testScheduler)
        val src = source("primary")
        val entry = topicEntry("blob-default")
        val response = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to entry)),
        )
        mockBackendSuccess(response)
        val capturedTopic = slot<Topic>()
        val capturedEntryId = slot<String>()
        val capturedEntry = slot<TopicEntry>()
        val capturedSource = slot<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(
                topic = capture(capturedTopic),
                entryId = capture(capturedEntryId),
                topicEntry = capture(capturedEntry),
                source = capture(capturedSource),
            )
        } returns TopicFetchResult.Success

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
        assertThat(capturedEntryId.captured).isEqualTo("default")
        assertThat(capturedEntry.captured).isEqualTo(entry)
        assertThat(capturedSource.captured).isEqualTo(src)
    }

    @Test
    fun `empty sources skips fetcher, completes with null, and does not cache`() = runTest {
        val manager = newManager(testScheduler)
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
        verify(exactly = 0) { diskCache.write(any()) }
        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }

        // Cache wasn't populated, so a follow-up call still hits the backend.
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `empty topics map skips fetcher, completes with null, and does not cache`() = runTest {
        val manager = newManager(testScheduler)
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
        verify(exactly = 0) { diskCache.write(any()) }
        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `topic without default entryId is skipped and not cached`() = runTest {
        val manager = newManager(testScheduler)
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
        verify(exactly = 0) { diskCache.write(any()) }
        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `selects one of the top priority sources and ignores lower priority ones`() = runTest {
        val manager = newManager(testScheduler)
        val highA = source("highA", priority = 1)
        val highB = source("highB", priority = 1)
        val lowA = source("lowA", priority = 0)
        val lowB = source("lowB", priority = 0)
        val response = response(
            blobSources = listOf(lowA, highA, lowB, highB),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val capturedSource = slot<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                entryId = any(),
                topicEntry = any(),
                source = capture(capturedSource),
            )
        } returns TopicFetchResult.Success

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(capturedSource.captured).isIn(highA, highB)
    }

    @Test
    fun `selects the highest priority source even with lower weight`() = runTest {
        val manager = newManager(testScheduler)
        val low = source("low", priority = 0, weight = 999)
        val high = source("high", priority = 5, weight = 1)
        val response = response(
            blobSources = listOf(low, high),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val capturedSource = slot<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(
                topic = any(),
                entryId = any(),
                topicEntry = any(),
                source = capture(capturedSource),
            )
        } returns TopicFetchResult.Success

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(capturedSource.captured).isEqualTo(high)
    }

    @Test
    fun `forwards fetcher error to completion`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val fetcherError = PurchasesError(PurchasesErrorCode.NetworkError, "fetcher failed")
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.InvalidatingFailure(fetcherError)

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
        val manager = newManager(testScheduler)
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
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

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
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        manager.updateRemoteConfigIfNeeded(appInBackground = false)
        testScheduler.advanceUntilIdle()
        // Reaching here means no exception was thrown.
    }

    @Test
    fun `refresh runs even when completion is null`() = runTest {
        val manager = newManager(testScheduler)
        val src = source("primary")
        val entry = topicEntry("blob-default")
        val response = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to entry)),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        manager.updateRemoteConfigIfNeeded(appInBackground = false, completion = null)
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
        coVerify(exactly = 1) {
            topicFetcher.fetchTopicIfNeeded(
                topic = Topic.PRODUCT_ENTITLEMENT_MAPPING,
                entryId = "default",
                topicEntry = entry,
                source = src,
            )
        }
    }

    @Test
    fun `triggers cleanup with all entryId blob refs after successful download`() = runTest {
        val manager = newManager(testScheduler)
        val defaultEntry = topicEntry("blob-default")
        val experimentEntry = topicEntry("blob-experiment")
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf(
                    "default" to defaultEntry,
                    "EXPERIMENT_A" to experimentEntry,
                ),
            ),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success
        val capturedReferenced = slot<Map<Topic, Set<String>>>()
        coEvery { topicFetcher.cleanupUnreferencedTopics(capture(capturedReferenced)) } returns Unit

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(capturedReferenced.captured).isEqualTo(
            mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to setOf("blob-default", "blob-experiment")),
        )
    }

    @Test
    fun `does not trigger cleanup when a fetcher download fails`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val fetcherError = PurchasesError(PurchasesErrorCode.NetworkError, "fetcher failed")
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.InvalidatingFailure(fetcherError)

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }
    }

    @Test
    fun `does not trigger cleanup when backend errors`() = runTest {
        val manager = newManager(testScheduler)
        val backendError = PurchasesError(PurchasesErrorCode.NetworkError, "backend down")
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke(backendError) }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }
    }

    @Test
    fun `does not trigger cleanup when manifest has no topics`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = emptyMap(),
        )
        mockBackendSuccess(response)

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { topicFetcher.cleanupUnreferencedTopics(any()) }
    }

    @Test
    fun `successful refresh writes response to disk cache`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) { diskCache.write(response) }
    }

    @Test
    fun `topic fetch error skips disk write and leaves cache unpopulated`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.InvalidatingFailure(
            PurchasesError(PurchasesErrorCode.NetworkError, "boom"),
        )

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { diskCache.write(any()) }

        // Subsequent call should hit backend again because cache was never populated.
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `backend error skips disk write and leaves cache unpopulated`() = runTest {
        val manager = newManager(testScheduler)
        val backendError = PurchasesError(PurchasesErrorCode.NetworkError, "backend down")
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke(backendError) }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { diskCache.write(any()) }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `second call within foreground TTL is skipped without hitting backend`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Advance 4 minutes — within 5-minute foreground TTL.
        fakeNow = Date(4.minutes.inWholeMilliseconds)
        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) { diskCache.write(any()) }
    }

    @Test
    fun `call after foreground TTL expiry refetches and updates cache`() = runTest {
        val manager = newManager(testScheduler)
        val first = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob-1"))),
        )
        val second = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob-2"))),
        )
        val onSuccessSlot = slot<(RemoteConfigResponse) -> Unit>()
        val responses = ArrayDeque(listOf(first, second))
        every {
            backend.getRemoteConfig(any(), capture(onSuccessSlot), any())
        } answers { onSuccessSlot.captured.invoke(responses.removeFirst()) }
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Advance just past 5-minute foreground TTL.
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) { diskCache.write(first) }
        verify(exactly = 1) { diskCache.write(second) }
    }

    @Test
    fun `30 minutes after foreground populate, foregrounded call refetches but backgrounded call is skipped`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // 30 minutes later: stale under 5 min foreground TTL, fresh under 25 hr background TTL.
        fakeNow = Date(30.minutes.inWholeMilliseconds)

        manager.updateRemoteConfigIfNeeded(appInBackground = true) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = any(), onSuccess = any(), onError = any())
        }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = any(), onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `call past background TTL refetches even when backgrounded`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = true) {}
        testScheduler.advanceUntilIdle()

        // Just past 25-hour background TTL.
        fakeNow = Date(26.hours.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = true) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = true, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `identical responses across TTL gap only write disk once`() = runTest {
        val manager = newManager(testScheduler)
        val response = response(
            blobSources = listOf(source("primary")),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) { diskCache.write(response) }
    }

    @Test
    fun `consecutive stale calls each issue a backend request`() = runTest {
        val manager = newManager(testScheduler)
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onErrorSlot))
        } answers {
            onErrorSlot.captured.invoke(PurchasesError(PurchasesErrorCode.NetworkError, "boom"))
        }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 2) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `falls back to a same-priority alternate when first source returns InvalidatingFailure`() = runTest {
        val manager = newManager(testScheduler)
        val first = source("first", priority = 5, weight = 50)
        val second = source("second", priority = 5, weight = 50)
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            val src = arg<BlobSource>(3)
            attempts += src
            if (attempts.size == 1) {
                TopicFetchResult.InvalidatingFailure(
                    PurchasesError(PurchasesErrorCode.NetworkError, "first-failed"),
                )
            } else {
                TopicFetchResult.Success
            }
        }

        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionError).isNull()
        assertThat(attempts).hasSize(2)
        assertThat(attempts[0]).isNotEqualTo(attempts[1])
    }

    @Test
    fun `falls back to a lower priority source only after top tier is exhausted`() = runTest {
        val manager = newManager(testScheduler)
        val high = source("high", priority = 5, weight = 100)
        val low = source("low", priority = 1, weight = 100)
        val response = response(
            blobSources = listOf(high, low),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            val src = arg<BlobSource>(3)
            attempts += src
            if (src.id == "high") {
                TopicFetchResult.InvalidatingFailure(
                    PurchasesError(PurchasesErrorCode.NetworkError, "high-failed"),
                )
            } else {
                TopicFetchResult.Success
            }
        }

        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionError).isNull()
        assertThat(attempts).containsExactly(high, low)
    }

    @Test
    fun `returns the most recent error after every source is exhausted`() = runTest {
        val manager = newManager(testScheduler)
        val first = source("first", priority = 5, weight = 50)
        val second = source("second", priority = 5, weight = 50)
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val attempts = mutableListOf<BlobSource>()
        val errorsBySource = mapOf(
            "first" to PurchasesError(PurchasesErrorCode.NetworkError, "first-fail"),
            "second" to PurchasesError(PurchasesErrorCode.NetworkError, "second-fail"),
        )
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            val src = arg<BlobSource>(3)
            attempts += src
            TopicFetchResult.InvalidatingFailure(errorsBySource.getValue(src.id))
        }

        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(attempts).hasSize(2)
        assertThat(completionError).isSameAs(errorsBySource.getValue(attempts.last().id))
        verify(exactly = 0) { diskCache.write(any()) }
    }

    @Test
    fun `cached source is preferred on the next refresh after a successful download`() = runTest {
        val manager = newManager(testScheduler, random = Random(42L))
        val first = source("first", priority = 5, weight = 50)
        val second = source("second", priority = 5, weight = 50)
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        val firstAttempt = attempts.single()

        // Past TTL: second refresh.
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).hasSize(2)
        assertThat(attempts[1]).isEqualTo(firstAttempt)
    }

    @Test
    fun `cached source missing from new response is ignored`() = runTest {
        val manager = newManager(testScheduler)
        val original = source("first", priority = 5, weight = 100)
        val replacement = source("replacement", priority = 5, weight = 100)
        val firstResponse = response(
            blobSources = listOf(original),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        val secondResponse = response(
            blobSources = listOf(replacement),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob-2"))),
        )
        val onSuccess = slot<(RemoteConfigResponse) -> Unit>()
        val responses = ArrayDeque(listOf(firstResponse, secondResponse))
        every {
            backend.getRemoteConfig(any(), capture(onSuccess), any())
        } answers { onSuccess.captured.invoke(responses.removeFirst()) }
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).containsExactly(original, replacement)
    }

    @Test
    fun `TransientFailure on cached source preserves the cached source id`() = runTest {
        val manager = newManager(testScheduler)
        val src = source("primary", priority = 5, weight = 100)
        val response = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        // First refresh: success, populates cachedSourceId.
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.Success
        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Second refresh: cached source fails transiently and no other source exists.
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.TransientFailure(
            PurchasesError(PurchasesErrorCode.NetworkError, "no network"),
        )
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        var secondError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error -> secondError = error }
        testScheduler.advanceUntilIdle()
        assertThat(secondError).isNotNull

        // Third refresh: cached source should still be tried first, success again.
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }
        fakeNow = Date(12.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).containsExactly(src)
    }

    @Test
    fun `InvalidatingFailure on cached source clears cachedSourceId so next refresh re-rolls`() = runTest {
        // QueuedRandom sequence:
        //   [0]  refresh 1 picks src    (nextInt(100)=0 -> src wins via cumulative<50 check)
        //   [1]  refresh 2 picks alternate from the remaining tier after src fails (only candidate)
        //   [99] refresh 3 picks alternate via re-rolled selectWeighted (nextInt(100)=99 -> alternate)
        //
        // If cachedSourceId were not cleared after refresh 2's InvalidatingFailure, refresh 3 would
        // pin to "src" and consume no random — the final assertion would see src instead of alternate.
        val random = QueuedRandom(listOf(0, 0, 99))
        val manager = newManager(testScheduler, random = random)
        val src = source("src", priority = 5, weight = 50)
        val alternate = source("alternate", priority = 5, weight = 50)
        val response = response(
            blobSources = listOf(src, alternate),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)

        // Refresh 1: src wins the weighted pick and succeeds.
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            if (arg<BlobSource>(3).id == "src") {
                TopicFetchResult.Success
            } else {
                TopicFetchResult.InvalidatingFailure(
                    PurchasesError(PurchasesErrorCode.NetworkError, "unexpected alt call"),
                )
            }
        }
        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Refresh 2: cached source src is tried first (pinned), then alternate — both fail with
        // InvalidatingFailure so the overall refresh fails and clearCachedSource fires.
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } returns TopicFetchResult.InvalidatingFailure(
            PurchasesError(PurchasesErrorCode.NetworkError, "both-failed"),
        )
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Refresh 3: both succeed. With cachedSourceId cleared the picker rolls selectWeighted and
        // the queued 99 lands in alternate's bucket. A surviving pin would pick src.
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }
        fakeNow = Date(12.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).containsExactly(alternate)
    }

    @Test
    fun `new response from backend clears cachedSourceId so picker restarts from top priority`() = runTest {
        val manager = newManager(testScheduler)
        val src = source("src", priority = 1, weight = 100)
        val top = source("top", priority = 5, weight = 100)
        val firstResponse = response(
            blobSources = listOf(src),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        val secondResponse = response(
            blobSources = listOf(src, top),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob-2"))),
        )
        val onSuccess = slot<(RemoteConfigResponse) -> Unit>()
        val responses = ArrayDeque(listOf(firstResponse, secondResponse))
        every {
            backend.getRemoteConfig(any(), capture(onSuccess), any())
        } answers { onSuccess.captured.invoke(responses.removeFirst()) }
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Refresh 1 pins src (only candidate). The second response is different, so the pin is
        // cleared and selectWeighted runs against the new top-priority tier — picking top, not src.
        assertThat(attempts).containsExactly(src, top)
    }

    @Test
    fun `fallback success updates cachedSourceId to the winning source`() = runTest {
        val manager = newManager(testScheduler)
        val first = source("first", priority = 5, weight = 50)
        val second = source("second", priority = 5, weight = 50)
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to topicEntry("blob"))),
        )
        mockBackendSuccess(response)
        // First refresh: "first" wins on the initial weighted pick (with seed 0L it picks first).
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            val src = arg<BlobSource>(3)
            if (src.id == "first") {
                TopicFetchResult.InvalidatingFailure(
                    PurchasesError(PurchasesErrorCode.NetworkError, "first failed"),
                )
            } else {
                TopicFetchResult.Success
            }
        }
        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Second refresh: cached source should now be "second"; it should be tried first.
        val attempts = mutableListOf<BlobSource>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            attempts += arg<BlobSource>(3)
            TopicFetchResult.Success
        }
        fakeNow = Date(6.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).containsExactly(second)
    }

    // Only one Topic enum value (PRODUCT_ENTITLEMENT_MAPPING) currently exists, so a refresh always
    // produces at most one TopicTask. The broader "topics that succeed on one source aren't
    // re-requested on the next source" property isn't testable through updateRemoteConfigIfNeeded
    // until another Topic value is added; this test covers the single-topic shape only.
    @Test
    fun `failing topic is retried at most once per source during fallback`() = runTest {
        val manager = newManager(testScheduler)
        val first = source("first", priority = 5, weight = 50)
        val second = source("second", priority = 5, weight = 50)
        val entryA = topicEntry("blob-a".padEnd(64, '0'))
        val response = response(
            blobSources = listOf(first, second),
            topics = mapOf(
                Topic.PRODUCT_ENTITLEMENT_MAPPING to mapOf("default" to entryA),
            ),
        )
        mockBackendSuccess(response)
        val attempts = mutableListOf<Pair<BlobSource, TopicEntry>>()
        coEvery {
            topicFetcher.fetchTopicIfNeeded(any(), any(), any(), any())
        } answers {
            val src = arg<BlobSource>(3)
            val entry = arg<TopicEntry>(2)
            attempts += src to entry
            if (src.id == "first") {
                TopicFetchResult.InvalidatingFailure(
                    PurchasesError(PurchasesErrorCode.NetworkError, "first failed"),
                )
            } else {
                TopicFetchResult.Success
            }
        }

        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        assertThat(attempts).containsExactly(first to entryA, second to entryA)
    }

    private fun newManager(
        scheduler: TestCoroutineScheduler,
        random: Random = Random(0L),
    ) = RemoteConfigManager(
        backend = backend,
        topicFetcher = topicFetcher,
        diskCache = diskCache,
        dateProvider = dateProvider,
        random = random,
        dispatcher = UnconfinedTestDispatcher(scheduler),
    )

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

    private fun source(
        id: String,
        priority: Int = 0,
        weight: Int = 100,
    ) = BlobSource(
        id = id,
        urlFormat = "https://assets.example/{blob_ref}",
        priority = priority,
        weight = weight,
    )

    private fun topicEntry(blobRef: String) = TopicEntry(blobRef = blobRef)

    private class QueuedRandom(values: List<Int>) : Random() {
        private val queue = ArrayDeque(values)
        override fun nextBits(bitCount: Int): Int = error("nextBits should not be used")
        override fun nextInt(until: Int): Int = queue.removeFirst()
    }
}
