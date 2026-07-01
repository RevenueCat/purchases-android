package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Orchestrates a single `/v1/config` sync: replays the persisted opaque manifest, then on `204` keeps the cache
 * untouched and on `200` persists the fresh server manifest plus the full per-topic item index — the
 * configuration (incl. each item's inline content) is the source of truth, and persisting it is the **entire**
 * sync commit, advancing the manifest unconditionally. Only then, gated on a successful persist, it writes the
 * inlined blobs the resolved config still wants and prunes the rest; a missing or un-parseable blob is
 * recoverable later (fetched on demand in a future phase) and never blocks the commit. It reports which prefetch
 * blobs are now cached locally on the next request.
 *
 * The manifest is opaque (stored and replayed verbatim); the active-topic set and removed-topic detection come
 * from the response's [RemoteConfiguration.activeTopics]. The manager is topic-agnostic: it never interprets item
 * shapes or branches on topic name — consumer topics are read lazily by providers through the manager.
 *
 * The `200` path runs on [scope]: persistence is synchronous, but the launch lets [clearCache] cancel in-flight
 * work and gives a later phase's network blob fetch a scope to run in. Identity changes call [clearCache], which
 * bumps an epoch so a late `/v1/config` response (its HTTP request cannot be socket-cancelled) is dropped instead
 * of persisting over the freshly wiped cache.
 *
 * Overlapping refreshes are deduped: only one [refreshRemoteConfig] runs at a time. A call made while one is
 * already in flight is skipped (the backend collapses concurrent requests but still fires every callback, which
 * would otherwise parse and persist the same response more than once).
 */
@OptIn(InternalRevenueCatAPI::class)
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val blobStore: RemoteConfigBlobStore,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
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
                    // 204: nothing changed, keep the cache. Release the guard only if we still own the sync:
                    // clearCache() could have bumped the epoch between the check above and here.
                    releaseGuardIfOwned(requestEpoch)
                    return@getRemoteConfig
                }
                scope.launch {
                    // Hold the in-flight guard until persistence completes so a concurrent refresh can't read
                    // the disk cache mid-write and merge against a stale snapshot.
                    try {
                        val response = RemoteConfiguration.parse(container.config.decode())
                        // Re-check the epoch and persist under the lock that also guards clearCache()'s wipe, so
                        // a racing identity change either runs entirely before this write or makes it skip — the
                        // synchronous persist can never write the old user's config after the cache was wiped.
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
                        // Only release the guard if we still own this sync; a clearCache()/newer refresh owns
                        // it otherwise.
                        releaseGuardIfOwned(requestEpoch)
                    }
                }
            },
            onError = { error ->
                // Release the guard and log only if we still own the sync; a stale error response (the cache
                // was cleared after this request started) is dropped and leaves a newer owner's guard alone.
                if (releaseGuardIfOwned(requestEpoch)) {
                    errorLog(error)
                }
            },
        )
    }

    /**
     * Wipes the cache on an identity change so configuration never bleeds across users (offerings-parity).
     * Cancels in-flight sync work and keeps the scope usable for the next user (unlike [close]). The epoch bump,
     * the in-flight guard release, and the wipe all run under [cacheLock]: a synchronous `persist()` that already
     * started either finished before the wipe, or re-checks the bumped epoch under the same lock and skips — so an
     * old user's config can never be written after the wipe. Releasing the guard here (rather than before the
     * lock) keeps it paired with the epoch a [refreshRemoteConfig] observes, so a refresh can't acquire the guard
     * with a stale epoch and then strand it when its callback drops on the mismatch. (`cancelChildren()` still
     * unwinds a suspended sync, e.g. Phase 5's blob fetches, but is not load-bearing for either guarantee since
     * `persist()` is synchronous; the lock is.)
     */
    fun clearCache() {
        scope.coroutineContext.cancelChildren()
        synchronized(cacheLock) {
            epoch.incrementAndGet()
            // Release the guard inside the lock, after the bump, so it stays paired with the epoch a
            // refresh observes: a refresh either acquires the guard with the new epoch (and its callback
            // matches, releasing it normally) or runs entirely before this block (and we release it here).
            // Never the gap where it acquires the guard with the old epoch and then strands it on mismatch.
            isRefreshing.set(false)
            diskCache.clear()
            blobStore.clear()
        }
    }

    /** Cancels in-flight sync work. Called on SDK teardown (Phase 7 wiring). */
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
                lastRefreshAt = dateProvider.now.time,
            ),
        )

        if (persisted) {
            extractInlineBlobs(container, blobRefsToKeep)
            blobStore.retainOnly(blobRefsToKeep)
        } else {
            errorLog { "Skipping remote config blob sync: failed to persist the configuration." }
        }
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
