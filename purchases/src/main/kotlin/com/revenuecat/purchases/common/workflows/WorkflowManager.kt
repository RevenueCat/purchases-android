@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.GetWorkflowsErrorHandlingBehavior
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Suppress("TooManyFunctions")
internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val workflowsCache: WorkflowsCache,
    // Detail fetches in the prefetch path run here so they fan out instead of serializing on the
    // backend's single-threaded dispatcher. On-demand fetches use the default dispatcher.
    private val prefetchDispatcher: Dispatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    // Tracks in-flight workflows-list fetches per appUserID so concurrent callers for the same user
    // join the running request, while a different user starts its own.
    private val callbackLock = Any()
    private val pendingCompletionCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()

    fun close() {
        scope.cancel()
        prefetchDispatcher.close()
    }

    /**
     * Fetches and resolves a single workflow using stale-while-revalidate, mirroring
     * [com.revenuecat.purchases.common.offerings.OfferingsManager]'s offerings serving:
     * a fresh cache hit is served directly; a stale-but-present hit is served immediately and the
     * cache is refreshed in the background (no callbacks, failures logged); a miss fetches from the
     * backend and delivers the outcome through [onSuccess]/[onError].
     *
     * @param staleWhileRevalidate when true (the default, used by the on-demand render path), a
     * stale-but-present cached workflow is served immediately with a background refresh. When false
     * (the prefetch path), a stale workflow blocks on a full refetch instead — so prefetch keeps
     * forcing a fresh fetch and persisting its envelope rather than serving a stale value.
     * @param persistEnvelopeOnResolve when true, also persists the raw detail envelope to disk after
     * a successful resolve, so it survives an app restart and can be re-resolved while the backend is
     * down (see [getWorkflowsList]'s recovery path). Only the prefetch path sets this: prefetched
     * workflows are the curated, bounded set the backend marked as mattering, so persisting all of
     * them is safe. On-demand fetches leave it false to avoid unbounded disk growth (a user can open
     * many distinct paywalls in a session), so persisting those behind an LRU cap is a planned
     * follow-up rather than part of this path.
     */
    @Suppress("LongParameterList")
    fun getWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
        callbackDispatcher: Dispatcher? = null,
        persistEnvelopeOnResolve: Boolean = false,
        staleWhileRevalidate: Boolean = true,
    ) {
        val cached = workflowsCache.cachedWorkflow(workflowId)
        when {
            cached != null && !workflowsCache.isWorkflowCacheStale(workflowId, appInBackground) -> {
                onSuccess(cached)
            }
            cached != null && staleWhileRevalidate -> {
                // Serve the stale value immediately, then refresh the cache in the background.
                // The caller already has a usable value, so the refresh delivers no callbacks: a
                // success updates the cache, and a failure goes through the same disk fallback as any
                // other fetch (see fetchAndCacheWorkflow) — on a backend-down error with a persisted
                // envelope it re-pins from disk and re-stamps fresh, matching OfferingsManager.
                // Known gap vs offerings: offerings persists every fetch, so its disk copy is always
                // the latest; the detail path persists prefetched workflows only, so a background
                // refresh can re-pin an older prefetched envelope over a newer on-demand value and
                // suppress the retry for a TTL. Bounded and self-healing; the on-demand envelope
                // persistence + LRU follow-up closes it. Concurrent stale callers can each fire a
                // refresh; this is not deduplicated, matching the offerings stale path.
                onSuccess(cached)
                fetchAndCacheWorkflow(
                    appUserID = appUserID,
                    workflowId = workflowId,
                    appInBackground = appInBackground,
                    callbackDispatcher = callbackDispatcher,
                    persistEnvelopeOnResolve = persistEnvelopeOnResolve,
                    onSuccess = {},
                    onError = { error ->
                        errorLog {
                            "Background workflow refresh failed for $workflowId: " +
                                error.underlyingErrorMessage
                        }
                    },
                )
            }
            else -> {
                // Miss, or stale with SWR disabled (the prefetch path): block on the fetch.
                fetchAndCacheWorkflow(
                    appUserID = appUserID,
                    workflowId = workflowId,
                    appInBackground = appInBackground,
                    callbackDispatcher = callbackDispatcher,
                    persistEnvelopeOnResolve = persistEnvelopeOnResolve,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            }
        }
    }

    @Suppress("LongParameterList")
    private fun fetchAndCacheWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        callbackDispatcher: Dispatcher?,
        persistEnvelopeOnResolve: Boolean,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val onSuccessHandler: (WorkflowDetailResponse) -> Unit = { response ->
            scope.launch {
                // resolve() can fail in several ways (missing inline data, CDN fetch/parse failures,
                // signature verification), so surface any of them through onError. Cancellation is not
                // a failure: let it propagate so the coroutine unwinds normally.
                val result = try {
                    workflowDetailResolver.resolve(response)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    onError(e.toPurchasesError())
                    return@launch
                }
                workflowsCache.cacheWorkflow(workflowId, result)
                if (persistEnvelopeOnResolve) {
                    runCatching { workflowsCache.cacheWorkflowDetailEnvelope(workflowId, response) }
                        .onFailure { errorLog(it) { "Failed to persist workflow detail envelope for $workflowId" } }
                }
                scope.launch {
                    runCatching { workflowAssetPreDownloader.preDownloadWorkflowAssets(result.workflow) }
                        .onFailure { errorLog(it) { "Failed to pre-download workflow assets" } }
                }
                onSuccess(result)
            }
        }
        val onErrorWithFallback: (PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit =
            { error, behavior ->
                if (behavior == GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK) {
                    // 4xx: the server intentionally changed/removed this workflow, so don't serve the
                    // saved copy. Invalidate the in-memory entry so the next call retries rather than
                    // serving a still-cached value — mirrors the list's invalidateWorkflowsListTimestamp
                    // and OfferingsManager's forceCacheStale on 4xx.
                    workflowsCache.invalidateWorkflowTimestamp(workflowId)
                    onError(error)
                } else {
                    // Transport error / 5xx / malformed body: the backend is unavailable, not refusing.
                    // Recover from the persisted envelope if we have one, matching OfferingsManager's
                    // disk fallback. Applies to every caller, including the SWR background refresh.
                    scope.launch { resolveDiskFallback(workflowId, error, onSuccess, onError) }
                }
            }
        if (callbackDispatcher != null) {
            backend.getWorkflow(
                appUserID = appUserID,
                workflowId = workflowId,
                appInBackground = appInBackground,
                callbackDispatcher = callbackDispatcher,
                onSuccess = onSuccessHandler,
                onError = onErrorWithFallback,
            )
        } else {
            backend.getWorkflow(
                appUserID = appUserID,
                workflowId = workflowId,
                appInBackground = appInBackground,
                onSuccess = onSuccessHandler,
                onError = onErrorWithFallback,
            )
        }
    }

    private enum class ListFetchAction { COMPLETE_NOW, STALE_WHILE_REVALIDATE, BLOCKING_FETCH }

    /**
     * Attempts to re-resolve the persisted disk envelope for [workflowId] after a
     * fallback-eligible backend error. If no envelope is present, or if re-resolution fails,
     * the in-memory entry is invalidated and the original [networkError] is forwarded through
     * [onError] so the caller is never left without a response and the next call retries instead of
     * serving a stale value — mirroring how the list and offerings invalidate when a fallback
     * yields nothing fresh. On success the result is cached in memory (which re-stamps it fresh) and
     * delivered via [onSuccess].
     */
    private suspend fun resolveDiskFallback(
        workflowId: String,
        networkError: PurchasesError,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        val envelope = workflowsCache.cachedWorkflowDetailEnvelopeFromDisk(workflowId)
        if (envelope == null) {
            workflowsCache.invalidateWorkflowTimestamp(workflowId)
            onError(networkError)
            return
        }
        val result = try {
            workflowDetailResolver.resolve(envelope)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to re-resolve disk envelope for $workflowId during fallback" }
            workflowsCache.invalidateWorkflowTimestamp(workflowId)
            onError(networkError)
            return
        }
        workflowsCache.cacheWorkflow(workflowId, result)
        onSuccess(result)
    }

    /**
     * Fetches the workflows list, then prefetches all entries marked `prefetch = true`.
     *
     * [forceRefresh] fetches a fresh list even when the cached one is still within its TTL. It is set
     * after a fresh offerings network response so the offeringId → workflowId map realigns with the
     * offerings the caller just received, since the workflows list otherwise tracks only a time TTL.
     *
     * Offerings delivery is gated on [onComplete] (see
     * [com.revenuecat.purchases.common.offerings.OfferingsManager]): it is invoked once the whole
     * sequence settles — list fetched, every prefetch finished or failed — so it must always fire
     * exactly once, otherwise offerings would never be delivered to the caller.
     *
     * When the cached list is stale but present and the call is not forced, it is served
     * stale-while-revalidate: [onComplete] fires immediately with the cached offeringId → workflowId
     * map and the list is refreshed in the background, mirroring
     * [com.revenuecat.purchases.common.offerings.OfferingsManager.vendCachedOfferingsAndMaybeRefresh].
     * A forced refresh or a cold miss still blocks until the fetch settles.
     *
     * Concurrent callers for the same [appUserID] while a request is in-flight are deduplicated: the
     * second call queues its [onComplete] and returns without a new network request, then all pending
     * callbacks for that user fire together when the in-flight sequence finishes. A call for a
     * different user starts its own fetch rather than joining the in-flight one. Note this join
     * ignores [forceRefresh]: an in-flight batch is not interrupted, so a forced refresh that arrives
     * while a batch is running joins it instead of starting a new fetch. That window self-heals on the
     * next fetch.
     */
    fun getWorkflowsList(
        appUserID: String,
        appInBackground: Boolean,
        forceRefresh: Boolean = false,
        onComplete: () -> Unit = {},
    ) {
        // Decide under the lock, act outside it so onComplete never fires while holding it.
        val action = synchronized(callbackLock) {
            val inFlight = pendingCompletionCallbacks[appUserID]
            if (inFlight != null) {
                // Already fetching for this user: queue onto it. Joining ignores cache freshness and
                // forceRefresh on purpose — the list response stamps the cache fresh mid-sequence, before
                // its prefetch finishes, and an in-flight batch is never interrupted.
                inFlight.add(onComplete)
                return
            }
            val stale = workflowsCache.isWorkflowsListCacheStale(appInBackground)
            when {
                // Fresh, nothing in flight: complete now.
                !forceRefresh && !stale -> {
                    pendingCompletionCallbacks[appUserID] = mutableListOf(onComplete)
                    ListFetchAction.COMPLETE_NOW
                }
                // Stale-but-present, not forced: serve the cached map now and refresh in the background.
                // Register an EMPTY batch so a concurrent caller joins this refresh instead of starting
                // another (the list keeps the dedup offerings doesn't need, because the refresh fires a
                // prefetch loop). onComplete is fired outside the lock, so it isn't in the batch and won't
                // be double-fired when the refresh settles.
                !forceRefresh && workflowsCache.hasCachedWorkflowsList() -> {
                    pendingCompletionCallbacks[appUserID] = mutableListOf()
                    ListFetchAction.STALE_WHILE_REVALIDATE
                }
                // Forced, or cold miss (no cached list): fetch and block.
                else -> {
                    pendingCompletionCallbacks[appUserID] = mutableListOf(onComplete)
                    ListFetchAction.BLOCKING_FETCH
                }
            }
        }

        when (action) {
            // Fresh cache and nothing in flight: complete now.
            ListFetchAction.COMPLETE_NOW -> completePendingCallbacks(appUserID)
            // Vend the cached map immediately, then refresh in the background, mirroring
            // OfferingsManager.vendCachedOfferingsAndMaybeRefresh. The background fetch's onSuccess/onError
            // fires any callers that joined the batch.
            ListFetchAction.STALE_WHILE_REVALIDATE -> {
                try {
                    onComplete()
                } finally {
                    // The refresh must start even if onComplete throws (it can run developer code
                    // synchronously): the batch registered above is only ever settled by the fetch,
                    // so skipping it would strand every later caller for this user on a dead batch.
                    fetchWorkflowsList(appUserID, appInBackground, forceRefresh = false)
                }
            }
            // Miss or forced: the request's onSuccess/onError fires the queued callbacks when it settles.
            ListFetchAction.BLOCKING_FETCH -> fetchWorkflowsList(appUserID, appInBackground, forceRefresh)
        }
    }

    /**
     * Fetches the workflows list from the backend, then prefetches all `prefetch = true` entries and
     * settles pending callbacks. On a fallback-eligible failure (transport/5xx/malformed) it restores
     * from disk; on a 4xx it invalidates the timestamp and settles without restoring. Shared by the
     * blocking and stale-while-revalidate paths of [getWorkflowsList].
     *
     * [forceRefresh] only controls whether the in-memory detail caches are cleared on a *successful*
     * fetch (inside [onSuccess], not before the network call, so a failed fetch leaves cached details
     * intact). A forced refresh then re-fetches every detail from the backend rather than serving
     * still-fresh cached values.
     */
    private fun fetchWorkflowsList(appUserID: String, appInBackground: Boolean, forceRefresh: Boolean) {
        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            type = "paywall",
            onSuccess = { response ->
                // Drop workflows without an offeringId: they can't be reached via
                // workflowIdForOfferingId, so caching or prefetching them is wasted work.
                val filtered = response.onlyWorkflowsWithOfferingId()
                // Clear detail caches after a successful fetch so the prefetch loop below is a
                // guaranteed cache miss and always populates fresh data. Cleared here rather than
                // before the network call so a failed fetch leaves in-memory details intact.
                if (forceRefresh) workflowsCache.clearWorkflowDetailCaches()
                workflowsCache.cacheWorkflowsList(filtered, buildOfferingIdMap(filtered.workflows))

                val prefetchWorkflows = filtered.workflows.filter { it.prefetch }
                scope.launch {
                    // Wait for all prefetches before completing so onComplete fires once the whole
                    // sequence settles. A failed prefetch is logged and does not fail the others.
                    // Concurrency is bounded downstream, not here: detail fetches by prefetchDispatcher's
                    // thread pool, CDN downloads by the workflows FileRepository's limited scope.
                    coroutineScope {
                        prefetchWorkflows.forEach { summary ->
                            launch {
                                prefetchWorkflow(appUserID, summary.id, appInBackground)
                            }
                        }
                    }
                    completePendingCallbacks(appUserID)
                }
            },
            onError = { error, behavior ->
                errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                if (behavior == GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK) {
                    // A 4xx means the server intentionally changed/removed these workflows. Don't
                    // resurrect a stale list from disk; just settle the callbacks so offerings
                    // delivery isn't stranded. Invalidate the timestamp so the next non-forced call
                    // retries rather than serving a still-fresh in-memory list — mirrors
                    // OfferingsManager.handleErrorFetchingOfferings calling forceCacheStale().
                    workflowsCache.invalidateWorkflowsListTimestamp()
                    completePendingCallbacks(appUserID)
                } else {
                    restoreWorkflowsListFromDisk(appUserID)
                }
            },
        )
    }

    /**
     * Restores the workflows list and persisted detail envelopes from disk after a fallback-eligible
     * fetch failure (transport error, 5xx, or malformed body), then settles pending callbacks. The
     * in-memory cache is rewritten from disk without re-persisting it — disk already holds this payload.
     * [completePendingCallbacks] always fires exactly once.
     */
    private fun restoreWorkflowsListFromDisk(appUserID: String) {
        val restoredFromDisk = workflowsCache.cachedWorkflowsListResponseFromDisk()?.let { response ->
            val filtered = response.onlyWorkflowsWithOfferingId()
            workflowsCache.cacheWorkflowsListInMemory(filtered, buildOfferingIdMap(filtered.workflows))
        } != null
        // Mirror OfferingsManager.handleErrorFetchingOfferings: when there is no disk cache to fall
        // back on, force the list stale so the next call retries. When a disk restore did succeed,
        // cacheWorkflowsListInMemory already stamped a fresh timestamp, so we leave it alone — same as
        // offerings leaving the cache fresh after createAndCacheOfferings runs on the disk-fallback path.
        if (!restoredFromDisk) {
            workflowsCache.invalidateWorkflowsListTimestamp()
        }
        val envelopes = workflowsCache.cachedWorkflowDetailEnvelopesFromDisk().orEmpty()
        if (envelopes.isEmpty()) {
            completePendingCallbacks(appUserID)
        } else {
            scope.launch {
                // Re-resolve persisted envelopes into the in-memory cache, mirroring the success-path
                // prefetch loop. A failed re-resolve is logged and does not fail its siblings.
                // completePendingCallbacks still fires exactly once.
                coroutineScope {
                    envelopes.forEach { (workflowId, envelope) ->
                        launch { restoreWorkflowFromEnvelope(workflowId, envelope) }
                    }
                }
                completePendingCallbacks(appUserID)
            }
        }
    }

    /**
     * Suspends until [getWorkflow] for [workflowId] resolves. Prefetch is best-effort, so a failure
     * is logged and the coroutine resumes normally instead of throwing, keeping one failed prefetch
     * from cancelling its siblings in the surrounding [coroutineScope].
     */
    private suspend fun prefetchWorkflow(appUserID: String, workflowId: String, appInBackground: Boolean) {
        suspendCancellableCoroutine { continuation ->
            getWorkflow(
                appUserID = appUserID,
                workflowId = workflowId,
                appInBackground = appInBackground,
                callbackDispatcher = prefetchDispatcher,
                persistEnvelopeOnResolve = true,
                staleWhileRevalidate = false,
                onSuccess = { continuation.safeResume(Unit) },
                onError = { error ->
                    errorLog { "Failed to prefetch workflow $workflowId: ${error.underlyingErrorMessage}" }
                    continuation.safeResume(Unit)
                },
            )
        }
    }

    /**
     * Re-resolves a persisted [envelope] into the in-memory cache during backend-down recovery. For
     * USE_CDN this avoids a backend call: when the CDN file is still cached locally (it was
     * pre-downloaded during the original prefetch) re-resolution needs no network. If that file was
     * evicted, resolution may need the CDN and can fail while the backend is down; the failure is
     * logged and swallowed so one bad envelope doesn't fail its siblings in the surrounding [coroutineScope].
     */
    private suspend fun restoreWorkflowFromEnvelope(workflowId: String, envelope: WorkflowDetailResponse) {
        val result = try {
            workflowDetailResolver.resolve(envelope)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to restore workflow $workflowId from disk cache" }
            return
        }
        workflowsCache.cacheWorkflow(workflowId, result)
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        workflowsCache.workflowIdForOfferingId(offeringId)

    private fun completePendingCallbacks(appUserID: String) {
        val callbacks = synchronized(callbackLock) {
            pendingCompletionCallbacks.remove(appUserID).orEmpty()
        }
        callbacks.forEach { it() }
    }

    private fun WorkflowsListResponse.onlyWorkflowsWithOfferingId(): WorkflowsListResponse =
        copy(workflows = workflows.filter { it.offeringId != null })

    private fun buildOfferingIdMap(workflows: List<WorkflowSummary>): Map<String, String> {
        val pairs = workflows.mapNotNull { summary -> summary.offeringId?.let { it to summary.id } }
        return pairs
            .groupBy { it.first }
            .also { grouped ->
                grouped.filter { it.value.size > 1 }.keys.forEach { duplicateOfferingId ->
                    warnLog { "Duplicate offeringId in workflows response: $duplicateOfferingId" }
                }
            }
            .mapValues { it.value.last().second }
    }
}
