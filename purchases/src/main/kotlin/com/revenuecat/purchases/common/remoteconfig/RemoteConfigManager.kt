package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Orchestrates a single `/v1/config` sync: replays the persisted opaque manifest, then on `204` keeps the cache
 * untouched and on `200` persists the fresh server manifest plus the full per-topic item index — the
 * configuration (incl. each item's inline content) is the source of truth, and persisting it is the **entire**
 * sync commit, advancing the manifest unconditionally. Only then, gated on a successful persist, it writes the
 * inlined blobs the resolved config still wants, prunes the rest, and best-effort prefetches the remaining
 * wanted blobs over the network ([RemoteConfigBlobFetcher], resolving blob source URLs through
 * [RemoteConfigSourceProvider]); a missing or un-parseable blob is recoverable later (re-fetched next sync / on
 * demand) and never blocks the commit. It reports which prefetch blobs are now cached locally on the next
 * request. (Live API base-URL rerouting from the `sources` topic is out of scope — a future phase.)
 *
 * The manifest is opaque (stored and replayed verbatim); the active-topic set and removed-topic detection come
 * from the response's [RemoteConfiguration.activeTopics]. The manager is topic-agnostic: it never interprets item
 * shapes or branches on topic name — consumer topics are read lazily by providers through the manager.
 *
 * The `200` path runs on [scope]: persistence is synchronous, but the launch lets [clearCache] cancel the
 * in-flight parse/persist. Blob prefetch runs on the fetcher's own worker pool (not [scope]). Identity changes
 * call [clearCache], which bumps an epoch so a late `/v1/config` response (its HTTP request cannot be
 * socket-cancelled) is dropped instead of persisting over the freshly wiped cache.
 *
 * Overlapping refreshes are deduped: only one [refreshRemoteConfig] runs at a time. A call made while one is
 * already in flight is skipped (the backend collapses concurrent requests but still fires every callback, which
 * would otherwise parse and persist the same response more than once).
 */
@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val blobStore: RemoteConfigBlobStore,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val sourceProvider: RemoteConfigSourceProvider =
        DefaultRemoteConfigSourceProvider({ diskCache.read()?.topics?.get(it) }),
    private val blobFetcher: RemoteConfigBlobFetcher = RemoteConfigBlobFetcher(blobStore, sourceProvider),
) {
    private val isRefreshing = AtomicBoolean(false)

    // Bumped by clearCache() on every identity change. A request captures the epoch when it starts; once it
    // changes, the in-flight request's callbacks drop their result (the /v1/config request itself cannot be
    // socket-cancelled), so an old user's response can never persist over the wiped cache.
    private val epoch = AtomicInteger(0)

    // Serializes a sync's "re-check epoch + persist" against clearCache()'s "bump epoch + wipe". persist() is
    // synchronous, so cancellation can't interrupt it; this lock makes the two critical sections atomic so a
    // late persist either runs fully before the wipe or sees the bumped epoch and skips — never writes after it.
    private val cacheLock = Any()

    @Volatile
    private var lastRefreshedAt: Date? = null

    fun refreshRemoteConfigIfStale(appInBackground: Boolean, appUserID: String) {
        if (lastRefreshedAt.isCacheStale(appInBackground, dateProvider)) {
            refreshRemoteConfig(appInBackground, appUserID)
        }
    }

    fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String) {
        val requestEpoch: Int
        // Acquire the in-flight guard and capture the epoch together under the lock that also guards
        // clearCache()'s epoch bump + guard release. Otherwise an identity change could slip its bump
        // between getAndSet(true) and epoch.get(), stranding this request with a stale epoch: its
        // callbacks would drop on the mismatch and never release the guard, freezing all future syncs.
        synchronized(cacheLock) {
            if (isRefreshing.getAndSet(true)) {
                debugLog { "Remote config refresh already in progress. Skipping." }
                return
            }
            requestEpoch = epoch.get()
        }
        val persisted = diskCache.read()
        val storedBlobs = blobStore.cachedRefs()
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            appUserID = appUserID,
            domain = persisted?.domain ?: DEFAULT_DOMAIN,
            // Opaque manifest replayed verbatim; null on the first run when nothing is persisted yet.
            manifest = persisted?.manifest,
            // Report only the prefetch blobs we actually hold, so the server stops re-inlining them.
            prefetchedBlobs = persisted?.prefetchBlobs?.filter { storedBlobs.contains(it) } ?: emptyList(),
            onSuccess = { container, _ ->
                if (epoch.get() != requestEpoch) {
                    // The cache was cleared (identity change) after this request started. Drop the stale
                    // response and leave isRefreshing alone: clearCache() reset it, or a newer refresh owns it.
                    return@getRemoteConfig
                }
                if (container == null) {
                    // 204: nothing changed
                    synchronized(cacheLock) {
                        if (epoch.get() == requestEpoch) {
                            lastRefreshedAt = dateProvider.now
                            isRefreshing.set(false)
                        }
                    }
                    return@getRemoteConfig
                }
                scope.launch {
                    try {
                        val response = RemoteConfiguration.parse(container.config.decode())
                        synchronized(cacheLock) {
                            if (epoch.get() != requestEpoch) return@launch
                            persist(previous = persisted, response = response, container = container)
                        }
                    } catch (e: SerializationException) {
                        errorLog(e) {
                            "Failed to parse remote config response. Keeping the cached configuration."
                        }
                    } catch (e: RCContainerFormatException) {
                        errorLog(e) {
                            "Failed to decode remote config response. Keeping the cached configuration."
                        }
                    } finally {
                        releaseGuardIfOwned(requestEpoch)
                    }
                }
            },
            onError = { error ->
                if (releaseGuardIfOwned(requestEpoch)) {
                    errorLog(error)
                }
            },
        )
    }

    /**
     * Wipes the cache on an identity change so configuration never bleeds across users (offerings-parity).
     */
    fun clearCache() {
        scope.coroutineContext.cancelChildren()
        synchronized(cacheLock) {
            epoch.incrementAndGet()
            isRefreshing.set(false)
            lastRefreshedAt = null
            diskCache.clear()
            blobStore.clear()
            sourceProvider.clear()
        }
    }

    fun close() {
        scope.cancel()
    }

    /**
     * Atomically releases the in-flight guard iff this request still owns the sync (its captured
     * [requestEpoch] is still current), under [cacheLock] so the epoch check and the release happen as one
     * step — paired with clearCache()'s bump+release and a refresh's acquire+capture. Prevents releasing the
     * guard out from under a newer owner (clearCache() or a newer refresh), which would let a duplicate
     * refresh start. Returns true if this call released the guard (it still owned the sync).
     */
    private fun releaseGuardIfOwned(requestEpoch: Int): Boolean = synchronized(cacheLock) {
        val owned = epoch.get() == requestEpoch
        if (owned) isRefreshing.set(false)
        owned
    }

    private fun persist(
        previous: PersistedRemoteConfigurationState?,
        response: RemoteConfiguration,
        container: RCContainer,
    ) {
        val previousTopics = previous?.topics ?: emptyMap()
        // Changed topics (present in the response) overwrite their item index; unchanged active topics keep their
        // carried-forward index (the server omits them); topics no longer active are pruned.
        val mergedTopics = (previousTopics + response.topics)
            .filterKeys { it in response.activeTopics }

        // Blobs the current config still wants: the prefetch set plus any active-topic blob ref.
        val blobRefsToKeep = response.prefetchBlobs.toSet() + mergedTopics.toTopicBlobRefs().values.flatten()

        // Persist the configuration first: the full topic index (incl. each item's inline content) plus the
        // manifest the server diffs against is the source of truth, and persisting it IS the entire commit (the
        // manifest advances unconditionally). Inline blobs are recoverable over the network, so only touch the
        // blob store once the state is durably committed — a failed persist never orphans or evicts blobs.
        val persisted = diskCache.write(
            PersistedRemoteConfigurationState(
                domain = response.domain,
                manifest = response.manifest,
                activeTopics = response.activeTopics,
                prefetchBlobs = response.prefetchBlobs,
                topics = mergedTopics,
            ),
        )

        if (persisted) {
            lastRefreshedAt = dateProvider.now
            extractInlineBlobs(container, blobRefsToKeep)
            blobStore.retainOnly(blobRefsToKeep)
            prefetchBlobs(response, mergedTopics)
        } else {
            errorLog { "Skipping remote config blob sync: failed to persist the configuration." }
        }
    }

    /**
     * Best-effort, topic-agnostic warm of the blobs the committed config wants prefetched: the server's
     * [RemoteConfiguration.prefetchBlobs] plus any item flagged `prefetch`. Re-arms the blob source provider
     * first (a prior cycle may have exhausted its sources), then hands the not-yet-cached refs to the fetcher's
     * LOW-priority queue. Runs on the manager's IO scope (inside [persist]), so it never blocks the main thread;
     * a failed download is tolerated (re-fetched next sync / on demand).
     */
    private fun prefetchBlobs(response: RemoteConfiguration, mergedTopics: Map<String, ConfigTopic>) {
        sourceProvider.restart(RemoteConfigSourceHandle.Purpose.BLOB)
        val refs = buildSet {
            addAll(response.prefetchBlobs)
            mergedTopics.values.forEach { topic ->
                topic.values.forEach { item -> if (item.prefetch) item.blobRef?.let(::add) }
            }
        }
        blobFetcher.prefetch(refs.filterNot { blobStore.contains(it) })
    }

    /** Caches inlined content elements the config still wants, whose bytes match their content-address ref. */
    private fun extractInlineBlobs(container: RCContainer, refsToKeep: Set<String>) {
        container.elements.forEach { (ref, element) ->
            if (ref !in refsToKeep) return@forEach
            // Decode once (the on-wire bytes may be compressed), then verify and store the uncompressed
            // bytes: the blob store is content-addressed by the hash of the uncompressed payload.
            val decoded = try {
                element.decode()
            } catch (e: RCContainerFormatException) {
                errorLog(e) { "Skipping remote config blob '$ref': could not decode element." }
                return@forEach
            }
            if (element.matchesChecksum(decoded)) {
                blobStore.write(ref, decoded)
            } else {
                errorLog { "Skipping remote config blob '$ref': checksum verification failed." }
            }
        }
    }

    private companion object {
        private const val DEFAULT_DOMAIN = "app"
    }
}

/** The blob refs each topic's items reference, keyed by topic name (empty list for inline-only topics). */
internal fun Map<String, ConfigTopic>.toTopicBlobRefs(): Map<String, List<String>> =
    mapValues { (_, topic) -> topic.values.mapNotNull { it.blobRef } }
