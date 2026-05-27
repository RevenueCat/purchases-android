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
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.warnLog
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException

internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val deviceCache: DeviceCache,
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val workflowsListCachedObject = InMemoryCachedObject<WorkflowsListResponse>(
        dateProvider = dateProvider,
    )

    @Volatile
    private var offeringIdToWorkflowIdMap: Map<String, String> = emptyMap()

    @Volatile
    private var isFetchingWorkflowsList = false

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
                    try {
                        val result = workflowDetailResolver.resolve(response)
                        scope.launch {
                            runCatching { workflowAssetPreDownloader.preDownloadWorkflowAssets(result.workflow) }
                                .onFailure { errorLog(it) { "Failed to pre-download workflow assets" } }
                        }
                        onSuccess(result)
                    } catch (e: IllegalStateException) {
                        onError(e.toPurchasesError())
                    } catch (e: IOException) {
                        onError(e.toPurchasesError())
                    } catch (e: SignatureVerificationException) {
                        onError(e.toPurchasesError())
                    }
                }
            },
            onError = onError,
        )
    }

    fun getWorkflowsList(appUserID: String, appInBackground: Boolean) {
        if (!BuildConfig.USE_WORKFLOWS_ENDPOINT ||
            !workflowsListCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider) ||
            isFetchingWorkflowsList
        ) {
            return
        }
        isFetchingWorkflowsList = true
        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            onSuccess = { response ->
                isFetchingWorkflowsList = false
                workflowsListCachedObject.cacheInstance(response)
                deviceCache.cacheWorkflowsListResponse(
                    JsonTools.json.encodeToString(WorkflowsListResponse.serializer(), response),
                )
                offeringIdToWorkflowIdMap = buildOfferingIdMap(response.workflows)
                response.workflows
                    .filter { it.prefetch }
                    .forEach { summary ->
                        getWorkflow(
                            appUserID = appUserID,
                            workflowId = summary.id,
                            appInBackground = appInBackground,
                            onSuccess = {},
                            onError = { error ->
                                errorLog {
                                    "Failed to prefetch workflow ${summary.id}: ${error.underlyingErrorMessage}"
                                }
                            },
                        )
                    }
            },
            onError = { error ->
                isFetchingWorkflowsList = false
                errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
                deviceCache.getWorkflowsListResponseCache()?.let { cached ->
                    runCatching { WorkflowJsonParser.parseWorkflowsListResponse(cached) }
                        .onSuccess { response ->
                            workflowsListCachedObject.cacheInstance(response)
                            offeringIdToWorkflowIdMap = buildOfferingIdMap(response.workflows)
                        }
                        .onFailure { errorLog(it) { "Failed to restore workflows list from disk cache" } }
                }
            },
        )
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        offeringIdToWorkflowIdMap[offeringId]

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
