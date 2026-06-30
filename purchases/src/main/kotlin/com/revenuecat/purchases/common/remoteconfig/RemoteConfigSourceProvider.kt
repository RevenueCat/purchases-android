package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.warnLog
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
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
}

/**
 * The address book for remote config: hands out the current healthy api and blob sources and falls
 * back to the next one when a source is reported unhealthy. Each purpose fails over independently.
 *
 * Reads the `sources` topic lazily from [topicStore] and rebuilds its ordered lists only when that
 * topic's [ConfigTopic.contentHash] changes: an unchanged topic keeps failover progress, while a
 * changed one restarts both lists from the top. Sources are deduped by url and ordered via
 * [WeightedSourceSelector].
 *
 * Thread-safe.
 */
internal class DefaultRemoteConfigSourceProvider(
    private val topicStore: RemoteConfigTopicStore,
    private val random: Random = Random.Default,
) : RemoteConfigSourceProvider {

    private val lock = Any()

    // The hash the current failovers were built from, and whether we've built at all. A null hash is a
    // valid built state (the `sources` topic is absent), so `built` distinguishes it from "never built".
    private var built = false
    private var builtHash: String? = null
    private var api = SourceFailover(RemoteConfigSourceHandle.Purpose.API, emptyList(), random)
    private var blob = SourceFailover(RemoteConfigSourceHandle.Purpose.BLOB, emptyList(), random)

    override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
        synchronized(lock) {
            rebuildIfChanged()
            failoverFor(purpose).current
        }

    override fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        synchronized(lock) {
            rebuildIfChanged()
            failoverFor(handle.purpose).reportUnhealthy(handle)
        }
    }

    override fun restart(purpose: RemoteConfigSourceHandle.Purpose) {
        synchronized(lock) {
            rebuildIfChanged()
            failoverFor(purpose).restart()
        }
    }

    private fun failoverFor(purpose: RemoteConfigSourceHandle.Purpose): SourceFailover =
        when (purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api
            RemoteConfigSourceHandle.Purpose.BLOB -> blob
        }

    /** Rebuilds both failovers from the latest `sources` topic when its hash changed. Guarded by [lock]. */
    private fun rebuildIfChanged() {
        val topic = topicStore.topic(SOURCES_TOPIC)
        val hash = topic?.contentHash
        if (built && hash == builtHash) return
        // Seed the new generation past any token the previous one could have handed out, so reports left
        // over from before the rebuild are ignored instead of advancing the freshly-restarted list.
        val nextToken = maxOf(api.currentToken, blob.currentToken) + 1
        api = SourceFailover(
            RemoteConfigSourceHandle.Purpose.API,
            dedupe(parseSources(topic, API_ITEM, URL_KEY)),
            random,
            nextToken,
        )
        blob = SourceFailover(
            RemoteConfigSourceHandle.Purpose.BLOB,
            dedupe(parseSources(topic, BLOB_ITEM, URL_FORMAT_KEY)),
            random,
            nextToken,
        )
        builtHash = hash
        built = true
    }

    private companion object {
        private const val SOURCES_TOPIC = "sources"
        private const val API_ITEM = "api"
        private const val BLOB_ITEM = "blob"
        private const val SOURCES_KEY = "sources"
        private const val URL_KEY = "url"
        private const val URL_FORMAT_KEY = "url_format"
        private const val PRIORITY_KEY = "priority"
        private const val WEIGHT_KEY = "weight"

        /**
         * Extracts the source list from the `sources` topic item [itemKey] (`api` or `blob`), reading each
         * entry's url from [urlKey] (`url` for api, `url_format` for blob). Malformed entries are skipped.
         */
        fun parseSources(topic: ConfigTopic?, itemKey: String, urlKey: String): List<RemoteConfigSource> {
            val entries = topic?.get(itemKey)?.content?.get(SOURCES_KEY) as? JsonArray ?: return emptyList()
            return entries.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val url = obj.string(urlKey) ?: return@mapNotNull null
                val priority = obj.int(PRIORITY_KEY) ?: return@mapNotNull null
                val weight = obj.int(WEIGHT_KEY) ?: return@mapNotNull null
                RemoteConfigSource(url = url, priority = priority, weight = weight)
            }
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

        /**
         * Collapses duplicate urls to the occurrence with the highest priority (tie-broken by
         * weight), keeping first-seen order. Done once per rebuild so reads never need to re-dedupe.
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
    initialToken: Int = 0,
) {
    private val selector = WeightedSourceSelector(sources, random)
    private val lock = Any()
    private var token = initialToken

    /** The token a handle handed out right now would carry. Used to seed the next generation on rebuild. */
    val currentToken: Int get() = synchronized(lock) { token }

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
