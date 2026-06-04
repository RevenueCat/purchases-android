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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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

    @Suppress("LongParameterList")
    fun getWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
        callbackDispatcher: Dispatcher? = null,
        persistEnvelopeOnResolve: Boolean = false,
    ) {
        val cached = workflowsCache.cachedWorkflow(workflowId)
        if (cached != null && !workflowsCache.isWorkflowCacheStale(workflowId, appInBackground)) {
            onSuccess(cached)
            return
        }
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
        if (callbackDispatcher != null) {
            backend.getWorkflow(
                appUserID = appUserID,
                workflowId = workflowId,
                appInBackground = appInBackground,
                callbackDispatcher = callbackDispatcher,
                onSuccess = onSuccessHandler,
                onError = onError,
            )
        } else {
            backend.getWorkflow(
                appUserID = appUserID,
                workflowId = workflowId,
                appInBackground = appInBackground,
                onSuccess = onSuccessHandler,
                onError = onError,
            )
        }
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
        // Decide under the lock, act outside it so onComplete never fires while holding it.
        val startFetch = synchronized(callbackLock) {
            val inFlight = pendingCompletionCallbacks[appUserID]
            if (inFlight != null) {
                // Already fetching for this user: queue onto it. Joining ignores cache freshness on
                // purpose — the list response stamps the cache fresh mid-sequence, before its prefetch
                // finishes, so a freshness check would let this caller complete early.
                inFlight.add(onComplete)
                return
            }
            // Nothing in flight: open a batch and fetch only if the cached list is stale.
            pendingCompletionCallbacks[appUserID] = mutableListOf(onComplete)
            workflowsCache.isWorkflowsListCacheStale(appInBackground)
        }

        // Fresh cache and nothing in flight: complete now. Otherwise the request's onSuccess/onError
        // fire the queued callbacks when it settles.
        if (!startFetch) {
            completePendingCallbacks(appUserID)
        } else {
            backend.getWorkflows(
                appUserID = appUserID,
                appInBackground = appInBackground,
                type = "paywall",
                onSuccess = { response ->
                    // Drop workflows without an offeringId: they can't be reached via
                    // workflowIdForOfferingId, so caching or prefetching them is wasted work.
                    val filtered = response.onlyWorkflowsWithOfferingId()
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
                onError = { error ->
                    errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                    // Restore the in-memory cache from disk without rewriting it — disk already holds
                    // this payload.
                    workflowsCache.cachedWorkflowsListResponseFromDisk()?.let { response ->
                        val filtered = response.onlyWorkflowsWithOfferingId()
                        workflowsCache.cacheWorkflowsListInMemory(filtered, buildOfferingIdMap(filtered.workflows))
                    }
                    val envelopes = workflowsCache.cachedWorkflowDetailEnvelopesFromDisk().orEmpty()
                    if (envelopes.isEmpty()) {
                        completePendingCallbacks(appUserID)
                    } else {
                        scope.launch {
                            // Re-resolve persisted envelopes into the in-memory cache, mirroring the
                            // success-path prefetch loop. A failed re-resolve is logged and does not
                            // fail its siblings. completePendingCallbacks still fires exactly once.
                            coroutineScope {
                                envelopes.forEach { (workflowId, envelope) ->
                                    launch { restoreWorkflowFromEnvelope(workflowId, envelope) }
                                }
                            }
                            completePendingCallbacks(appUserID)
                        }
                    }
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
                persistEnvelopeOnResolve = true,
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
     * USE_CDN this is a local file read (the CDN file is already cached). A failure is logged and
     * swallowed so one bad envelope doesn't fail its siblings in the surrounding [coroutineScope].
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
