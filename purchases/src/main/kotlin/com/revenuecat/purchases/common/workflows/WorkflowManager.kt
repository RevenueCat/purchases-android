@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.Backend
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

@Suppress("LongParameterList")
internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val workflowsCache: WorkflowsCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val useWorkflowsEndpoint: Boolean = BuildConfig.USE_WORKFLOWS_ENDPOINT,
) {

    // Guards pendingCompletionCallbacks. A key is present while a workflows-list fetch (plus its
    // prefetch) is in flight for that appUserID, so concurrent callers join only their own user's
    // request — a different user starts its own fetch instead of receiving the wrong user's list.
    private val callbackLock = Any()
    private val pendingCompletionCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()

    fun close() {
        scope.cancel()
    }

    fun getWorkflow(
        appUserID: String,
        workflowId: String,
        appInBackground: Boolean,
        onSuccess: (WorkflowDataResult) -> Unit,
        onError: (PurchasesError) -> Unit,
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
        when (resolveWorkflowsListFetch(appUserID, appInBackground, onComplete)) {
            // Callback queued onto in-flight work; it completes when that work finishes.
            FetchDecision.JOIN -> return
            FetchDecision.COMPLETE_NOW -> {
                onComplete()
                return
            }
            FetchDecision.FETCH -> Unit
        }

        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            type = "paywall",
            onSuccess = { response ->
                workflowsCache.cacheWorkflowsList(response, buildOfferingIdMap(response.workflows))

                // A workflow with no offeringId can't be reached via workflowIdForOfferingId, so
                // prefetching its assets would be wasted work. The backend should already only set
                // prefetch = true for workflows tied to an offering; this is a defensive guard on top.
                val prefetchWorkflows = response.workflows.filter { it.prefetch && it.offeringId != null }
                scope.launch {
                    // Prefetch each workflow concurrently and wait for all of them before completing,
                    // so onComplete fires only once the whole sequence settles (empty list completes
                    // right away). A failed prefetch is logged and does not fail the others.
                    coroutineScope {
                        prefetchWorkflows.forEach { summary ->
                            launch { prefetchWorkflow(appUserID, summary.id, appInBackground) }
                        }
                    }
                    completePendingCallbacks(appUserID)
                }
            },
            onError = { error ->
                errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                workflowsCache.cachedWorkflowsListResponseFromDisk()?.let { response ->
                    workflowsCache.cacheWorkflowsList(response, buildOfferingIdMap(response.workflows))
                }
                completePendingCallbacks(appUserID)
            },
        )
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
     * What a [getWorkflowsList] call should do, decided under [callbackLock] by
     * [resolveWorkflowsListFetch] and acted on *outside* the lock so the completion callback is
     * never invoked while holding it.
     */
    private enum class FetchDecision {
        /** No in-flight fetch and the list cache is stale: start a new backend request. */
        FETCH,

        /**
         * A fetch for the same appUserID is already running. This call's completion callback is
         * queued onto it and fires when that in-flight sequence finishes, so the caller just returns
         * without a new request. Joining is independent of cache freshness on purpose: the list
         * response stamps the cache fresh partway through the sequence (before its prefetch
         * finishes), so a freshness-only check would let this caller complete early, before the
         * sequence it joined is done.
         */
        JOIN,

        /**
         * Nothing to wait for, so the completion callback fires right away. Either the workflows
         * endpoint is disabled, or the list cache is fresh and no fetch is in flight.
         */
        COMPLETE_NOW,
    }

    /**
     * Decides how a [getWorkflowsList] call should proceed, queueing [onComplete] when it must wait.
     * See [FetchDecision] for what each outcome means.
     *
     * The decision is keyed by [appUserID]: a call only joins an in-flight fetch for the *same*
     * user, so an identity switch starts its own fetch instead of inheriting the previous one.
     */
    private fun resolveWorkflowsListFetch(
        appUserID: String,
        appInBackground: Boolean,
        onComplete: () -> Unit,
    ): FetchDecision {
        if (!useWorkflowsEndpoint) return FetchDecision.COMPLETE_NOW
        return synchronized(callbackLock) {
            val inFlight = pendingCompletionCallbacks[appUserID]
            when {
                inFlight != null -> {
                    inFlight.add(onComplete)
                    FetchDecision.JOIN
                }
                workflowsCache.isWorkflowsListCacheStale(appInBackground) -> {
                    pendingCompletionCallbacks[appUserID] = mutableListOf(onComplete)
                    FetchDecision.FETCH
                }
                else -> FetchDecision.COMPLETE_NOW
            }
        }
    }

    private fun completePendingCallbacks(appUserID: String) {
        val callbacks = synchronized(callbackLock) {
            pendingCompletionCallbacks.remove(appUserID).orEmpty()
        }
        callbacks.forEach { it() }
    }

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
