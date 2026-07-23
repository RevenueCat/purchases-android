package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle.Purpose
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteConfigSourceProviderTest {

    // region Initial selection

    @Test
    fun `current api source falls back to the embedded default when the sources topic is absent`() {
        val provider = DefaultRemoteConfigSourceProvider(FakeTopicStore(null), FakeRandom())
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.revenuecat.com/")
        // Blob has no embedded default, so it stays empty.
        assertThat(provider.getCurrent(Purpose.BLOB)).isNull()
    }

    @Test
    fun `current api source falls back to the embedded default when the topic has no sources`() {
        val provider = provider(api = emptyList(), blob = emptyList())
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.revenuecat.com/")
        // Blob has no embedded default, so it stays empty.
        assertThat(provider.getCurrent(Purpose.BLOB)).isNull()
    }

    @Test
    fun `embedded api defaults fall back to backup when primary unhealthy`() {
        val provider = DefaultRemoteConfigSourceProvider(FakeTopicStore(null), FakeRandom(0))
        // The primary default is preferred (lower priority number); the backup is the next fallback.
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.revenuecat.com/")
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.rc-backup.com/")
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    @Test
    fun `current source returns lowest priority number source`() {
        val low = source("low", priority = 0, weight = 1)
        val high = source("high", priority = 10, weight = 100)
        val provider = apiProvider(listOf(low, high))

        val handle = provider.getCurrent(Purpose.API)
        assertThat(handle?.url).isEqualTo(url("low"))
        assertThat(handle?.purpose).isEqualTo(Purpose.API)
    }

    @Test
    fun `current source is stable across reads`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(provider.getCurrent(Purpose.API)?.url)
    }

    // endregion

    // region reportUnhealthy advances

    @Test
    fun `reportUnhealthy advances to next source`() {
        val high = source("high", priority = 10, weight = 1)
        val low = source("low", priority = 0, weight = 1)
        val provider = apiProvider(listOf(high, low))

        val first = provider.getCurrent(Purpose.API)
        assertThat(first?.url).isEqualTo(url("low"))

        provider.reportUnhealthy(first!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("high"))
    }

    @Test
    fun `current source is null when exhausted`() {
        val provider = apiProvider(listOf(source("only")))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    @Test
    fun `reportUnhealthy walks full fallback order`() {
        val first = source("1", priority = 10, weight = 1)
        val second = source("2", priority = 20, weight = 1)
        val third = source("3", priority = 30, weight = 1)
        val provider = apiProvider(listOf(first, second, third))

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("1"))
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("2"))
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("3"))
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    // endregion

    // region Dedup

    @Test
    fun `dedups sources by url`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 10, weight = 1),
                source("a", priority = 5, weight = 1),
                source("b", priority = 20, weight = 1),
            ),
        )

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    @Test
    fun `dedup keeps lowest priority number regardless of order`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 10, weight = 1),
                source("a", priority = 0, weight = 1),
                source("b", priority = 5, weight = 1),
            ),
        )

        // `a` is kept at priority 0 (lowest number, i.e. highest priority), so it outranks `b`
        // (priority 5) despite appearing first at 10.
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
        assertThat(provider.getCurrent(Purpose.API)?.source?.priority).isEqualTo(0)
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `dedup tie-breaks by weight for equal priority`() {
        val provider = apiProvider(
            listOf(
                source("a", priority = 0, weight = 1),
                source("a", priority = 0, weight = 100),
            ),
        )

        assertThat(provider.getCurrent(Purpose.API)?.source?.weight).isEqualTo(100)
    }

    // endregion

    // region api / blob independence

    @Test
    fun `api and blob are exposed independently`() {
        val provider = provider(api = listOf(source("api")), blob = listOf(source("blob")))

        val api = provider.getCurrent(Purpose.API)
        val blob = provider.getCurrent(Purpose.BLOB)
        assertThat(api?.url).isEqualTo(url("api"))
        assertThat(api?.purpose).isEqualTo(Purpose.API)
        assertThat(blob?.url).isEqualTo(url("blob"))
        assertThat(blob?.purpose).isEqualTo(Purpose.BLOB)
    }

    @Test
    fun `reporting api unhealthy does not affect blob`() {
        val provider = provider(
            api = listOf(source("api1", priority = 0), source("api2", priority = 10)),
            blob = listOf(source("blob1", priority = 0), source("blob2", priority = 10)),
        )

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("api2"))
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob1"))

        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("api2"))
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob2"))
    }

    // endregion

    // region Stale report handling (race conditions)

    @Test
    fun `stale report is ignored after another caller advanced`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        // Two callers grab the same current source.
        val handleA = provider.getCurrent(Purpose.API)
        val handleB = provider.getCurrent(Purpose.API)
        assertThat(handleA?.url).isEqualTo(handleB?.url)

        // Caller A reports it unhealthy: the provider advances.
        provider.reportUnhealthy(handleA!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))

        // Caller B reports the *same* (now superseded) source: this must NOT advance again.
        provider.reportUnhealthy(handleB!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `reporting same source twice advances only once`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        val handle = provider.getCurrent(Purpose.API)
        provider.reportUnhealthy(handle!!)
        provider.reportUnhealthy(handle)
        provider.reportUnhealthy(handle)

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `reporting fresh source after stale report still advances`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        val stale = provider.getCurrent(Purpose.API)
        provider.reportUnhealthy(stale!!) // a -> b
        provider.reportUnhealthy(stale) // ignored, still b

        val fresh = provider.getCurrent(Purpose.API) // b
        provider.reportUnhealthy(fresh!!) // b -> c
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("c"))
    }

    @Test
    fun `stale report on exhausted provider is ignored`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        val first = provider.getCurrent(Purpose.API)
        provider.reportUnhealthy(first!!)
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()

        // Reporting the original stale source again must not resurrect or change anything.
        provider.reportUnhealthy(first)
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    // endregion

    // region restart

    @Test
    fun `restart rewinds to first source`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("c"))

        provider.restart(Purpose.API)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `restart only rewinds requested purpose`() {
        val provider = provider(
            api = listOf(source("api1", priority = 0), source("api2", priority = 10)),
            blob = listOf(source("blob1", priority = 0), source("blob2", priority = 10)),
        )

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("api2"))
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob2"))

        provider.restart(Purpose.API)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("api1"))
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob2"))

        provider.restart(Purpose.BLOB)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob1"))
    }

    @Test
    fun `stale report from before restart is ignored`() {
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")))

        // A caller grabs `a`, then the provider is restarted before that caller reports back.
        val stale = provider.getCurrent(Purpose.API)
        assertThat(stale?.url).isEqualTo(url("a"))
        provider.restart(Purpose.API)

        // The stale report belongs to a pre-restart cycle, so it must not advance past `a`.
        provider.reportUnhealthy(stale!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))

        // A handle obtained after the restart still advances normally.
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    // endregion

    // region restartIfExhausted

    @Test
    fun `restartIfExhausted rewinds and returns true once the sources are exhausted`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)).isNull()

        assertThat(provider.restartIfExhausted(Purpose.API)).isTrue()
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `restartIfExhausted is a no-op and returns false while a healthy source is current`() {
        val provider = apiProvider(listOf(source("a"), source("b")))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, still healthy
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))

        // Not exhausted, so it must keep failover progress instead of rewinding to `a`.
        assertThat(provider.restartIfExhausted(Purpose.API)).isFalse()
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `restartIfExhausted only rewinds the requested purpose`() {
        val provider = provider(
            api = listOf(source("api1", priority = 0)),
            blob = listOf(source("blob1", priority = 0), source("blob2", priority = 10)),
        )

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // api exhausted
        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!) // blob1 -> blob2, still healthy
        assertThat(provider.getCurrent(Purpose.API)).isNull()

        assertThat(provider.restartIfExhausted(Purpose.API)).isTrue()
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("api1"))
        // Blob was not exhausted, so its progress is untouched.
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob2"))
    }

    @Test
    fun `restartIfExhausted returns true when a changed sources topic already re-armed the list`() {
        val store = FakeTopicStore(sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList()))
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b

        // A changed topic rebuilds from the top; restartIfExhausted reports the re-arm and leaves it at the top.
        store.sources = sourcesTopic(api = listOf(source("x"), source("y")), blob = emptyList())
        assertThat(provider.restartIfExhausted(Purpose.API)).isTrue()
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("x"))
    }

    // endregion

    // region API restart interval (TTL re-arm)

    @Test
    fun `advanced api list restarts from the top after the restart interval`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))

        dateProvider.advanceTime(600_000L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `advanced api list keeps its position just under the restart interval`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b

        dateProvider.advanceTime(599_999L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `exhausted api list re-arms after the restart interval`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // b -> exhausted
        assertThat(provider.getCurrent(Purpose.API)).isNull()

        dateProvider.advanceTime(599_999L)
        assertThat(provider.getCurrent(Purpose.API)).isNull()

        dateProvider.advanceTime(1L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `the restart interval counts from the first advance, not the latest`() {
        // A flapping list must not postpone the periodic return to the primary: advancing again
        // mid-interval does not reset the timer.
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b"), source("c")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, timer starts
        dateProvider.advanceTime(500_000L)
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // b -> c, timer unchanged

        dateProvider.advanceTime(100_000L) // 600s since the first advance
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `handle from before the interval restart is stale`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b
        val stale = provider.getCurrent(Purpose.API)
        assertThat(stale?.url).isEqualTo(url("b"))

        dateProvider.advanceTime(600_000L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))

        // The pre-restart handle belongs to the previous cycle, so it must not advance the list.
        provider.reportUnhealthy(stale!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `a new failover cycle after an interval restart needs a full interval again`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b
        dateProvider.advanceTime(600_000L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, new timer
        dateProvider.advanceTime(599_999L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))

        dateProvider.advanceTime(1L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    @Test
    fun `explicit restart resets the interval timer`() {
        val dateProvider = FakeDateProvider()
        val provider = apiProvider(listOf(source("a"), source("b")), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, timer starts
        dateProvider.advanceTime(500_000L)
        provider.restart(Purpose.API) // timer cleared

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, fresh timer
        dateProvider.advanceTime(599_999L) // over 600s since the ORIGINAL advance, under since the fresh one
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `a changed sources topic resets the interval timer`() {
        val dateProvider = FakeDateProvider()
        val store = FakeTopicStore(sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList()))
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0), dateProvider)

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b, timer starts
        dateProvider.advanceTime(500_000L)

        store.sources = sourcesTopic(api = listOf(source("x"), source("y")), blob = emptyList())
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // x -> y, fresh timer

        dateProvider.advanceTime(599_999L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("y"))

        dateProvider.advanceTime(1L)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("x"))
    }

    @Test
    fun `blob progress is unaffected by elapsed time`() {
        val dateProvider = FakeDateProvider()
        val provider = provider(
            api = listOf(source("api1")),
            blob = listOf(source("blob1", priority = 0), source("blob2", priority = 10)),
            dateProvider = dateProvider,
        )

        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!) // blob1 -> blob2
        dateProvider.advanceTime(600_000L)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(url("blob2"))
    }

    // endregion

    // region Sources topic changes

    @Test
    fun `changed sources topic rebuilds and restarts from the top`() {
        val store = FakeTopicStore(sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList()))
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))

        // The sources topic changes: the provider rebuilds and starts the new list from the top.
        store.sources = sourcesTopic(api = listOf(source("x"), source("y")), blob = emptyList())
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("x"))
    }

    @Test
    fun `unchanged sources topic preserves failover progress`() {
        val store = FakeTopicStore(sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList()))
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0))

        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!) // a -> b

        // Re-providing content-equal sources must not rebuild, so the position is kept at `b`.
        store.sources = sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList())
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("b"))
    }

    @Test
    fun `stale report from before a sources change is ignored`() {
        val store = FakeTopicStore(
            sourcesTopic(api = listOf(source("a"), source("b"), source("c")), blob = emptyList()),
        )
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0))

        val stale = provider.getCurrent(Purpose.API)
        assertThat(stale?.url).isEqualTo(url("a"))

        // Sources change before the stale handle is reported back. Its report belongs to the old list, so
        // it must not advance the freshly-rebuilt one.
        store.sources = sourcesTopic(api = listOf(source("x"), source("y"), source("z")), blob = emptyList())
        provider.reportUnhealthy(stale!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("x"))

        // A handle obtained after the change still advances normally.
        provider.reportUnhealthy(provider.getCurrent(Purpose.API)!!)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("y"))
    }

    @Test
    fun `sources topic appearing after being absent replaces the embedded defaults`() {
        val store = FakeTopicStore(null)
        val provider = DefaultRemoteConfigSourceProvider(store, FakeRandom(0))

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.revenuecat.com/")

        // A sources topic shows up where there was none: the provider builds the list from the top.
        store.sources = sourcesTopic(api = listOf(source("a"), source("b")), blob = emptyList())
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("a"))
    }

    // endregion

    // region Threading

    @Test
    fun `concurrent reports of same source advance exactly once`() {
        val sources = (0 until 100).map { source("$it") }
        val provider = apiProvider(sources)

        val first = provider.getCurrent(Purpose.API)
        assertThat(first?.url).isEqualTo(url("0"))

        // Many threads report the *same* source concurrently. The first report advances to the next
        // source; every other report now refers to a superseded url and must be ignored. So no matter
        // how the threads interleave, the provider advances exactly one step.
        runConcurrently(iterations = 500) {
            provider.reportUnhealthy(first!!)
        }

        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(url("1"))
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
                    var current = provider.getCurrent(Purpose.API)
                    while (current != null) {
                        seen.add(current.url)
                        provider.reportUnhealthy(current)
                        current = provider.getCurrent(Purpose.API)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        pool.shutdown()

        assertThat(seen).isEqualTo(sources.map { it.url }.toSet())
        assertThat(provider.getCurrent(Purpose.API)).isNull()
    }

    // endregion

    // region Helpers

    private fun url(host: String): String = "https://$host.revenuecat.com"

    private fun source(host: String, priority: Int = 0, weight: Int = 0): RemoteConfigSource =
        RemoteConfigSource(url = url(host), priority = priority, weight = weight)

    private fun apiProvider(
        sources: List<RemoteConfigSource>,
        dateProvider: DateProvider = FakeDateProvider(),
    ): RemoteConfigSourceProvider =
        provider(api = sources, blob = emptyList(), dateProvider = dateProvider)

    private fun provider(
        api: List<RemoteConfigSource>,
        blob: List<RemoteConfigSource>,
        dateProvider: DateProvider = FakeDateProvider(),
    ): RemoteConfigSourceProvider =
        DefaultRemoteConfigSourceProvider(FakeTopicStore(sourcesTopic(api, blob)), FakeRandom(0), dateProvider)

    /** Builds a `sources` ConfigTopic matching the backend shape: api entries use `url`, blob use `url_format`. */
    private fun sourcesTopic(api: List<RemoteConfigSource>, blob: List<RemoteConfigSource>): ConfigTopic {
        fun item(sources: List<RemoteConfigSource>, urlKey: String) = RemoteConfiguration.ConfigItem(
            metadata = buildJsonObject {
                putJsonArray("sources") {
                    sources.forEach { s ->
                        addJsonObject {
                            put(urlKey, s.url)
                            put("priority", s.priority)
                            put("weight", s.weight)
                        }
                    }
                }
            },
        )
        return ConfigTopic(mapOf("api" to item(api, "url"), "blob" to item(blob, "url_format")))
    }

    private class FakeTopicStore(var sources: ConfigTopic?) : RemoteConfigTopicStore {
        override fun topic(topic: RemoteConfigTopic): ConfigTopic? =
            if (topic == RemoteConfigTopic.Sources) sources else null
    }

    private class FakeDateProvider(private val currentTime: AtomicLong = AtomicLong(1_000_000L)) : DateProvider {
        override val now: Date
            get() = Date(currentTime.get())

        fun advanceTime(millis: Long) {
            currentTime.addAndGet(millis)
        }
    }

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
