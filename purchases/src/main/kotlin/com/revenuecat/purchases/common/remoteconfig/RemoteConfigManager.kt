package com.revenuecat.purchases.common.remoteconfig

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.between
import com.revenuecat.purchases.common.caching.cacheDuration
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.networking.RCContainer
import com.revenuecat.purchases.common.networking.RCContainerFormatException
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDomainFetcher.DomainFetchResult
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigDomainFetcher.FetchOutcome
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Orchestrates `/v1/config` syncs over the domain tree: a sync pass fetches the root domain, unfolds the
 * subdomains each response declares ([RemoteConfiguration.subdomains]), and syncs every domain the same way —
 * a `204` keeps that domain's cached entry untouched, a `200` persists its fresh server manifest plus its full
 * per-topic item index. The persisted configuration (incl. each item's inline content) is the source of truth,
 * and persisting it is a domain's **entire** commit, advancing that domain's manifest unconditionally. Commits
 * run post-order — children before their parent, the root last — so a parent's topic/subdomain deletions never
 * land before the subdomains that may have absorbed the moved data; consumers read the parent-wins merged view
 * across the tree ([PersistedRemoteConfigurationState.mergedTopics]), so a topic can move between domains
 * without consumers noticing. Each commit writes the inlined blobs its config still wants; once per pass, gated
 * on at least one successful commit, the blob store is pruned against every persisted domain's live refs and
 * the remaining wanted blobs are best-effort prefetched over the network ([RemoteConfigBlobFetcher], resolving
 * blob source URLs through [RemoteConfigSourceProvider]); a missing or un-parseable blob is recoverable later
 * (re-fetched next sync / on demand) and never blocks a commit. Each domain's request reports which of its
 * prefetch blobs are now cached locally. (Live API base-URL rerouting from the `sources` topic is out of scope
 * — a future phase.)
 *
 * Manifests are opaque (stored and replayed verbatim) and domain-scoped; the active-topic set and removed-topic
 * detection come from each response's [RemoteConfiguration.activeTopics]. The manager is topic-agnostic: it
 * never interprets item shapes or branches on topic name — consumer topics are read lazily by providers through
 * the manager.
 *
 * The pass runs on [scope]: persistence is synchronous, but the launch lets [clearCache] cancel the
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
 * failing — waiting for a refresh in progress, or triggering one on demand when none is and the committed
 * configuration has aged past the refresh cadence (a cold read fetches its own data; a read against freshly
 * committed configuration treats absence as the server's answer) — unless the endpoint is [isDisabled] or no app
 * user is known yet.
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
    private val topicStore: RemoteConfigTopicStore,
    private val sourceProvider: RemoteConfigSourceProvider,
    private val blobFetcher: RemoteConfigBlobFetcher,
    private val appUserIDProvider: () -> String? = { null },
    private val cacheDurationProvider: (Boolean) -> Duration = ::cacheDuration,
) {
    private val isRefreshing = AtomicBoolean(false)

    // The suspend bridge to the callback-based backend endpoints. The 4xx hook runs at callback time so a root
    // 4xx disables the session even when the requesting pass was cancelled meanwhile.
    private val domainFetcher = RemoteConfigDomainFetcher(backend, diskCache, blobStore, ::disableForSession)

    // Session kill-switches for individual subdomains that answered 4xx: their fetch is skipped (last-good
    // persisted data keeps serving) until app restart, an identity change, or the domain dropping out of the
    // tree. Unlike the root [disabled] flag, [clearCache] DOES clear these: subdomain trees are config- and
    // user-derived, and a stale disable would outlive the last-good state the wipe destroys, turning into a
    // session-long block of the parent's commits for the new user. Guarded by [cacheLock].
    private val disabledDomains = mutableSetOf<String>()

    // Bumped by clearCache() on every identity change. A request captures the epoch when it starts; once it
    // changes, the in-flight request's callbacks drop their result (the /v1/config request itself cannot be
    // socket-cancelled), so an old user's response can never persist over the wiped cache.
    private val epoch = AtomicInteger(0)

    // Serializes a sync's "re-check epoch + persist" against clearCache()'s "bump epoch + wipe". commitDomain()
    // and finalizePass() are synchronous, so cancellation can't interrupt them; this lock makes the critical
    // sections atomic so a late commit either runs fully before the wipe or sees the bumped epoch and skips —
    // never writes after it.
    private val cacheLock = Any()

    @Volatile
    private var lastRefreshedAt: Date? = null

    @Volatile
    private var lastRefreshAttemptAt: Date? = null

    // Forces requests to report AppStart until the session's first config is committed, regardless of the caller's
    // context, so the backend always sees app_start on a fresh app open. Guarded by [cacheLock]; set only once config
    // is durably committed (persisted from a 200 or the fallback) or confirmed current (204), so a failed request or
    // an undecodable/unpersistable 200 keeps forcing AppStart until a later attempt actually commits config.
    private var hasCommittedInitialConfig = false

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

    // A monotonic commit token, bumped on every mutation of the committed state: a successful persist(), an
    // identity-change clearCache(), and the 4xx disable. Handed to listeners so an async in-memory warm can
    // store-if-newer and never clobber a fresher commit (see RemoteConfigCommitListener). Distinct from `epoch`,
    // which only advances on identity change and can't order a disk warm against an ordinary version bump.
    private val generation = AtomicInteger(0)

    /** The current commit generation; see [generation]. */
    val configGeneration: Int
        get() = generation.get()

    private val listeners = CopyOnWriteArrayList<RemoteConfigCommitListener>()

    /** Registers a [RemoteConfigCommitListener]; safe to call at construction/wiring time. */
    fun registerListener(listener: RemoteConfigCommitListener) {
        listeners.add(listener)
    }

    fun refreshRemoteConfigIfStale(
        appInBackground: Boolean,
        appUserID: String,
        fetchContext: RemoteConfigFetchContext,
    ) {
        if (lastRefreshedAt.isCacheStale(appInBackground, dateProvider, cacheDurationProvider)) {
            refreshRemoteConfig(appInBackground, appUserID, fetchContext, staleGated = true)
        } else {
            verboseLog {
                "Committed remote configuration is still fresh; skipping the ${fetchContext.wireName} refresh."
            }
        }
    }

    fun refreshRemoteConfig(
        appInBackground: Boolean,
        appUserID: String,
        fetchContext: RemoteConfigFetchContext,
    ) {
        refreshRemoteConfig(appInBackground, appUserID, fetchContext, staleGated = false)
    }

    private fun refreshRemoteConfig(
        appInBackground: Boolean,
        appUserID: String,
        fetchContext: RemoteConfigFetchContext,
        staleGated: Boolean,
    ) {
        if (disabled) {
            debugLog { "Remote config is disabled for this session (4xx). Skipping refresh." }
            return
        }
        var requestEpoch = 0
        var requestAppUserID = appUserID
        var requestFetchContext = fetchContext
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
                    requestFetchContext = fetchContextForRequest(fetchContext)
                    refreshCompletion = CompletableDeferred()
                    true
                }
            }
        }
        if (!shouldRefresh) {
            return
        }
        // The whole tree pass runs on [scope] so clearCache() can cancel it, and the guard is released at the
        // pass's single terminal point — reads waiting on [refreshCompletion] wake when the full tree settles.
        scope.launch {
            try {
                runTreePass(appInBackground, requestEpoch, requestAppUserID, requestFetchContext)
            } finally {
                releaseGuardIfOwned(requestEpoch)
            }
        }
    }

    /**
     * One sync pass over the domain tree for a refresh that already owns the in-flight guard: fetch the root,
     * unfold and sync the subdomains each response declares, and commit post-order (children before their
     * parent) so a parent's topic/subdomain deletions never land before the domains that may have absorbed the
     * moved data. Per-domain commits are individually atomic writes of the one persisted tree; the pass-wide
     * work (batched 204 refresh times, blob retention, the generation bump, and the staleness bookkeeping) runs
     * once in [finalizePass].
     */
    private suspend fun runTreePass(
        appInBackground: Boolean,
        requestEpoch: Int,
        requestAppUserID: String,
        requestFetchContext: RemoteConfigFetchContext,
    ) {
        val rootDomain = diskCache.read()?.rootDomain ?: DEFAULT_DOMAIN
        val ctx = TreePassContext(requestEpoch, appInBackground, requestAppUserID, requestFetchContext, rootDomain)
        syncDomainTree(ctx, rootDomain, depth = 0, isRoot = true)
        finalizePass(ctx)
    }

    /**
     * Syncs [domain] and, recursively, the subdomains its response declares, committing the children before the
     * domain itself. Returns the domain's terminal outcome for this pass. A domain already synced this pass
     * returns its recorded outcome without a second fetch; a cycle (a domain re-listed inside its own subtree)
     * and a domain beyond [PersistedRemoteConfigurationState.MAX_SUBDOMAIN_DEPTH] are skipped, which counts as
     * fresh — stalling commits on a malformed server tree would freeze configuration updates indefinitely.
     */
    private suspend fun syncDomainTree(
        ctx: TreePassContext,
        domain: String,
        depth: Int,
        isRoot: Boolean,
    ): DomainSyncOutcome {
        val shortCircuit = shortCircuitOutcome(ctx, domain, depth)
        if (shortCircuit != null) return shortCircuit
        ctx.inProgress += domain
        return try {
            val fetch = domainFetcher.fetch(domain, isRoot, ctx.appInBackground, ctx.appUserID, ctx.fetchContext)
            val outcome = when (fetch.outcome) {
                // The root's 4xx side effect (the session kill-switch) already ran at callback time.
                FetchOutcome.ClientError ->
                    if (isRoot) DomainSyncOutcome.Failed else subdomainClientErrorOutcome(domain)
                FetchOutcome.FailedRetryable ->
                    if (isRoot) rootRetryableFailureOutcome(ctx, domain) else DomainSyncOutcome.Failed
                FetchOutcome.Success -> syncFetchedDomain(ctx, domain, depth, isRoot, fetch)
            }
            ctx.outcomes[domain] = outcome
            outcome
        } finally {
            ctx.inProgress -= domain
        }
    }

    /**
     * The outcomes that resolve without a fetch: already synced this pass, a cycle, beyond the depth cap, or
     * disabled for the session after an earlier 4xx.
     */
    private fun shortCircuitOutcome(ctx: TreePassContext, domain: String, depth: Int): DomainSyncOutcome? = when {
        domain in ctx.outcomes -> ctx.outcomes.getValue(domain)
        domain in ctx.inProgress -> {
            warnLog { "Remote config domain '$domain' is re-listed inside its own subtree; ignoring the cycle." }
            DomainSyncOutcome.Skipped
        }
        depth > PersistedRemoteConfigurationState.MAX_SUBDOMAIN_DEPTH -> {
            warnLog {
                "Remote config domain '$domain' is deeper than the supported subdomain depth " +
                    "(${PersistedRemoteConfigurationState.MAX_SUBDOMAIN_DEPTH}); skipping it."
            }
            DomainSyncOutcome.Skipped
        }
        isSessionDisabled(domain) -> {
            verboseLog { "Remote config domain '$domain' is disabled for this session (4xx); skipping its fetch." }
            disabledSubdomainOutcome(domain)
        }
        else -> null
    }

    private fun isSessionDisabled(domain: String): Boolean = synchronized(cacheLock) { domain in disabledDomains }

    /**
     * A subdomain answered 4xx: disable it for the session and keep serving its last-good persisted data —
     * fresh for the commit rule, so the rest of the tree keeps updating. The disable clears on app restart, on
     * an identity change ([clearCache]), or when the domain drops out of the tree (prune), so a later re-add
     * starts over.
     */
    private fun subdomainClientErrorOutcome(domain: String): DomainSyncOutcome {
        synchronized(cacheLock) { disabledDomains += domain }
        return disabledSubdomainOutcome(domain)
    }

    /**
     * Without last-good data a refused subdomain fails instead of being skipped, deferring its parent's commit:
     * committing would vanish any topic that migrated into the refused domain. The block self-heals — the
     * commit rule evaluates each pass's fresh parent response, so it ends as soon as the server stops listing
     * the refused domain.
     */
    private fun disabledSubdomainOutcome(domain: String): DomainSyncOutcome =
        if (diskCache.read()?.domains?.get(domain) != null) {
            DomainSyncOutcome.DisabledLastGood
        } else {
            log(LogIntent.RC_ERROR) {
                "Remote config subdomain '$domain' is refused (4xx) and has no cached data; deferring its " +
                    "parent's updates until the server stops listing it."
            }
            DomainSyncOutcome.Failed
        }

    private suspend fun syncFetchedDomain(
        ctx: TreePassContext,
        domain: String,
        depth: Int,
        isRoot: Boolean,
        fetch: DomainFetchResult,
    ): DomainSyncOutcome {
        val container = fetch.container
            ?: return domainNotModifiedOutcome(ctx, domain, depth, isRoot, fetch.requestDate)
        val response = parseConfigResponse(container)
        return when {
            response == null -> DomainSyncOutcome.Failed
            !syncSubdomains(ctx, response.subdomains, depth) -> {
                warnLog { "Not committing remote config domain '$domain': part of its subtree failed to sync." }
                DomainSyncOutcome.Failed
            }
            commitDomain(ctx, domain, isRoot, response, container, fetch.requestDate) -> DomainSyncOutcome.Committed
            else -> DomainSyncOutcome.Failed
        }
    }

    private fun parseConfigResponse(container: RCContainer): RemoteConfiguration? = try {
        RemoteConfiguration.parse(container.config)
    } catch (e: SerializationException) {
        errorLog(e) { "Failed to parse remote config response. Keeping the cached configuration." }
        null
    } catch (e: RCContainerFormatException) {
        errorLog(e) { "Failed to decode remote config response. Keeping the cached configuration." }
        null
    }

    /**
     * Syncs a domain's listed subdomains before its own commit: the commit applies its response's
     * topic/subdomain deletions, which must not land before the subdomains that may now carry the moved data
     * have synced. Every child is synced even when a sibling fails (a successful child's commit is additive and
     * safe); a failure only defers the parent's commit — old state and manifest are kept, so the next pass
     * re-diffs cheaply. Returns whether every child ended the pass fresh.
     */
    private suspend fun syncSubdomains(ctx: TreePassContext, subdomains: List<String>, depth: Int): Boolean =
        subdomains
            .map { child -> syncDomainTree(ctx, child, depth + 1, isRoot = false) }
            .all { it.isFresh }

    /**
     * Handles a domain's `204 Not Modified`: its cached entry is confirmed current, so record the confirmed
     * refresh time for the pass-end batch write — but still recurse, because a domain's 204 says nothing about
     * its subtree's freshness. Without a persisted entry there is nothing the 204 can confirm or update (a 204
     * must never resurrect state that was wiped meanwhile): the root still counts as unchanged — the pass
     * bookkeeping advances like any confirmed-current sync — but a child fails, because a missing child entry
     * is exactly the gap its parent's commit must not paper over.
     */
    private suspend fun domainNotModifiedOutcome(
        ctx: TreePassContext,
        domain: String,
        depth: Int,
        isRoot: Boolean,
        requestDate: Date?,
    ): DomainSyncOutcome {
        debugLog { "Remote config unchanged (204 Not Modified)." }
        val persistedDomain = diskCache.read()?.domains?.get(domain)
        if (persistedDomain == null) {
            warnLog { "Remote config domain '$domain' returned 204 but nothing is persisted for it." }
            return if (isRoot) DomainSyncOutcome.Unchanged else DomainSyncOutcome.Failed
        }
        // Only the server's own time is worth recording; a response without it carries the old value forward.
        requestDate?.let { ctx.pending204RefreshTimes[domain] = it.time }
        val childrenFresh = syncSubdomains(ctx, persistedDomain.subdomains, depth)
        return if (childrenFresh) DomainSyncOutcome.Unchanged else DomainSyncOutcome.Failed
    }

    /**
     * A retryable root failure prefers cached data over the fallback: only a cold start (nothing persisted for
     * the root) tries the fallback endpoint, and only for the root — the fallback host is the emergency,
     * least-capable endpoint, so its load is not multiplied by a tree of cold requests. A fallback-committed
     * root still persists its subdomains list, so the next regular pass completes the tree once the main API
     * recovers.
     */
    private suspend fun rootRetryableFailureOutcome(ctx: TreePassContext, domain: String): DomainSyncOutcome {
        if (diskCache.read()?.domains?.get(domain) != null) return DomainSyncOutcome.Failed
        verboseLog { "Main remote config request failed with no cached config; trying the fallback endpoint." }
        val response = domainFetcher.fetchFallback(ctx.appInBackground, domain)
        // No persisted state exists on this path, and the fallback host's request time is not borrowed as the
        // refresh time: the value is only meaningful to the endpoint that issued it.
        return when {
            response == null -> DomainSyncOutcome.Failed
            commitDomain(ctx, domain, isRoot = true, response, container = null, requestDate = null) ->
                DomainSyncOutcome.Committed
            else -> DomainSyncOutcome.Failed
        }
    }

    /**
     * The root's 4xx session kill-switch. Applied regardless of epoch ownership (a late response for an old
     * identity is still a valid signal that the endpoint refuses this app's requests). Reads now return null,
     * so in-memory caches are dropped too.
     */
    private fun disableForSession(error: PurchasesError) {
        if (!disabled) {
            disabled = true
            val invalidatedGeneration = generation.incrementAndGet()
            listeners.forEach { it.onConfigInvalidated(invalidatedGeneration) }
            // Distinct one-shot signal (guarded by !disabled): lets consumers refetch offerings so paywall
            // components — skipped while the endpoint was live — get decoded for the fallback render path.
            listeners.forEach { it.onRemoteConfigDisabled(invalidatedGeneration) }
        }
        log(LogIntent.RC_ERROR) {
            "Disabling remote config for this session after receiving a 4xx response. Error: $error"
        }
    }

    private fun isRefreshAttemptCooldownElapsed(now: Date): Boolean {
        val lastAttempt = lastRefreshAttemptAt
        return lastAttempt == null || Duration.between(lastAttempt, now) >= REFRESH_ATTEMPT_COOLDOWN
    }

    // Overrides a request's context to AppStart until the session's first config is committed. Must be called within
    // [cacheLock].
    private fun fetchContextForRequest(requested: RemoteConfigFetchContext): RemoteConfigFetchContext {
        return if (hasCommittedInitialConfig) requested else RemoteConfigFetchContext.AppStart
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
            // identity change. It clears only on app restart. The per-subdomain disables DO clear (see
            // [disabledDomains]): they are config/user-derived and their last-good data is being wiped.
            disabledDomains.clear()
            diskCache.clear()
            blobStore.clear()
            sourceProvider.clear()
            // Identity change wiped the committed state: advance the generation and tell listeners to drop
            // their in-memory caches. A warm started for an older generation is rejected by store-if-newer.
            val invalidatedGeneration = generation.incrementAndGet()
            listeners.forEach { it.onConfigInvalidated(invalidatedGeneration) }
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
     *   returning `null` — **unless** the committed configuration is still fresh, the endpoint is [isDisabled]
     *   (the 4xx session kill-switch), or no app user is known yet, in which case it gives up without a network
     *   call.
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
            verboseLog { "Cold remote config read requesting an on-demand sync." }
            refreshRemoteConfigIfStale(
                appInBackground = false,
                appUserID = appUserID,
                fetchContext = RemoteConfigFetchContext.Read,
            )
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
     * triggers one on demand, subject to its freshness gate — and then re-reads before giving up, so a read
     * during the initial sync (or before any sync) returns fresh data instead of `null`, mirroring [blobData]. A
     * committed topic returns immediately, never delayed by an unrelated in-flight refresh. Use [blobData] for a
     * resolved item payload that also resolves the referenced blob. Reads disk on [ioDispatcher].
     */
    suspend fun topic(topic: RemoteConfigTopic): ConfigTopic? = withContext(ioDispatcher) {
        committedTopic(topic)
    }

    /**
     * A topic's already-committed item index, or `null` when nothing is committed for [topic] yet or the
     * endpoint is [isDisabled]. Unlike [topic], this **never** waits for or triggers a `/v1/config` sync: it is
     * a pure read of whatever is currently committed (in-memory snapshot / disk). Used by cache-warming, which
     * must not kick off a network fetch (e.g. on a cold-disk SDK init, before any user is known). Once a topic
     * is committed, subsequent [blobData]/[mergeItemsBlobData] reads for its items also resolve without
     * triggering a sync. Reads disk on [ioDispatcher].
     */
    suspend fun committedTopicOrNull(topic: RemoteConfigTopic): ConfigTopic? = withContext(ioDispatcher) {
        if (disabled) null else topicStore.topic(topic)
    }

    /**
     * Waits for the refresh that is currently in flight, if any, then returns the latest committed [topic].
     * Unlike [topic], this never starts a refresh. This lets a consumer avoid treating a stale-but-committed
     * empty topic as authoritative while AppStart is already fetching a newer configuration.
     */
    suspend fun committedTopicAfterInFlightRefresh(topic: RemoteConfigTopic): ConfigTopic? =
        withContext(ioDispatcher) {
            awaitInFlightRefresh()
            if (disabled) null else topicStore.topic(topic)
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
     * triggers one on demand, subject to its freshness gate — and then re-reads before giving up, so a read
     * during the initial sync (or before any sync) returns fresh data instead of `null`. A committed item returns
     * immediately, never delayed by an unrelated in-flight refresh.
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

    /**
     * Commits one domain's `200` response into its entry of the persisted tree. Persisting the updated tree IS
     * this domain's commit — the full topic index plus the manifest the server diffs against is the source of
     * truth, and the manifest advances unconditionally with the write. Inline blobs are recoverable over the
     * network, so the blob store is only touched once the state is durably committed (a failed persist never
     * orphans blobs); blob retention and the generation bump are pass-wide and deferred to [finalizePass].
     *
     * Runs under [cacheLock] with an epoch re-check, so a commit racing [clearCache] either lands fully before
     * the wipe or sees the bumped epoch and skips. Returns whether the state was durably persisted.
     *
     * [container] is null on the fallback path (plain-JSON response with no inlined blob elements to extract);
     * the wanted blobs are then fetched over the network by [prefetchBlobs] instead. [requestDate] is the
     * server's own request time — never a device-clock value — and is null when the response carried no such
     * header (the previous value carries forward), or on the fallback path.
     */
    private fun commitDomain(
        ctx: TreePassContext,
        requestedDomain: String,
        isRoot: Boolean,
        response: RemoteConfiguration,
        container: RCContainer?,
        requestDate: Date?,
    ): Boolean = synchronized(cacheLock) {
        if (epoch.get() != ctx.requestEpoch) return false
        // Children stay keyed by the name their parent listed — the linkage the parent's subdomains list is
        // resolved against. Only the root follows the response's echoed domain.
        if (!isRoot && response.domain != requestedDomain) {
            warnLog {
                "Remote config response for domain '$requestedDomain' echoed domain '${response.domain}'; " +
                    "keeping the requested name."
            }
        }
        val domainKey = if (isRoot) response.domain else requestedDomain
        debugLog {
            val changed = response.topics.entries.joinToString { (name, topic) ->
                "$name -> items=${topic.keys}"
            }
            "Received remote config: active topics=${response.activeTopics}; changed topics: " +
                "[${changed.ifEmpty { "none" }}]."
        }
        val previousState = diskCache.read()
        val previousDomainState = previousState?.domains?.get(domainKey)
        // Changed topics (present in the response) overwrite their item index; unchanged active topics keep their
        // carried-forward index (the server omits them); topics no longer active are pruned.
        val mergedTopics = ((previousDomainState?.topics ?: emptyMap()) + response.topics)
            .filterKeys { it in response.activeTopics }
        // Blobs this domain's config still wants: the prefetch set plus any active-topic blob ref.
        val domainBlobRefs = response.prefetchBlobs.toSet() + mergedTopics.toTopicBlobRefs().values.flatten()
        val newState = previousState.withCommittedDomain(
            domainKey = domainKey,
            isRoot = isRoot,
            fallbackRootDomain = ctx.rootDomainName,
            response = response,
            mergedTopics = mergedTopics,
            // Server time only: a response without it carries the last server-supplied value forward rather
            // than substituting a device-clock reading the server cannot compare against its own.
            lastRefreshTime = requestDate?.time ?: previousDomainState?.lastRefreshTime,
        )
        val persisted = diskCache.write(newState)

        if (persisted) {
            ctx.committedCount++
            ctx.lastPersistedState = newState
            debugLog {
                "Persisted remote config (domain=$domainKey, ${response.activeTopics.size} active topics, " +
                    "${domainBlobRefs.size} blobs wanted)."
            }
            container?.let { extractInlineBlobs(it, domainBlobRefs) }
            prefetchBlobs(response, mergedTopics)
        } else {
            errorLog { "Skipping remote config blob sync: failed to persist the configuration." }
        }
        persisted
    }

    /**
     * The pass-wide epilogue, run once after the tree settles, under [cacheLock] with an epoch re-check:
     * 1. Writes the 204-confirmed refresh times in one batch (skipped when nothing was confirmed).
     * 2. Prunes the blob store against [PersistedRemoteConfigurationState.liveBlobRefs] — the union across
     *    every persisted domain, never a single domain's set — and advances the generation once for the whole
     *    pass, so listeners re-warm from a tree-consistent state. Both only when this pass committed something,
     *    mirroring the "blob work only after a successful persist" gating.
     * 3. Advances the staleness bookkeeping only when every synced domain ended fresh: a partial pass leaves the
     *    cache stale so the next trigger retries the tree (already-committed domains answer with cheap 204s,
     *    and the attempt cooldown keeps retries from hammering).
     */
    private fun finalizePass(ctx: TreePassContext) {
        synchronized(cacheLock) {
            if (epoch.get() != ctx.requestEpoch) return
            // The last commit's write is the current tree; falling back to a read covers all-204 passes.
            var state = ctx.lastPersistedState ?: diskCache.read()
            if (state != null && ctx.pending204RefreshTimes.isNotEmpty()) {
                val updated = state.copy(
                    domains = state.domains.mapValues { (name, domainState) ->
                        ctx.pending204RefreshTimes[name]
                            ?.let { domainState.copy(lastRefreshTime = it) }
                            ?: domainState
                    },
                )
                if (diskCache.write(updated)) state = updated
            }
            if (ctx.allFresh) {
                state = state?.let { pruneUnreachableDomains(it) }
                // The staleness gate measures elapsed *local* time (isCacheStale subtracts from
                // dateProvider.now), so it stays on the device clock, unlike the persisted server refresh times.
                lastRefreshedAt = dateProvider.now
                lastRefreshAttemptAt = null
                hasCommittedInitialConfig = true
            }
            if (ctx.committedCount > 0) {
                state?.let { blobStore.retainOnly(it.liveBlobRefs) }
                val committedGeneration = generation.incrementAndGet()
                listeners.forEach { it.onConfigCommitted(committedGeneration) }
            }
        }
    }

    /**
     * Drops domain entries no longer reachable from the root, clearing any session disable with them so a
     * later re-add starts over. Runs only after a fully-fresh pass: after a partial pass a freshly-committed
     * child can be unreachable merely because the parent that links it deferred its commit, and after a crashed
     * first pass children can be persisted before any root state exists — pruning in either state would discard
     * fresh data the next pass reuses. Called within [cacheLock]; falls back to the unpruned state on a failed
     * write (the entries stay inert for reads and merely pin their blobs until a later pass prunes them).
     */
    private fun pruneUnreachableDomains(
        state: PersistedRemoteConfigurationState,
    ): PersistedRemoteConfigurationState {
        val unreachable = if (state.rootState == null) {
            emptySet()
        } else {
            state.domains.keys - state.domainsInPrecedenceOrder.toSet()
        }
        if (unreachable.isEmpty()) return state
        debugLog { "Pruning remote config domain(s) no longer part of the tree: $unreachable." }
        disabledDomains -= unreachable
        val pruned = state.copy(domains = state.domains - unreachable)
        return if (diskCache.write(pruned)) pruned else state
    }

    /**
     * Best-effort, topic-agnostic warm of the blobs the committed config wants prefetched: the server's
     * [RemoteConfiguration.prefetchBlobs] plus any item flagged `prefetch`. Re-arms the blob source provider
     * first **only if a prior cycle exhausted its sources** (otherwise failover progress is kept, so a
     * known-bad higher-priority source isn't re-tried every sync), then hands the not-yet-cached refs to the
     * fetcher's LOW-priority queue. Runs on the manager's IO scope (inside [commitDomain]), so it never blocks the
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
        // Decide by ref before decoding so a blob we don't need (not referenced, or already cached) is never
        // decompressed, and decode one at a time so the whole uncompressed payload is never held at once —
        // inline blobs can be large.
        container.contentElements.forEach { element ->
            val ref = element.checksumBase64()
            if (ref !in refsToKeep || blobStore.contains(ref)) return@forEach
            val decoded = try {
                element.decode()
            } catch (e: RCContainerFormatException) {
                errorLog(e) { "Skipping remote config blob '$ref': could not decode or verify its content." }
                return@forEach
            }
            // write() logs its own error on failure; only report success when it actually stored the blob.
            if (blobStore.write(ref, decoded)) {
                verboseLog { "Stored inlined remote config blob '$ref' (${decoded.size} bytes)." }
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
