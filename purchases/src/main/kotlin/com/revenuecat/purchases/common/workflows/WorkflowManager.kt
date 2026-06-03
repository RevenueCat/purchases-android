@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val workflowsCache: WorkflowsCache,
    // Dispatcher the prefetch detail fetches run on, so they fan out instead of serializing on the
    // backend's default single-threaded dispatcher. Only the prefetch path uses it; on-demand fetches
    // stay on the default dispatcher.
    private val prefetchDispatcher: Dispatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private companion object {
        // Caps how many workflows are prefetched at once, so a long workflows list does not fan out
        // into an unbounded number of concurrent CDN/asset downloads.
        private const val MAX_CONCURRENT_PREFETCHES = 4
    }

    // Guards pendingCompletionCallbacks. A key is present while a workflows-list fetch (plus its
    // prefetch) is in flight for that appUserID, so concurrent callers join only their own user's
    // request — a different user starts its own fetch instead of receiving the wrong user's list.
    private val callbackLock = Any()
    private val pendingCompletionCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()

    // A Semaphore rather than a limitedParallelism dispatcher (as RemoteConfig's TopicFetcher uses)
    // because a single prefetch is suspended almost the whole time: the detail fetch is a callback
    // suspending on backend.getWorkflow, and the CDN/asset downloads run on the FileRepository/Coil
    // scopes, not this one. A limitedParallelism dispatcher only caps actively-running coroutines, so
    // a suspended prefetch would free its slot and let the next one start — capping nothing. The
    // Semaphore holds its permit across those suspension points, bounding the in-flight pipeline end
    // to end. The detail HTTP calls themselves run on [prefetchDispatcher] so they fan out rather than
    // serialize.
    private val prefetchSemaphore = Semaphore(MAX_CONCURRENT_PREFETCHES)

    fun close() {
        scope.cancel()
        prefetchDispatcher.close()
    }

    @Suppress("LongParameterList")
    fun getWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
        // Set by the prefetch path to fan the detail fetches out across [prefetchDispatcher]. Left null
        // for on-demand fetches, which run on the backend's default dispatcher.
        callbackDispatcher: Dispatcher? = null,
    ) {
        val cached = workflowsCache.cachedWorkflow(workflowId)
        if (cached != null && !workflowsCache.isWorkflowCacheStale(workflowId, appInBackground)) {
            onSuccess(cached)
            return
        }
        backend.getWorkflow(
            appUserID = appUserID,
            workflowId = workflowId,
            appInBackground = appInBackground,
            callbackDispatcher = callbackDispatcher,
            onSuccess = { response ->
                scope.launch {
                    // resolve() can throw a range of exceptions (IllegalStateException, IOException,
                    // SignatureVerificationException, and SerializationException from parsing CDN json).
                    // CancellationException is caught here intentionally: rethrowing it would skip the
                    // callback and deadlock the prefetch counter in getWorkflowsList. The scope is already
                    // canceled so further coroutine work in this scope will not execute regardless.
                    val result = try {
                        workflowDetailResolver.resolve(response)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        onError(e.toPurchasesError())
                        return@launch
                    }
                    workflowsCache.cacheWorkflow(workflowId, result)
                    scope.launch {
                        runCatching { workflowAssetPreDownloader.preDownloadWorkflowAssets(result.workflow) }
                            .onFailure { errorLog(it) { "Failed to pre-download workflow assets" } }
                    }
                    onSuccess(result)
                }
            },
            onError = onError,
        )
    }

    /**
     * Fetches the workflows list, then prefetches all entries marked `prefetch = true`.
     *
     * Offerings delivery is gated on [onComplete] (see
     * [com.revenuecat.purchases.common.offerings.OfferingsManager]): it is invoked once the whole
     * sequence settles — list fetched, every prefetch finished or failed — so it must always fire
     * exactly once, otherwise offerings would never be delivered to the caller.
     *
     * Concurrent callers for the same [appUserID] while a request is in-flight are deduplicated:
     * the second call queues its [onComplete] and returns without a new network request, then all
     * pending callbacks for that user fire together when the in-flight sequence finishes. A call
     * for a different user starts its own fetch rather than joining the in-flight one.
     */
    fun getWorkflowsList(appUserID: String, appInBackground: Boolean, onComplete: () -> Unit = {}) {
        // Decide under the lock and act outside it, so onComplete never fires while the lock is held.
        // The decision is keyed by appUserID: a call only joins an in-flight fetch for the *same* user,
        // so an identity switch starts its own fetch instead of inheriting the previous one.
        val startFetch = synchronized(callbackLock) {
            val inFlight = pendingCompletionCallbacks[appUserID]
            if (inFlight != null) {
                // A fetch for this user is already running. Queue our callback onto it and return; it
                // fires when that in-flight sequence finishes. Joining is independent of cache freshness
                // on purpose: the list response stamps the cache fresh partway through the sequence
                // (before its prefetch finishes), so a freshness-only check would let this caller
                // complete early, before the sequence it joined is done.
                inFlight.add(onComplete)
                return
            }
            // Nothing in flight: register this callback as the head of a new batch, then fetch only if
            // the cached list is stale. A fresh cache means there's nothing to wait for, so we complete
            // the batch immediately below instead of starting a request.
            pendingCompletionCallbacks[appUserID] = mutableListOf(onComplete)
            workflowsCache.isWorkflowsListCacheStale(appInBackground)
        }

        // Fresh cache and nothing in flight: nothing to wait for, so complete the batch right away.
        // Otherwise start the request; its onSuccess/onError fire the queued callbacks when it settles.
        if (!startFetch) {
            completePendingCallbacks(appUserID)
        } else {
            backend.getWorkflows(
                appUserID = appUserID,
                appInBackground = appInBackground,
                type = "paywall",
                onSuccess = { response ->
                    // Drop workflows without an offeringId up front: they can't be reached via
                    // workflowIdForOfferingId, so caching or prefetching them would be wasted work. The
                    // backend should only return offering-tied workflows here; this is a guard on top.
                    val filtered = response.onlyWorkflowsWithOfferingId()
                    workflowsCache.cacheWorkflowsList(filtered, buildOfferingIdMap(filtered.workflows))

                    val prefetchWorkflows = filtered.workflows.filter { it.prefetch }
                    scope.launch {
                        // Prefetch workflows with bounded concurrency (at most MAX_CONCURRENT_PREFETCHES
                        // at a time) and wait for all of them before completing, so onComplete fires only
                        // once the whole sequence settles (empty list completes right away). A failed
                        // prefetch is logged and does not fail the others.
                        coroutineScope {
                            prefetchWorkflows.forEach { summary ->
                                launch {
                                    prefetchSemaphore.withPermit {
                                        prefetchWorkflow(appUserID, summary.id, appInBackground)
                                    }
                                }
                            }
                        }
                        completePendingCallbacks(appUserID)
                    }
                },
                onError = { error ->
                    errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                    // Restore the in-memory cache from the disk copy without rewriting disk: the disk
                    // already holds this payload, so a write would just persist the same bytes again.
                    workflowsCache.cachedWorkflowsListResponseFromDisk()?.let { response ->
                        val filtered = response.onlyWorkflowsWithOfferingId()
                        workflowsCache.cacheWorkflowsListInMemory(filtered, buildOfferingIdMap(filtered.workflows))
                    }
                    completePendingCallbacks(appUserID)
                },
            )
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
                onSuccess = { continuation.safeResume(Unit) },
                onError = { error ->
                    errorLog { "Failed to prefetch workflow $workflowId: ${error.underlyingErrorMessage}" }
                    continuation.safeResume(Unit)
                },
            )
        }
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        workflowsCache.workflowIdForOfferingId(offeringId)

    /**
     * Marks the workflows list stale so the next [getWorkflowsList] refetches it. Called when
     * offerings are refetched off their normal TTL (forced/locale change) to keep the workflow map
     * aligned with the offerings the caller just received.
     */
    fun forceWorkflowsListCacheStale() {
        workflowsCache.forceWorkflowsListCacheStale()
    }

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
