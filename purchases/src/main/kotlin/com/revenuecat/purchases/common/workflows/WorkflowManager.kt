@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.DefaultDateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.caching.InMemoryCachedObject
import com.revenuecat.purchases.common.caching.isCacheStale
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException

internal typealias WorkflowPreWarmer = (appUserID: String, offeringIdentifier: String, appInBackground: Boolean) -> Unit

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
        if (!workflowsListCachedObject.lastUpdatedAt.isCacheStale(appInBackground, dateProvider)) {
            return
        }
        backend.getWorkflows(
            appUserID = appUserID,
            appInBackground = appInBackground,
            onSuccess = { response ->
                workflowsListCachedObject.cacheInstance(response)
                // Write-only: warms disk for future read-back; in-memory cache governs staleness within a session.
                deviceCache.cacheWorkflowsListResponse(
                    JsonTools.json.encodeToString(WorkflowsListResponse.serializer(), response),
                )
                offeringIdToWorkflowIdMap = response.workflows
                    .mapNotNull { summary -> summary.offeringId?.let { it to summary.id } }
                    .toMap()
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
                errorLog { "Failed to fetch workflows list: ${error.underlyingErrorMessage}" }
            },
        )
    }

    fun workflowIdForOfferingId(offeringId: String): String? =
        offeringIdToWorkflowIdMap[offeringId]
}
