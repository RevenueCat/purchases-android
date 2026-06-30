package com.revenuecat.purchases.common.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle.Purpose
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
class LazyRemoteConfigSourceProviderTest {

    // region Fallbacks

    @Test
    fun `with no sources topic, falls back to the hardcoded blob and api sources`() {
        val provider = provider(store("m1", sources = null))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo(DEFAULT_API_URL)
    }

    @Test
    fun `a configured blob source is tried before the hardcoded fallback`() {
        val configured = "https://cdn.example.com/{blob_ref}"
        val provider = provider(store("m1", sourcesOf("blob" to item("url" to configured))))

        val first = provider.getCurrent(Purpose.BLOB)
        assertThat(first?.url).isEqualTo(configured)

        // Only after the configured source is condemned do we fall through to the hardcoded one (lowest priority).
        provider.reportUnhealthy(first!!)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)
    }

    @Test
    fun `a sources topic with no blob source still serves the hardcoded blob fallback`() {
        val provider = provider(store("m1", sourcesOf("api" to item("url" to "https://api.example.com"))))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)
        assertThat(provider.getCurrent(Purpose.API)?.url).isEqualTo("https://api.example.com")
    }

    // endregion

    // region Parsing

    @Test
    fun `url_format is preferred over url`() {
        val item = item("url" to "https://plain.example.com/{blob_ref}", "url_format" to "https://fmt.example.com/{blob_ref}")
        val provider = provider(store("m1", sourcesOf("blob" to item)))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://fmt.example.com/{blob_ref}")
    }

    @Test
    fun `an item with no string url is skipped`() {
        val provider = provider(store("m1", sourcesOf("blob" to item("priority" to 5))))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)
    }

    @Test
    fun `an item is classified as a blob source by the blob_ref placeholder regardless of its key`() {
        // Keyed "extra" (not "blob"), but its url carries the placeholder, so it routes to the blob purpose.
        val provider = provider(store("m1", sourcesOf("extra" to item("url" to "https://cdn.example.com/{blob_ref}"))))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://cdn.example.com/{blob_ref}")
    }

    @Test
    fun `configured sources are ordered by priority, then the hardcoded fallback`() {
        val high = item("url" to "https://high.example.com/{blob_ref}", "priority" to 10)
        val low = item("url" to "https://low.example.com/{blob_ref}", "priority" to 0)
        val provider = provider(store("m1", sourcesOf("blob" to high, "blob2" to low)))

        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://high.example.com/{blob_ref}")
        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://low.example.com/{blob_ref}")
        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)
    }

    // endregion

    // region Memoization

    @Test
    fun `failover state persists across calls with the same manifest`() {
        val provider = provider(
            store(
                "m1",
                sourcesOf(
                    "blob" to item("url" to "https://a.example.com/{blob_ref}", "priority" to 10),
                    "blob2" to item("url" to "https://b.example.com/{blob_ref}", "priority" to 0),
                ),
            ),
        )

        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        // A second read with the same manifest must NOT re-parse and reset to the first source.
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://b.example.com/{blob_ref}")
    }

    @Test
    fun `the memo rebuilds with fresh failover when the manifest changes`() {
        val store = mockk<RemoteConfigTopicStore>()
        var current = state("m1", sourcesOf("blob" to item("url" to "https://a.example.com/{blob_ref}")))
        every { store.read() } answers { current }
        every { store.topic("sources") } answers { current.topics["sources"] }
        val provider = LazyRemoteConfigSourceProvider(store, random = FakeRandom(0))

        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)

        current = state("m2", sourcesOf("blob" to item("url" to "https://b.example.com/{blob_ref}")))
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://b.example.com/{blob_ref}")
    }

    @Test
    fun `clear re-parses from the first source on the next call`() {
        val provider = provider(store("m1", sourcesOf("blob" to item("url" to "https://a.example.com/{blob_ref}"))))

        provider.reportUnhealthy(provider.getCurrent(Purpose.BLOB)!!)
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo(DEFAULT_BLOB_URL_TEMPLATE)

        provider.clear()
        assertThat(provider.getCurrent(Purpose.BLOB)?.url).isEqualTo("https://a.example.com/{blob_ref}")
    }

    // endregion

    // region Threading

    @Test
    fun `concurrent reports never skip sources when serialized`() {
        val urls = (0 until 50).map { "https://cdn$it.example.com/{blob_ref}" }
        val configured = urls.withIndex().associate { (i, url) -> "blob$i" to item("url" to url) }
        val provider = provider(store("m1", configured))

        val seen = ConcurrentHashMap.newKeySet<String>()
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(8)
        repeat(8) {
            pool.execute {
                try {
                    var current = provider.getCurrent(Purpose.BLOB)
                    while (current != null) {
                        seen.add(current.url)
                        provider.reportUnhealthy(current)
                        current = provider.getCurrent(Purpose.BLOB)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(10, TimeUnit.SECONDS)
        pool.shutdown()

        assertThat(seen).isEqualTo(urls.toSet() + DEFAULT_BLOB_URL_TEMPLATE)
        assertThat(provider.getCurrent(Purpose.BLOB)).isNull()
    }

    // endregion

    // region Helpers

    private fun provider(store: RemoteConfigTopicStore): LazyRemoteConfigSourceProvider =
        LazyRemoteConfigSourceProvider(store, random = FakeRandom(0))

    private fun store(manifest: String, sources: ConfigTopic?): RemoteConfigTopicStore {
        val store = mockk<RemoteConfigTopicStore>()
        val state = state(manifest, sources)
        every { store.read() } returns state
        every { store.topic("sources") } returns sources
        return store
    }

    private fun state(manifest: String, sources: ConfigTopic?): PersistedRemoteConfigurationState =
        PersistedRemoteConfigurationState(
            domain = "app",
            manifest = manifest,
            topics = sources?.let { mapOf("sources" to it) } ?: emptyMap(),
        )

    private fun sourcesOf(vararg items: Pair<String, RemoteConfiguration.ConfigItem>): ConfigTopic = items.toMap()

    private fun item(vararg pairs: Pair<String, Any?>): RemoteConfiguration.ConfigItem =
        RemoteConfiguration.ConfigItem(
            content = buildJsonObject {
                pairs.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        else -> {}
                    }
                }
            },
        )

    /** Returns queued values from [nextInt], repeating the last once drained. Mirrors `RemoteConfigSourceProviderTest`. */
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
