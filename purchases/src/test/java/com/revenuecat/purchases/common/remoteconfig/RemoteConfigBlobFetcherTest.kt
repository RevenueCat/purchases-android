package com.revenuecat.purchases.common.remoteconfig

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle.Purpose
import com.revenuecat.purchases.utils.UrlConnection
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigBlobFetcherTest {

    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var urlConnectionFactory: UrlConnectionFactory

    // Real multi-threaded scope for the behavioural / real-concurrency tests; cancelled in tearDown.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun setup() {
        blobStore = mockk(relaxed = true)
        every { blobStore.contains(any()) } returns false
        urlConnectionFactory = mockk()
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `returns true without downloading when the blob is already cached`() {
        every { blobStore.contains(REF_A) } returns true
        val fetcher = realFetcher(provider(blobSource(TEMPLATE)))

        assertThat(download(fetcher, REF_A)).isTrue()

        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
    }

    @Test
    fun `downloads, verifies against the ref, and stores the blob`() {
        val bytes = "a workflow body".toByteArray()
        val ref = refOf(bytes)
        stubConnection(urlFor(ref), code = 200, body = bytes)
        val fetcher = realFetcher(provider(blobSource(TEMPLATE)))

        assertThat(download(fetcher, ref)).isTrue()

        // The placeholder is substituted with the ref before the request is made.
        verify { urlConnectionFactory.createConnection(urlFor(ref), any()) }
        val written = slot<ByteBuffer>()
        verify { blobStore.write(ref, capture(written)) }
        assertThat(written.captured.toBytes()).isEqualTo(bytes)
    }

    @Test
    fun `a checksum mismatch fails the blob without condemning the source`() {
        val ref = refOf("expected".toByteArray())
        stubConnection(urlFor(ref), code = 200, body = "tampered".toByteArray())
        val provider = provider(blobSource(TEMPLATE))
        val fetcher = realFetcher(provider)

        assertThat(download(fetcher, ref)).isFalse()

        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { provider.reportUnhealthy(any()) }
    }

    @Test
    fun `a 404 fails the blob without condemning the source`() {
        val ref = refOf("missing".toByteArray())
        stubConnection(urlFor(ref), code = 404)
        val provider = provider(blobSource(TEMPLATE))
        val fetcher = realFetcher(provider)

        assertThat(download(fetcher, ref)).isFalse()

        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { provider.reportUnhealthy(any()) }
    }

    @Test
    fun `a server error fails over to the next source`() {
        val bytes = "body".toByteArray()
        val ref = refOf(bytes)
        val primary = "https://primary.example/$PLACEHOLDER"
        val secondary = "https://secondary.example/$PLACEHOLDER"
        // Higher priority is tried first.
        val provider = provider(blobSource(primary, priority = 2), blobSource(secondary, priority = 1))
        stubConnection(primary.replace(PLACEHOLDER, ref), code = 500)
        stubConnection(secondary.replace(PLACEHOLDER, ref), code = 200, body = bytes)
        val fetcher = realFetcher(provider)

        assertThat(download(fetcher, ref)).isTrue()

        verify { provider.reportUnhealthy(match { it.url == primary }) }
        verify { blobStore.write(ref, any()) }
    }

    @Test
    fun `stops and fails once the provider has exhausted its sources, without restarting`() {
        val ref = refOf("body".toByteArray())
        val provider = provider(blobSource(TEMPLATE))
        // The only source errors, so it is reported unhealthy and the provider becomes exhausted.
        stubConnection(urlFor(ref), code = 500)
        val fetcher = realFetcher(provider)

        // First download fails over to exhaustion; a later download must not re-arm the provider and retry.
        assertThat(download(fetcher, ref)).isFalse()
        assertThat(download(fetcher, ref)).isFalse()

        verify(exactly = 0) { provider.restart(any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
        // Only the first download reached a source; the second saw no current source and stopped immediately.
        verify(exactly = 1) { urlConnectionFactory.createConnection(urlFor(ref), any()) }
    }

    @Test
    fun `never runs more downloads at once than the worker pool allows`() {
        val concurrency = 4
        val live = AtomicInteger(0)
        val maxLive = AtomicInteger(0)
        val allBlocked = CountDownLatch(concurrency)
        val release = CountDownLatch(1)
        every { urlConnectionFactory.createConnection(any(), any()) } answers {
            maxLive.accumulateAndGet(live.incrementAndGet()) { a, b -> maxOf(a, b) }
            allBlocked.countDown()
            release.await(WAIT_SECONDS, TimeUnit.SECONDS)
            live.decrementAndGet()
            connection(code = 200)
        }
        val fetcher = realFetcher(provider(blobSource(TEMPLATE)))

        // Far more work than workers; only the pool size may run at once.
        fetcher.prefetch((1..12).map { refOf("blob-$it".toByteArray()) })

        check(allBlocked.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "workers did not all start" }
        assertThat(live.get()).isEqualTo(concurrency)
        assertThat(maxLive.get()).isEqualTo(concurrency)

        release.countDown()
    }

    @Test
    fun `concurrent requests for the same ref share a single download`() = runTest {
        val bytes = "shared".toByteArray()
        val ref = refOf(bytes)
        stubConnection(urlFor(ref), code = 200, body = bytes)
        val fetcherScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val fetcher = RemoteConfigBlobFetcher(blobStore, provider(blobSource(TEMPLATE)), urlConnectionFactory, fetcherScope)

        // Both requests are enqueued before any worker runs (StandardTestDispatcher), so the second joins the first.
        val first = async { fetcher.ensureDownloaded(ref) }
        val second = async { fetcher.ensureDownloaded(ref) }
        advanceUntilIdle()

        assertThat(first.await()).isTrue()
        assertThat(second.await()).isTrue()
        verify(exactly = 1) { urlConnectionFactory.createConnection(urlFor(ref), any()) }
        fetcherScope.cancel()
    }

    @Test
    fun `an on-demand request is downloaded ahead of the prefetch backlog`() = runTest {
        val started = CopyOnWriteArrayList<String>()
        recordStartedConnections(started)
        val fetcherScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val fetcher = RemoteConfigBlobFetcher(blobStore, provider(blobSource(TEMPLATE)), urlConnectionFactory, fetcherScope)

        val backlog = (1..3).map { refOf("low-$it".toByteArray()) }
        val onDemand = refOf("on-demand".toByteArray())

        fetcher.prefetch(backlog)                        // LOW backlog
        fetcherScope.launch { fetcher.ensureDownloaded(onDemand) } // HIGH
        advanceUntilIdle()

        // The HIGH request is served first, ahead of the whole LOW backlog (which keeps its FIFO order).
        assertThat(started).containsExactly(onDemand, *backlog.toTypedArray())
        fetcherScope.cancel()
    }

    @Test
    fun `an on-demand request boosts and joins a blob already queued for prefetch`() = runTest {
        val started = CopyOnWriteArrayList<String>()
        recordStartedConnections(started)
        val fetcherScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val fetcher = RemoteConfigBlobFetcher(blobStore, provider(blobSource(TEMPLATE)), urlConnectionFactory, fetcherScope)

        val others = (1..3).map { refOf("low-$it".toByteArray()) }
        val boosted = refOf("boosted".toByteArray())

        // `boosted` is enqueued LOW, last in the backlog; the on-demand request must raise it to the front.
        fetcher.prefetch(others + boosted)
        fetcherScope.launch { fetcher.ensureDownloaded(boosted) }
        advanceUntilIdle()

        assertThat(started.first()).isEqualTo(boosted)
        assertThat(started.count { it == boosted }).isEqualTo(1) // prefetch + on-demand shared one download
        assertThat(started).containsExactlyInAnyOrderElementsOf(others + boosted)
        fetcherScope.cancel()
    }

    // region helpers

    private fun realFetcher(provider: RemoteConfigSourceProvider) =
        RemoteConfigBlobFetcher(blobStore, provider, urlConnectionFactory, scope)

    /** A spy over a real provider so we get real failover behaviour and can still verify calls. */
    private fun provider(vararg blobSources: RemoteConfigSource): RemoteConfigSourceProvider =
        spyk(DefaultRemoteConfigSourceProvider(apiSources = emptyList(), blobSources = blobSources.toList()))

    private fun blobSource(url: String, priority: Int = 0, weight: Int = 1) =
        RemoteConfigSource(url = url, priority = priority, weight = weight)

    private fun download(fetcher: RemoteConfigBlobFetcher, ref: String): Boolean =
        runBlocking { withTimeout(WAIT_MS) { fetcher.ensureDownloaded(ref) } }

    private fun stubConnection(url: String, code: Int, body: ByteArray = ByteArray(0)) {
        every { urlConnectionFactory.createConnection(url, any()) } returns connection(code, body)
    }

    private fun connection(code: Int, body: ByteArray = ByteArray(0)): UrlConnection {
        val connection = mockk<UrlConnection>(relaxed = true)
        every { connection.responseCode } returns code
        every { connection.inputStream } returns ByteArrayInputStream(body)
        return connection
    }

    /** Records the requested ref of every connection (in start order) so tests can assert scheduling order. */
    private fun recordStartedConnections(started: MutableList<String>) {
        every { urlConnectionFactory.createConnection(any(), any()) } answers {
            started.add(refFromUrl(firstArg<String>()))
            connection(code = 200)
        }
    }

    private fun urlFor(ref: String) = TEMPLATE.replace(PLACEHOLDER, ref)

    private fun refFromUrl(url: String) = url.substringAfterLast('/')

    private fun refOf(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest.copyOf(REF_HASH_BYTES), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun ByteBuffer.toBytes(): ByteArray {
        val view = duplicate().apply { rewind() }
        return ByteArray(view.remaining()).also { view.get(it) }
    }

    // endregion

    private companion object {
        private const val PLACEHOLDER = "{blob_ref}"
        private const val TEMPLATE = "https://config.revenuecat-static.com/$PLACEHOLDER"
        private const val REF_HASH_BYTES = 24
        private const val REF_A = "AAAABBBBCCCCDDDDEEEEFFFFGGGGHHHH"
        private const val WAIT_SECONDS = 5L
        private const val WAIT_MS = 5_000L
    }
}
