package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.common.warnLog
import kotlin.random.Random

/** A remote config source: a URL plus the metadata used to order sources. */
internal data class RemoteConfigSource(
    /** A plain URL or a URL format with placeholders (e.g. `{blob_ref}`), to be resolved by the caller. */
    val url: String,
    val priority: Int,
    val weight: Int,
)

/**
 * A source handed out by a [RemoteConfigSourceProvider], tagged with its [purpose] (api or blob).
 * Report it back via [RemoteConfigSourceProvider.reportUnhealthy] to fall back to the next source.
 * The [url] is its identity: a report is ignored once the provider has already moved past that url.
 */
internal data class RemoteConfigSourceHandle(
    val purpose: Purpose,
    val source: RemoteConfigSource,
) : WeightedSource {

    /** What the source is used for: calling the config api or downloading a blob. */
    enum class Purpose { API, BLOB }

    /** A plain URL or a URL format with placeholders, to be resolved by the caller. */
    val url: String get() = source.url
    override val priority: Int get() = source.priority
    override val weight: Int get() = source.weight
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

    private val api = SourceFailover(handles(apiSources, RemoteConfigSourceHandle.Purpose.API), random)
    private val blob = SourceFailover(handles(blobSources, RemoteConfigSourceHandle.Purpose.BLOB), random)

    override fun getCurrent(purpose: RemoteConfigSourceHandle.Purpose): RemoteConfigSourceHandle? =
        when (purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api.current
            RemoteConfigSourceHandle.Purpose.BLOB -> blob.current
        }

    override fun reportUnhealthy(handle: RemoteConfigSourceHandle) {
        when (handle.purpose) {
            RemoteConfigSourceHandle.Purpose.API -> api.reportUnhealthy(handle.url)
            RemoteConfigSourceHandle.Purpose.BLOB -> blob.reportUnhealthy(handle.url)
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
         * Builds the handles for a purpose, collapsing duplicate urls to the occurrence with the
         * highest priority (tie-broken by weight). Done once here so handles never need to be rebuilt
         * on reads.
         */
        fun handles(
            sources: List<RemoteConfigSource>,
            purpose: RemoteConfigSourceHandle.Purpose,
        ): List<RemoteConfigSourceHandle> {
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
            return bestByUrl.values.map { RemoteConfigSourceHandle(purpose, it) }
        }
    }
}

/**
 * Walks a single list of handles in fallback order, using each handle's url as its identity so a
 * stale [reportUnhealthy] (one the list has already moved past) is ignored.
 *
 * Thread-safe.
 */
private class SourceFailover(
    handles: List<RemoteConfigSourceHandle>,
    random: Random,
) {
    private val selector = WeightedSourceSelector(handles, random)
    private val lock = Any()

    val current: RemoteConfigSourceHandle?
        get() = synchronized(lock) { selector.current }

    fun reportUnhealthy(url: String) {
        synchronized(lock) {
            // Only advance when the report is about the current source: a url the list has already
            // moved past (e.g. from a concurrent caller) no longer matches, so it can't advance twice.
            if (selector.current?.url != url) return
            selector.advance()
        }
    }

    fun restart() {
        synchronized(lock) { selector.reset() }
    }
}
