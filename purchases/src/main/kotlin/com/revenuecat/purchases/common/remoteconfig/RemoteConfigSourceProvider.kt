package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.warnLog
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

internal interface RemoteConfigSourceProviderType {

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
 * Sources are deduped by url and ordered once via [WeightedSourceSelector].
 *
 * Thread-safe.
 */
internal class RemoteConfigSourceProvider(
    apiSources: List<RemoteConfigSource>,
    blobSources: List<RemoteConfigSource>,
    random: Random = Random.Default,
) : RemoteConfigSourceProviderType {

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
