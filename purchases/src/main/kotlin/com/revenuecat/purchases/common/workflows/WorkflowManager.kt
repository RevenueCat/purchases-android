@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.safeResume
import com.revenuecat.purchases.common.toPurchasesError
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException

internal class WorkflowManager(
    private val backend: Backend,
    private val workflowDetailResolver: WorkflowDetailResolver,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    fun close() {
        scope.cancel()
    }

    fun fetchWorkflowsForAllOfferings(
        appUserID: String,
        offeringIdentifiers: Collection<String>,
        appInBackground: Boolean,
        onComplete: () -> Unit,
    ) {
        if (offeringIdentifiers.isEmpty()) {
            onComplete()
            return
        }
        scope.launch {
            offeringIdentifiers
                .map { offeringId ->
                    async {
                        suspendCancellableCoroutine<Unit> { continuation ->
                            getWorkflow(
                                appUserID = appUserID,
                                workflowId = offeringId,
                                appInBackground = appInBackground,
                                onSuccess = { continuation.safeResume(Unit) },
                                onError = { continuation.safeResume(Unit) }, // best-effort: resume on error too
                            )
                        }
                    }
                }
                .awaitAll()
            onComplete()
        }
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
}
