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
            paywallComponentsImagePreDownloader.preDownloadImages(
                paywallComponentsConfig = screen.componentsConfig.base,
                localizations = screen.componentsLocalizations,
            )
        }
        offeringFontPreDownloader.preDownloadFontsIfNeeded(workflow.uiConfig.app.fonts.values)
    }
}
