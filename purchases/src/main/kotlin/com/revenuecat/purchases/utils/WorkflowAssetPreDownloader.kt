@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.utils

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader

internal class WorkflowAssetPreDownloader(
    private val paywallComponentsImagePreDownloader: PaywallComponentsImagePreDownloader,
    private val offeringFontPreDownloader: OfferingFontPreDownloader,
) {

    private val preDownloadedWorkflowIds = mutableSetOf<String>()

    fun preDownloadWorkflowAssets(workflow: PublishedWorkflow) {
        synchronized(preDownloadedWorkflowIds) {
            if (!preDownloadedWorkflowIds.add(workflow.id)) return
        }

        debugLog { "Pre-downloading workflow assets for workflow '${workflow.id}'" }

        workflow.screens.values.forEach { screen ->
            paywallComponentsImagePreDownloader.preDownloadImages(screen.componentsConfig.base)
        }
        // ui_config no longer lives on PublishedWorkflow — it has its own independent read path
        // (UiConfigProvider / PurchasesOrchestrator.getUiConfig). Font pre-download needs to be re-wired to
        // pull fonts from that path; left as a no-op pending that wiring.
        offeringFontPreDownloader.preDownloadFontsIfNeeded(emptyList())
    }
}
