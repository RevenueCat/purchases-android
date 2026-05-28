@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CancellationException
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
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val useWorkflowsEndpoint: Boolean = BuildConfig.USE_WORKFLOWS_ENDPOINT,
) {

    private val workflowsListCachedObject = InMemoryCachedObject<WorkflowsListResponse>(
        dateProvider = dateProvider,
    )

    @Volatile
    private var offeringIdToWorkflowIdMap: Map<String, String> = emptyMap()

    // Guards isFetchingWorkflowsList and pendingCompletionCallbacks together.
    private val callbackLock = Any()
    private var isFetchingWorkflowsList = false
    private val pendingCompletionCallbacks = mutableListOf<() -> Unit>()

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
        backend.getWorkflow(
            appUserID = appUserID,
            workflowId = workflowId,
            appInBackground = appInBackground,
            onSuccess = { response ->
                scope.launch {
                    // resolve() can throw a range of exceptions (IllegalStateException, IOException,
                    // SignatureVerificationException, and SerializationException from parsing CDN json).
                    // getWorkflow MUST always invoke exactly one of onSuccess/onError: the prefetch
                    // counter in getWorkflowsList (and the offerings delivery gated on it) deadlocks if
                    // a callback is ever skipped. Catch broadly so no unexpected type breaks that contract.
                    val result = try {
                        workflowDetailResolver.resolve(response)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        onError(e.toPurchasesError())
                        return@launch
                    }
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
     * Concurrent callers while a request is in-flight are deduplicated: the second call queues
     * its [onComplete] and returns without a new network request. All pending callbacks are
     * drained together when the in-flight work finishes.
     */
    fun getWorkflowsList(appUserID: String, appInBackground: Boolean, onComplete: () -> Unit = {}) {
        if (!useWorkflowsEndpoint ||
            !workflowsListCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider)
        ) {
            onComplete()
            return
        }

        synchronized(callbackLock) {
            pendingCompletionCallbacks.add(onComplete)
            if (isFetchingWorkflowsList) return
            isFetchingWorkflowsList = true
        }

        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            type = "paywall",
            onSuccess = { response ->
                workflowsListCachedObject.cacheInstance(response)
                deviceCache.cacheWorkflowsListResponse(
                    JsonTools.json.encodeToString(WorkflowsListResponse.serializer(), response),
                )
                offeringIdToWorkflowIdMap = buildOfferingIdMap(response.workflows)

                val prefetchWorkflows = response.workflows.filter { it.prefetch }
                if (prefetchWorkflows.isEmpty()) {
                    drainCompletionCallbacks()
                } else {
                    val remaining = AtomicInteger(prefetchWorkflows.size)
                    prefetchWorkflows.forEach { summary ->
                        getWorkflow(
                            appUserID = appUserID,
                            workflowId = summary.id,
                            appInBackground = appInBackground,
                            onSuccess = {
                                if (remaining.decrementAndGet() == 0) drainCompletionCallbacks()
                            },
                            onError = { error ->
                                errorLog {
                                    "Failed to prefetch workflow ${summary.id}: ${error.underlyingErrorMessage}"
                                }
                                if (remaining.decrementAndGet() == 0) drainCompletionCallbacks()
                            },
                        )
                    }
                }
            },
            onError = { error ->
                errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                deviceCache.getWorkflowsListResponseCache()?.let { cached ->
                    runCatching { WorkflowJsonParser.parseWorkflowsListResponse(cached) }
                        .onSuccess { response ->
                            workflowsListCachedObject.cacheInstance(response)
                            offeringIdToWorkflowIdMap = buildOfferingIdMap(response.workflows)
                        }
                        .onFailure { errorLog(it) { "Failed to restore workflows list from disk cache" } }
                }
                drainCompletionCallbacks()
            },
        )
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        offeringIdToWorkflowIdMap[offeringId]

    private fun drainCompletionCallbacks() {
        val callbacks = synchronized(callbackLock) {
            isFetchingWorkflowsList = false
            pendingCompletionCallbacks.toList().also { pendingCompletionCallbacks.clear() }
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
