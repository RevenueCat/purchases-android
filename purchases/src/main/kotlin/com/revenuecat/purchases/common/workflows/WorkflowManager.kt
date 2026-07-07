@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The consumer-facing entry point for reading workflows. It stays as the seam the SDK calls (so
 * `PurchasesOrchestrator` and the public API are unchanged), but it is now a thin adapter that reads from the
 * `/v1/config` layer through [WorkflowsConfigProvider] instead of calling the backend per workflow.
 *
 * Everything the old backend-backed path owned is gone: there is no per-workflow `Backend.getWorkflow` call, no
 * CDN envelope resolution, no `WorkflowsCache`, no stale-while-revalidate, and no disk-fallback. Freshness comes
 * from the shared config sync ([com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager]); bodies are
 * read — or downloaded on demand and deduped — by that manager.
 *
 * `appUserID` has dropped out of the read entirely: a workflow body is a shared, content-addressed blob, not a
 * per-user document. (Per-user data — A/B `enrolled_variants` — is being designed separately.)
 */
internal class WorkflowManager(
    private val workflowsConfigProvider: WorkflowsConfigProvider,
    private val uiConfigProvider: UiConfigProvider,
    private val workflowAssetPreDownloader: WorkflowAssetPreDownloader,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    fun close() {
        scope.cancel()
    }

    /**
     * Resolves a workflow by workflow id or offering id and delivers it through [onSuccess], or an error through
     * [onError] when it cannot be served. Callbacks fire on [scope]; the consumer boundary
     * (`PurchasesOrchestrator`) normalizes delivery to the main thread, as it always has.
     */
    fun getWorkflow(
        workflowOrOfferingId: String,
        onSuccess: (PublishedWorkflow) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        scope.launch {
            // An offering id resolves to its workflow id up front from persisted config metadata; a workflow id
            // passes through. No backend round-trip, no lazy offering→workflow conversion.
            val workflowId = workflowsConfigProvider.workflowIdForOfferingId(workflowOrOfferingId)
                ?: workflowOrOfferingId
            when (val result = workflowsConfigProvider.getWorkflow(workflowId)) {
                null -> onError(
                    PurchasesError(
                        PurchasesErrorCode.UnknownError,
                        "Workflow '$workflowId' is unavailable from remote config.",
                    ),
                )
                else -> {
                    // Warm the workflow's images and ui_config fonts in parallel with delivery, like the old
                    // fetch path did on resolve; a prewarm failure must never fail the read itself. The fonts
                    // now come from the ui_config topic rather than a uiConfig embedded in the workflow body.
                    scope.launch {
                        runCatching {
                            workflowAssetPreDownloader.preDownloadWorkflowAssets(
                                result,
                                uiConfigProvider.getUiConfig(),
                            )
                        }.onFailure { errorLog(it) { "Failed to pre-download workflow assets" } }
                    }
                    onSuccess(result)
                }
            }
        }
    }

    suspend fun workflowIdForOfferingId(offeringId: String): String? =
        workflowsConfigProvider.workflowIdForOfferingId(offeringId)

    /**
     * Ensures the workflows topic is synced before invoking [onComplete] — the config-topic replacement for the
     * old `getWorkflowsList(onComplete=)` gate `OfferingsManager` used to preserve the guarantee that
     * `getOfferings` doesn't return until workflow data is ready to be queried.
     */
    fun awaitWorkflowsReady(onComplete: () -> Unit) {
        scope.launch {
            workflowsConfigProvider.awaitReady()
            onComplete()
        }
    }
}
