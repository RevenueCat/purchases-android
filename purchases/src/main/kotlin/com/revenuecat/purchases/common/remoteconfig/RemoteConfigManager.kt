package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.GetRemoteConfigErrorHandlingBehavior
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 *
 * Consumers read through the facade: [topic] for a topic's committed item index (metadata only) and [blobData]
 * for a resolved item's blob payload (fetched on demand). Both run on [ioDispatcher] so callers never touch disk
 * on their own thread. When either read finds no committed data it calls [awaitConfigForRead] rather than
 * failing — waiting for a refresh in progress, or triggering one on demand when none is (a cold read fetches its
 * own data) — unless the endpoint is [isDisabled] or no app user is known yet. [awaitConfigReady] exposes the
 * same wait as a callback-based readiness gate for consumers that only need to order their delivery after the
 * sync (offerings), without reading anything themselves.
 */
@OptIn(InternalRevenueCatAPI::class)
@Suppress("LongParameterList", "TooManyFunctions")
internal class RemoteConfigManager(
    private val backend: Backend,
    private val diskCache: RemoteConfigDiskCache,
    private val blobStore: RemoteConfigBlobStore,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val topicStore: RemoteConfigTopicStore =
        RemoteConfigTopicStore { diskCache.read()?.topics?.get(it.wireName) },
    private val sourceProvider: RemoteConfigSourceProvider =
        DefaultRemoteConfigSourceProvider(topicStore),
    private val blobFetcher: RemoteConfigBlobFetcher = RemoteConfigBlobFetcher(blobStore, sourceProvider),
    private val appUserIDProvider: () -> String? = { null },
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

    // Completion signal for the single in-flight refresh, so a read that finds no cached data can wait for the
    // refresh already in progress instead of failing. Created under [cacheLock] when a refresh starts, completed
    // (never cancelled, so waiting reads never throw) at every terminal point and by clearCache(); null when no
    // refresh is in flight. isRefreshing keeps its skip semantics; this only adds an awaitable handle.
    @Volatile
    private var refreshCompletion: CompletableDeferred<Unit>? = null

    // Session-scoped kill-switch. Set when `/v1/config` returns a 4xx (the endpoint intentionally refused the
    // request). While set, no config request is issued and — since blob prefetch only runs after a successful
    // persist — no blob fetch happens either. Memory-only: an app restart re-enables the endpoint. Intentionally
    // NOT reset by clearCache() (a 4xx is an endpoint/app-level fact, not per-user).
    @Volatile
    private var disabled = false

    /**
     * Whether `/v1/config` has been disabled for this session after a 4xx client error. Consumers can read this
     * to know the remote config endpoint is not being used. Resets only on app restart.
     */
    val isDisabled: Boolean
        get() = disabled

    fun refreshRemoteConfigIfStale(appInBackground: Boolean, appUserID: String) {
        if (lastRefreshedAt.isCacheStale(appInBackground, dateProvider)) {
            refreshRemoteConfig(appInBackground, appUserID)
        }
    }

    fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String) {
        if (disabled) {
            debugLog { "Remote config is disabled for this session (4xx). Skipping refresh." }
            return
        }
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
            refreshCompletion = CompletableDeferred()
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
                            completeRefresh()
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
            onError = { error, behavior -> handleRefreshError(requestEpoch, error, behavior) },
        )
    }

    private fun handleRefreshError(
        requestEpoch: Int,
        error: PurchasesError,
        behavior: GetRemoteConfigErrorHandlingBehavior,
    ) {
        if (behavior == GetRemoteConfigErrorHandlingBehavior.SHOULD_DISABLE) {
            // A 4xx: disable the endpoint for the rest of the session. This is an endpoint-level fact, so set it
            // regardless of epoch ownership (a late response for an old identity is still a valid signal that the
            // endpoint refuses this app's requests).
            disabled = true
        }
        if (releaseGuardIfOwned(requestEpoch)) {
            errorLog(error)
        }
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
            completeRefresh()
            // Intentionally NOT resetting `disabled`: a 4xx is an endpoint/app-level fact that outlives an
            // identity change. It clears only on app restart.
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
        if (owned) {
            isRefreshing.set(false)
            completeRefresh()
        }
        owned
    }

    private fun completeRefresh() {
        refreshCompletion?.complete(Unit)
        refreshCompletion = null
    }

    /**
     * Makes a best effort to have the configuration loaded before a read that found no cached data gives up:
     * - a refresh already in progress → wait for it (a read during the initial sync sees its result);
     * - otherwise trigger a sync on demand and wait for it, so a cold read fetches its own data instead of
     *   returning `null` — **unless** the endpoint is [isDisabled] (the 4xx session kill-switch) or no app
     *   user is known yet, in which case it gives up without a network call.
     *
     * The on-demand sync is issued as foreground (`appInBackground = false`): a read is blocking on the result,
     * so it wants the un-jittered, prompt request.
     */
    private suspend fun awaitConfigForRead() {
        if (awaitInFlightRefresh()) return
        // Nothing in flight: trigger a sync on demand, unless the endpoint is disabled or no user is known yet.
        val appUserID = appUserIDProvider()?.takeIf { it.isNotBlank() }
        if (!disabled && appUserID != null) {
            refreshRemoteConfig(appInBackground = false, appUserID = appUserID)
            // Join whatever is now in flight — the sync we just triggered, or one a concurrent caller started.
            awaitInFlightRefresh()
        }
    }

    /**
     * Suspends until the refresh already in progress finishes; returns `true` if there was one to await, `false`
     * when none was in flight. The handle is captured under [cacheLock] but awaited outside it, so this never
     * holds the lock across suspension.
     */
    private suspend fun awaitInFlightRefresh(): Boolean {
        val completion = synchronized(cacheLock) { refreshCompletion } ?: return false
        completion.await()
        return true
    }

    /**
     * Invokes [onReady] (on [scope]) once the configuration consumers gate on is in place:
     * - something already committed → wait only for any refresh in flight, so a delivery issued together with a
     *   sync sees that sync's result;
     * - cold cache → join the refresh in flight, or trigger one and wait for it, so the first delivery after
     *   install doesn't run ahead of the initial sync.
     *
     * It never waits for data that cannot arrive: when the endpoint is [isDisabled] it reports ready
     * immediately, and every refresh terminal path (200, 204, error, [clearCache]) completes the wait. This is
     * a readiness signal, not a success signal — a failed sync still reports ready.
     */
    fun awaitConfigReady(appInBackground: Boolean, appUserID: String, onReady: () -> Unit) {
        scope.launch {
            if (!disabled) {
                val committed = withContext(ioDispatcher) { diskCache.read() != null }
                if (!committed && !awaitInFlightRefresh()) {
                    refreshRemoteConfig(appInBackground, appUserID)
                }
                awaitInFlightRefresh()
            }
            onReady()
        }
    }

    /**
     * A topic's persisted item index (metadata only — inline `metadata` + `blob_ref`, no blob bytes), or `null`
     * when nothing is cached for [topic] even after a refresh, or when the endpoint is [isDisabled] (the 4xx
     * session kill-switch).
     *
     * When the topic isn't committed yet, [awaitConfigForRead] first waits for a refresh in progress — or
     * triggers one on demand — and then re-reads before giving up, so a read during the initial sync (or before
     * any sync) returns fresh data instead of `null`, mirroring [blobData]. A committed topic returns
     * immediately, never delayed by an unrelated in-flight refresh. Use [blobData] for a resolved item payload
     * that also resolves the referenced blob. Reads disk on [ioDispatcher].
     */
    suspend fun topic(topic: RemoteConfigTopic): ConfigTopic? = withContext(ioDispatcher) {
        if (disabled) return@withContext null
        topicStore.topic(topic)?.let { return@withContext it }
        awaitConfigForRead()
        topicStore.topic(topic)
    }

    /**
     * The resolved blob payload for `itemKey` in [topic], parsed from JSON into [T], or `null` when the item
     * is unknown, has no `blob_ref`, its blob can't be resolved, or its bytes don't deserialize into [T]. [T]
     * must be a concrete `@Serializable` type; parsing uses the shared [JsonProvider.defaultJson]. For non-JSON
     * payloads use the `transform` overload, which also documents the resolution and waiting rules.
     */
    suspend inline fun <reified T> blobData(topic: RemoteConfigTopic, itemKey: String): T? =
        blobData(topic, itemKey) { bytes ->
            try {
                JsonProvider.defaultJson.decodeFromString<T>(bytes.decodeToString())
            } catch (e: SerializationException) {
                errorLog(e) { "Failed to parse remote config blob for item '$itemKey' as JSON." }
                null
            }
        }

    /**
     * Resolves the blob payload bytes for `itemKey` in [topic] and maps them through [transform], or `null`
     * when the item is unknown or its blob can't be resolved. Use this for non-JSON blobs the caller parses
     * itself; the reified overload is the JSON convenience built on top of it.
     *
     * Owns the `blob_ref` rule: an item with **no** `blob_ref` resolves to `null` (its inline metadata is
     * exposed only through [topic], never as a payload); otherwise the referenced blob is resolved on demand
     * (HIGH priority, joining any in-flight prefetch of the same ref) and read back.
     *
     * When the item isn't committed yet, [awaitConfigForRead] first waits for a refresh in progress — or
     * triggers one on demand — and then re-reads before giving up, so a read during the initial sync (or
     * before any sync) returns fresh data instead of `null`. A committed item returns immediately, never
     * delayed by an unrelated in-flight refresh.
     *
     * Returns `null` without any read when the endpoint is [isDisabled] (the 4xx session kill-switch). Runs on
     * [ioDispatcher].
     */
    suspend fun <T> blobData(
        topic: RemoteConfigTopic,
        itemKey: String,
        transform: (ByteArray) -> T?,
    ): T? = withContext(ioDispatcher) {
        resolveBlobBytes(topic, itemKey)?.let(transform)
    }

    /**
     * Resolves an item's referenced-blob bytes, or `null` when the endpoint is [isDisabled], the item is
     * unknown, or it has no `blob_ref`.
     */
    private suspend fun resolveBlobBytes(topic: RemoteConfigTopic, itemKey: String): ByteArray? {
        if (disabled) return null
        val ref = committedItem(topic, itemKey)?.blobRef
        return when {
            ref == null -> null
            blobFetcher.ensureDownloaded(ref) -> blobStore.read(ref)
            else -> null
        }
    }

    /**
     * The committed item for `itemKey` in [topic], awaiting an in-flight or on-demand refresh once when it
     * isn't cached yet (see [awaitConfigForRead]), or `null` when it still isn't found.
     */
    private suspend fun committedItem(topic: RemoteConfigTopic, itemKey: String): RemoteConfiguration.ConfigItem? {
        topicStore.topic(topic)?.get(itemKey)?.let { return it }
        awaitConfigForRead()
        return topicStore.topic(topic)?.get(itemKey)
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
     * first **only if a prior cycle exhausted its sources** (otherwise failover progress is kept, so a
     * known-bad higher-priority source isn't re-tried every sync), then hands the not-yet-cached refs to the
     * fetcher's LOW-priority queue. Runs on the manager's IO scope (inside [persist]), so it never blocks the
     * main thread; a failed download is tolerated (re-fetched next sync / on demand).
     */
    private fun prefetchBlobs(response: RemoteConfiguration, mergedTopics: Map<String, ConfigTopic>) {
        sourceProvider.restartIfExhausted(RemoteConfigSourceHandle.Purpose.BLOB)
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
