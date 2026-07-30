@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.utils.PaywallComponentsImagePreDownloader
import kotlinx.coroutines.CancellationException

/**
 * Warms a workflow's assets (its screen images + the `ui_config` fonts) exactly once, so a paywall renders
 * without blocking on the network. It serves both entry points through one dedup set:
 *
 * - **Render path** — [preDownloadWorkflowAssets]: `WorkflowManager.getWorkflow` already has the decoded
 *   workflow and resolved `ui_config`, so it hands them straight in.
 * - **Load path** — [onCurrentWorkflowLoaded]: wired as [WorkflowsConfigProvider]'s load callback, it runs when
 *   the config layer loads the **current offering's** workflow — mirroring the offerings path, which
 *   pre-downloads only the current offering's assets, not every offering's. It dedups by id **before** decoding,
 *   then decodes **transiently** (via the decoder the provider hands it, which never populates the provider's
 *   retained decode cache — the workflows cache stays raw-bytes-only). Because both paths share
 *   [warmedWorkflowIds], a workflow already warmed on render is skipped here before it is ever decoded.
 */
internal class WorkflowAssetPrewarmer(
    private val uiConfigProvider: UiConfigProvider,
    private val paywallComponentsImagePreDownloader: PaywallComponentsImagePreDownloader,
    private val offeringFontPreDownloader: OfferingFontPreDownloader,
) {

    // Ids whose assets have been enqueued, so neither path warms the same workflow twice. Never reset for the
    // SDK's lifetime (assets are content-addressed, so a re-download would be wasted work either way).
    private val warmedWorkflowIds = mutableSetOf<String>()

    /**
     * Enqueues [workflow]'s image + font downloads once; a repeat call for the same workflow id is a no-op.
     * Fonts come from the `ui_config` topic ([uiConfig]), not the workflow body.
     */
    fun preDownloadWorkflowAssets(workflow: PublishedWorkflow, uiConfig: UiConfig) {
        synchronized(warmedWorkflowIds) {
            if (!warmedWorkflowIds.add(workflow.id)) return
        }
        workflow.screens.values.forEach { screen ->
            paywallComponentsImagePreDownloader.preDownloadImages(screen.componentsConfig.base)
        }
        offeringFontPreDownloader.preDownloadFontsIfNeeded(uiConfig.app.fonts.values)
    }

    /** Load-path callback for [WorkflowsConfigProvider]; see the class KDoc. */
    suspend fun onCurrentWorkflowLoaded(
        workflowId: String,
        transientDecode: suspend (String) -> PublishedWorkflow?,
    ) {
        // Dedup before decoding, so a workflow the render path already warmed is never transiently decoded here.
        val alreadyWarmed = synchronized(warmedWorkflowIds) { warmedWorkflowIds.contains(workflowId) }
        if (alreadyWarmed) return

        // Fonts come from the ui_config topic, not the workflow body. A missing ui_config skips this attempt
        // (retried on the next commit, since nothing was marked warmed).
        val uiConfig = loadUiConfig() ?: return

        // A decode that returns null (or throws) leaves the id unmarked, so it is retried on the next commit.
        try {
            transientDecode(workflowId)?.let { preDownloadWorkflowAssets(it, uiConfig) }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to prewarm assets for workflow '$workflowId'." }
        }
    }

    /** Resolves `ui_config`, swallowing non-cancellation failures to null so a prewarm can be retried later. */
    private suspend fun loadUiConfig(): UiConfig? =
        try {
            uiConfigProvider.getUiConfig()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            errorLog(e) { "Failed to load ui_config; skipping workflow asset prewarm." }
            null
        }
}
