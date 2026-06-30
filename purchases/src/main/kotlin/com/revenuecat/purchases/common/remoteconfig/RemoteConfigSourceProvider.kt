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
 * changed one restarts both lists from the top. While the topic is absent, it falls back to embedded
 * default sources so the SDK can reach the config api before any config is fetched. Sources are
 * deduped by url and ordered via [WeightedSourceSelector].
 *
 * Thread-safe.
 */
internal class DefaultRemoteConfigSourceProvider(
    private val topicStore: RemoteConfigTopicStore,
    private val random: Random = Random.Default,
) : RemoteConfigSourceProvider {

    private val lock = Any()

    // Content hash of the `sources` topic the current failovers were built from. Null means there is no
    // sources topic (absent, or none seen yet), in which case the failovers hold the embedded defaults.
    private var builtHash: String? = null
    private var api = SourceFailover(
        RemoteConfigSourceHandle.Purpose.API,
        dedupe(sourcesFor(null, RemoteConfigSourceHandle.Purpose.API)),
        random,
    )
    private var blob = SourceFailover(
        RemoteConfigSourceHandle.Purpose.BLOB,
        dedupe(sourcesFor(null, RemoteConfigSourceHandle.Purpose.BLOB)),
        random,
    )

    override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
        synchronized(lock) {
            rebuildIfChanged()
            failoverFor(purpose).current
        }

    override fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        synchronized(lock) {
            // Rebuild happened, no need to report unhealthy
            if (!rebuildIfChanged()) {
                failoverFor(handle.purpose).reportUnhealthy(handle)
            }
        }
    }

    override fun restart(purpose: RemoteConfigSourceHandle.Purpose) {
        synchronized(lock) {
            // Rebuild happened, no need to restart
            if (!rebuildIfChanged()) {
                failoverFor(purpose).restart()
            }
        }
    }

    private fun failoverFor(purpose: RemoteConfigSourceHandle.Purpose): SourceFailover =
        when (purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api
            RemoteConfigSourceHandle.Purpose.BLOB -> blob
        }

    /**
     * Rebuilds both failovers from the latest `sources` topic when its hash changed, returning whether
     * a rebuild happened. Callers must hold [lock].
     */
    private fun rebuildIfChanged(): Boolean {
        val topic = topicStore.topic(SOURCES_TOPIC)
        val hash = topic?.contentHash
        if (hash == builtHash) return false
        // Seed the new generation past any token the previous one could have handed out, so reports left
        // over from before the rebuild are ignored instead of advancing the freshly-restarted list.
        val nextToken = maxOf(api.currentToken, blob.currentToken) + 1
        api = SourceFailover(
            RemoteConfigSourceHandle.Purpose.API,
            dedupe(sourcesFor(topic, RemoteConfigSourceHandle.Purpose.API)),
            random,
            nextToken,
        )
        blob = SourceFailover(
            RemoteConfigSourceHandle.Purpose.BLOB,
            dedupe(sourcesFor(topic, RemoteConfigSourceHandle.Purpose.BLOB)),
            random,
            nextToken,
        )
        builtHash = hash
        return true
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

        // Embedded defaults used until a `sources` topic is fetched, so the SDK can always reach config.
        private val DEFAULT_API_SOURCES = listOf(
            RemoteConfigSource(url = "https://api.revenuecat.com", priority = 0, weight = 1),
        )
        private val DEFAULT_BLOB_SOURCES = listOf(
            RemoteConfigSource(url = "https://config.revenuecat-static.com/{blob_ref}", priority = 0, weight = 1),
        )

        /**
         * The sources for [purpose]: parsed from the `sources` [topic], or the embedded defaults while
         * the topic is absent.
         */
        fun sourcesFor(
            topic: ConfigTopic?,
            purpose: RemoteConfigSourceHandle.Purpose,
        ): List<RemoteConfigSource> = when (purpose) {
            RemoteConfigSourceHandle.Purpose.API ->
                if (topic == null) DEFAULT_API_SOURCES else parseSources(topic, API_ITEM, URL_KEY)
            RemoteConfigSourceHandle.Purpose.BLOB ->
                if (topic == null) DEFAULT_BLOB_SOURCES else parseSources(topic, BLOB_ITEM, URL_FORMAT_KEY)
        }

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
 * Not thread-safe on its own: [DefaultRemoteConfigSourceProvider] serializes every access under its
 * own lock, which also provides the happens-before for these mutations.
 */
private class SourceFailover(
    private val purpose: RemoteConfigSourceHandle.Purpose,
    sources: List<RemoteConfigSource>,
    random: Random,
    initialToken: Int = 0,
) {
    private val selector = WeightedSourceSelector(sources, random)
    private var token = initialToken

    /** The token a handle handed out right now would carry. Used to seed the next generation on rebuild. */
    val currentToken: Int get() = token

    val current: RemoteConfigSourceHandle?
        get() = selector.current?.let { RemoteConfigSourceHandle(purpose, it, token) }

    fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        if (handle.token != token) return
        selector.advance()
        token++
    }

    fun restart() {
        selector.reset()
        token++
    }
}
