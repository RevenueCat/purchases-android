@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
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
    private val workflowsCache: WorkflowsCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

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
                    try {
                        val result = workflowDetailResolver.resolve(response)
                        workflowsCache.cacheWorkflow(workflowId, result)
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
}
