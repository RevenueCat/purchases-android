package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigSourceProviderTest {

    // region Initial selection

    @Test
    fun `current sources are null when there are no sources`() {
        val provider = RemoteConfigSourceProvider(
            RemoteConfigSources(api = emptyList(), blob = emptyList()),
            FakeRandom(),
        )
        assertThat(provider.currentApiSource).isNull()
        assertThat(provider.currentBlobSource).isNull()
    }

    @Test
    fun `current source returns highest priority source`() {
        val low = source("low", priority = 0, weight = 100)
        val high = source("high", priority = 10, weight = 1)
        val provider = apiProvider(listOf(low, high))

        val handle = provider.currentApiSource
        assertThat(handle?.url).isEqualTo(url("high"))
        assertThat(handle?.purpose).isEqualTo(RemoteConfigSourceHandle.Purpose.API)
    }

    @Test
    fun `current source is stable across reads`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        assertThat(provider.currentApiSource?.url).isEqualTo(provider.currentApiSource?.url)
    }

    // endregion

    // region reportUnhealthy advances

    @Test
    fun `reportUnhealthy advances to next source`() {
        val high = source("high", priority = 10, weight = 1)
        val low = source("low", priority = 0, weight = 1)
        val provider = apiProvider(listOf(high, low))

        val first = provider.currentApiSource
        assertThat(first?.url).isEqualTo(url("high"))

        provider.reportUnhealthy(first!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("low"))
    }

    @Test
    fun `current source is null when exhausted`() {
        val provider = apiProvider(listOf(source("only")))

        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource).isNull()
    }

    @Test
    fun `reportUnhealthy walks full fallback order`() {
        val first = source("1", priority = 30, weight = 1)
        val second = source("2", priority = 20, weight = 1)
        val third = source("3", priority = 10, weight = 1)
        val provider = apiProvider(listOf(first, second, third))

        assertThat(provider.currentApiSource?.url).isEqualTo(url("1"))
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("2"))
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("3"))
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource).isNull()
    }

    // endregion

    // region Dedup

    @Test
    fun `dedups sources by url`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 10, weight = 1),
                source("a", priority = 5, weight = 1),
                source("b", priority = 0, weight = 1),
            ),
        )

        assertThat(provider.currentApiSource?.url).isEqualTo(url("a"))
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("b"))
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource).isNull()
    }

    @Test
    fun `dedup keeps highest priority regardless of order`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 0, weight = 1),
                source("a", priority = 10, weight = 1),
                source("b", priority = 5, weight = 1),
            ),
        )

        // `a` is kept at priority 10, so it outranks `b` (priority 5) despite appearing first at 0.
        assertThat(provider.currentApiSource?.url).isEqualTo(url("a"))
        assertThat(provider.currentApiSource?.priority).isEqualTo(10)
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("b"))
    }

    @Test
    fun `dedup tie-breaks by weight for equal priority`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 0, weight = 1),
                source("a", priority = 0, weight = 100),
            ),
        )

        assertThat(provider.currentApiSource?.weight).isEqualTo(100)
    }

    // endregion

    // region api / blob independence

    @Test
    fun `api and blob are exposed independently`() {
        val provider = RemoteConfigSourceProvider(
            RemoteConfigSources(api = listOf(source("api")), blob = listOf(source("blob"))),
            FakeRandom(0),
        )

        val api = provider.currentApiSource
        val blob = provider.currentBlobSource
        assertThat(api?.url).isEqualTo(url("api"))
        assertThat(api?.purpose).isEqualTo(RemoteConfigSourceHandle.Purpose.API)
        assertThat(blob?.url).isEqualTo(url("blob"))
        assertThat(blob?.purpose).isEqualTo(RemoteConfigSourceHandle.Purpose.BLOB)
    }

    @Test
    fun `reporting api unhealthy does not affect blob`() {
        val provider = RemoteConfigSourceProvider(
            RemoteConfigSources(
                api = listOf(source("api1", priority = 10), source("api2", priority = 0)),
                blob = listOf(source("blob1", priority = 10), source("blob2", priority = 0)),
            ),
            FakeRandom(0),
        )

        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("api2"))
        assertThat(provider.currentBlobSource?.url).isEqualTo(url("blob1"))

        provider.reportUnhealthy(provider.currentBlobSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("api2"))
        assertThat(provider.currentBlobSource?.url).isEqualTo(url("blob2"))
    }

    // endregion

    // region Stale report handling (race conditions)

    @Test
    fun `stale report is ignored after another caller advanced`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        // Two callers grab the same current source.
        val handleA = provider.currentApiSource
        val handleB = provider.currentApiSource
        assertThat(handleA?.url).isEqualTo(handleB?.url)

        // Caller A reports it unhealthy: the provider advances.
        provider.reportUnhealthy(handleA!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("b"))

        // Caller B reports the *same* (now superseded) source: this must NOT advance again.
        provider.reportUnhealthy(handleB!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("b"))
    }

    @Test
    fun `reporting same source twice advances only once`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        val handle = provider.currentApiSource
        provider.reportUnhealthy(handle!!)
        provider.reportUnhealthy(handle)
        provider.reportUnhealthy(handle)

        assertThat(provider.currentApiSource?.url).isEqualTo(url("b"))
    }

    @Test
    fun `reporting fresh source after stale report still advances`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        val stale = provider.currentApiSource
        provider.reportUnhealthy(stale!!) // a -> b
        provider.reportUnhealthy(stale) // ignored, still b

        val fresh = provider.currentApiSource // b
        provider.reportUnhealthy(fresh!!) // b -> c
        assertThat(provider.currentApiSource?.url).isEqualTo(url("c"))
    }

    @Test
    fun `stale report on exhausted provider is ignored`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        val first = provider.currentApiSource
        provider.reportUnhealthy(first!!)
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource).isNull()

        // Reporting the original stale source again must not resurrect or change anything.
        provider.reportUnhealthy(first)
        assertThat(provider.currentApiSource).isNull()
    }

    // endregion

    // region restart

    @Test
    fun `restart rewinds to first source`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        provider.reportUnhealthy(provider.currentApiSource!!)
        provider.reportUnhealthy(provider.currentApiSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("c"))

        provider.restart(RemoteConfigSourceHandle.Purpose.API)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("a"))
    }

    @Test
    fun `restart only rewinds requested purpose`() {
        val provider = RemoteConfigSourceProvider(
            RemoteConfigSources(
                api = listOf(source("api1", priority = 10), source("api2", priority = 0)),
                blob = listOf(source("blob1", priority = 10), source("blob2", priority = 0)),
            ),
            FakeRandom(0),
        )

        provider.reportUnhealthy(provider.currentApiSource!!)
        provider.reportUnhealthy(provider.currentBlobSource!!)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("api2"))
        assertThat(provider.currentBlobSource?.url).isEqualTo(url("blob2"))

        provider.restart(RemoteConfigSourceHandle.Purpose.API)
        assertThat(provider.currentApiSource?.url).isEqualTo(url("api1"))
        assertThat(provider.currentBlobSource?.url).isEqualTo(url("blob2"))

        provider.restart(RemoteConfigSourceHandle.Purpose.BLOB)
        assertThat(provider.currentBlobSource?.url).isEqualTo(url("blob1"))
    }

    // endregion

    // region Threading

    @Test
    fun `concurrent reports of same source advance exactly once`() {
        val sources = (0 until 100).map { source("$it") }
        val provider = apiProvider(sources)

        val first = provider.currentApiSource
        assertThat(first?.url).isEqualTo(url("0"))

        // Many threads report the *same* source concurrently. The first report advances to the next
        // source; every other report now refers to a superseded url and must be ignored. So no matter
        // how the threads interleave, the provider advances exactly one step.
        runConcurrently(iterations = 500) {
            provider.reportUnhealthy(first!!)
        }

        assertThat(provider.currentApiSource?.url).isEqualTo(url("1"))
    }

    @Test
    fun `concurrent reports never skip sources when serialized`() {
        // Drive the provider to exhaustion by always reporting the *current* source. Collect every
        // distinct url handed out; because stale reports are ignored, no source may be skipped.
        val sources = (0 until 50).map { source("$it") }
        val provider = apiProvider(sources)

        val seen = ConcurrentHashMap.newKeySet<String>()
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(8)
        repeat(8) {
            pool.execute {
                try {
                    var current = provider.currentApiSource
                    while (current != null) {
                        seen.add(current.url)
                        provider.reportUnhealthy(current)
                        current = provider.currentApiSource
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        pool.shutdown()

        assertThat(seen).isEqualTo(sources.map { it.url }.toSet())
        assertThat(provider.currentApiSource).isNull()
    }

    // endregion

    // region Helpers

    private fun url(host: String): String = "https://$host.revenuecat.com"

    private fun source(host: String, priority: Int = 0, weight: Int = 0): RemoteConfigSource =
        RemoteConfigSource(url = url(host), priority = priority, weight = weight)

    private fun apiProvider(sources: List<RemoteConfigSource>): RemoteConfigSourceProvider =
        RemoteConfigSourceProvider(RemoteConfigSources(api = sources, blob = emptyList()), FakeRandom(0))

    private fun runConcurrently(iterations: Int, block: () -> Unit) {
        val pool = Executors.newFixedThreadPool(16)
        val start = CountDownLatch(1)
        val done = CountDownLatch(iterations)
        repeat(iterations) {
            pool.execute {
                try {
                    start.await()
                    block()
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        done.await(10, TimeUnit.SECONDS)
        pool.shutdown()
    }

    /**
     * Returns queued values from [nextInt], clamped into range, repeating the last value once the
     * queue is drained. Mirrors the iOS `FakeRandomizer` test helper.
     */
    private class FakeRandom(vararg values: Int) : Random() {
        private val values: IntArray = if (values.isEmpty()) intArrayOf(0) else values
        private var index = 0

        override fun nextBits(bitCount: Int): Int = error("nextBits should not be used")

        override fun nextInt(until: Int): Int {
            val value = if (index < values.size) values[index] else values.last()
            index++
            return value.coerceIn(0, until - 1)
        }
    }

    // endregion
}
