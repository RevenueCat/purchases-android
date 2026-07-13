package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
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
import com.revenuecat.purchases.common.verboseLog
import com.revenuecat.purchases.common.warnLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes

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
 * socket-cancelled) is dropped instead of persisting over the freshly wiped cache. [clearCache] also rebinds
 * the current app user atomically with that bump, so a cold on-demand read triggered right after an identity
 * change never fetches for the previous user (the cached app user ID the [appUserIDProvider] reads can still
 * lag the change for a window).
 *
 * Overlapping refreshes are deduped: only one [refreshRemoteConfig] runs at a time. A call made while one is
 * already in flight is skipped (the backend collapses concurrent requests but still fires every callback, which
 * would otherwise parse and persist the same response more than once).
 *
 * Consumers read through the facade: [topic] for a topic's committed item index (metadata only) and [blobData]
 * for a resolved item's blob payload (fetched on demand). Both run on [ioDispatcher] so callers never touch disk
 * on their own thread. When either read finds no committed data it calls [awaitConfigForRead] rather than
 * failing — waiting for a refresh in progress, or triggering one on demand when none is (a cold read fetches its
 * own data) — unless the endpoint is [isDisabled] or no app user is known yet.
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

    @Volatile
    private var lastRefreshAttemptAt: Date? = null

    // The app user a cold on-demand read should sync for, rebound by clearCache() under [cacheLock] atomically
    // with the epoch bump, so it can never lag the identity change the way [appUserIDProvider] (backed by the
    // device cache) can. Null until the first identity change; awaitConfigForRead() falls back to the provider
    // for that pre-first-change bootstrap window, where it is accurate and no transition is racing.
    @Volatile
    private var currentAppUserID: String? = null

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
            refreshRemoteConfig(appInBackground, appUserID, staleGated = true)
        }
    }

    fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String) {
        refreshRemoteConfig(appInBackground, appUserID, staleGated = false)
    }

    private fun refreshRemoteConfig(appInBackground: Boolean, appUserID: String, staleGated: Boolean) {
        if (disabled) {
            debugLog { "Remote config is disabled for this session (4xx). Skipping refresh." }
            return
        }
        var requestEpoch = 0
        var requestAppUserID = appUserID
        // Acquire the in-flight guard and capture the epoch together under the lock that also guards
        // clearCache()'s epoch bump + guard release. Otherwise an identity change could slip its bump
        // between getAndSet(true) and epoch.get(), stranding this request with a stale epoch: its
        // callbacks would drop on the mismatch and never release the guard, freezing all future syncs.
        // The user is snapshotted here too, so the (user, epoch) pair the request carries is consistent:
        // any clearCache() that would change the user also bumps the epoch under this same lock, so it
        // either lands fully before this capture (we see its new user) or after (the epoch mismatch drops
        // our response) — never a stale user paired with the post-clear epoch. currentAppUserID (bound by
        // clearCache) wins; the passed appUserID is only the pre-first-identity-change bootstrap fallback.
        // Read only in-memory state under the lock — never appUserIDProvider(), which can re-enter clearCache().
        val shouldRefresh = synchronized(cacheLock) {
            val now = dateProvider.now
            when {
                isRefreshing.get() -> {
                    debugLog { "Remote config refresh already in progress. Skipping." }
                    false
                }
                staleGated && !isRefreshAttemptCooldownElapsed(now) -> {
                    debugLog { "Remote config refresh was attempted recently. Skipping stale-gated refresh." }
                    false
                }
                else -> {
                    if (staleGated) {
                        lastRefreshAttemptAt = now
                    }
                    isRefreshing.set(true)
                    requestEpoch = epoch.get()
                    requestAppUserID = currentAppUserID ?: appUserID
                    refreshCompletion = CompletableDeferred()
                    true
                }
            }
        }
        if (!shouldRefresh) {
            return
        }
        val persisted = diskCache.read()
        val storedBlobs = blobStore.cachedRefs()
        val domain = persisted?.domain ?: DEFAULT_DOMAIN
        logRefreshStart(persisted, appInBackground)
        backend.getRemoteConfig(
            appInBackground = appInBackground,
            appUserID = requestAppUserID,
            domain = domain,
            // Opaque manifest replayed verbatim; null on the first run when nothing is persisted yet.
            manifest = persisted?.manifest,
            // Report only the prefetch blobs we actually hold, so the server stops re-inlining them.
            prefetchedBlobs = persisted?.prefetchBlobs?.filter { storedBlobs.contains(it) } ?: emptyList(),
            onSuccess = { container, _ -> handleMainRefreshSuccess(requestEpoch, persisted, container) },
            onError = { error, behavior ->
                handleMainRefreshError(
                    requestEpoch,
                    appInBackground,
                    domain,
                    hasCachedConfig = persisted != null,
                    error,
                    behavior,
                )
            },
        )
    }

    private fun isRefreshAttemptCooldownElapsed(now: Date): Boolean {
        val lastAttempt = lastRefreshAttemptAt
        return lastAttempt == null || now.time - lastAttempt.time >= REFRESH_ATTEMPT_COOLDOWN.inWholeMilliseconds
    }

    /**
     * Handles a successful **main** `/v1/config` response: a `204` keeps the cache (bookkeeping only), a `200`
     * parses the config element and commits it (on [scope] so [clearCache] can cancel the parse/persist). Drops
     * the result if the epoch changed meanwhile (an identity change already reset the guard via [clearCache]).
     */
    private fun handleMainRefreshSuccess(
        requestEpoch: Int,
        persisted: PersistedRemoteConfigurationState?,
        container: RCContainer?,
    ) {
        if (epoch.get() != requestEpoch) {
            // The cache was cleared (identity change) after this request started. Drop the stale
            // response and leave isRefreshing alone: clearCache() reset it, or a newer refresh owns it.
            return
        }
        if (container == null) {
            handleNotModified(requestEpoch)
            return
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
    }

    /**
     * Handles a failure of the **main** `/v1/config` request. Prefers cached data over the fallback: only when
     * the request fails with a retryable error AND nothing is cached yet (cold start) is the fallback endpoint
     * tried, using the same [domain] the main request used. Any cached config wins (keep it), and a 4xx
     * (`SHOULD_DISABLE`) still disables the endpoint without a fallback.
     */
    private fun handleMainRefreshError(
        requestEpoch: Int,
        appInBackground: Boolean,
        domain: String,
        hasCachedConfig: Boolean,
        error: PurchasesError,
        behavior: GetRemoteConfigErrorHandlingBehavior,
    ) {
        if (behavior == GetRemoteConfigErrorHandlingBehavior.SHOULD_RETRY && !hasCachedConfig) {
            fetchFromFallback(requestEpoch, appInBackground, domain)
        } else {
            handleRefreshError(requestEpoch, error, behavior)
        }
    }

    /**
     * The cold-start fallback: the main `/v1/config` request failed with a retryable error and no config is
     * cached, so fetch the plain-JSON [RemoteConfiguration] from the fallback endpoint (for the same [domain] the
     * main request used) and commit it. There is never persisted state on this path, so the commit passes
     * `previous = null`. The in-flight guard captured by the originating [refreshRemoteConfig] is **kept held**
     * across this call — the fallback continues that same sync/epoch and releases the guard at its own terminal
     * points (success `finally` / `onError`). Does nothing if the epoch changed meanwhile (an identity change
     * already reset the guard via [clearCache]).
     */
    private fun fetchFromFallback(
        requestEpoch: Int,
        appInBackground: Boolean,
        domain: String,
    ) {
        if (epoch.get() != requestEpoch) return
        verboseLog { "Main remote config request failed with no cached config; trying the fallback endpoint." }
        backend.getRemoteConfigFallback(
            appInBackground = appInBackground,
            domain = domain,
            onSuccess = { response, _ ->
                if (epoch.get() != requestEpoch) {
                    // Identity changed after the fallback started; clearCache() reset the guard. Drop the result.
                    return@getRemoteConfigFallback
                }
                scope.launch {
                    try {
                        synchronized(cacheLock) {
                            if (epoch.get() != requestEpoch) return@launch
                            // No persisted state exists on the fallback path (it only runs on a cold start).
                            persist(previous = null, response = response, container = null)
                        }
                    } finally {
                        releaseGuardIfOwned(requestEpoch)
                    }
                }
            },
            onError = { error, behavior -> handleRefreshError(requestEpoch, error, behavior) },
        )
    }

    /** Handles a `204 Not Modified`: nothing changed, so keep the cache and just advance the refresh bookkeeping. */
    private fun handleNotModified(requestEpoch: Int) {
        debugLog { "Remote config unchanged (204 Not Modified)." }
        synchronized(cacheLock) {
            if (epoch.get() == requestEpoch) {
                lastRefreshedAt = dateProvider.now
                lastRefreshAttemptAt = null
                isRefreshing.set(false)
                completeRefresh()
            }
        }
    }

    private fun logRefreshStart(persisted: PersistedRemoteConfigurationState?, appInBackground: Boolean) {
        verboseLog {
            "Refreshing remote config (domain=${persisted?.domain ?: DEFAULT_DOMAIN}, " +
                "manifest present=${persisted?.manifest != null}, appInBackground=$appInBackground)."
        }
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
     * Wipes the cache on an identity change so configuration never bleeds across users (offerings-parity), and
     * rebinds [currentAppUserID] to [appUserID] atomically with the epoch bump. Binding the new identity here —
     * rather than reading it back through [appUserIDProvider] — closes the window where a cold on-demand read
     * could sync for the previous user: the device-cache-backed provider can still return the old user until the
     * caller finishes caching the new one, but the epoch is already bumped, so that stale-user response would not
     * be dropped and would repopulate the freshly wiped cache for the wrong user.
     */
    fun clearCache(appUserID: String) {
        scope.coroutineContext.cancelChildren()
        synchronized(cacheLock) {
            epoch.incrementAndGet()
            currentAppUserID = appUserID
            isRefreshing.set(false)
            lastRefreshedAt = null
            lastRefreshAttemptAt = null
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
        synchronized(cacheLock) {
            isRefreshing.set(false)
            completeRefresh()
        }
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
     * so it wants the un-jittered, prompt request. The user it syncs for is snapshotted atomically with the
     * epoch inside [refreshRemoteConfig] (the identity [clearCache] bound wins); the value resolved here is only
     * the pre-first-identity-change bootstrap and the "is any user known" gate, so a read racing an identity
     * change can never fetch and persist the previous user's config.
     */
    private suspend fun awaitConfigForRead() {
        if (awaitInFlightRefresh()) {
            verboseLog { "Cold remote config read waiting on the refresh already in progress." }
            return
        }
        // Nothing in flight: trigger a sync on demand, unless the endpoint is disabled or no user is known yet.
        // This value is only the bootstrap fallback + the "user known" gate; refreshRemoteConfig re-resolves the
        // effective user under the lock (preferring the clearCache-bound [currentAppUserID]) when it runs.
        val appUserID = (currentAppUserID ?: appUserIDProvider())?.takeIf { it.isNotBlank() }
        if (!disabled && appUserID != null) {
            verboseLog { "Cold remote config read triggering an on-demand sync." }
            refreshRemoteConfig(appInBackground = false, appUserID = appUserID, staleGated = true)
            // Join whatever is now in flight — the sync we just triggered, or one a concurrent caller started.
            awaitInFlightRefresh()
        } else {
            verboseLog {
                "Cold remote config read skipped on-demand sync " +
                    "(disabled=$disabled, user known=${appUserID != null})."
            }
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
        committedTopic(topic)
    }

    /**
     * Like [topic], but additionally waits for the topic's `prefetch`-marked blobs to finish caching before
     * returning: the config request must be committed **and** every item in [topic] flagged `prefetch` must have
     * its referenced blob resolved (already inlined-and-cached, or downloaded now). Inlined blobs are cached
     * synchronously when the topic commits, so this only adds a wait on the **non-inlined** prefetch blobs, which
     * the per-sync prefetch enqueues fire-and-forget at LOW priority.
     *
     * This is best-effort: a prefetch blob that fails to download does not block the return (it stays recoverable
     * on demand / next sync). Returns the committed [ConfigTopic], or `null` when nothing is cached for [topic]
     * even after a refresh, or when the endpoint is [isDisabled] (the 4xx session kill-switch) — in which case the
     * fetcher is never touched. Runs on [ioDispatcher].
     *
     * The blob wait can suspend for a while, so after it the committed topic is re-read: if a [clearCache]
     * (identity change) or a newer sync committed a different topic meanwhile, the snapshot we waited on no longer
     * matches the current user, so the wait is repeated against the fresh topic instead of returning stale data.
     * The re-read goes through [committedTopic], so a wipe that cleared the cache mid-wait self-primes a fresh sync
     * for the new user before comparing. Converges once the committed topic stops changing (it re-reads null → the
     * cache was wiped with nothing re-committed, and returns null).
     */
    suspend fun awaitTopicAndPrefetchBlobsReady(topic: RemoteConfigTopic): ConfigTopic? =
        withContext(ioDispatcher) {
            var committed = committedTopic(topic)
            while (committed != null) {
                // Only the prefetch-marked items matter here; on-demand items are resolved lazily by blobData reads.
                val prefetchRefs = committed.values
                    .filter { it.prefetch }
                    .mapNotNull { it.blobRef }
                if (prefetchRefs.isNotEmpty()) {
                    verboseLog { "Awaiting ${prefetchRefs.size} prefetch blob(s) for topic '${topic.wireName}'." }
                    // Joins/boosts any in-flight LOW-priority prefetch; already-cached (inlined) refs return at once.
                    blobFetcher.ensureDownloaded(prefetchRefs)
                }
                // Re-read: if the committed topic is unchanged the snapshot we waited on is still current, so return
                // it. If it changed under us (identity change wiped it → null, or a newer sync committed a different
                // topic) loop: a null exits with null, a different topic re-awaits its own prefetch blobs.
                val latest = committedTopic(topic)
                if (latest == committed) break
                verboseLog { "Committed '${topic.wireName}' changed during prefetch wait; re-awaiting." }
                committed = latest
            }
            committed
        }

    /**
     * Reads a topic's committed item index, waiting for (or triggering) a refresh once on a miss. Assumes it is
     * already running on [ioDispatcher] (its callers wrap it), so it doesn't switch context itself.
     */
    private suspend fun committedTopic(topic: RemoteConfigTopic): ConfigTopic? {
        if (disabled) {
            verboseLog { "Remote config disabled (4xx); skipping topic read '${topic.wireName}'." }
            return null
        }
        // A committed topic returns immediately; only a miss waits for (or triggers) a refresh, then re-reads.
        val result = topicStore.topic(topic) ?: run {
            awaitConfigForRead()
            topicStore.topic(topic)
        }
        return result.also {
            verboseLog {
                val state = it?.let { committed -> "${committed.size} items" } ?: "not cached"
                "Reading remote config topic '${topic.wireName}': $state."
            }
        }
    }

    /**
     * The resolved blob payload for `itemKey` in [topic], parsed from JSON into [T], or `null` when the item
     * is unknown, has no `blob_ref`, its blob can't be resolved, or its bytes don't deserialize into [T]. [T]
     * must be a concrete `@Serializable` type; parsing uses [JsonTools.json] (not [JsonProvider.defaultJson],
     * whose `classDiscriminator` is overridden for [com.revenuecat.purchases.common.events.BackendEvent] and
     * would break any topic payload relying on the default `type` discriminator, e.g. paywall components). For
     * non-JSON payloads use the `transform` overload, which also documents the resolution and waiting rules.
     */
    suspend inline fun <reified T> blobData(topic: RemoteConfigTopic, itemKey: String): T? =
        blobData(topic, itemKey) { bytes ->
            try {
                JsonTools.json.decodeFromString<T>(bytes.decodeToString())
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
     * Resolves the blobs for every key in [itemKeys] within [topic] **concurrently**, builds a single JSON
     * object mapping **each item key to that item's parsed blob JSON**, and decodes it into a single [T]. Use
     * this to assemble one object from several items in a topic: given items `wf1 -> {"a":...}` and
     * `wf2 -> {"b":...}`, the merged object is `{"wf1": {"a":...}, "wf2": {"b":...}}`, so [T] declares a field
     * per item key.
     *
     * This is **all-or-nothing**: if any requested item is unknown, has no `blob_ref`, or its blob can't be
     * resolved, the call returns `null` (a partial object is never produced) and warn-logs the missing keys.
     * It also returns `null` if any resolved blob isn't valid JSON, or the merged object doesn't deserialize
     * into [T]. Duplicate keys are de-duplicated; an empty [itemKeys] returns `null` without any read; [T] must
     * be a concrete `@Serializable` type and parsing uses [JsonTools.json] (see [blobData] for why not
     * [JsonProvider.defaultJson]).
     *
     * Each item resolves through the same path as the single-key [blobData] (see its KDoc for the `blob_ref`,
     * on-demand fetch, and waiting rules) — this only fans them out. The fan-out is safe: a shared in-flight
     * `/v1/config` refresh is deduped across all keys, and the blob fetcher dedupes concurrent downloads of the
     * same ref. When the endpoint is [isDisabled] (the 4xx session kill-switch) the call returns `null`
     * immediately without any read. Runs on [ioDispatcher].
     */
    suspend inline fun <reified T> mergeItemsBlobData(topic: RemoteConfigTopic, itemKeys: Collection<String>): T? =
        mergeItemsBlobData(topic, itemKeys) { merged ->
            try {
                JsonTools.json.decodeFromJsonElement<T>(merged)
            } catch (e: SerializationException) {
                errorLog(e) { "Failed to decode merged remote config blobs from topic '${topic.wireName}' as JSON." }
                null
            }
        }

    /**
     * Resolves + merges the [itemKeys] blobs (see the reified [mergeItemsBlobData] for the merge shape and rules)
     * and maps the resulting keyed JSON object through [transform] — resolution, merge, and [transform] all run
     * on [ioDispatcher] so JSON decoding never runs on the caller's thread. Returns `null` when the merged
     * object can't be built (any item unresolvable or non-JSON). The non-inline worker behind the reified
     * overload; kept non-`private` so the `inline` function can call it.
     */
    suspend fun <T> mergeItemsBlobData(
        topic: RemoteConfigTopic,
        itemKeys: Collection<String>,
        transform: (JsonObject) -> T?,
    ): T? = withContext(ioDispatcher) {
        mergedBlobObject(topic, itemKeys)?.let(transform)
    }

    /**
     * Resolves every item in [itemKeys] concurrently and builds a JSON object keyed by item key, each mapping
     * to that item's parsed blob JSON, or `null` if any item can't be resolved or any resolved blob isn't valid
     * JSON. Assumes it is already running on [ioDispatcher] (its only caller wraps it), so it doesn't switch
     * context itself.
     */
    @Suppress("ReturnCount")
    private suspend fun mergedBlobObject(topic: RemoteConfigTopic, itemKeys: Collection<String>): JsonObject? {
        if (disabled) {
            verboseLog { "Remote config disabled (4xx); skipping merged read for topic '${topic.wireName}'." }
            return null
        }
        val keys = itemKeys.distinct()
        if (keys.isEmpty()) {
            verboseLog { "No item keys requested for merged remote config read in topic '${topic.wireName}'." }
            return null
        }
        val resolved = coroutineScope {
            keys.associateWith { key -> async { resolveBlobBytes(topic, key) } }
                .mapValues { (_, deferred) -> deferred.await() }
        }
        val missing = resolved.filterValues { it == null }.keys
        if (missing.isNotEmpty()) {
            warnLog {
                "Could not resolve remote config blob(s) for ${missing.size} of ${resolved.size} " +
                    "requested item(s) in topic '${topic.wireName}': $missing. Returning null."
            }
            return null
        }
        val merged = LinkedHashMap<String, JsonElement>()
        for (key in keys) {
            val element = try {
                JsonProvider.defaultJson.parseToJsonElement(resolved.getValue(key)!!.decodeToString())
            } catch (e: SerializationException) {
                errorLog(e) { "Remote config blob for item '$key' in topic '${topic.wireName}' is not valid JSON." }
                return null
            }
            // Nest each item's blob JSON under its item key.
            merged[key] = element
        }
        return JsonObject(merged)
    }

    /**
     * Resolves an item's referenced-blob bytes, or `null` when the endpoint is [isDisabled], the item is
     * unknown, or it has no `blob_ref`.
     */
    private suspend fun resolveBlobBytes(topic: RemoteConfigTopic, itemKey: String): ByteArray? {
        if (disabled) {
            verboseLog { "Remote config disabled (4xx); skipping read of item '$itemKey'." }
            return null
        }
        verboseLog { "Reading remote config blob (topic='${topic.wireName}', item='$itemKey')." }
        val ref = committedItem(topic, itemKey)?.blobRef
        return when {
            ref == null -> {
                verboseLog { "Remote config item '$itemKey' is missing or has no blob ref; returning null." }
                null
            }
            blobFetcher.ensureDownloaded(ref) -> {
                blobStore.read(ref).also { bytes ->
                    if (bytes != null) {
                        verboseLog { "Resolved '$itemKey' from remote config blob '$ref' (${bytes.size} bytes)." }
                    } else {
                        warnLog { "Remote config blob '$ref' for item '$itemKey' downloaded but read back null." }
                    }
                }
            }
            else -> {
                warnLog { "Failed to resolve remote config blob '$ref' for item '$itemKey'." }
                null
            }
        }
    }

    /** The committed item for [itemKey], waiting for or triggering a sync once when it is not cached yet. */
    private suspend fun committedItem(topic: RemoteConfigTopic, itemKey: String): RemoteConfiguration.ConfigItem? {
        topicStore.topic(topic)?.get(itemKey)?.let { return it }
        verboseLog { "Remote config item '$itemKey' not committed yet; awaiting config." }
        awaitConfigForRead()
        return topicStore.topic(topic)?.get(itemKey).also {
            if (it == null) verboseLog { "Remote config item '$itemKey' not found in topic '${topic.wireName}'." }
        }
    }

    private fun persist(
        previous: PersistedRemoteConfigurationState?,
        response: RemoteConfiguration,
        // Null on the fallback path (plain-JSON response with no inlined blob elements to extract); the wanted
        // blobs are then fetched over the network by prefetchBlobs instead.
        container: RCContainer?,
    ) {
        debugLog {
            val changed = response.topics.entries.joinToString { (name, topic) ->
                "$name -> items=${topic.keys}"
            }
            "Received remote config: active topics=${response.activeTopics}; changed topics: " +
                "[${changed.ifEmpty { "none" }}]."
        }
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
            lastRefreshAttemptAt = null
            debugLog {
                "Persisted remote config (domain=${response.domain}, ${response.activeTopics.size} active topics, " +
                    "${blobRefsToKeep.size} blobs wanted)."
            }
            container?.let { extractInlineBlobs(it, blobRefsToKeep) }
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
        val refs = buildList {
            addAll(response.prefetchBlobs)
            mergedTopics.values.forEach { topic ->
                topic.values.forEach { item -> if (item.prefetch) item.blobRef?.let(::add) }
            }
        }.distinct()
        val toPrefetch = refs.filterNot { blobStore.contains(it) }
        verboseLog { "Prefetching ${toPrefetch.size} remote config blob(s)." }
        blobFetcher.prefetch(toPrefetch)
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
                val size = decoded.remaining()
                // write() logs its own error on failure; only report success when it actually stored the blob.
                if (blobStore.write(ref, decoded)) {
                    verboseLog { "Stored inlined remote config blob '$ref' ($size bytes)." }
                }
            } else {
                errorLog { "Skipping remote config blob '$ref': checksum verification failed." }
            }
        }
    }

    private companion object {
        private const val DEFAULT_DOMAIN = "app"
        private val REFRESH_ATTEMPT_COOLDOWN = 1.minutes
    }
}

/** The blob refs each topic's items reference, keyed by topic name (empty list for inline-only topics). */
internal fun Map<String, ConfigTopic>.toTopicBlobRefs(): Map<String, List<String>> =
    mapValues { (_, topic) -> topic.values.mapNotNull { it.blobRef } }
