package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigSourceHandle.Purpose
import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/** A remote config source: a URL plus the metadata used to order sources. */
internal data class RemoteConfigSource(
    /** A plain URL or a URL format with placeholders (e.g. `{blob_ref}`), to be resolved by the caller. */
    val url: String,
    override val priority: Int,
    override val weight: Int,
) : WeightedSource

/**
 * A source handed out by a [RemoteConfigSourceProvider], tagged with its [purpose] (api or blob).
 * Report it back via [RemoteConfigSourceProvider.reportUnhealthy] to fall back to the next source.
 * The opaque [token] is its identity: a report is ignored once the provider has moved past it (via
 * fallback or [RemoteConfigSourceProvider.restart]), so stale or concurrent reports can't advance
 * the order more than once.
 */
internal data class RemoteConfigSourceHandle(
    val purpose: Purpose,
    val source: RemoteConfigSource,
    /**
     * Identifies the handout that produced this handle. Only meaningful to the provider, which uses
     * it to discard stale reports.
     */
    val token: Int,
) {

    /** What the source is used for: calling the config api or downloading a blob. */
    enum class Purpose { API, BLOB }

    /** A plain URL or a URL format with placeholders, to be resolved by the caller. */
    val url: String get() = source.url
}

internal interface RemoteConfigSourceProvider {

    /** The current healthy source for [purpose], or null once all of its sources are reported unhealthy. */
    fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle?

    /** Falls back to the next source for the handle's purpose. No-op if [handle] is no longer current. */
    fun reportUnhealthy(handle: RemoteConfigSourceHandle)

    /** Rewinds the given purpose to its first source, e.g. to start fresh on a new fetch cycle. */
    fun restart(purpose: RemoteConfigSourceHandle.Purpose)

    /** Drops any memoized source resolution (e.g. on identity change). No-op for fixed-source providers. */
    fun clear() {}
}

/**
 * The address book for remote config: hands out the current healthy api and blob sources and falls
 * back to the next one when a source is reported unhealthy. Each purpose fails over independently.
 * Sources are deduped by url and ordered once via [WeightedSourceSelector].
 *
 * Thread-safe.
 */
internal class DefaultRemoteConfigSourceProvider(
    apiSources: List<RemoteConfigSource>,
    blobSources: List<RemoteConfigSource>,
    random: Random = Random.Default,
) : RemoteConfigSourceProvider {

    private val api = SourceFailover(RemoteConfigSourceHandle.Purpose.API, dedupe(apiSources), random)
    private val blob = SourceFailover(RemoteConfigSourceHandle.Purpose.BLOB, dedupe(blobSources), random)

    override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
        when (purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api.current
            RemoteConfigSourceHandle.Purpose.BLOB -> blob.current
        }

    override fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        when (handle.purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api.reportUnhealthy(handle)
            RemoteConfigSourceHandle.Purpose.BLOB -> blob.reportUnhealthy(handle)
        }
    }

    override fun restart(purpose: RemoteConfigSourceHandle.Purpose) {
        when (purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api.restart()
            RemoteConfigSourceHandle.Purpose.BLOB -> blob.restart()
        }
    }

    private companion object {

        /**
         * Collapses duplicate urls to the occurrence with the highest priority (tie-broken by
         * weight), keeping first-seen order. Done once at init so reads never need to re-dedupe.
         */
        fun dedupe(sources: List<RemoteConfigSource>): List<RemoteConfigSource> {
            // LinkedHashMap keeps first-seen url order for deterministic ordering downstream.
            val bestByUrl = LinkedHashMap<String, RemoteConfigSource>()
            for (source in sources) {
                val existing = bestByUrl[source.url]
                if (existing == null) {
                    bestByUrl[source.url] = source
                    continue
                }
                if (source.priority != existing.priority || source.weight != existing.weight) {
                    warnLog {
                        "Found remote config sources sharing the same URL with conflicting priority/weight " +
                            "(${source.url}). Keeping the highest-priority one, tie-broken by weight."
                    }
                }
                if (source.priority > existing.priority ||
                    (source.priority == existing.priority && source.weight > existing.weight)
                ) {
                    bestByUrl[source.url] = source
                }
            }
            return bestByUrl.values.toList()
        }
    }
}

/**
 * Walks a single list of sources in fallback order. Every handout is stamped with the current
 * [token], which is bumped whenever the position changes (fallback or [restart]). A report only
 * advances if its handle still carries the current token, so stale or concurrent reports - and
 * reports left over from before a [restart] - are ignored.
 *
 * Thread-safe.
 */
private class SourceFailover(
    private val purpose: RemoteConfigSourceHandle.Purpose,
    sources: List<RemoteConfigSource>,
    random: Random,
) {
    private val selector = WeightedSourceSelector(sources, random)
    private val lock = Any()
    private var token = 0

    val current: RemoteConfigSourceHandle?
        get() = synchronized(lock) {
            selector.current?.let { RemoteConfigSourceHandle(purpose, it, token) }
        }

    fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        synchronized(lock) {
            if (handle.token != token) return
            selector.advance()
            token++
        }
    }

    fun restart() {
        synchronized(lock) {
            selector.reset()
            token++
        }
    }
}

/** The hardcoded blob URL template used as a last-resort fallback when the `sources` topic supplies none. */
internal const val DEFAULT_BLOB_URL_TEMPLATE = "https://config.revenuecat-static.com/{blob_ref}"

/** The hardcoded API base URL used as a last-resort fallback. Held for the future; live API rerouting is not wired. */
internal const val DEFAULT_API_URL = "https://api.revenuecat.com"

/**
 * A [RemoteConfigSourceProvider] that resolves its sources **lazily** from the committed `sources` topic
 * (read through [RemoteConfigTopicStore]) instead of taking fixed lists at construction. It wraps a memoized
 * [DefaultRemoteConfigSourceProvider] — it does not reimplement the failover/token logic.
 *
 * - **Initialized from the cached configuration.** The first [resolve] reads the persisted `sources` topic
 *   (from a prior session, if any), so configured sources are in effect from the very first download — the
 *   fetcher calls [getCurrent] before downloading.
 * - **Hardcoded fallbacks, always present, lowest priority.** Parsed sources are followed by
 *   [DEFAULT_API_URL] / [DEFAULT_BLOB_URL_TEMPLATE] at [Int.MIN_VALUE] priority, so configured/cached
 *   sources are always tried first and blob fetching never regresses below the hardcoded source (never empty).
 * - **Non-blocking on the consuming thread.** The memo is keyed on the persisted manifest, which advances on
 *   every commit, so only the first [resolve] after a config change touches disk; steady state is in-memory.
 *   Every call here runs on the blob fetcher's IO pool (or the manager's IO scope), never the main thread.
 */
internal class LazyRemoteConfigSourceProvider(
    private val topicStore: RemoteConfigTopicStore,
    private val apiFallbacks: List<RemoteConfigSource> = listOf(
        RemoteConfigSource(url = DEFAULT_API_URL, priority = Int.MIN_VALUE, weight = 1),
    ),
    private val blobFallbacks: List<RemoteConfigSource> = listOf(
        RemoteConfigSource(url = DEFAULT_BLOB_URL_TEMPLATE, priority = Int.MIN_VALUE, weight = 1),
    ),
    private val random: Random = Random.Default,
) : RemoteConfigSourceProvider {

    private val lock = Any()

    /** The manifest the memoized [inner] was built from. [NOT_BUILT] until the first [resolve]. */
    private var memoKey: String? = NOT_BUILT
    private var inner: DefaultRemoteConfigSourceProvider? = null

    override fun getCurrent(purpose: Purpose): RemoteConfigSourceHandle? = resolve().getCurrent(purpose)

    override fun reportUnhealthy(handle: RemoteConfigSourceHandle) = resolve().reportUnhealthy(handle)

    override fun restart(purpose: Purpose) = resolve().restart(purpose)

    /** Drops the memoized sources (identity change). The next call re-parses from the (cleared) disk cache. */
    override fun clear() = synchronized(lock) {
        memoKey = NOT_BUILT
        inner = null
    }

    /**
     * Returns the inner provider for the currently committed `sources`, rebuilding it only when the persisted
     * manifest has changed since the memo was built. The lock guards the cheap read/rebuild/return; the inner
     * provider is independently thread-safe, so no lock is held across a delegated call.
     */
    private fun resolve(): DefaultRemoteConfigSourceProvider = synchronized(lock) {
        val key = topicStore.read()?.manifest
        val current = inner
        if (current != null && key == memoKey) return current

        val (apiSources, blobSources) = parseSources(topicStore.topic(SOURCES_TOPIC))
        DefaultRemoteConfigSourceProvider(
            apiSources = apiSources + apiFallbacks,
            blobSources = blobSources + blobFallbacks,
            random = random,
        ).also {
            inner = it
            memoKey = key
        }
    }

    private fun parseSources(topic: ConfigTopic?): Pair<List<RemoteConfigSource>, List<RemoteConfigSource>> {
        val apiSources = mutableListOf<RemoteConfigSource>()
        val blobSources = mutableListOf<RemoteConfigSource>()
        topic?.forEach { (key, item) ->
            val source = item.toSource() ?: return@forEach
            // A blob source carries the `{blob_ref}` slot; otherwise treat it as an api source. The item key
            // (`blob`/`api`) is only a tiebreak so an unexpected shape still routes by the placeholder.
            if (BLOB_REF_PLACEHOLDER in source.url || key == BLOB_ITEM_KEY) {
                blobSources.add(source)
            } else {
                apiSources.add(source)
            }
        }
        if (topic != null && blobSources.isEmpty()) {
            warnLog { "Remote config 'sources' topic has no usable blob source; using the hardcoded fallback." }
        }
        return apiSources to blobSources
    }

    private fun RemoteConfiguration.ConfigItem.toSource(): RemoteConfigSource? {
        val url = content.string(URL_FORMAT_KEY) ?: content.string(URL_KEY) ?: return null
        return RemoteConfigSource(
            url = url,
            priority = content[PRIORITY_KEY]?.jsonPrimitive?.intOrNull ?: 0,
            weight = content[WEIGHT_KEY]?.jsonPrimitive?.intOrNull ?: 1,
        )
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private companion object {
        private const val SOURCES_TOPIC = "sources"
        private const val BLOB_ITEM_KEY = "blob"
        private const val BLOB_REF_PLACEHOLDER = "{blob_ref}"
        private const val URL_FORMAT_KEY = "url_format"
        private const val URL_KEY = "url"
        private const val PRIORITY_KEY = "priority"
        private const val WEIGHT_KEY = "weight"

        /** Sentinel distinguishing "never built" from "built from a null manifest" (first run). */
        private const val NOT_BUILT = " <unbuilt>"
    }
}
