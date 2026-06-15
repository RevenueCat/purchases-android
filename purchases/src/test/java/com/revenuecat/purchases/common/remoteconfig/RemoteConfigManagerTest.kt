package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.networking.RCContainer
import io.mockk.coEvery
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
import kotlin.time.Duration.Companion.minutes

// WIP: The RC Container Format migration disabled the RemoteConfigResponse-based processing
// (weighted source selection, topic fetching, disk caching). Those tests will return alongside the
// processing in follow-up PRs. For now this covers the manager's still-active control flow: fetching
// the container, caching it (TTL skip), forwarding the background flag, and propagating backend errors.
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
    fun `successful refresh completes with null`() = runTest {
        val manager = newManager(testScheduler)
        mockBackendSuccess(mockk())

        var completionInvoked = false
        var completionError: PurchasesError? = PurchasesError(PurchasesErrorCode.UnknownError)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionInvoked = true
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionInvoked).isTrue
        assertThat(completionError).isNull()
    }

    @Test
    fun `backend error forwards to completion`() = runTest {
        val manager = newManager(testScheduler)
        val backendError = PurchasesError(PurchasesErrorCode.NetworkError, "backend down")
        val onErrorSlot = slot<(PurchasesError) -> Unit>()
        every {
            backend.getRemoteConfig(any(), any(), capture(onErrorSlot))
        } answers { onErrorSlot.captured.invoke(backendError) }

        var completionError: PurchasesError? = null
        manager.updateRemoteConfigIfNeeded(appInBackground = false) { error ->
            completionError = error
        }
        testScheduler.advanceUntilIdle()

        assertThat(completionError).isSameAs(backendError)
    }

    @Test
    fun `forwards background flag to backend`() = runTest {
        val manager = newManager(testScheduler)
        mockBackendSuccess(mockk())

        manager.updateRemoteConfigIfNeeded(appInBackground = true) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = true, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `null completion does not crash on success`() = runTest {
        val manager = newManager(testScheduler)
        mockBackendSuccess(mockk())

        manager.updateRemoteConfigIfNeeded(appInBackground = false)
        testScheduler.advanceUntilIdle()
        // Reaching here means no exception was thrown.
    }

    @Test
    fun `second call within foreground TTL is skipped without hitting backend`() = runTest {
        val manager = newManager(testScheduler)
        mockBackendSuccess(mockk())

        fakeNow = Date(0)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        // Advance 4 minutes — within 5-minute foreground TTL.
        fakeNow = Date(4.minutes.inWholeMilliseconds)
        manager.updateRemoteConfigIfNeeded(appInBackground = false) {}
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            backend.getRemoteConfig(appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `call after foreground TTL expiry refetches`() = runTest {
        val manager = newManager(testScheduler)
        mockBackendSuccess(mockk())

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

    private fun mockBackendSuccess(container: RCContainer) {
        val onSuccess = slot<(RCContainer) -> Unit>()
        every {
            backend.getRemoteConfig(any(), capture(onSuccess), any())
        } answers { onSuccess.captured.invoke(container) }
    }
}
