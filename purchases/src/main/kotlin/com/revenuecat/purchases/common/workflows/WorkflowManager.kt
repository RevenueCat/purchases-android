@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

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
     * [onComplete] fires only after the list fetch AND all prefetch CDN fetches finish
     * (success or failure), making it safe to call [workflowIdForOfferingId] in [onComplete].
     *
     * Concurrent callers for the same [appUserID] while a request is in-flight are deduplicated:
     * the second call queues its [onComplete] and returns without a new network request. All
     * pending callbacks for that user complete together when the in-flight work finishes.
     * A call for a different user starts its own fetch rather than joining the in-flight one.
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
                if (prefetchWorkflows.isEmpty()) {
                    completePendingCallbacks(appUserID)
                } else {
                    val remaining = AtomicInteger(prefetchWorkflows.size)
                    prefetchWorkflows.forEach { summary ->
                        getWorkflow(
                            appUserID = appUserID,
                            workflowId = summary.id,
                            appInBackground = appInBackground,
                            onSuccess = {
                                if (remaining.decrementAndGet() == 0) completePendingCallbacks(appUserID)
                            },
                            onError = { error ->
                                errorLog {
                                    "Failed to prefetch workflow ${summary.id}: ${error.underlyingErrorMessage}"
                                }
                                if (remaining.decrementAndGet() == 0) completePendingCallbacks(appUserID)
                            },
                        )
                    }
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

    fun workflowIdForOfferingId(offeringId: String): String? =
        workflowsCache.workflowIdForOfferingId(offeringId)

    private enum class FetchDecision { FETCH, JOIN, COMPLETE_NOW }

    /**
     * Decides how a [getWorkflowsList] call should proceed, queueing [onComplete] when it must wait.
     *
     * Returns [FetchDecision.JOIN] when a fetch for the same [appUserID] is already in flight: the
     * call joins it regardless of list-cache freshness, because the list response refreshes the
     * cache before its prefetch details finish — completing on a fresh cache would fire [onComplete]
     * before the prefetched workflows land. A fetch in flight for a *different* user is not joined,
     * so an identity switch starts its own fetch instead of inheriting the previous user's list.
     * [FetchDecision.COMPLETE_NOW] means there is no in-flight work for this user to wait for.
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
