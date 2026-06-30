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
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigBlobFetcherTest {

    private lateinit var blobStore: RemoteConfigBlobStore
    private lateinit var urlConnectionFactory: UrlConnectionFactory
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        blobStore = mockk(relaxed = true)
        every { blobStore.contains(any()) } returns false
        urlConnectionFactory = mockk()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `returns true without downloading when the blob is already cached`() {
        every { blobStore.contains(REF_A) } returns true
        val fetcher = fetcher(provider(blobSource(TEMPLATE)))

        assertThat(download(fetcher, REF_A)).isTrue()

        verify(exactly = 0) { urlConnectionFactory.createConnection(any(), any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
    }

    @Test
    fun `downloads, verifies against the ref, and stores the blob`() {
        val bytes = "a workflow body".toByteArray()
        val ref = refOf(bytes)
        stubConnection(urlFor(ref), code = 200, body = bytes)
        val fetcher = fetcher(provider(blobSource(TEMPLATE)))

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
        val fetcher = fetcher(provider)

        assertThat(download(fetcher, ref)).isFalse()

        verify(exactly = 0) { blobStore.write(any(), any()) }
        verify(exactly = 0) { provider.reportUnhealthy(any()) }
    }

    @Test
    fun `a 404 fails the blob without condemning the source`() {
        val ref = refOf("missing".toByteArray())
        stubConnection(urlFor(ref), code = 404)
        val provider = provider(blobSource(TEMPLATE))
        val fetcher = fetcher(provider)

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
        val fetcher = fetcher(provider)

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
        val fetcher = fetcher(provider)

        // First download fails over to exhaustion; a later download must not re-arm the provider and retry.
        assertThat(download(fetcher, ref)).isFalse()
        assertThat(download(fetcher, ref)).isFalse()

        verify(exactly = 0) { provider.restart(any()) }
        verify(exactly = 0) { blobStore.write(any(), any()) }
        // Only the first download reached a source; the second saw no current source and stopped immediately.
        verify(exactly = 1) { urlConnectionFactory.createConnection(urlFor(ref), any()) }
    }

    @Test
    fun `concurrent requests for the same ref share a single download`() {
        val bytes = "shared".toByteArray()
        val ref = refOf(bytes)
        val downloadStarted = CountDownLatch(1)
        val releaseDownload = CountDownLatch(1)
        every { urlConnectionFactory.createConnection(urlFor(ref), any()) } answers {
            downloadStarted.countDown()
            releaseDownload.await(WAIT_SECONDS, TimeUnit.SECONDS)
            connection(code = 200, body = bytes)
        }
        val fetcher = fetcher(provider(blobSource(TEMPLATE)))

        val results = CopyOnWriteArrayList<Boolean>()
        val first = thread { results.add(download(fetcher, ref)) }
        check(downloadStarted.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "first download did not start" }
        // Second request arrives while the first is in flight: it must join, not start a new download.
        val second = thread { results.add(download(fetcher, ref)) }
        awaitUntil { fetcher.awaiterCount(ref) == 2 } // both callers attached to the one in-flight download

        releaseDownload.countDown()
        first.join(WAIT_MS)
        second.join(WAIT_MS)

        assertThat(results).containsExactly(true, true)
        verify(exactly = 1) { urlConnectionFactory.createConnection(urlFor(ref), any()) }
    }

    @Test
    fun `never runs more downloads at once than the configured concurrency`() {
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
        val fetcher = fetcher(provider(blobSource(TEMPLATE)), maxConcurrentDownloads = concurrency)

        // Far more work than workers; only `concurrency` may run at once.
        fetcher.prefetch((1..12).map { refOf("blob-$it".toByteArray()) })

        check(allBlocked.await(WAIT_SECONDS, TimeUnit.SECONDS)) { "workers did not all start" }
        assertThat(live.get()).isEqualTo(concurrency)
        assertThat(maxLive.get()).isEqualTo(concurrency)

        release.countDown()
    }

    @Test
    fun `an on-demand request is queued ahead of the prefetch backlog`() {
        val (started, release) = blockingConnections()
        val fetcher = fetcher(provider(blobSource(TEMPLATE)), maxConcurrentDownloads = 4)

        val busy = (1..4).map { refOf("busy-$it".toByteArray()) }
        val queued = (5..8).map { refOf("queued-$it".toByteArray()) }
        val onDemand = refOf("on-demand".toByteArray())

        fetcher.prefetch(busy)
        awaitUntil { started.size == 4 }                                 // all workers occupied
        fetcher.prefetch(queued)                                          // LOW backlog
        thread { download(fetcher, onDemand) }                            // HIGH, must jump the backlog
        awaitUntil { fetcher.awaiterCount(onDemand) == 1 }                // on-demand request has landed

        // The HIGH request sits at the head of the queue, ahead of the whole LOW backlog (which keeps its FIFO order).
        assertThat(fetcher.queuedRefsInPriorityOrder()).containsExactly(onDemand, *queued.toTypedArray())

        release.countDown()
    }

    @Test
    fun `an on-demand request boosts and joins a blob already queued for prefetch`() {
        val (started, release) = blockingConnections()
        val fetcher = fetcher(provider(blobSource(TEMPLATE)), maxConcurrentDownloads = 4)

        val busy = (1..4).map { refOf("busy-$it".toByteArray()) }
        val queued = (5..7).map { refOf("queued-$it".toByteArray()) }
        val boosted = refOf("boosted".toByteArray())

        fetcher.prefetch(busy)
        awaitUntil { started.size == 4 }
        fetcher.prefetch(queued)                                          // queued LOW first
        fetcher.prefetch(listOf(boosted))                                 // boosted enqueued LOW, behind the others
        awaitUntil { fetcher.queuedRefsInPriorityOrder().containsAll(queued + boosted) }
        // On-demand request for the already-queued ref: it must be raised to the front, joining the same download.
        thread { download(fetcher, boosted) }
        awaitUntil { fetcher.awaiterCount(boosted) == 2 }                 // prefetch + on-demand share one download

        assertThat(fetcher.queuedRefsInPriorityOrder().first()).isEqualTo(boosted)
        assertThat(fetcher.queuedRefsInPriorityOrder()).containsExactly(boosted, *queued.toTypedArray())

        release.countDown()
    }

    // region helpers

    private fun fetcher(
        provider: RemoteConfigSourceProvider,
        maxConcurrentDownloads: Int = 4,
    ) = RemoteConfigBlobFetcher(
        blobStore = blobStore,
        sourceProvider = provider,
        urlConnectionFactory = urlConnectionFactory,
        scope = scope,
        maxConcurrentDownloads = maxConcurrentDownloads,
    )

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

    /**
     * Stubs every connection to record the requested ref (in start order) then block until released, so callers can
     * keep the worker pool occupied and observe scheduling order. Returns the start log and the release latch.
     */
    private fun blockingConnections(): Pair<CopyOnWriteArrayList<String>, CountDownLatch> {
        val started = CopyOnWriteArrayList<String>()
        val release = CountDownLatch(1)
        val live = AtomicInteger(0)
        every { urlConnectionFactory.createConnection(any(), any()) } answers {
            started.add(refFromUrl(firstArg<String>()))
            // Block only while workers are saturated; later (post-release) downloads pass straight through.
            if (live.incrementAndGet() <= 4) {
                release.await(WAIT_SECONDS, TimeUnit.SECONDS)
            }
            connection(code = 200)
        }
        return started to release
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + WAIT_MS
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met within ${WAIT_MS}ms" }
            Thread.sleep(POLL_MS)
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
        private const val POLL_MS = 5L
    }
}
